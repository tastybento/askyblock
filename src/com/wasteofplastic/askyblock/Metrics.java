package com.wasteofplastic.askyblock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.Lists;

public class Metrics {
    private static String encode(final String text) throws UnsupportedEncodingException {
	return URLEncoder.encode(text, "UTF-8");
    }

    private static void encodeDataPair(final StringBuilder buffer, final String key, final String value) throws UnsupportedEncodingException {
	buffer.append('&').append(encode(key)).append('=').append(encode(value));
    }

    private final YamlConfiguration configuration;
    private final File configurationFile;
    private final boolean debug;
    private final String guid;

    private final Object optOutLock = new Object();

    private final Plugin plugin;

    private volatile BukkitTask task = null;

    public Metrics(final Plugin plugin) throws IOException {
	if (plugin == null) {
	    throw new IllegalArgumentException("Plugin cannot be null");
	}

	this.plugin = plugin;

	configurationFile = getConfigFile();
	configuration = YamlConfiguration.loadConfiguration(configurationFile);

	configuration.addDefault("opt-out", Boolean.valueOf(false));
	configuration.addDefault("guid", UUID.randomUUID().toString());
	configuration.addDefault("debug", Boolean.valueOf(false));

	if (configuration.get("guid", null) == null) {
	    configuration.options().header("http://mcstats.org").copyDefaults(true);
	    configuration.save(configurationFile);
	}

	guid = configuration.getString("guid");
	debug = configuration.getBoolean("debug", false);
    }

    public void disable() throws IOException {
	synchronized (optOutLock) {
	    if (!isOptOut()) {
		configuration.set("opt-out", Boolean.valueOf(true));
		configuration.save(configurationFile);
	    }

	    if (task != null) {
		task.cancel();
		task = null;
	    }
	}
    }

    public void enable() throws IOException {
	synchronized (optOutLock) {
	    if (isOptOut()) {
		configuration.set("opt-out", Boolean.valueOf(false));
		configuration.save(configurationFile);
	    }

	    if (task == null) {
		start();
	    }
	}
    }

    public File getConfigFile() {
	final File pluginsFolder = plugin.getDataFolder().getParentFile();

	return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
    }

    private boolean isMineshafterPresent() {
	try {
	    Class.forName("mineshafter.MineServer");
	    return true;
	} catch (final Exception e) {
	}
	return false;
    }

    public boolean isOptOut() {
	synchronized (optOutLock) {
	    try {
		configuration.load(getConfigFile());
	    } catch (final IOException ex) {
		if (debug) {
		    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
		}
		return true;
	    } catch (final InvalidConfigurationException ex) {
		if (debug) {
		    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
		}
		return true;
	    }
	    return configuration.getBoolean("opt-out", false);
	}
    }

    private void postPlugin(final boolean isPing) throws IOException {
	final PluginDescriptionFile description = plugin.getDescription();
	final String pluginName = description.getName();
	final boolean onlineMode = Bukkit.getServer().getOnlineMode();
	final String pluginVersion = description.getVersion();
	final String serverVersion = Bukkit.getVersion();
	final int playersOnline = getOnlinePlayers().size();

	final StringBuilder data = new StringBuilder();

	data.append(encode("guid")).append('=').append(encode(guid));
	encodeDataPair(data, "version", pluginVersion);
	encodeDataPair(data, "server", serverVersion);
	encodeDataPair(data, "players", Integer.toString(playersOnline));
	encodeDataPair(data, "revision", String.valueOf(6));

	final String osname = System.getProperty("os.name");
	String osarch = System.getProperty("os.arch");
	final String osversion = System.getProperty("os.version");
	final String java_version = System.getProperty("java.version");
	final int coreCount = Runtime.getRuntime().availableProcessors();

	if (osarch.equals("amd64")) {
	    osarch = "x86_64";
	}

	encodeDataPair(data, "osname", osname);
	encodeDataPair(data, "osarch", osarch);
	encodeDataPair(data, "osversion", osversion);
	encodeDataPair(data, "cores", Integer.toString(coreCount));
	encodeDataPair(data, "online-mode", Boolean.toString(onlineMode));
	encodeDataPair(data, "java_version", java_version);

	if (isPing) {
	    encodeDataPair(data, "ping", "true");
	}

	final URL url = new URL("http://mcstats.org" + String.format("/report/%s", new Object[] { encode(pluginName) }));
	URLConnection connection;
	if (isMineshafterPresent()) {
	    connection = url.openConnection(Proxy.NO_PROXY);
	} else {
	    connection = url.openConnection();
	}

	connection.setDoOutput(true);

	final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
	writer.write(data.toString());
	writer.flush();

	final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	final String response = reader.readLine();

	writer.close();
	reader.close();

	if (response == null || response.startsWith("ERR")) {
	    throw new IOException(response);
	}
    }

    public static List<Player> getOnlinePlayers() {
	List<Player> list = Lists.newArrayList();
	for (World world : Bukkit.getWorlds()) {
	    list.addAll(world.getPlayers());
	}
	return Collections.unmodifiableList(list);
    }

    public boolean start() {
	synchronized (optOutLock) {
	    if (isOptOut()) {
		return false;
	    }

	    if (task != null) {
		return true;
	    }

	    task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
		private boolean firstPost = true;

		public void run() {
		    try {
			synchronized (optOutLock) {
			    if (Metrics.this.isOptOut() && task != null) {
				task.cancel();
				task = null;
			    }

			}

			Metrics.this.postPlugin(!firstPost);

			firstPost = false;
		    } catch (final IOException e) {
			if (debug) {
			    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + e.getMessage());
			}
		    }
		}
	    }, 0L, 12000L);

	    return true;
	}
    }
}