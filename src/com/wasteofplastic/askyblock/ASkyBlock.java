/*******************************************************************************
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.NotSetup.Reason;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.askyblock.Updater.UpdateResult;
import com.wasteofplastic.askyblock.Updater.UpdateType;
import com.wasteofplastic.askyblock.commands.AdminCmd;
import com.wasteofplastic.askyblock.commands.Challenges;
import com.wasteofplastic.askyblock.commands.IslandCmd;
import com.wasteofplastic.askyblock.events.IslandPreDeleteEvent;
import com.wasteofplastic.askyblock.events.IslandDeleteEvent;
import com.wasteofplastic.askyblock.events.ReadyEvent;
import com.wasteofplastic.askyblock.generators.ChunkGeneratorWorld;
import com.wasteofplastic.askyblock.listeners.AcidEffect;
import com.wasteofplastic.askyblock.listeners.ChatListener;
import com.wasteofplastic.askyblock.listeners.CleanSuperFlat;
import com.wasteofplastic.askyblock.listeners.EntityLimits;
import com.wasteofplastic.askyblock.listeners.FlyingMobEvents;
import com.wasteofplastic.askyblock.listeners.IslandGuard;
import com.wasteofplastic.askyblock.listeners.IslandGuard1_8;
import com.wasteofplastic.askyblock.listeners.IslandGuard1_9;
import com.wasteofplastic.askyblock.listeners.JoinLeaveEvents;
import com.wasteofplastic.askyblock.listeners.LavaCheck;
import com.wasteofplastic.askyblock.listeners.NetherPortals;
import com.wasteofplastic.askyblock.listeners.NetherSpawning;
import com.wasteofplastic.askyblock.listeners.PlayerEvents;
import com.wasteofplastic.askyblock.listeners.PlayerEvents2;
import com.wasteofplastic.askyblock.listeners.PlayerEvents3;
import com.wasteofplastic.askyblock.listeners.WorldEnter;
import com.wasteofplastic.askyblock.listeners.WorldLoader;
import com.wasteofplastic.askyblock.panels.BiomesPanel;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.panels.SchematicsPanel;
import com.wasteofplastic.askyblock.panels.SettingsPanel;
import com.wasteofplastic.askyblock.panels.WarpPanel;
import com.wasteofplastic.askyblock.util.HeadGetter;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Main ASkyBlock class - provides an island minigame in a sea of acid
 */
public class ASkyBlock extends JavaPlugin {
    private static final boolean DEBUG = false;
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
    private AcidTask acidTask;

    // Player events listener
    private PlayerEvents playerEvents;

    // Metrics
    private Metrics metrics;

    // Localization Strings
    private Map<String,ASLocale> availableLocales = new HashMap<>();

