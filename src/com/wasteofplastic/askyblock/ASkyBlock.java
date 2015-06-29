/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.askyblock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.NotSetup.Reason;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.askyblock.commands.AdminCmd;
import com.wasteofplastic.askyblock.commands.Challenges;
import com.wasteofplastic.askyblock.commands.IslandCmd;
import com.wasteofplastic.askyblock.generators.ChunkGeneratorWorld;
import com.wasteofplastic.askyblock.listeners.AcidEffect;
import com.wasteofplastic.askyblock.listeners.AcidInventory;
import com.wasteofplastic.askyblock.listeners.ChatListener;
import com.wasteofplastic.askyblock.listeners.HeroChatListener;
import com.wasteofplastic.askyblock.listeners.IslandGuard;
import com.wasteofplastic.askyblock.listeners.IslandGuardNew;
import com.wasteofplastic.askyblock.listeners.JoinLeaveEvents;
import com.wasteofplastic.askyblock.listeners.LavaCheck;
import com.wasteofplastic.askyblock.listeners.NetherPortals;
import com.wasteofplastic.askyblock.listeners.PlayerEvents;
import com.wasteofplastic.askyblock.listeners.WitherEvents;
import com.wasteofplastic.askyblock.listeners.WorldEnter;
import com.wasteofplastic.askyblock.panels.BiomesPanel;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.panels.SchematicsPanel;
import com.wasteofplastic.askyblock.panels.WarpPanel;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Main ASkyBlock class - provides an island minigame in a sea of acid
 */
public class ASkyBlock extends JavaPlugin {
    // This plugin
    private static ASkyBlock plugin;
    // The ASkyBlock world
    private static World islandWorld = null;
    private static World netherWorld = null;
    // Flag indicating if a new islands is in the process of being generated or
    // not
    private boolean newIsland = false;
    // Player folder file
    private File playersFolder;
    // Challenges object
    private Challenges challenges;
    // Localization Strings
    private HashMap<String,Locale> availableLocales = new HashMap<String,Locale>();
    // Players object
    private PlayerCache players;
    // Listeners
    private WarpSigns warpSignsListener;
    private LavaCheck lavaListener;
    // Biome chooser object
    private BiomesPanel biomes;
    // Island grid manager
    private GridManager grid;
    // Island command object
    private IslandCmd islandCmd;
    // Database
    private TinyDB tinyDB;
    // Warp panel
    private WarpPanel warpPanel;
    // V1.8 or later
    private boolean onePointEight;

    private boolean debug = false;

    // Level calc
    private boolean calculatingLevel = false;

    // Update object
    private Update updateCheck = null;

    // Messages object
    private Messages messages;

    // Team chat listener
    private ChatListener chatListener;

    // Schematics panel object
    private SchematicsPanel schematicsPanel;

