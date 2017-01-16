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
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SpawnEgg;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.NotSetup.Reason;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.askyblock.Updater.UpdateResult;
import com.wasteofplastic.askyblock.Updater.UpdateType;
import com.wasteofplastic.askyblock.commands.AdminCmd;
import com.wasteofplastic.askyblock.commands.Challenges;
import com.wasteofplastic.askyblock.commands.IslandCmd;
import com.wasteofplastic.askyblock.events.IslandDeleteEvent;
import com.wasteofplastic.askyblock.events.ReadyEvent;
import com.wasteofplastic.askyblock.generators.ChunkGeneratorWorld;
import com.wasteofplastic.askyblock.listeners.AcidEffect;
import com.wasteofplastic.askyblock.listeners.ChatListener;
import com.wasteofplastic.askyblock.listeners.CleanSuperFlat;
import com.wasteofplastic.askyblock.listeners.EntitySpawning;
import com.wasteofplastic.askyblock.listeners.FlyingMobEvents;
import com.wasteofplastic.askyblock.listeners.HeroChatListener;
import com.wasteofplastic.askyblock.listeners.IslandGuard;
import com.wasteofplastic.askyblock.listeners.IslandGuard1_8;
import com.wasteofplastic.askyblock.listeners.IslandGuard1_9;
import com.wasteofplastic.askyblock.listeners.JoinLeaveEvents;
import com.wasteofplastic.askyblock.listeners.LavaCheck;
import com.wasteofplastic.askyblock.listeners.NetherPortals;
import com.wasteofplastic.askyblock.listeners.NetherSpawning;
import com.wasteofplastic.askyblock.listeners.PlayerEvents;
import com.wasteofplastic.askyblock.listeners.WorldEnter;
import com.wasteofplastic.askyblock.listeners.WorldLoader;
import com.wasteofplastic.askyblock.panels.BiomesPanel;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.panels.SchematicsPanel;
import com.wasteofplastic.askyblock.panels.SettingsPanel;
import com.wasteofplastic.askyblock.panels.WarpPanel;
import com.wasteofplastic.askyblock.util.SpawnEgg1_9;
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
    // No push scoreboard name
    private final static String NO_PUSH_TEAM_NAME = "ASkyBlockNP";
    // Flag indicating if a new islands is in the process of being generated or
    // not
    private boolean newIsland = false;
    // Player folder file
    private File playersFolder;
    // Challenges object
    private Challenges challenges;
    // Localization Strings
    private HashMap<String,ASLocale> availableLocales = new HashMap<String,ASLocale>();
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
    // Top Ten
    private TopTen topTen;
    // V1.8 or later
    private boolean onePointEight;

    private boolean debug = false;

    // Level calc
    private boolean calculatingLevel = false;

    // Update object
    private Updater updateCheck = null;

    // Messages object
    private Messages messages;

    // Team chat listener
    private ChatListener chatListener;

    // Schematics panel object
    private SchematicsPanel schematicsPanel;

    // Settings panel object
    private SettingsPanel settingsPanel;
    
    // Acid Item Removal Task
    AcidTask acidTask;

    /**
     * Returns the World object for the island world named in config.yml.
     * If the world does not exist then it is created.
     *
     * @return islandWorld - Bukkit World object for the ASkyBlock world
     */
    public static World getIslandWorld() {
        if (islandWorld == null) {
            //Bukkit.getLogger().info("DEBUG worldName = " + Settings.worldName);
            //
            if (Settings.useOwnGenerator) {
                islandWorld = Bukkit.getServer().getWorld(Settings.worldName);
                //Bukkit.getLogger().info("DEBUG world is " + islandWorld);
            } else {
                islandWorld = WorldCreator.name(Settings.worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL).generator(new ChunkGeneratorWorld())
                        .createWorld();
            }
            // Make the nether if it does not exist
            if (Settings.createNether) {
                getNetherWorld();
            }
            // Multiverse configuration

            if (!Settings.useOwnGenerator && Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
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
        if (islandWorld != null) {
            islandWorld.setWaterAnimalSpawnLimit(Settings.waterAnimalSpawnLimit);
            islandWorld.setMonsterSpawnLimit(Settings.monsterSpawnLimit);
            islandWorld.setAnimalSpawnLimit(Settings.animalSpawnLimit);
        }

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
                // Save grid synchronously
                grid.saveGrid(false);
            }
            // Save the warps and do not reload the panel
            if (warpSignsListener != null) {
                warpSignsListener.saveWarpList();
            }
            if (messages != null) {
                messages.saveMessages();
            }
            TopTen.topTenSave();
            // Close the name database
            if (tinyDB != null) {
                tinyDB.saveDB();
            }
            // Save the coops
            CoopPlay.getInstance().saveCoops();
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
        // If the world exists, load it, even without the generator
        /*
	if (Settings.createNether) {
	    Bukkit.getWorld(Settings.worldName + "_nether");
	}
	if (Bukkit.getWorld(Settings.worldName) == null) {
	    islandWorld = WorldCreator.name(Settings.worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL).createWorld();
	}*/
        // Get challenges
        challenges = new Challenges(this);
        // Set and make the player's directory if it does not exist and then
        // load players into memory
        playersFolder = new File(getDataFolder() + File.separator + "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdir();
        }
        players = new PlayerCache(this);
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
        // Register world load event
        if (getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
            getServer().getPluginManager().registerEvents(new WorldLoader(this), this);
        }
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
                if (!Settings.useOwnGenerator && getServer().getWorld(Settings.worldName).getGenerator() == null) {
                    // Check if the world generator is registered correctly
                    getLogger().severe("********* The Generator for " + plugin.getName() + " is not registered so the plugin cannot start ********");
                    getLogger().severe("If you are using your own generator or server.properties, set useowngenerator: true in config.yml");
                    getLogger().severe("Otherwise:");
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
                    HandlerList.unregisterAll(plugin);
                    return;
                }
                // Try to register Herochat
                if (Bukkit.getServer().getPluginManager().isPluginEnabled("Herochat")) {
                    try {
                        getServer().getPluginManager().registerEvents(new HeroChatListener(plugin), plugin);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not register with Herochat");
                    }
                }
                // Run these one tick later to ensure worlds are loaded.
                getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        // load the list - order matters - grid first, then top
                        // ten to optimize upgrades
                        // Load grid
                        if (grid == null) {
                            grid = new GridManager(plugin);
                        }
                        // Register events
                        registerEvents();

                        // Load TinyDb
                        if (tinyDB == null) {
                            tinyDB = new TinyDB(plugin);
                        }
                        // Load warps
                        getWarpSignsListener().loadWarpList();
                        // Load the warp panel
                        if (Settings.useWarpPanel) {
                            warpPanel = new WarpPanel(plugin);
                            getServer().getPluginManager().registerEvents(warpPanel, plugin);
                        }						
                        // Load the TopTen GUI
                        if (!Settings.displayIslandTopTenInChat){
                            topTen = new TopTen(plugin);
                            getServer().getPluginManager().registerEvents(topTen, plugin);
                        }
                        // Minishop - must wait for economy to load before we can use
                        // econ
                        getServer().getPluginManager().registerEvents(new ControlPanel(plugin), plugin);
                        // Settings
                        settingsPanel = new SettingsPanel(plugin);
                        getServer().getPluginManager().registerEvents(settingsPanel, plugin);
                        // Biomes
                        // Load Biomes
                        biomes = new BiomesPanel(plugin);
                        getServer().getPluginManager().registerEvents(biomes, plugin);

                        TopTen.topTenLoad();

                        // Add any online players to the DB
                        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                            tinyDB.savePlayerName(onlinePlayer.getName(), onlinePlayer.getUniqueId());
                        }
                        if (Settings.backupDuration > 0) {
                            new AsyncBackup(plugin);
                        }
                        // Load the coops
                        if (Settings.persistantCoops) {
                            CoopPlay.getInstance().loadCoops();
                        }
                        getLogger().info("All files loaded. Ready to play...");
                        // Fire event
                        getServer().getPluginManager().callEvent(new ReadyEvent());
                    }
                });
                // Check for updates asynchronously
                if (Settings.updateCheck) {
                    checkUpdates();
                    new BukkitRunnable() {
                        int count = 0;
                        @Override
                        public void run() {
                            if (count++ > 20) {
                                plugin.getLogger().info("No updates found. (No response from server after 20s)");
                                this.cancel();
                            } else {
                                // Wait for the response
                                if (updateCheck != null) {
                                    switch (updateCheck.getResult()) {
                                    case DISABLED:
                                        plugin.getLogger().info("Updating has been disabled");
                                        break;
                                    case FAIL_APIKEY:
                                        plugin.getLogger().info("API key failed");
                                        break;
                                    case FAIL_BADID:
                                        plugin.getLogger().info("Bad ID");
                                        break;
                                    case FAIL_DBO:
                                        plugin.getLogger().info("Could not connect to updating service");
                                        break;
                                    case FAIL_DOWNLOAD:
                                        plugin.getLogger().info("Downloading failed");
                                        break;
                                    case FAIL_NOVERSION:
                                        plugin.getLogger().info("Could not recognize version");
                                        break;
                                    case NO_UPDATE:
                                        plugin.getLogger().info("No update available.");
                                        break;
                                    case SUCCESS:
                                        plugin.getLogger().info("Success!");
                                        break;
                                    case UPDATE_AVAILABLE:
                                        plugin.getLogger().info("Update available " + updateCheck.getLatestName());
                                        break;
                                    default:
                                        break;

                                    }
                                    this.cancel();
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L); // Check status every second
                }
                
                // Run acid tasks
                acidTask = new AcidTask(plugin);

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
                    updateCheck = new Updater(plugin, 85189, plugin.getFile(), UpdateType.NO_DOWNLOAD, true); // ASkyBlock
                } else {
                    updateCheck = new Updater(plugin, 80095, plugin.getFile(), UpdateType.NO_DOWNLOAD, true); // AcidIsland
                }                
            }
        });
    }

    public void checkUpdatesNotify(Player p) {
        if (updateCheck != null) {
            if (updateCheck.getResult().equals(UpdateResult.UPDATE_AVAILABLE)) {
                // Player login
                p.sendMessage(ChatColor.GOLD + updateCheck.getLatestName() + " is available! You are running V" + getDescription().getVersion());
                p.sendMessage(ChatColor.RED + "Update at:");
                p.sendMessage(ChatColor.RED + getUpdateCheck().getLatestFileLink());
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
        //getLogger().info("DEBUG: deleting player island");
        CoopPlay.getInstance().clearAllIslandCoops(player);
        getWarpSignsListener().removeWarp(player);
        Island island = grid.getIsland(player);
        if (island != null) {
            if (removeBlocks) {
                grid.removePlayersFromIsland(island, player);
                new DeleteIslandChunk(this, island);
                //new DeleteIslandByBlock(this, island);
            } else {
                island.setLocked(false);
                grid.setIslandOwner(island, null);
            }
            getServer().getPluginManager().callEvent(new IslandDeleteEvent(player, island.getCenter()));
        } else {
            getLogger().severe("Could not delete player: " + player.toString() + " island!");
            getServer().getPluginManager().callEvent(new IslandDeleteEvent(player, null));
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
        /*
	if (challenges == null) {
	    challenges = new Challenges(this);
	}*/
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
        /*
	if (grid == null) {
	    grid = new GridManager(this);
	}*/
        return grid;
    }

    /**
     * @return the players
     */
    public PlayerCache getPlayers() {
        /*
	if (players == null) {
	    players = new PlayerCache(this);
	}*/
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
    public Updater getUpdateCheck() {
        return updateCheck;
    }

    /**
     * @param updateCheck the updateCheck to set
     */
    public void setUpdateCheck(Updater updateCheck) {
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
        // Look in the locale folder. If it is not there, then 
        //getLocale();
        // Add this to the config      
        FileLister fl = new FileLister(this);
        try {
            int index = 1;
            for (String code: fl.list()) {
                availableLocales.put(code, new ASLocale(this,code, index++)); 
            }
        } catch (IOException e1) {
            getLogger().severe("Could not add locales!");
        }
        // Default is locale.yml
        availableLocales.put("locale", new ASLocale(this, "locale", 0));
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
        // Recover superflat
        Settings.recoverSuperFlat = getConfig().getBoolean("general.recoversuperflat");
        if (Settings.recoverSuperFlat) {
            getLogger().warning("*********************************************************");
            getLogger().warning("WARNING: Recover super flat mode is enabled");
            getLogger().warning("This will regenerate any chunks with bedrock at y=0 when they are loaded");
            getLogger().warning("Switch off when superflat chunks are cleared");
            getLogger().warning("You should back up your world before running this");
            getLogger().warning("*********************************************************");
        }
        // Destroy items in acid timer
        Settings.acidItemDestroyTime = getConfig().getLong("general.itemdestroyafter",0L) * 20L;
        // Hack skeleton spawners for 1.11
        Settings.hackSkeletonSpawners = getConfig().getBoolean("schematicsection.hackskeletonspawners", true);
        // Allow Obsidian Scooping
        Settings.allowObsidianScooping = getConfig().getBoolean("general.allowobsidianscooping", true);
        // Kicked players keep inventory
        Settings.kickedKeepInv = getConfig().getBoolean("general.kickedkeepinv", false);
        // Chat prefixes
        Settings.chatLevelPrefix = getConfig().getString("general.chatlevelprefix","{ISLAND_LEVEL}");
        Settings.chatChallengeLevelPrefix = getConfig().getString("general.chatchallanegelevelprefix","{ISLAND_CHALLENGE_LEVEL}");
        Settings.chatIslandPlayer = getConfig().getString("general.chatislandplayer","{ISLAND_PLAYER}");
        // Nether roof option
        Settings.netherRoof = getConfig().getBoolean("general.netherroof", true);
        // FTB Autoamtic Activators
        Settings.allowAutoActivator = getConfig().getBoolean("general.autoactivator");
        // Debug
        Settings.debug = getConfig().getInt("debug", 0);
        // Persistent coops
        Settings.persistantCoops = getConfig().getBoolean("general.persistentcoops");
        // Level logging
        Settings.levelLogging = getConfig().getBoolean("general.levellogging");
        // Allow pushing
        Settings.allowPushing = getConfig().getBoolean("general.allowpushing", true);
        // try to remove the team from the scoreboard
        if (Settings.allowPushing) {
            try {
                Scoreboard scoreboard = getServer().getScoreboardManager().getMainScoreboard();
                if (scoreboard != null) {
                    Team pTeam = scoreboard.getTeam(NO_PUSH_TEAM_NAME);
                    if (pTeam != null) {
                        pTeam.unregister();
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Problem removing no push from scoreboard.");
            }
        }
        // Custom generator
        Settings.useOwnGenerator = getConfig().getBoolean("general.useowngenerator", false);
        // How often the grid will be saved to file. Default is 5 minutes
        Settings.backupDuration = (getConfig().getLong("general.backupduration", 5) * 20 * 60);
        // How long a player has to wait after deactivating PVP until they can activate PVP again
        Settings.pvpRestartCooldown = getConfig().getLong("general.pvpcooldown",60);
        // Max Islands
        Settings.maxIslands = getConfig().getInt("general.maxIslands",0);
        // Mute death messages
        Settings.muteDeathMessages = getConfig().getBoolean("general.mutedeathmessages", false);
        // Warp Restriction
        Settings.warpLevelsRestriction = getConfig().getInt("general.warplevelrestriction", 10);
        // Warp panel
        Settings.useWarpPanel = getConfig().getBoolean("general.usewarppanel", true);
        // Fast level calculation (this is really fast)
        Settings.fastLevelCalc = getConfig().getBoolean("general.fastlevelcalc", true);
        // Restrict wither
        Settings.restrictWither = getConfig().getBoolean("general.restrictwither", true);
        // Team chat
        Settings.teamChat = getConfig().getBoolean("general.teamchat", true);
        Settings.logTeamChat = getConfig().getBoolean("general.logteamchat", true);
        // TEAMSUFFIX as island level
        Settings.setTeamName = getConfig().getBoolean("general.setteamsuffix", false);
        Settings.teamSuffix = getConfig().getString("general.teamsuffix","([level])");
        // Immediate teleport
        Settings.immediateTeleport = getConfig().getBoolean("general.immediateteleport", false);
        // Make island automatically
        Settings.makeIslandIfNone = getConfig().getBoolean("general.makeislandifnone", false);
        // Use physics when pasting island block schematics
        Settings.usePhysics = getConfig().getBoolean("general.usephysics", false);
        // Use old display (chat instead of GUI) for Island top ten
        Settings.displayIslandTopTenInChat = getConfig().getBoolean("general.islandtopteninchat", false);
        // Run level calc at login
        Settings.loginLevel = getConfig().getBoolean("general.loginlevel", false);
        // Use economy or not
        // In future expand to include internal economy
        Settings.useEconomy = getConfig().getBoolean("general.useeconomy", true);
        // Use the minishop or not
        Settings.useMinishop = getConfig().getBoolean("general.useminishop", true);
        // Check for updates
        Settings.updateCheck = getConfig().getBoolean("general.checkupdates", true);
        // Island reset commands
        Settings.resetCommands = getConfig().getStringList("general.resetcommands");
        Settings.leaveCommands = getConfig().getStringList("general.leavecommands");
        Settings.startCommands = getConfig().getStringList("general.startcommands");
        Settings.teamStartCommands = getConfig().getStringList("general.teamstartcommands");
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
        // Flymode expiration while flying oustide island boundaries
        Settings.flyTimeOutside = getConfig().getInt("island.flytimeoutside", 0);
        if(Settings.flyTimeOutside < 0) {
            Settings.flyTimeOutside = 0;
        }
	
	// Temporary Permissions while inside island
	Settings.temporaryPermissions = getConfig().getStringList("island.islandtemporaryperms");
	
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
        Settings.islandCompanion = null;
        if (!companion.equalsIgnoreCase("NOTHING")) {
            String commaList = "NOTHING, ";
            for (EntityType type : EntityType.values()) {
                if (companion.equalsIgnoreCase(type.toString())) {
                    Settings.islandCompanion = type;
                    break;
                }
                commaList += ", " + type.toString();
            }
            if (Settings.islandCompanion == null) {
                getLogger().warning("Island companion is not recognized. Pick from " + commaList);
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
        Settings.island_protectionRange = getConfig().getInt("island.protectionRange", 100);
        if (Settings.island_protectionRange % 2 != 0) {
            Settings.island_protectionRange--;
            getLogger().warning("Protection range must be even, using " + Settings.island_protectionRange);
        }
        if (Settings.island_protectionRange > Settings.islandDistance) {
            getLogger().warning("Protection range cannot be > island distance. Setting them to be equal.");
            Settings.island_protectionRange = Settings.islandDistance;
        }
        if (Settings.island_protectionRange < 0) {
            Settings.island_protectionRange = 0;
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
        if (Settings.animalSpawnLimit < -1) {
            Settings.animalSpawnLimit = -1;
        }

        Settings.monsterSpawnLimit = getConfig().getInt("general.monsterspawnlimit", 100);
        if (Settings.monsterSpawnLimit < -1) {
            Settings.monsterSpawnLimit = -1;
        }

        Settings.waterAnimalSpawnLimit = getConfig().getInt("general.wateranimalspawnlimit", 15);
        if (Settings.waterAnimalSpawnLimit < -1) {
            Settings.waterAnimalSpawnLimit = -1;
        }

        Settings.abandonedIslandLevel = getConfig().getInt("general.abandonedislandlevel", 10);
        if (Settings.abandonedIslandLevel < 0) {
            Settings.abandonedIslandLevel = 0;
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
        // Invincible visitors
        Settings.invincibleVisitors = getConfig().getBoolean("general.invinciblevisitors", false);
        if(Settings.invincibleVisitors){
            Settings.visitorDamagePrevention = new HashSet<DamageCause>();
            List<String> damageSettings = getConfig().getStringList("general.invinciblevisitorsoptions");
            for (DamageCause cause: DamageCause.values()) {
                if (damageSettings.contains(cause.toString())) {
                    Settings.visitorDamagePrevention.add(cause);
                }
            }
        }

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
        String chestItems = getConfig().getString("island.chestItems","");
        if (!chestItems.isEmpty()) {
            final String[] chestItemString = chestItems.split(" ");
            // getLogger().info("DEBUG: chest items = " + chestItemString);
            final ItemStack[] tempChest = new ItemStack[chestItemString.length];
            for (int i = 0; i < tempChest.length; i++) {
                String[] amountdata = chestItemString[i].split(":");
                try {
                    if (amountdata.length == 3 && amountdata[0].equalsIgnoreCase("MONSTER_EGG")) {
                        try {
                            EntityType type = EntityType.valueOf(amountdata[1].toUpperCase());
                            if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                                tempChest[i] = new SpawnEgg(type).toItemStack(Integer.parseInt(amountdata[2]));
                            } else {
                                try {
                                    tempChest[i] = new SpawnEgg1_9(type).toItemStack(Integer.parseInt(amountdata[2]));
                                } catch (Exception ex) {
                                    tempChest[i] = new ItemStack(Material.MONSTER_EGG);
                                    plugin.getLogger().severe("Monster eggs not supported with this server version.");
                                }
                            }
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Spawn eggs must be described by name. Try one of these (not all are possible):");
                            for (EntityType type : EntityType.values()) {
                                if (type.isSpawnable() && type.isAlive()) {
                                    plugin.getLogger().severe(type.toString());
                                }
                            }
                        }
                    } else if (amountdata[0].equals("POTION")) {
                        // getLogger().info("DEBUG: Potion length " +
                        // amountdata.length);
                        if (amountdata.length == 6) {
                            tempChest[i] = Challenges.getPotion(amountdata, Integer.parseInt(amountdata[5]), "config.yml");
                        } else {
                            getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
                            getLogger().severe("Potions for the chest must be fully defined as POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY");
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
                    ex.printStackTrace();
                    getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
                    getLogger().severe("Error is : " + ex.getMessage());
                    getLogger().info("Potential potion types are: ");
                    for (PotionType c : PotionType.values())
                        getLogger().info(c.name());
                } catch (Exception e) {
                    e.printStackTrace();
                    getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
                    getLogger().info("Potential material types are: ");
                    for (Material c : Material.values())
                        getLogger().info(c.name());
                    // e.printStackTrace();
                }
            }
            Settings.chestItems = tempChest;
        } else {
            // Nothing in the chest
            Settings.chestItems = new ItemStack[0];
        }
        // System settings
        Settings.endermanDeathDrop = getConfig().getBoolean("island.endermandeathdrop", true);
        Settings.allowEndermanGriefing = getConfig().getBoolean("island.allowendermangriefing", true);
        Settings.allowCreeperDamage = getConfig().getBoolean("island.allowcreeperdamage", true);
        Settings.allowCreeperGriefing = getConfig().getBoolean("island.allowcreepergriefing", false);
        Settings.allowTNTDamage = getConfig().getBoolean("island.allowtntdamage", false);
        Settings.allowMonsterEggs = getConfig().getBoolean("island.allowspawneggs", false);
        Settings.allowFire = getConfig().getBoolean("island.allowfire", false);
        Settings.allowFireSpread = getConfig().getBoolean("island.allowfirespread", false);
        Settings.allowFireExtinguish = getConfig().getBoolean("island.allowfireextinguish", false);
        Settings.allowChestDamage = getConfig().getBoolean("island.allowchestdamage", false);
        Settings.allowHurtMonsters = getConfig().getBoolean("island.allowhurtmonsters", true);
        Settings.allowVisitorKeepInvOnDeath = getConfig().getBoolean("island.allowvisitorkeepinvondeath", false);
        Settings.allowPistonPush = getConfig().getBoolean("island.allowpistonpush", true);
        Settings.allowMobDamageToItemFrames = getConfig().getBoolean("island.allowitemframedamage", false);

        // Default settings hashmap - make sure this is kept up to date with new settings
        Settings.defaultIslandSettings.clear();
        for (SettingsFlag flag: SettingsFlag.values()) {
            Settings.defaultIslandSettings.put(flag, getConfig().getBoolean("island.settings." + flag.name(), false));
        }
        Settings.spawnSettings.clear();
        for (SettingsFlag flag: SettingsFlag.values()) {
            Settings.spawnSettings.put(flag, getConfig().getBoolean("spawn." + flag.name(), false));
        }
        // Challenges
        getChallenges();
        // Challenge completion
        Settings.broadcastMessages = getConfig().getBoolean("general.broadcastmessages", true);

        // Levels
        // Get the blockvalues.yml file
        YamlConfiguration blockValuesConfig = Util.loadYamlFile("blockvalues.yml");
        // Get the under water multiplier
        Settings.deathpenalty = blockValuesConfig.getInt("deathpenalty", 0);
        Settings.sumTeamDeaths = blockValuesConfig.getBoolean("sumteamdeaths");
        Settings.maxDeaths = blockValuesConfig.getInt("maxdeaths", 10);
        Settings.islandResetDeathReset = blockValuesConfig.getBoolean("islandresetdeathreset", true);
        Settings.teamJoinDeathReset = blockValuesConfig.getBoolean("teamjoindeathreset", true);
        Settings.underWaterMultiplier = blockValuesConfig.getDouble("underwater", 1D);
        Settings.levelCost = blockValuesConfig.getInt("levelcost", 100);
        if (Settings.levelCost < 1) {
            Settings.levelCost = 1;
            getLogger().warning("levelcost in blockvalues.yml cannot be less than 1. Setting to 1.");
        }
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
        Settings.limitedBlocks = new HashMap<String,Integer>();
        Settings.entityLimits = new HashMap<EntityType, Integer>();
        ConfigurationSection entityLimits = getConfig().getConfigurationSection("general.entitylimits");
        if (entityLimits != null) {
            for (String entity: entityLimits.getKeys(false)) {
                int limit = entityLimits.getInt(entity.toUpperCase(), -1);
                if (limit > 0) {
                    getLogger().info(entity.toUpperCase() + " will be limited to " + limit);
                }
                if (Material.getMaterial(entity.toUpperCase()) == null) {
                    // Check if this is a living entity
                    EntityType type = EntityType.valueOf(entity.toUpperCase());
                    if (type != null && type.isAlive()) {
                        Settings.entityLimits.put(type, limit);
                    } else {
                        getLogger().warning("general.entitylimits section has unknown entity type: " + entity.toUpperCase() + " skipping...");
                    }
                } else if (limit > -1) {
                    Settings.limitedBlocks.put(entity.toUpperCase(), limit);
                    if (entity.equalsIgnoreCase("REDSTONE_COMPARATOR")) {
                        // Player can only ever place a redstone comparator in the OFF state
                        Settings.limitedBlocks.put("REDSTONE_COMPARATOR_OFF", limit);
                    } else if (entity.equalsIgnoreCase("BANNER")) {
                        // To simplify banners, the banner is allowed and automatically made wall and standing banner
                        Settings.limitedBlocks.put("WALL_BANNER", limit);
                        Settings.limitedBlocks.put("STANDING_BANNER", limit);
                    } else if (entity.equalsIgnoreCase("SIGN")) {
                        // To simplify signs, the sign is allowed and automatically made wall and standing signs
                        Settings.limitedBlocks.put("WALL_SIGN", limit);
                        Settings.limitedBlocks.put("SIGN_POST", limit);
                    }
                }
            }
        }
        // Legacy setting support for hopper limiting
        if (Settings.limitedBlocks.isEmpty()) {
            Settings.hopperLimit = getConfig().getInt("general.hopperlimit", -1);
            if (Settings.hopperLimit > 0) {
                Settings.limitedBlocks.put("HOPPER", Settings.hopperLimit);
            }
        }
        Settings.mobLimit = getConfig().getInt("general.moblimit", 0);
        Settings.removeCompleteOntimeChallenges = getConfig().getBoolean("general.removecompleteonetimechallenges", false);
        Settings.addCompletedGlow = getConfig().getBoolean("general.addcompletedglow", true);
        // Clean up blocks around edges when deleting islands
        Settings.cleanRate = getConfig().getInt("island.cleanrate", 2);
        if (Settings.cleanRate < 1) {
            Settings.cleanRate = 1;
        }
        // No acid bottles or buckets
        Settings.acidBottle = getConfig().getBoolean("general.acidbottles", true);
        // Island name length
        Settings.minNameLength = getConfig().getInt("island.minnamelength", 1);
        Settings.maxNameLength = getConfig().getInt("island.maxnamelength", 20);
        if (Settings.minNameLength < 0) {
            Settings.minNameLength = 0;
        }
        if (Settings.maxNameLength < 1) {
            Settings.maxNameLength = 1;
        }
        if (Settings.minNameLength > Settings.maxNameLength) {
            Settings.minNameLength = Settings.maxNameLength;
        }
        // Magic Cobble Generator
        Settings.useMagicCobbleGen = getConfig().getBoolean("general.usemagiccobblegen", false);
        if(Settings.useMagicCobbleGen && getConfig().isSet("general.magiccobblegenchances")){
            //getLogger().info("DEBUG: magic cobble gen enabled and chances section found");
            Settings.magicCobbleGenChances = new TreeMap<Integer, TreeMap<Double,Material>>();
            for(String level : getConfig().getConfigurationSection("general.magiccobblegenchances").getKeys(false)){
                int levelInt = 0;
                try{
                    if(level.equals("default")) {
                        levelInt = Integer.MIN_VALUE;
                    } else {
                        levelInt = Integer.parseInt(level);
                    } 
                    TreeMap<Double,Material> blockMapTree = new TreeMap<Double, Material>();
                    double chanceTotal = 0;
                    for(String block : getConfig().getConfigurationSection("general.magiccobblegenchances." + level).getKeys(false)){
                        double chance = getConfig().getDouble("general.magiccobblegenchances." + level + "." + block, 0D);
                        if(chance > 0 && Material.getMaterial(block) != null && Material.getMaterial(block).isBlock()) {
                            // Store the cumulative chance in the treemap. It does not need to add up to 100%
                            chanceTotal += chance;
                            blockMapTree.put(chanceTotal, Material.getMaterial(block));
                        }
                    }
                    if (!blockMapTree.isEmpty()) {
                        Settings.magicCobbleGenChances.put(levelInt, blockMapTree);
                    }
                } catch(NumberFormatException e){
                    // Putting the catch here means that an invalid level is skipped completely
                    getLogger().severe("Unknown level '" + level + "' listed in magiccobblegenchances section! Must be an integer or 'default'. Skipping...");
                }
            }
        }
        // Disable offline redstone
        Settings.disableOfflineRedstone = getConfig().getBoolean("general.disableofflineredstone", false);

        // Fancy island level display
        Settings.fancyIslandLevelDisplay = getConfig().getBoolean("general.fancylevelinchat", false);

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
        // Nether spawning events
        manager.registerEvents(new NetherSpawning(this), this);
        // Island Protection events
        manager.registerEvents(new IslandGuard(this), this);
        // Player events
        manager.registerEvents(new PlayerEvents(this), this);
        // New V1.8 events
        if (onePointEight) {
            manager.registerEvents(new IslandGuard1_8(this), this);
        }
        // Check for 1.9 material
        for (Material m : Material.values()) {
            if (m.name().equalsIgnoreCase("END_CRYSTAL")) {
                manager.registerEvents(new IslandGuard1_9(this), this);
                break;
            }
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
        //manager.registerEvents(new AcidInventory(this), this);
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
            manager.registerEvents(new FlyingMobEvents(this), this);
        }
        if (Settings.recoverSuperFlat) {
            manager.registerEvents(new CleanSuperFlat(), this);
        }
        // Entity limits
        manager.registerEvents(new EntitySpawning(this), this);
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
        if (!player.isOp()) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        if (Settings.resetChallenges) {
            // Reset the player's challenge status
            players.resetAllChallenges(player.getUniqueId(), false);
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
            if (Settings.useOwnGenerator) {
                return Bukkit.getServer().getWorld(Settings.worldName +"_nether");
            }
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
    public ASLocale myLocale(UUID player) {
        String locale = players.getLocale(player);
        if (locale.isEmpty() || !availableLocales.containsKey(locale)) {
            return availableLocales.get("locale");
        }
        return availableLocales.get(locale);
    }

    /**
     * @return System locale
     */
    public ASLocale myLocale() {
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

    /**
     * @return the settingsPanel
     */
    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }

    /**
     * @return the availableLocales
     */
    public HashMap<String, ASLocale> getAvailableLocales() {
        return availableLocales;
    }

    /**
     * @return the acidTask
     */
    public AcidTask getAcidTask() {
        return acidTask;
    }
}