    // Head getter
    private HeadGetter headGetter;
    private EntityLimits entityLimits;

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
                // Run sync
                if (!Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTask(plugin, ASkyBlock::registerMultiverse);
                } else {
                    registerMultiverse();
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

    private static void registerMultiverse() {
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
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
        }
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
                warpSignsListener.saveWarpList(false);
            }
            if (messages != null) {
                messages.saveMessages(false);
            }
            if (topTen != null) {
                topTen.topTenSave();
            }
            // Close the name database
            if (tinyDB != null) {
                tinyDB.saveDB();
            }
            // Save the coops
            CoopPlay.getInstance().saveCoops();
            // Remove temporary perms
            if (playerEvents != null) {
                playerEvents.removeAllTempPerms();
            }
            // Save entitiy limits
            if (entityLimits != null) {
                entityLimits.disable();
            }
        } catch (final Exception e) {
            getLogger().severe("Something went wrong saving files!");
            e.printStackTrace();
        }

        metrics = null;
    }

    /*
     * (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        // instance of this plugin
        plugin = this;
        // Initialize the API
        new ASkyBlockAPI(this);
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
        if (!PluginConfig.loadPluginConfig(this)) {
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
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: Setting up player cache");
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
        if (getServer().getVersion().contains("(MC: 1.8") || getServer().getVersion().contains("(MC: 1.7")) {
            getServer().getPluginManager().registerEvents(new WorldLoader(this), this);
        }
        // Metrics
        metrics = new Metrics(this);

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
                    getLogger().severe("********* The Generator for " + ASkyBlock.this.getName() + " is not registered so the plugin cannot start ********");
                    getLogger().severe("If you are using your own generator or server.properties, set useowngenerator: true in config.yml");
                    getLogger().severe("Otherwise:");
                    getLogger().severe("Make sure you have the following in bukkit.yml (case sensitive):");
                    getLogger().severe("worlds:");
                    getLogger().severe("  # The next line must be the name of your world:");
                    getLogger().severe("  " + Settings.worldName + ":");
                    getLogger().severe("    generator: " + ASkyBlock.this.getName());
                    if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
                        getCommand("island").setExecutor(new NotSetup(Reason.GENERATOR));
                        getCommand("asc").setExecutor(new NotSetup(Reason.GENERATOR));
                        getCommand("asadmin").setExecutor(new NotSetup(Reason.GENERATOR));
                    } else {
                        getCommand("ai").setExecutor(new NotSetup(Reason.GENERATOR));
                        getCommand("aic").setExecutor(new NotSetup(Reason.GENERATOR));
                        getCommand("acid").setExecutor(new NotSetup(Reason.GENERATOR));
                    }
                    HandlerList.unregisterAll(ASkyBlock.this);
                    return;
                }

                // Run game rule to keep things quiet
                if (Settings.silenceCommandFeedback){
                    try {
                        getLogger().info("Silencing command feedback for Ops...");
                        getServer().dispatchCommand(getServer().getConsoleSender(), "minecraft:gamerule sendCommandFeedback false");
                        getLogger().info("If you do not want this, do /gamerule sendCommandFeedback true");
                    } catch (Exception ignored) {} // do nothing
                }

                // Run these one tick later to ensure worlds are loaded.
                getServer().getScheduler().runTask(ASkyBlock.this, () -> {
                    // load the list - order matters - grid first, then top
                    // ten to optimize upgrades
                    // Load grid
                    if (grid == null) {
                        grid = new GridManager(ASkyBlock.this);
                    }
                    // Register events
                    registerEvents();

                    // Load TinyDb
                    if (tinyDB == null) {
                        tinyDB = new TinyDB(ASkyBlock.this);
                    }
                    // Run head getter
                    headGetter = new HeadGetter(plugin);

                    // Load warps
                    getWarpSignsListener().loadWarpList();
                    // Load the warp panel
                    if (Settings.useWarpPanel) {
                        plugin.getLogger().info("Loading warp panel...");
                        warpPanel = new WarpPanel(ASkyBlock.this);
                        getServer().getPluginManager().registerEvents(warpPanel, ASkyBlock.this);
                    }
                    topTen = new TopTen(ASkyBlock.this);
                    // Load the TopTen GUI
                    if (!Settings.displayIslandTopTenInChat){
                        getServer().getPluginManager().registerEvents(topTen, ASkyBlock.this);
                    }
                    // Minishop - must wait for economy to load before we can use
                    // econ
                    getServer().getPluginManager().registerEvents(new ControlPanel(ASkyBlock.this), ASkyBlock.this);
                    // Settings
                    settingsPanel = new SettingsPanel(ASkyBlock.this);
                    getServer().getPluginManager().registerEvents(settingsPanel, ASkyBlock.this);
                    // Biomes
                    // Load Biomes
                    biomes = new BiomesPanel(ASkyBlock.this);
                    getServer().getPluginManager().registerEvents(biomes, ASkyBlock.this);

                    // Add any online players to the DB
                    for (Player onlinePlayer : ASkyBlock.this.getServer().getOnlinePlayers()) {
                        tinyDB.savePlayerName(onlinePlayer.getName(), onlinePlayer.getUniqueId());
                    }
                    if (Settings.backupDuration > 0) {
                        new AsyncBackup(ASkyBlock.this);
                    }
                    // Load the coops
                    if (Settings.persistantCoops) {
                        CoopPlay.getInstance().loadCoops();
                    }
                    // Give temp permissions
                    playerEvents.giveAllTempPerms();

                    getLogger().info("All files loaded. Ready to play...");

                    registerCustomCharts();
                    getLogger().info("Metrics loaded.");

                    // Fire event
                    getServer().getPluginManager().callEvent(new ReadyEvent());
                });
                // Check for updates asynchronously
                if (Settings.updateCheck) {
                    checkUpdates();
                    new BukkitRunnable() {
                        int count = 0;
                        @Override
                        public void run() {
                            if (count++ > 20) {
                                ASkyBlock.this.getLogger().info("No updates found. (No response from server after 20s)");
                                this.cancel();
                            } else {
                                // Wait for the response
                                if (updateCheck != null) {
                                    switch (updateCheck.getResult()) {
                                    case DISABLED:
                                        ASkyBlock.this.getLogger().info("Updating has been disabled");
                                        break;
                                    case FAIL_APIKEY:
                                        ASkyBlock.this.getLogger().info("API key failed");
                                        break;
                                    case FAIL_BADID:
                                        ASkyBlock.this.getLogger().info("Bad ID");
                                        break;
                                    case FAIL_DBO:
                                        ASkyBlock.this.getLogger().info("Could not connect to updating service");
                                        break;
                                    case FAIL_DOWNLOAD:
                                        ASkyBlock.this.getLogger().info("Downloading failed");
                                        break;
                                    case FAIL_NOVERSION:
                                        ASkyBlock.this.getLogger().info("Could not recognize version");
                                        break;
                                    case NO_UPDATE:
                                        ASkyBlock.this.getLogger().info("No update available.");
                                        break;
                                    case SUCCESS:
                                        ASkyBlock.this.getLogger().info("Success!");
                                        break;
                                    case UPDATE_AVAILABLE:
                                        ASkyBlock.this.getLogger().info("Update available " + updateCheck.getLatestName());
                                        break;
                                    default:
                                        break;

                                    }
                                    this.cancel();
                                }
                            }
                        }
                    }.runTaskTimer(ASkyBlock.this, 0L, 20L); // Check status every second
                }

                // Run acid tasks
                acidTask = new AcidTask(ASkyBlock.this);

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
                    updateCheck = new Updater(ASkyBlock.this, 85189, ASkyBlock.this.getFile(), UpdateType.NO_DOWNLOAD, true); // ASkyBlock
                } else {
                    updateCheck = new Updater(ASkyBlock.this, 80095, ASkyBlock.this.getFile(), UpdateType.NO_DOWNLOAD, true); // AcidIsland
                }                
            }
        });
    }

    public void checkUpdatesNotify(Player p) {
        if (updateCheck != null) {
            if (updateCheck.getResult().equals(UpdateResult.UPDATE_AVAILABLE)) {
                // Player login
                Util.sendMessage(p, ChatColor.GOLD + updateCheck.getLatestName() + " is available! You are running V" + getDescription().getVersion());
                Util.sendMessage(p, ChatColor.RED + "Update at:");
                Util.sendMessage(p, ChatColor.RED + getUpdateCheck().getLatestFileLink());
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
            getServer().getPluginManager().callEvent(new IslandPreDeleteEvent(player, island));
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
     * @return the newIsland
     */
    public boolean isNewIsland() {
        return newIsland;
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
        // Island Entity Limits
        entityLimits = new EntityLimits(this);
        manager.registerEvents(entityLimits, this);
        // Player events
        playerEvents = new PlayerEvents(this);
        manager.registerEvents(playerEvents, this);
        try {
            Class<?> clazz = Class.forName("org.bukkit.event.entity.EntityPickupItemEvent", false, getClassLoader());
            if (clazz != null) {
                manager.registerEvents(new PlayerEvents3(this), this);
            }
        } catch (ClassNotFoundException e) {
            manager.registerEvents(new PlayerEvents2(this), this);
        }
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
    }



    /**
     * Resets a player's inventory, armor slots, equipment, enderchest and
     * potion effects
     *
     * @param player - player
     */
    @SuppressWarnings("deprecation")
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
        topTen.topTenAddEntry(player.getUniqueId(), 0);
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
     * @param player - player
     * @return Locale for this player
     */
    public ASLocale myLocale(UUID player) {
        String locale = players.getLocale(player);
        if (locale.isEmpty() || !availableLocales.containsKey(locale)) {
            return availableLocales.get(Settings.defaultLanguage);
        }
        return availableLocales.get(locale);
    }

    /**
     * @return System locale
     */
    public ASLocale myLocale() {
        return availableLocales.get(Settings.defaultLanguage);
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
    public Map<String, ASLocale> getAvailableLocales() {
        return availableLocales;
    }

    /**
     * @param availableLocales the availableLocales to set
     */
    public void setAvailableLocales(HashMap<String, ASLocale> availableLocales) {
        this.availableLocales = availableLocales;
    }

    /**
     * @return the acidTask
     */
    public AcidTask getAcidTask() {
        return acidTask;
    }

    /**
     * @return the playerEvents
     */
    public PlayerEvents getPlayerEvents() {
        return playerEvents;
    }

    /**
     * Registers the custom charts for Metrics
     */
    public void registerCustomCharts(){
        metrics.addCustomChart(new Metrics.SimplePie("challenges_count", () -> {

            int count = challenges.getAllChallenges().size();
            if(count <= 0) return "0";
            else if(count <= 10) return "1-10";
            else if(count <= 20) return "11-20";
            else if(count <= 30) return "21-30";
            else if(count <= 40) return "31-40";
            else if(count <= 50) return "41-50";
            else if(count <= 75) return "51-75";
            else if(count <= 100) return "76-100";
            else if(count <= 150) return "101-150";
            else if(count <= 200) return "151-200";
            else if(count <= 300) return "201-300";
            else return "300+";
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("islands_count",
            () -> getGrid().getIslandCount()));
    }

    /**
     * @return the headGetter
     */
    public HeadGetter getHeadGetter() {
        return headGetter;
    }

    /**
     * @return the topTen
     */
    public TopTen getTopTen() {
        return topTen;
    }
}