    /**
     * Returns the World object for the island world named in config.yml.
     * If the world does not exist then it is created.
     * 
     * @return islandWorld - Bukkit World object for the ASkyBlock world
     */
    public static World getIslandWorld() {
	if (islandWorld == null) {
	    // Bukkit.getLogger().info("DEBUG worldName = " +
	    // Settings.worldName);
	    islandWorld = WorldCreator.name(Settings.worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL).generator(new ChunkGeneratorWorld())
		    .createWorld();
	    // Make the nether if it does not exist
	    if (Settings.createNether) {
		getNetherWorld();
	    }
	    // Multiverse configuration
	    if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
		Bukkit.getLogger().info("Trying to register generator with Multiverse ");
		try {
		    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
			    "mv import " + Settings.worldName + " normal -g " + plugin.getName());
		    if (!Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
			    "mv modify set generator " + plugin.getName() + " " + Settings.worldName)) {
			Bukkit.getLogger().severe("Multiverse is out of date! - Upgrade to latest version!");
		    }
		    if (Settings.createNether) {
			if (Settings.newNether) {
			    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
				    "mv import " + Settings.worldName + "_nether nether -g " + plugin.getName());
			    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
				    "mv modify set generator " + plugin.getName() + " " + Settings.worldName + "_nether");
			} else {
			    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "mv import " + Settings.worldName + "_nether nether");
			}
		    }
		} catch (Exception e) {
		    Bukkit.getLogger().severe("Not successfull! Disabling " + plugin.getName() + "!");
		    e.printStackTrace();
		    Bukkit.getServer().getPluginManager().disablePlugin(plugin);
		}
	    }

	}
	// Set world settings
	islandWorld.setWaterAnimalSpawnLimit(Settings.waterAnimalSpawnLimit);
	islandWorld.setMonsterSpawnLimit(Settings.monsterSpawnLimit);
	islandWorld.setAnimalSpawnLimit(Settings.animalSpawnLimit);

	return islandWorld;
    }

    /**
     * @return ASkyBlock object instance
     */
    public static ASkyBlock getPlugin() {
	return plugin;
    }

    /*
     * (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
	try {
	    if (players != null) {
		players.removeAllPlayers();
	    }
	    if (grid != null) {
		grid.saveGrid();
	    }
	    // Save the warps and do not reload the panel
	    if (warpSignsListener != null) {
		warpSignsListener.saveWarpList(false);
	    }
	    if (messages != null) {
		messages.saveMessages();
	    }
	    TopTen.topTenSave();
	    // Close the name database
	    if (tinyDB != null) {
		tinyDB.closeDB();
	    }
	} catch (final Exception e) {
	    getLogger().severe("Something went wrong saving files!");
	    e.printStackTrace();
	}
    }

    /*
     * (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
	// instance of this plugin
	plugin = this;
	// Check server version - check for a class that only 1.8 has
	Class<?> clazz;
	try {
	    clazz = Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
	} catch (Exception e) {
	    //getLogger().info("No PlayerInteractAtEntityEvent found.");
	    clazz = null;
	}
	if (clazz != null) {
	    onePointEight = true;
	}
	saveDefaultConfig();
	// Check to see if island distance is set or not
	if (getConfig().getInt("island.distance", -1) < 1) {
	    getLogger().severe("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
	    getLogger().severe("More set up is required. Go to config.yml and edit it.");
	    getLogger().severe("");
	    getLogger().severe("Make sure you set island distance. If upgrading, set it to what it was before.");
	    getLogger().severe("");
	    getLogger().severe("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
	    if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
		getCommand("island").setExecutor(new NotSetup(Reason.DISTANCE));
		getCommand("asc").setExecutor(new NotSetup(Reason.DISTANCE));
		getCommand("asadmin").setExecutor(new NotSetup(Reason.DISTANCE));
	    } else {
		getCommand("ai").setExecutor(new NotSetup(Reason.DISTANCE));
		getCommand("aic").setExecutor(new NotSetup(Reason.DISTANCE));
		getCommand("acid").setExecutor(new NotSetup(Reason.DISTANCE));
	    }
	    return;
	}
	// Load all the configuration of the plugin and localization strings
	if (!loadPluginConfig()) {
	    // Currently, the only setup error is where the world_name does not match
	    if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
		getCommand("island").setExecutor(new NotSetup(Reason.WORLD_NAME));
		getCommand("asc").setExecutor(new NotSetup(Reason.WORLD_NAME));
		getCommand("asadmin").setExecutor(new NotSetup(Reason.WORLD_NAME));
	    } else {
		getCommand("ai").setExecutor(new NotSetup(Reason.WORLD_NAME));
		getCommand("aic").setExecutor(new NotSetup(Reason.WORLD_NAME));
		getCommand("acid").setExecutor(new NotSetup(Reason.WORLD_NAME));
	    }
	    return;
	}
	if (Settings.useEconomy && !VaultHelper.setupEconomy()) {
	    getLogger().warning("Could not set up economy! - Running without an economy.");
	    Settings.useEconomy = false;
	}
	if (!VaultHelper.setupPermissions()) {
	    getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	}

	// This can no longer be run in onEnable because the plugin is loaded at
	// startup and so key variables are
	// not known to the server. Instead it is run one tick after startup.
	// getIslandWorld();

	// Set and make the player's directory if it does not exist and then
	// load players into memory
	playersFolder = new File(getDataFolder() + File.separator + "players");
	if (!playersFolder.exists()) {
	    playersFolder.mkdir();
	}
	// Set up commands for this plugin
	islandCmd = new IslandCmd(this);
	if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
	    AdminCmd adminCmd = new AdminCmd(this);

	    getCommand("island").setExecutor(islandCmd);
	    getCommand("island").setTabCompleter(islandCmd);

	    getCommand("asc").setExecutor(getChallenges());
	    getCommand("asc").setTabCompleter(getChallenges());

	    getCommand("asadmin").setExecutor(adminCmd);
	    getCommand("asadmin").setTabCompleter(adminCmd);
	} else {
	    AdminCmd adminCmd = new AdminCmd(this);

	    getCommand("ai").setExecutor(islandCmd);
	    getCommand("ai").setTabCompleter(islandCmd);

	    getCommand("aic").setExecutor(getChallenges());
	    getCommand("aic").setTabCompleter(getChallenges());

	    getCommand("acid").setExecutor(adminCmd);
	    getCommand("acid").setTabCompleter(adminCmd);
	}
	// Register events that this plugin uses
	// registerEvents();
	// Load messages
	messages = new Messages(this);
	messages.loadMessages();
	// Register events
	registerEvents();
	// Metrics
	try {
	    final Metrics metrics = new Metrics(this);
	    metrics.start();
	} catch (final IOException localIOException) {
	}
	// Kick off a few tasks on the next tick
	// By calling getIslandWorld(), if there is no island
	// world, it will be created
	getServer().getScheduler().runTask(this, new Runnable() {
	    @Override
	    public void run() {
		// Create the world if it does not exist. This is run after the
		// server starts.
		getIslandWorld();
		// Load warps
		getWarpSignsListener().loadWarpList();
		// Load the warp panel
		if (Settings.useWarpPanel) {
		    warpPanel = new WarpPanel(plugin);
		    getServer().getPluginManager().registerEvents(warpPanel, plugin);
		}
		// Minishop - must wait for economy to load before we can use
		// econ
		getServer().getPluginManager().registerEvents(new ControlPanel(plugin), plugin);
		// Try to register Herochat
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("Herochat")) {
		    getServer().getPluginManager().registerEvents(new HeroChatListener(plugin), plugin);
		}
		if (getServer().getWorld(Settings.worldName).getGenerator() == null) {
		    // Check if the world generator is registered correctly
		    getLogger().severe("********* The Generator for " + plugin.getName() + " is not registered so the plugin cannot start ********");
		    getLogger().severe("Make sure you have the following in bukkit.yml (case sensitive):");
		    getLogger().severe("worlds:");
		    getLogger().severe("  # The next line must be the name of your world:");
		    getLogger().severe("  " + Settings.worldName + ":");
		    getLogger().severe("    generator: " + plugin.getName());
		    if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
			getCommand("island").setExecutor(new NotSetup(Reason.GENERATOR));
			getCommand("asc").setExecutor(new NotSetup(Reason.GENERATOR));
			getCommand("asadmin").setExecutor(new NotSetup(Reason.GENERATOR));
		    } else {
			getCommand("ai").setExecutor(new NotSetup(Reason.GENERATOR));
			getCommand("aic").setExecutor(new NotSetup(Reason.GENERATOR));
			getCommand("acid").setExecutor(new NotSetup(Reason.GENERATOR));
		    }
		    return;
		}
		getServer().getScheduler().runTask(plugin, new Runnable() {
		    @Override
		    public void run() {
			// load the list - order matters - grid first, then top
			// ten to optimize upgrades
			// Load grid
			if (grid == null) {
			    grid = new GridManager(plugin);
			}
			TopTen.topTenLoad();
			if (tinyDB == null) {
			    tinyDB = new TinyDB(plugin);
			}
			// Add any online players to the DB
			for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
			    tinyDB.savePlayerName(onlinePlayer.getName(), onlinePlayer.getUniqueId());
			}
			getLogger().info("All files loaded. Ready to play...");
		    }
		});
		// Check for updates asynchronously
		if (Settings.updateCheck) {
		    checkUpdates();
		    new BukkitRunnable() {
			int count = 0;
			@Override
			public void run() {
			    if (count++ > 10) {
				plugin.getLogger().info("No updates found. (No response from server after 10s)");
				this.cancel();
			    } else {
				// Wait for the response
				if (updateCheck != null) {
				    if (updateCheck.isSuccess()) {
					checkUpdatesNotify(null);
				    } else {
					plugin.getLogger().info("No update.");
				    }
				    this.cancel();
				}
			    }
			}
		    }.runTaskTimer(plugin, 0L, 20L); // Check status every second
		}
		// This part will kill monsters if they fall into the water
		// because it
		// is acid
		if (Settings.mobAcidDamage > 0D || Settings.animalAcidDamage > 0D) {
		    getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
			    List<Entity> entList = islandWorld.getEntities();
			    for (Entity current : entList) {
				if (plugin.isOnePointEight() && current instanceof Guardian) {
				    // Guardians are immune to acid too
				    continue;
				}
				if ((current instanceof Monster) && Settings.mobAcidDamage > 0D) {
				    if ((current.getLocation().getBlock().getType() == Material.WATER)
					    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
					((Monster) current).damage(Settings.mobAcidDamage);
					// getLogger().info("Killing monster");
				    }
				} else if ((current instanceof Animals) && Settings.animalAcidDamage > 0D) {
				    if ((current.getLocation().getBlock().getType() == Material.WATER)
					    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
					if (!current.getType().equals(EntityType.CHICKEN)) {
					    ((Animals) current).damage(Settings.animalAcidDamage);
					} else if (Settings.damageChickens) {
					    ((Animals) current).damage(Settings.animalAcidDamage);
					}
					// getLogger().info("Killing animal");
				    }
				}
			    }
			}
		    }, 0L, 20L);
		}
	    }
	});
    }

    /**
     * Checks to see if there are any plugin updates
     * Called when reloading settings too
     */
    public void checkUpdates() {
	// Version checker
	getLogger().info("Checking for new updates...");
	getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
	    @Override
	    public void run() {
		if (Settings.GAMETYPE.equals(GameType.ASKYBLOCK)) {
		    updateCheck = new Update(85189); // ASkyBlock
		} else {
		    updateCheck = new Update(80095); // AcidIsland
		}
		if (!updateCheck.isSuccess()) {
		    updateCheck = null;
		}
	    }
	});
    }

    public void checkUpdatesNotify(Player p) {
	boolean update = false;
	final String pluginVersion = plugin.getDescription().getVersion();
	// Check to see if the latest file is newer that this one
	String[] split = plugin.getUpdateCheck().getVersionName().split(" V");
	// Only do this if the format is what we expect
	if (split.length == 2) {
	    //getLogger().info("DEBUG: " + split[1]);
	    // Need to escape the period in the regex expression
	    String[] updateVer = split[1].split("\\.");
	    //getLogger().info("DEBUG: split length = " + updateVer.length);
	    // CHeck the version #'s
	    String[] pluginVer = pluginVersion.split("\\.");
	    //getLogger().info("DEBUG: split length = " + pluginVer.length);
	    // Run through major, minor, sub
	    for (int i = 0; i < Math.max(updateVer.length, pluginVer.length); i++) {
		try {
		    int updateCheck = 0;
		    if (i < updateVer.length) {
			updateCheck = Integer.valueOf(updateVer[i]);
		    }
		    int pluginCheck = 0;
		    if (i < pluginVer.length) {
			pluginCheck = Integer.valueOf(pluginVer[i]);
		    }
		    //getLogger().info("DEBUG: update is " + updateCheck + " plugin is " + pluginCheck);
		    if (updateCheck < pluginCheck) {
			//getLogger().info("DEBUG: plugin is newer!");
			//plugin is newer
			update = false;
			break;
		    } else if (updateCheck > pluginCheck) {
			//getLogger().info("DEBUG: update is newer!");
			update = true;
			break;
		    }
		} catch (Exception e) {
		    getLogger().warning("Could not determine update's version # ");
		    getLogger().warning("Plugin version: "+ pluginVersion);
		    getLogger().warning("Update version: " + plugin.getUpdateCheck().getVersionName());
		    return;
		}
	    }
	}
	// Show the results
	if (p != null) {
	    if (!update) {
		return;
	    } else {
		// Player login
		p.sendMessage(ChatColor.GOLD + plugin.getUpdateCheck().getVersionName() + " is available! You are running " + pluginVersion);
		if (Settings.GAMETYPE.equals(GameType.ASKYBLOCK)) {
		    p.sendMessage(ChatColor.RED + "Update at: http://dev.bukkit.org/bukkit-plugins/skyblock");
		} else {
		    p.sendMessage(ChatColor.RED + "Update at: http://dev.bukkit.org/bukkit-plugins/acidisland");
		}
	    }
	} else {
	    // Console
	    if (!update) {
		getLogger().info("No updates available.");
		return;
	    } else {
		getLogger().info(plugin.getUpdateCheck().getVersionName() + " is available! You are running " + pluginVersion);
		if (Settings.GAMETYPE.equals(GameType.ASKYBLOCK)) {
		    getLogger().info("Update at: http://dev.bukkit.org/bukkit-plugins/skyblock");
		} else {
		    getLogger().info("Update at: http://dev.bukkit.org/bukkit-plugins/acidisland");
		}
	    }
	}
    }

    /**
     * Delete Island
     * Called when an island is restarted or reset
     * 
     * @param player
     *            - player name String
     * @param removeBlocks
     *            - true to remove the island blocks
     */
    public void deletePlayerIsland(final UUID player, boolean removeBlocks) {
	// Removes the island
	// getLogger().info("DEBUG: deleting player island");
	CoopPlay.getInstance().clearAllIslandCoops(player);
	getWarpSignsListener().removeWarp(player);
	Island island = grid.getIsland(player);
	if (island != null) {
	    if (removeBlocks) {
		grid.removePlayersFromIsland(island);
		new DeleteIslandChunk(this, island);
	    } else {
		island.setLocked(false);
		grid.setIslandOwner(island, null);
	    }
	} else {
	    getLogger().severe("Could not delete player: " + player.toString() + " island!");
	}
	players.zeroPlayerData(player);
    }

    /**
     * @return the biomes
     */
    public BiomesPanel getBiomes() {
	return biomes;
    }

    /**
     * @return the challenges
     */
    public Challenges getChallenges() {
	if (challenges == null) {
	    challenges = new Challenges(this);
	}
	return challenges;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(final String worldName, final String id) {
	return new ChunkGeneratorWorld();
    }

    /**
     * @return the grid
     */
    public GridManager getGrid() {
	if (grid == null) {
	    grid = new GridManager(this);
	}
	return grid;
    }

    /**
     * @return the players
     */
    public PlayerCache getPlayers() {
	if (players == null) {
	    players = new PlayerCache(this);
	}
	return players;
    }

    /**
     * @return the playersFolder
     */
    public File getPlayersFolder() {
	return playersFolder;
    }

    /**
     * @return the updateCheck
     */
    public Update getUpdateCheck() {
	return updateCheck;
    }

    /**
     * @param updateCheck the updateCheck to set
     */
    public void setUpdateCheck(Update updateCheck) {
	this.updateCheck = updateCheck;
    }

    /**
     * @return the calculatingLevel
     */
    public boolean isCalculatingLevel() {
	return calculatingLevel;
    }

    /**
     * @return the newIsland
     */
    public boolean isNewIsland() {
	return newIsland;
    }

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    @SuppressWarnings("deprecation")
    public boolean loadPluginConfig() {
	// getLogger().info("*********************************************");
	try {
	    getConfig();
	} catch (final Exception e) {
	    e.printStackTrace();
	}
	//CompareConfigs.compareConfigs();
	// Get the localization strings
	//getLocale();
	// Add this to the config
	// Default is locale.yml
	availableLocales.put("locale", new Locale(this, "locale"));
	availableLocales.put("de-DE", new Locale(this,"de-DE"));
	availableLocales.put("en-US", new Locale(this,"en-US"));
	availableLocales.put("es-ES", new Locale(this,"es-ES"));
	availableLocales.put("fr-FR", new Locale(this,"fr-FR"));
	availableLocales.put("it-IT", new Locale(this,"it-IT"));
	availableLocales.put("ko-KR", new Locale(this,"ko-KR"));
	availableLocales.put("pl-PL", new Locale(this,"pl-PL"));
	availableLocales.put("pt-BR", new Locale(this,"pt-BR"));
	availableLocales.put("zh-CN", new Locale(this,"zh-CN"));
	availableLocales.put("cs-CS", new Locale(this,"cs-CS"));
	availableLocales.put("sk-SK", new Locale(this,"sk-SK"));
	availableLocales.put("zh-TW", new Locale(this,"zh-TW"));

	// Assign settings
	String configVersion = getConfig().getString("general.version", "");
	//getLogger().info("DEBUG: config ver length " + configVersion.split("\\.").length);
	// Ignore last digit if it is 4 digits long
	if (configVersion.split("\\.").length == 4) {
	    configVersion = configVersion.substring(0, configVersion.lastIndexOf('.')); 
	}
	// Save for plugin version
	String version = plugin.getDescription().getVersion();
	//getLogger().info("DEBUG: version length " + version.split("\\.").length);
	if (version.split("\\.").length == 4) {
	    version = version.substring(0, version.lastIndexOf('.')); 
	}
	if (configVersion.isEmpty() || !configVersion.equalsIgnoreCase(version)) {
	    // Check to see if this has already been shared
	    File newConfig = new File(plugin.getDataFolder(),"config.new.yml");
	    getLogger().warning("***********************************************************");
	    getLogger().warning("Config file is out of date. See config.new.yml for updates!");
	    getLogger().warning("config.yml version is '" + configVersion + "'");
	    getLogger().warning("Latest config version is '" + version + "'");
	    getLogger().warning("***********************************************************");
	    if (!newConfig.exists()) {
		File oldConfig = new File(plugin.getDataFolder(),"config.yml");
		File bakConfig = new File(plugin.getDataFolder(),"config.bak");
		if (oldConfig.renameTo(bakConfig)) {
		    plugin.saveResource("config.yml", false);
		    oldConfig.renameTo(newConfig);
		    bakConfig.renameTo(oldConfig);
		} 
	    }
	}
	// Debug
	Settings.debug = getConfig().getInt("debug", 0);
	// Mute death messages
	Settings.muteDeathMessages = getConfig().getBoolean("general.mutedeathmessages", false);
	// Warp panel
	Settings.useWarpPanel = getConfig().getBoolean("general.usewarppanel", true);
	// Fast level calculation (this is really fast)
	Settings.fastLevelCalc = getConfig().getBoolean("general.fastlevelcalc", true);
	// Restrict wither
	Settings.restrictWither = getConfig().getBoolean("general.restrictwither", true);
	// Team chat
	Settings.teamChat = getConfig().getBoolean("general.teamchat", true);
	// TEAMSUFFIX as island level
	Settings.setTeamName = getConfig().getBoolean("general.setteamsuffix", false);
	Settings.teamSuffix = getConfig().getString("general.teamsuffix","([level])");
	// Immediate teleport
	Settings.immediateTeleport = getConfig().getBoolean("general.immediateteleport", false);
	// Make island automatically
	Settings.makeIslandIfNone = getConfig().getBoolean("general.makeislandifnone", false);
	// Use physics when pasting island block schematics
	Settings.usePhysics = getConfig().getBoolean("general.usephysics", false);
	// Run level calc at login
	Settings.loginLevel = getConfig().getBoolean("general.loginlevel", false);
	// Use economy or not
	// In future expand to include internal economy
	Settings.useEconomy = getConfig().getBoolean("general.useeconomy", true);
	// Check for updates
	Settings.updateCheck = getConfig().getBoolean("general.checkupdates", true);
	// Island reset commands
	Settings.resetCommands = getConfig().getStringList("general.resetcommands");
	Settings.leaveCommands = getConfig().getStringList("general.leavecommands");
	Settings.startCommands = getConfig().getStringList("general.startcommands");
	Settings.useControlPanel = getConfig().getBoolean("general.usecontrolpanel", false);
	// Check if /island command is allowed when falling
	Settings.allowTeleportWhenFalling = getConfig().getBoolean("general.allowfallingteleport", true);
	Settings.fallingCommandBlockList = getConfig().getStringList("general.blockingcommands");
	// Visitor command banned list
	Settings.visitorCommandBlockList = getConfig().getStringList("general.visitorbannedcommands");
	// Max team size
	Settings.maxTeamSize = getConfig().getInt("island.maxteamsize", 4);
	// Deprecated settings - use permission askyblock.team.maxsize.<number> instead
	Settings.maxTeamSizeVIP = getConfig().getInt("island.vipteamsize", 0);
	Settings.maxTeamSizeVIP2 = getConfig().getInt("island.vip2teamsize", 0);
	if (Settings.maxTeamSizeVIP > 0 || Settings.maxTeamSizeVIP2 > 0) {
	    getLogger().warning(Settings.PERMPREFIX + "team.vip and " + Settings.PERMPREFIX + "team.vip2 are deprecated!");
	    getLogger().warning("Use permission " + Settings.PERMPREFIX + "team.maxsize.<number> instead.");
	}
	// Max home number
	Settings.maxHomes = getConfig().getInt("general.maxhomes",1);
	if (Settings.maxHomes < 1) {
	    Settings.maxHomes = 1;
	}
	// Settings from config.yml
	Settings.worldName = getConfig().getString("general.worldName");
	// Check if the world name matches island.yml info
	File islandFile = new File(plugin.getDataFolder(), "islands.yml");
	if (islandFile.exists()) {
	    YamlConfiguration islandYaml = new YamlConfiguration();
	    try {
		islandYaml.load(islandFile);
		if (!islandYaml.contains(Settings.worldName)) {
		    // Bad news, stop everything and tell the admin
		    getLogger().severe("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
		    getLogger().severe("More set up is required. Go to config.yml and edit it.");
		    getLogger().severe("");
		    getLogger().severe("Check island world name is same as world in islands.yml.");
		    getLogger().severe("If you are resetting and changing world, delete island.yml and restart.");
		    getLogger().severe("");
		    getLogger().severe("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
		    return false;
		}
	    } catch (Exception e) {}	    
	}
	Settings.createNether = getConfig().getBoolean("general.createnether", true);
	if (!Settings.createNether) {
	    getLogger().info("The Nether is disabled");
	}

	String companion = getConfig().getString("island.companion", "COW").toUpperCase();
	if (companion.equalsIgnoreCase("NOTHING")) {
	    Settings.islandCompanion = null;
	} else {
	    try {
		Settings.islandCompanion = EntityType.valueOf(companion);
		// Limit types
		switch (Settings.islandCompanion) {
		case BAT:
		case CHICKEN:
		case COW:
		case HORSE:
		case IRON_GOLEM:
		case MUSHROOM_COW:
		case OCELOT:
		case PIG:
		case RABBIT:
		case SHEEP:
		case SNOWMAN:
		case VILLAGER:
		case WOLF:
		    break;
		default:
		    getLogger()
		    .warning(
			    "Island companion is not recognized. Pick from COW, PIG, SHEEP, CHICKEN, VILLAGER, HORSE, IRON_GOLEM, OCELOT, RABBIT, WOLF, SNOWMAN, BAT, MUSHROOM_COW");
		    Settings.islandCompanion = EntityType.COW;
		    break;
		}
	    } catch (Exception e) {
		getLogger()
		.warning(
			"Island companion is not recognized. Pick from COW, PIG, SHEEP, CHICKEN, VILLAGER, HORSE, IRON_GOLEM, OCELOT, RABBIT, WOLF, BAT, MUSHROOM_COW, SNOWMAN");
		Settings.islandCompanion = EntityType.COW;
	    }
	}
	// Companion names
	List<String> companionNames = getConfig().getStringList("island.companionnames");
	Settings.companionNames = new ArrayList<String>();
	for (String name : companionNames) {
	    Settings.companionNames.add(ChatColor.translateAlternateColorCodes('&', name));
	}
	Settings.islandDistance = getConfig().getInt("island.distance", 200);
	if (Settings.islandDistance < 50) {
	    Settings.islandDistance = 50;
	    getLogger().info("Setting minimum island distance to 50");
	}
	Settings.islandXOffset = getConfig().getInt("island.xoffset", 0);
	if (Settings.islandXOffset < 0) {
	    Settings.islandXOffset = 0;
	    getLogger().info("Setting minimum island X Offset to 0");
	} else if (Settings.islandXOffset > Settings.islandDistance) {
	    Settings.islandXOffset = Settings.islandDistance;
	    getLogger().info("Setting maximum island X Offset to " + Settings.islandDistance);
	}
	Settings.islandZOffset = getConfig().getInt("island.zoffset", 0);
	if (Settings.islandZOffset < 0) {
	    Settings.islandZOffset = 0;
	    getLogger().info("Setting minimum island Z Offset to 0");
	} else if (Settings.islandZOffset > Settings.islandDistance) {
	    Settings.islandZOffset = Settings.islandDistance;
	    getLogger().info("Setting maximum island Z Offset to " + Settings.islandDistance);
	}
	long x = getConfig().getLong("island.startx", 0);
	// Check this is a multiple of island distance
	long z = getConfig().getLong("island.startz", 0);
	Settings.islandStartX = Math.round((double) x / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
	Settings.islandStartZ = Math.round((double) z / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;

	// ASkyBlock and AcidIsland difference
	if (Settings.GAMETYPE.equals(Settings.GameType.ACIDISLAND)) {
	    Settings.acidDamage = getConfig().getDouble("general.aciddamage", 5D);
	    if (Settings.acidDamage > 100D) {
		Settings.acidDamage = 100D;
	    } else if (Settings.acidDamage < 0D) {
		Settings.acidDamage = 0D;
	    }
	    Settings.mobAcidDamage = getConfig().getDouble("general.mobaciddamage", 10D);
	    if (Settings.mobAcidDamage > 100D) {
		Settings.mobAcidDamage = 100D;
	    } else if (Settings.mobAcidDamage < 0D) {
		Settings.mobAcidDamage = 0D;
	    }
	    Settings.rainDamage = getConfig().getDouble("general.raindamage", 0.5D);
	    if (Settings.rainDamage > 100D) {
		Settings.rainDamage = 100D;
	    } else if (Settings.rainDamage < 0D) {
		Settings.rainDamage = 0D;
	    }
	    // The island's center is actually 5 below sea level
	    Settings.sea_level = getConfig().getInt("general.sealevel", 50);
	    if (Settings.sea_level < 0) {
		Settings.sea_level = 0;
	    }
	    Settings.island_level = getConfig().getInt("general.islandlevel", 50) - 5;
	    if (Settings.island_level < 0) {
		Settings.island_level = 0;
	    }
	} else {
	    Settings.acidDamage = getConfig().getDouble("general.aciddamage", 0D);
	    if (Settings.acidDamage > 100D) {
		Settings.acidDamage = 100D;
	    } else if (Settings.acidDamage < 0D) {
		Settings.acidDamage = 0D;
	    }
	    Settings.mobAcidDamage = getConfig().getDouble("general.mobaciddamage", 0D);
	    if (Settings.mobAcidDamage > 100D) {
		Settings.mobAcidDamage = 100D;
	    } else if (Settings.mobAcidDamage < 0D) {
		Settings.mobAcidDamage = 0D;
	    }
	    Settings.rainDamage = getConfig().getDouble("general.raindamage", 0D);
	    if (Settings.rainDamage > 100D) {
		Settings.rainDamage = 100D;
	    } else if (Settings.rainDamage < 0D) {
		Settings.rainDamage = 0D;
	    }
	    // The island's center is actually 5 below sea level
	    Settings.sea_level = getConfig().getInt("general.sealevel", 0);
	    if (Settings.sea_level < 0) {
		Settings.sea_level = 0;
	    }
	    Settings.island_level = getConfig().getInt("general.islandlevel", 120) - 5;
	    if (Settings.island_level < 0) {
		Settings.island_level = 0;
	    }
	}
	Settings.animalAcidDamage = getConfig().getDouble("general.animaldamage", 0D);
	if (Settings.animalAcidDamage > 100D) {
	    Settings.animalAcidDamage = 100D;
	} else if (Settings.animalAcidDamage < 0D) {
	    Settings.animalAcidDamage = 0D;
	}
	Settings.damageChickens = getConfig().getBoolean("general.damagechickens", false);
	// Damage Type
	List<String> acidDamageType = getConfig().getStringList("general.damagetype");
	Settings.acidDamageType.clear();
	if (acidDamageType != null) {
	    for (String effect : acidDamageType) {
		PotionEffectType newPotionType = PotionEffectType.getByName(effect);
		if (newPotionType != null) {
		    // Check if it is a valid addition
		    if (newPotionType.equals(PotionEffectType.BLINDNESS) || newPotionType.equals(PotionEffectType.CONFUSION)
			    || newPotionType.equals(PotionEffectType.HUNGER) || newPotionType.equals(PotionEffectType.POISON)
			    || newPotionType.equals(PotionEffectType.SLOW) || newPotionType.equals(PotionEffectType.SLOW_DIGGING)
			    || newPotionType.equals(PotionEffectType.WEAKNESS)) {
			Settings.acidDamageType.add(newPotionType);
		    }
		} else {
		    getLogger().warning("Could not interpret acid damage modifier: " + effect + " - skipping");
		    getLogger().warning("Types can be : SLOW, SLOW_DIGGING, CONFUSION,");
		    getLogger().warning("BLINDNESS, HUNGER, WEAKNESS and POISON");
		}
	    }
	}

	Settings.animalSpawnLimit = getConfig().getInt("general.animalspawnlimit", 15);
	if (Settings.animalSpawnLimit > 100) {
	    Settings.animalSpawnLimit = 100;
	} else if (Settings.animalSpawnLimit < -1) {
	    Settings.animalSpawnLimit = -1;
	}

	Settings.monsterSpawnLimit = getConfig().getInt("general.monsterspawnlimit", 70);
	if (Settings.monsterSpawnLimit > 100) {
	    Settings.monsterSpawnLimit = 100;
	} else if (Settings.monsterSpawnLimit < -1) {
	    Settings.monsterSpawnLimit = -1;
	}

	Settings.waterAnimalSpawnLimit = getConfig().getInt("general.wateranimalspawnlimit", 15);
	if (Settings.waterAnimalSpawnLimit > 100) {
	    Settings.waterAnimalSpawnLimit = 100;
	} else if (Settings.waterAnimalSpawnLimit < -1) {
	    Settings.waterAnimalSpawnLimit = -1;
	}

	Settings.abandonedIslandLevel = getConfig().getInt("general.abandonedislandlevel", 10);
	if (Settings.abandonedIslandLevel < 0) {
	    Settings.abandonedIslandLevel = 0;
	}

	Settings.island_protectionRange = getConfig().getInt("island.protectionRange", 100);
	if (Settings.island_protectionRange % 2 != 0) {
	    Settings.island_protectionRange--;
	    getLogger().warning("Protection range must be even, using " + Settings.island_protectionRange);
	}
	if (Settings.island_protectionRange > Settings.islandDistance) {
	    if (!getConfig().getBoolean("island.overridelimit", false)) {
		if (Settings.island_protectionRange > (Settings.islandDistance - 16)) {
		    Settings.island_protectionRange = Settings.islandDistance - 16;
		    getLogger().warning(
			    "*** Island protection range must be " + (Settings.islandDistance - 16) + " or less, (island range -16). Setting to: "
				    + Settings.island_protectionRange);
		}
	    } else {
		Settings.island_protectionRange = Settings.islandDistance;
	    }
	}
	if (Settings.island_protectionRange < 0) {
	    Settings.island_protectionRange = 0;
	}
	Settings.resetChallenges = getConfig().getBoolean("general.resetchallenges", true);
	Settings.resetMoney = getConfig().getBoolean("general.resetmoney", true);
	Settings.clearInventory = getConfig().getBoolean("general.resetinventory", true);
	Settings.resetEnderChest = getConfig().getBoolean("general.resetenderchest", false);

	Settings.startingMoney = getConfig().getDouble("general.startingmoney", 0D);
	Settings.respawnOnIsland = getConfig().getBoolean("general.respawnonisland", false);
	Settings.newNether = getConfig().getBoolean("general.newnether", true);
	Settings.netherTrees = getConfig().getBoolean("general.nethertrees", true);
	// Nether spawn protection radius
	Settings.netherSpawnRadius = getConfig().getInt("general.netherspawnradius", 25);
	if (Settings.netherSpawnRadius < 0) {
	    Settings.netherSpawnRadius = 0;
	} else if (Settings.netherSpawnRadius > 100) {
	    Settings.netherSpawnRadius = 100;
	}

	Settings.resetWait = getConfig().getInt("general.resetwait", 300);
	if (Settings.resetWait < 0) {
	    Settings.resetWait = 0;
	}
	Settings.resetLimit = getConfig().getInt("general.resetlimit", 0);
	if (Settings.resetWait < 0) {
	    Settings.resetWait = -1;
	}
	Settings.inviteWait = getConfig().getInt("general.invitewait", 60);
	if (Settings.inviteWait < 0) {
	    Settings.inviteWait = 0;
	}
	Settings.levelWait = getConfig().getInt("general.levelwait", 60);
	if (Settings.levelWait < 0) {
	    Settings.levelWait = 0;
	}
	// Seconds to wait for a confirmation of reset
	Settings.resetConfirmWait = getConfig().getInt("general.resetconfirmwait", 10);
	if (Settings.resetConfirmWait < 0) {
	    Settings.resetConfirmWait = 0;
	}
	Settings.damageOps = getConfig().getBoolean("general.damageops", false);
	// Settings.ultraSafeBoats =
	// getConfig().getBoolean("general.ultrasafeboats", true);
	Settings.logInRemoveMobs = getConfig().getBoolean("general.loginremovemobs", true);
	Settings.islandRemoveMobs = getConfig().getBoolean("general.islandremovemobs", false);
	List<String> mobWhiteList = getConfig().getStringList("general.mobwhitelist");
	Settings.mobWhiteList.clear();
	String valid = "BLAZE, CREEPER, SKELETON, SPIDER, GIANT, ZOMBIE, GHAST, PIG_ZOMBIE, "
		+ "ENDERMAN, CAVE_SPIDER, SILVERFISH,  WITHER, WITCH, ENDERMITE,"
		+ " GUARDIAN";
	for (String mobName : mobWhiteList) {
	    if (valid.contains(mobName.toUpperCase())) {
		try {
		    Settings.mobWhiteList.add(EntityType.valueOf(mobName.toUpperCase()));
		} catch (Exception e) {
		    plugin.getLogger().severe("Error in config.yml, mobwhitelist value '" + mobName + "' is invalid.");
		    plugin.getLogger().severe("Possible values are : Blaze, Cave_Spider, Creeper, Enderman, Endermite, Giant, Guardian, "
			    + "Pig_Zombie, Silverfish, Skeleton, Spider, Witch, Wither, Zombie");
		}
	    } else {
		plugin.getLogger().severe("Error in config.yml, mobwhitelist value '" + mobName + "' is invalid.");
		plugin.getLogger().severe("Possible values are : Blaze, Cave_Spider, Creeper, Enderman, Endermite, Giant, Guardian, "
			+ "Pig_Zombie, Silverfish, Skeleton, Spider, Witch, Wither, Zombie");
	    }
	}
	// getLogger().info("DEBUG: island level is " + Settings.island_level);
	// Get chest items
	final String[] chestItemString = getConfig().getString("island.chestItems").split(" ");
	// getLogger().info("DEBUG: chest items = " + chestItemString);
	final ItemStack[] tempChest = new ItemStack[chestItemString.length];
	for (int i = 0; i < tempChest.length; i++) {
	    try {
		String[] amountdata = chestItemString[i].split(":");
		if (amountdata[0].equals("POTION")) {
		    // getLogger().info("DEBUG: Potion length " +
		    // amountdata.length);
		    if (amountdata.length == 2) {
			final String chestPotionEffect = getConfig().getString("island.chestPotion", "");
			if (!chestPotionEffect.isEmpty()) {
			    // Change the water bottle stack to a potion of some
			    // kind
			    Potion chestPotion = new Potion(PotionType.valueOf(chestPotionEffect));
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[1]));
			}
		    } else if (amountdata.length == 3) {
			// getLogger().info("DEBUG: Potion type :" +
			// amountdata[1]);
			Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1]));
			// getLogger().info("Potion in chest is :" +
			// chestPotion.getType().toString() + " x " +
			// amountdata[2]);
			tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[2]));
		    } else if (amountdata.length == 4) {
			// Extended or splash potions
			if (amountdata[2].equals("EXTENDED")) {
			    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).extend();
			    // getLogger().info("Potion in chest is :" +
			    // chestPotion.getType().toString() +
			    // " extended duration x " + amountdata[3]);
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
			} else if (amountdata[2].equals("SPLASH")) {
			    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).splash();
			    // getLogger().info("Potion in chest is :" +
			    // chestPotion.getType().toString() + " splash x " +
			    // amountdata[3]);
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
			} else if (amountdata[2].equals("EXTENDEDSPLASH")) {
			    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).extend().splash();
			    // getLogger().info("Potion in chest is :" +
			    // chestPotion.getType().toString() +
			    // " splash, extended duration x " + amountdata[3]);
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
			}
		    }
		} else {
		    Material mat;
		    if (StringUtils.isNumeric(amountdata[0])) {
			mat = Material.getMaterial(Integer.parseInt(amountdata[0]));
		    } else {
			mat = Material.getMaterial(amountdata[0].toUpperCase());
		    }
		    if (amountdata.length == 2) {
			tempChest[i] = new ItemStack(mat, Integer.parseInt(amountdata[1]));
		    } else if (amountdata.length == 3) {
			tempChest[i] = new ItemStack(mat, Integer.parseInt(amountdata[2]), Short.parseShort(amountdata[1]));
		    }
		}
	    } catch (java.lang.IllegalArgumentException ex) {
		getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
		getLogger().severe("Error is : " + ex.getMessage());
		getLogger().info("Potential potion types are: ");
		for (PotionType c : PotionType.values())
		    getLogger().info(c.name());
	    } catch (Exception e) {
		getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
		getLogger().info("Potential material types are: ");
		for (Material c : Material.values())
		    getLogger().info(c.name());
		// e.printStackTrace();
	    }
	}
	Settings.chestItems = tempChest;
	Settings.allowPvP = getConfig().getBoolean("island.allowPvP", false);
	Settings.allowNetherPvP = getConfig().getBoolean("island.allowNetherPvP", false);
	Settings.allowBreakBlocks = getConfig().getBoolean("island.allowbreakblocks", false);
	Settings.allowPlaceBlocks = getConfig().getBoolean("island.allowplaceblocks", false);
	Settings.allowBedUse = getConfig().getBoolean("island.allowbeduse", false);
	Settings.allowBucketUse = getConfig().getBoolean("island.allowbucketuse", false);
	Settings.allowShearing = getConfig().getBoolean("island.allowshearing", false);
	Settings.allowEnderPearls = getConfig().getBoolean("island.allowenderpearls", false);
	Settings.allowDoorUse = getConfig().getBoolean("island.allowdooruse", false);
	Settings.allowLeverButtonUse = getConfig().getBoolean("island.allowleverbuttonuse", false);
	Settings.allowCropTrample = getConfig().getBoolean("island.allowcroptrample", false);
	Settings.allowChestAccess = getConfig().getBoolean("island.allowchestaccess", false);
	Settings.allowFurnaceUse = getConfig().getBoolean("island.allowfurnaceuse", false);
	Settings.allowRedStone = getConfig().getBoolean("island.allowredstone", false);
	Settings.allowMusic = getConfig().getBoolean("island.allowmusic", false);
	Settings.allowCrafting = getConfig().getBoolean("island.allowcrafting", false);
	Settings.allowBrewing = getConfig().getBoolean("island.allowbrewing", false);
	Settings.allowGateUse = getConfig().getBoolean("island.allowgateuse", false);
	Settings.allowHurtMobs = getConfig().getBoolean("island.allowhurtmobs", true);
	Settings.endermanDeathDrop = getConfig().getBoolean("island.endermandeathdrop", true);
	Settings.allowEndermanGriefing = getConfig().getBoolean("island.allowendermangriefing", true);
	Settings.allowCreeperDamage = getConfig().getBoolean("island.allowcreeperdamage", true);
	Settings.allowCreeperGriefing = getConfig().getBoolean("island.allowcreepergriefing", false);
	Settings.allowTNTDamage = getConfig().getBoolean("island.allowtntdamage", false);
	Settings.allowMonsterEggs = getConfig().getBoolean("island.allowspawneggs", false);
	Settings.allowBreeding = getConfig().getBoolean("island.allowbreeding", false);
	Settings.allowFire = getConfig().getBoolean("island.allowfire", false);
	Settings.allowFireSpread = getConfig().getBoolean("island.allowfirespread", false);
	Settings.allowChestDamage = getConfig().getBoolean("island.allowchestdamage", false);
	Settings.allowLeashUse = getConfig().getBoolean("island.allowleashuse", false);
	Settings.allowHurtMonsters = getConfig().getBoolean("island.allowhurtmonsters", true);
	Settings.allowEnchanting = getConfig().getBoolean("island.allowenchanting", true);
	Settings.allowAnvilUse = getConfig().getBoolean("island.allowanviluse", true);
	Settings.allowVisitorKeepInvOnDeath = getConfig().getBoolean("island.allowvisitorkeepinvondeath", false);
	Settings.allowVisitorItemDrop = getConfig().getBoolean("island.allowvisitoritemdrop", true);
	Settings.allowVisitorItemPickup = getConfig().getBoolean("island.allowvisitoritempickup", true);
	Settings.allowArmorStandUse = getConfig().getBoolean("island.allowarmorstanduse", false);
	Settings.allowBeaconAccess = getConfig().getBoolean("island.allowbeaconaccess", false);
	Settings.allowPortalUse = getConfig().getBoolean("island.allowportaluse", true);
	// Spawn Settings
	Settings.allowSpawnDoorUse = getConfig().getBoolean("spawn.allowdooruse", true);
	Settings.allowSpawnLeverButtonUse = getConfig().getBoolean("spawn.allowleverbuttonuse", true);
	Settings.allowSpawnChestAccess = getConfig().getBoolean("spawn.allowchestaccess", true);
	Settings.allowSpawnFurnaceUse = getConfig().getBoolean("spawn.allowfurnaceuse", true);
	Settings.allowSpawnRedStone = getConfig().getBoolean("spawn.allowredstone", false);
	Settings.allowSpawnMusic = getConfig().getBoolean("spawn.allowmusic", true);
	Settings.allowSpawnCrafting = getConfig().getBoolean("spawn.allowcrafting", true);
	Settings.allowSpawnBrewing = getConfig().getBoolean("spawn.allowbrewing", true);
	Settings.allowSpawnGateUse = getConfig().getBoolean("spawn.allowgateuse", true);
	Settings.allowSpawnMobSpawn = getConfig().getBoolean("spawn.allowmobspawn", false);
	Settings.allowSpawnAnimalSpawn = getConfig().getBoolean("spawn.allowanimalspawn", true);
	Settings.allowSpawnAnimalKilling = getConfig().getBoolean("spawn.allowanimalkilling", false);
	Settings.allowSpawnMobKilling = getConfig().getBoolean("spawn.allowmobkilling", true);
	Settings.allowSpawnMonsterEggs = getConfig().getBoolean("spawn.allowspawneggs", false);
	Settings.allowSpawnEggs = getConfig().getBoolean("spawn.alloweggs", false);
	Settings.allowSpawnBreakBlocks = getConfig().getBoolean("spawn.allowbreakblocks", false);
	Settings.allowSpawnPlaceBlocks = getConfig().getBoolean("spawn.allowplaceblocks", false);
	Settings.allowSpawnNoAcidWater = getConfig().getBoolean("spawn.allowspawnnoacidwater", false);
	Settings.allowSpawnEnchanting = getConfig().getBoolean("spawn.allowenchanting", true);
	Settings.allowSpawnAnvilUse = getConfig().getBoolean("spawn.allowanviluse", true);
	Settings.allowSpawnBeaconAccess = getConfig().getBoolean("spawn.allowbeaconaccess", false);
	Settings.allowSpawnPVP = getConfig().getBoolean("spawn.allowPVP", false);
	Settings.allowSpawnMilking = getConfig().getBoolean("spawn.allowmilking", false);
	Settings.allowSpawnLavaCollection = getConfig().getBoolean("spawn.allowlavacollection", false);
	Settings.allowSpawnWaterCollection = getConfig().getBoolean("spawn.allowwatercollection", false);
	// Challenges
	getChallenges();
	// Challenge completion
	Settings.broadcastMessages = getConfig().getBoolean("general.broadcastmessages", true);

	// Levels
	// Get the blockvalues.yml file
	YamlConfiguration blockValuesConfig = Util.loadYamlFile("blockvalues.yml");
	// Get the under water multiplier
	Settings.underWaterMultiplier = blockValuesConfig.getDouble("underwater", 1D);
	Settings.levelCost = blockValuesConfig.getInt("levelcost", 100);
	Settings.blockLimits = new HashMap<MaterialData, Integer>();
	if (blockValuesConfig.isSet("limits")) {
	    for (String material : blockValuesConfig.getConfigurationSection("limits").getKeys(false)) {
		try {
		    String[] split = material.split(":");
		    byte data = 0;
		    if (split.length>1) {
			data = Byte.valueOf(split[1]);
		    }
		    Material mat;
		    if (StringUtils.isNumeric(split[0])) {
			mat = Material.getMaterial(Integer.parseInt(split[0]));
		    } else {
			mat = Material.valueOf(split[0].toUpperCase());
		    }
		    MaterialData materialData = new MaterialData(mat);
		    materialData.setData(data);
		    Settings.blockLimits.put(materialData, blockValuesConfig.getInt("limits." + material, 0));
		    if (debug) {
			getLogger().info("Maximum number of " + materialData + " will be " + Settings.blockLimits.get(materialData));
		    }
		} catch (Exception e) {
		    getLogger().warning("Unknown material (" + material + ") in blockvalues.yml Limits section. Skipping...");
		}
	    }
	}
	Settings.blockValues = new HashMap<MaterialData, Integer>();
	if (blockValuesConfig.isSet("blocks")) {
	    for (String material : blockValuesConfig.getConfigurationSection("blocks").getKeys(false)) {
		try {
		    String[] split = material.split(":");
		    byte data = 0;
		    if (split.length>1) {
			data = Byte.valueOf(split[1]);
		    }
		    Material mat;
		    if (StringUtils.isNumeric(split[0])) {
			mat = Material.getMaterial(Integer.parseInt(split[0]));
		    } else {
			mat = Material.valueOf(split[0].toUpperCase());
		    }
		    MaterialData materialData = new MaterialData(mat);
		    materialData.setData(data);
		    Settings.blockValues.put(materialData, blockValuesConfig.getInt("blocks." + material, 0));
		    if (debug) {
			getLogger().info(mat.toString() + " value is " + Settings.blockValues.get(mat));
		    }
		} catch (Exception e) {
		    // e.printStackTrace();
		    getLogger().warning("Unknown material (" + material + ") in blockvalues.yml blocks section. Skipping...");
		}
	    }
	} else {
	    getLogger().severe("No block values in blockvalues.yml! All island levels will be zero!");
	}
	// Biome Settings
	Settings.biomeCost = getConfig().getDouble("biomesettings.defaultcost", 100D);
	if (Settings.biomeCost < 0D) {
	    Settings.biomeCost = 0D;
	    getLogger().warning("Biome default cost is < $0, so set to zero.");
	}
	String defaultBiome = getConfig().getString("biomesettings.defaultbiome", "PLAINS");
	try {
	    Settings.defaultBiome = Biome.valueOf(defaultBiome);
	} catch (Exception e) {
	    getLogger().severe("Could not parse biome " + defaultBiome + " using PLAINS instead.");
	    Settings.defaultBiome = Biome.PLAINS;
	}
	Settings.breedingLimit = getConfig().getInt("general.breedinglimit", 0);
	Settings.villagerLimit = getConfig().getInt("general.villagerlimit", 0);
	Settings.hopperLimit = getConfig().getInt("general.hopperlimit", 0);
	Settings.mobLimit = getConfig().getInt("general.moblimit", 0);
	Settings.removeCompleteOntimeChallenges = getConfig().getBoolean("general.removecompleteonetimechallenges", false);
	Settings.addCompletedGlow = getConfig().getBoolean("general.addcompletedglow", true);
	// All done
	return true;
    }


    /**
     * Registers events
     */
    public void registerEvents() {
	final PluginManager manager = getServer().getPluginManager();
	// Nether portal events
	manager.registerEvents(new NetherPortals(this), this);
	// Island Protection events
	manager.registerEvents(new IslandGuard(this), this);
	// Player events
	manager.registerEvents(new PlayerEvents(this), this);
	// New V1.8 events
	if (onePointEight) {
	    manager.registerEvents(new IslandGuardNew(this), this);
	}
	// Events for when a player joins or leaves the server
	manager.registerEvents(new JoinLeaveEvents(this), this);
	// Ensures Lava flows correctly in ASkyBlock world
	lavaListener = new LavaCheck(this);
	manager.registerEvents(lavaListener, this);
	// Ensures that water is acid
	manager.registerEvents(new AcidEffect(this), this);
	// Ensures that boats are safe in ASkyBlock
	if (Settings.acidDamage > 0D) {
	    manager.registerEvents(new SafeBoat(this), this);
	}
	// Enables warp signs in ASkyBlock
	warpSignsListener = new WarpSigns(this);
	manager.registerEvents(warpSignsListener, this);
	// Control panel - for future use
	// manager.registerEvents(new ControlPanel(), this);
	// Change names of inventory items
	manager.registerEvents(new AcidInventory(this), this);
	// Biomes
	// Load Biomes
	biomes = new BiomesPanel(this);
	manager.registerEvents(biomes, this);
	// Schematics panel
	schematicsPanel = new SchematicsPanel(this);
	manager.registerEvents(schematicsPanel, this);
	// Track incoming world teleports
	manager.registerEvents(new WorldEnter(this), this);
	// Team chat
	chatListener = new ChatListener(this);
	manager.registerEvents(chatListener, this);
	// Wither
	if (Settings.restrictWither) {
	    manager.registerEvents(new WitherEvents(this), this);
	}
    }



    /**
     * Resets a player's inventory, armor slots, equipment, enderchest and
     * potion effects
     * 
     * @param player
     */
    public void resetPlayer(Player player) {
	// getLogger().info("DEBUG: clear inventory = " +
	// Settings.clearInventory);
	if (Settings.clearInventory
		&& (player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || player.getWorld().getName()
			.equalsIgnoreCase(Settings.worldName + "_nether"))) {
	    // Clear their inventory and equipment and set them as survival
	    player.getInventory().clear(); // Javadocs are wrong - this does not
	    // clear armor slots! So...
	    player.getInventory().setArmorContents(null);
	    player.getInventory().setHelmet(null);
	    player.getInventory().setChestplate(null);
	    player.getInventory().setLeggings(null);
	    player.getInventory().setBoots(null);
	    player.getEquipment().clear();
	}
	player.setGameMode(GameMode.SURVIVAL);
	if (Settings.resetChallenges) {
	    // Reset the player's challenge status
	    players.resetAllChallenges(player.getUniqueId());
	}
	// Reset the island level
	players.setIslandLevel(player.getUniqueId(), 0);
	// Clear the starter island
	players.clearStartIslandRating(player.getUniqueId());
	// Save the player
	players.save(player.getUniqueId());
	TopTen.topTenAddEntry(player.getUniqueId(), 0);
	// Update the inventory
	player.updateInventory();
	if (Settings.resetEnderChest) {
	    // Clear any Enderchest contents
	    final ItemStack[] items = new ItemStack[player.getEnderChest().getContents().length];
	    player.getEnderChest().setContents(items);
	}
	// Clear any potion effects
	for (PotionEffect effect : player.getActivePotionEffects())
	    player.removePotionEffect(effect.getType());
    }

    public void restartEvents() {
	final PluginManager manager = getServer().getPluginManager();
	lavaListener = new LavaCheck(this);
	manager.registerEvents(lavaListener, this);
	// Enables warp signs in ASkyBlock
	warpSignsListener = new WarpSigns(this);
	manager.registerEvents(warpSignsListener, this);
    }

    /**
     * @param calculatingLevel
     *            the calculatingLevel to set
     */
    public void setCalculatingLevel(boolean calculatingLevel) {
	this.calculatingLevel = calculatingLevel;
    }

    /**
     * @param newIsland
     *            the newIsland to set
     */
    public void setNewIsland(boolean newIsland) {
	this.newIsland = newIsland;
    }

    public void unregisterEvents() {
	HandlerList.unregisterAll(warpSignsListener);
	HandlerList.unregisterAll(lavaListener);
    }

    /**
     * @return the netherWorld
     */
    public static World getNetherWorld() {
	if (netherWorld == null && Settings.createNether) {
	    if (plugin.getServer().getWorld(Settings.worldName + "_nether") == null) {
		Bukkit.getLogger().info("Creating " + plugin.getName() + "'s Nether...");
	    }
	    if (!Settings.newNether) {
		netherWorld = WorldCreator.name(Settings.worldName + "_nether").type(WorldType.NORMAL).environment(World.Environment.NETHER).createWorld();
	    } else {
		netherWorld = WorldCreator.name(Settings.worldName + "_nether").type(WorldType.FLAT).generator(new ChunkGeneratorWorld())
			.environment(World.Environment.NETHER).createWorld();
	    }
	    netherWorld.setMonsterSpawnLimit(Settings.monsterSpawnLimit);
	    netherWorld.setAnimalSpawnLimit(Settings.animalSpawnLimit);
	}
	return netherWorld;
    }

    /**
     * @return Locale for this player
     */
    public Locale myLocale(UUID player) {
	String locale = players.getLocale(player);
	if (locale.isEmpty() || !availableLocales.containsKey(locale)) {
	    return availableLocales.get("locale");
	}
	return availableLocales.get(locale);
    }

    /**
     * @return System locale
     */
    public Locale myLocale() {
	return availableLocales.get("locale");
    }

    /**
     * @return the messages
     */
    public Messages getMessages() {
	return messages;
    }

    /**
     * @return the islandCmd
     */
    public IslandCmd getIslandCmd() {
	return islandCmd;
    }

    /**
     * @return the nameDB
     */
    public TinyDB getTinyDB() {
	return tinyDB;
    }

    public ChatListener getChatListener() {
	return chatListener;	
    }

    /**
     * @return the warpSignsListener
     */
    public WarpSigns getWarpSignsListener() {
	return warpSignsListener;
    }

    /**
     * @return the warpPanel
     */
    public WarpPanel getWarpPanel() {
	if (warpPanel == null) {
	    // Probably due to a reload
	    warpPanel = new WarpPanel(this);
	    getServer().getPluginManager().registerEvents(warpPanel, plugin);
	}
	return warpPanel;
    }

    /**
     * @return the schematicsPanel
     */
    public SchematicsPanel getSchematicsPanel() {
	return schematicsPanel;
    }

    /**
     * @return the onePointEight
     */
    public boolean isOnePointEight() {
        return onePointEight;
    }
}
