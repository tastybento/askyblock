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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.askyblock.DeleteIsland.Pair;

/**
 * @author ben
 * Main ASkyBlock class - provides an island minigame in a sea of acid
 */
public class ASkyBlock extends JavaPlugin {
    private boolean newIsland = false;
    // This plugin
    private static ASkyBlock plugin;
    // The ASkyBlock world
    public static World acidWorld = null;
    // Player YAMLs
    public YamlConfiguration playerFile;
    public File playersFolder;
    // Where challenges are stored
    private FileConfiguration challengeFile = null;
    private File challengeConfigFile = null;
    private Challenges challenges;
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    // Where warps are stored
    public YamlConfiguration welcomeWarps;
    // Map of all warps stored as player, warp sign Location
    private HashMap<UUID, Object> warpList = new HashMap<UUID, Object>();
    // Top ten list of players
    private Map<UUID, Integer> topTenList;
    // Players object
    private PlayerCache players;
    // Acid Damage Potion
    PotionEffectType acidPotion;
    // Listeners
    private Listener warpSignsListener;
    private Listener lavaListener;
    // Spawn object
    Spawn spawn;
    // A set of falling players
    HashSet<UUID> fallingPlayers = new HashSet<UUID>();
    // Biome chooser object
    Biomes biomes;

    public boolean debug = false;
    public boolean flag = false;

    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;


    /**
     * @return ASkyBlock object instance
     */
    public static ASkyBlock getPlugin() {
	return plugin;
    }

    /**
     * @return the challenges
     */
    public Challenges getChallenges() {
	if (challenges == null) {
	    challenges = new Challenges(this, getPlayers());
	}
	return challenges;
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
     * Returns the World object for the Acid Island world named in config.yml.
     * If the world does not exist then it is created.
     * 
     * @return islandWorld - Bukkit World object for the ASkyBlock world
     */
    public static World getIslandWorld() {
	if (acidWorld == null) {
	    acidWorld = WorldCreator.name(Settings.worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL)
		    .generator(new AcidChunkGenerator()).createWorld();
	    // Make the nether if it does not exist
	    // Make the nether if it does not exist
	    if (Settings.createNether) {
		if (plugin.getServer().getWorld(Settings.worldName + "_nether") == null) {
		    Bukkit.getLogger().info("Creating ASkyBlock's nether...");
		    WorldCreator.name(Settings.worldName + "_nether").type(WorldType.NORMAL).environment(World.Environment.NETHER).createWorld();
		}
	    }
	}
	// Set world settings
	acidWorld.setWaterAnimalSpawnLimit(Settings.waterAnimalSpawnLimit);
	acidWorld.setMonsterSpawnLimit(Settings.monsterSpawnLimit);
	acidWorld.setAnimalSpawnLimit(Settings.animalSpawnLimit);
	return acidWorld;
    }

    /**
     * Delete Island
     * Called when an island is restarted or reset
     * Uses NMS fast delete if possible
     * @param player - player name String
     */
    public void deletePlayerIsland(final UUID player) {
	// Removes the island
	removeWarp(player);
	removeIsland(players.getIslandLocation(player));
	DeleteIsland deleteIsland = new DeleteIsland(plugin,players.getIslandLocation(player));
	deleteIsland.runTaskTimer(plugin, 40L, 40L);
	players.removeIsland(player);
    }

    /**
     * Displays the Top Ten list if it exists in chat
     * 
     * @param player
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public boolean showTopTen(final Player player) {
	player.sendMessage(ChatColor.GOLD + Locale.topTenheader);
	if (topTenList == null) {
	    updateTopTen();
	    //player.sendMessage(ChatColor.RED + Locale.topTenerrorNotReady);
	    //return true;
	}
	int i = 1;
	for (Map.Entry<UUID, Integer> m : topTenList.entrySet()) {
	    final UUID playerUUID = m.getKey();
	    if (players.inTeam(playerUUID)) {
		final List<UUID> pMembers = players.getMembers(playerUUID);
		String memberList = "";
		for (UUID members : pMembers) {
		    memberList += players.getName(members) + ", ";
		}
		if (memberList.length()>2) {
		    memberList = memberList.substring(0, memberList.length() - 2);
		}
		player.sendMessage(ChatColor.AQUA + "#" + i + ": " + players.getName(playerUUID) + " (" + memberList + ") - " + Locale.levelislandLevel + " "+ m.getValue());
	    } else {
		player.sendMessage(ChatColor.AQUA + "#" + i + ": " + players.getName(playerUUID) + " - " + Locale.levelislandLevel + " " + m.getValue());
	    }
	    if (i++ == 10) {
		break;
	    }
	}
	return true;
    }


    @Override
    public ChunkGenerator getDefaultWorldGenerator(final String worldName, final String id) {
	return new AcidChunkGenerator();
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is empty
     * @param s - serialized location in format "world:x:y:z"
     * @return Location
     */
    static public Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
    }

    /**
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     * @param l
     * @return
     */
    static public String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    /**
     * Determines a safe teleport spot on player's island or the team island
     * they belong to.
     * 
     * @param p
     *            PlayerInfo for active player
     * @return Location of a safe teleport spot
     */
    public Location getSafeHomeLocation(final UUID p) {
	//getLogger().info("DEBUG: getSafeHomeLocation called for " + p.toString());
	// Try home location first
	Location l = players.getHomeLocation(p);
	//getLogger().info("DEBUG: Home location " + l.toString());
	if (l != null) {
	    if (isSafeLocation(l)) {
		return l;
	    }    
	} 
	//getLogger().info("DEBUG: Home location either isn't safe, or does not exist so try the island");
	// Home location either isn't safe, or does not exist so try the island
	// location
	if (players.inTeam(p)) {
	    l = players.getTeamIslandLocation(p);
	    if (isSafeLocation(l)) {
		return l;
	    } else {
		// try team leader's home
		Location tlh = players.getHomeLocation(players.getTeamLeader(p));
		if (tlh != null) {
		    if (isSafeLocation(tlh)) {
			return tlh;
		    }
		}
	    }
	} else {
	    l = players.getIslandLocation(p);
	    if (isSafeLocation(l)) {
		return l;
	    }
	}
	if (l == null) {
	    getLogger().warning(players.getName(p) + " player has no island!");
	    return null;
	}
	//getLogger().info("DEBUG: If these island locations are not safe, then we need to get creative");
	// If these island locations are not safe, then we need to get creative
	// Try the default location
	Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
	if (isSafeLocation(dl)) {
	    players.setHomeLocation(p, dl);
	    return dl;
	}
	// Try just above the bedrock
	dl = new Location(l.getWorld(), l.getX(), l.getY() + 5D, l.getZ(), 0F, 30F);
	if (isSafeLocation(dl)) {
	    players.setHomeLocation(p, dl);
	    return dl;
	}

	// Try higher up - 25 blocks high and then move down
	for (int y = l.getBlockY() + 25; y > 0; y--) {
	    final Location n = new Location(l.getWorld(), l.getBlockX(), y, l.getBlockZ());
	    if (isSafeLocation(n)) {
		return n;
	    }
	}
	// Try all the way up to the sky
	for (int y = l.getBlockY(); y < 255; y++) {
	    final Location n = new Location(l.getWorld(), l.getBlockX(), y, l.getBlockZ());
	    if (isSafeLocation(n)) {
		return n;
	    }
	}
	// Try anywhere in the island area
	// Start from up above and work down
	for (int y = l.getWorld().getMaxHeight(); y>0; y--) {
	    for (int x = l.getBlockX() - Settings.islandDistance/2; x < l.getBlockX() + Settings.islandDistance/2; x++) {
		for (int z = l.getBlockZ() - Settings.islandDistance/2; z < l.getBlockZ() + Settings.islandDistance/2; z++) {
		    Location ultimate = new Location(l.getWorld(),x,y,z);
		    if (!ultimate.getBlock().equals(Material.AIR)) {
			if (isSafeLocation(ultimate)) {
			    players.setHomeLocation(p, ultimate);
			    return ultimate;
			}
		    }
		}
	    }
	}
	// Nothing worked
	return null;
    }



    /**
     * Sets the home location based on where the player is now
     * 
     * @param player
     * @return
     */
    public void homeSet(final Player player) {
	if (playerIsOnIsland(player)) {
	    players.setHomeLocation(player.getUniqueId(),player.getLocation());
	    player.sendMessage(ChatColor.GREEN + Locale.setHomehomeSet);
	} else {
	    player.sendMessage(ChatColor.RED + Locale.setHomeerrorNotOnIsland);
	}
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     * 
     * @param player
     * @return
     */
    public boolean homeTeleport(final Player player) {
	Location home = null;
	home = getSafeHomeLocation(player.getUniqueId());
	// Check if the player is a passenger in a boat
	if (player.isInsideVehicle()) {
	    Entity boat = player.getVehicle();
	    if (boat instanceof Boat) {
		player.leaveVehicle();
		// Remove the boat so they don't lie around everywhere
		boat.remove();
		player.getInventory().addItem(new ItemStack(Material.BOAT, 1));
		player.updateInventory();
	    }
	}
	if (home == null) {
	    // The home is not safe
	    if (!player.performCommand("spawn")) {
		player.teleport(player.getWorld().getSpawnLocation());
	    }
	    player.sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
	    return true;
	}
	//home.getWorld().refreshChunk(home.getChunk().getX(), home.getChunk().getZ());
	player.teleport(home);	
	player.sendMessage(ChatColor.GREEN + Locale.islandteleport);
	return true;
    }

    /**
     * Determines if an island is at a location in this area
     * location. Also checks if the spawn island is in this area.
     * 
     * @param loc
     * @return
     */
    public boolean islandAtLocation(final Location loc) {
	//getLogger().info("DEBUG checking islandAtLocation");
	if (loc == null) {
	    return true;
	}
	// Immediate check
	if (loc.getBlock().getType().equals(Material.BEDROCK)) {
	    return true;
	}
	// Near spawn?
	if ((getSpawn().getSpawnLoc() != null && loc.distanceSquared(getSpawn().getSpawnLoc()) < (double)((double)Settings.islandDistance) * Settings.islandDistance)) {
	    return true;
	}
	// Check the file system
	String checkName = loc.getBlockX() + "," + loc.getBlockZ() + ".yml";
	final File islandFile = new File(plugin.getDataFolder() + File.separator + "islands" + File.separator + checkName);
	if (islandFile.exists()) {
	    return true;
	}
	// Look around
	final int px = loc.getBlockX();
	final int pz = loc.getBlockZ();
	for (int x = -5; x <= 5; x++) {
	    for (int y = 0; y <= 255; y++) {
		for (int z = -5; z <= 5; z++) {
		    if (loc.getWorld().getBlockAt(x + px, y, z + pz).getType().equals(Material.BEDROCK)) {
			return true;
		    }
		}
	    }
	}
	return false;
    }


    /**
     * @return the newIsland
     */
    public boolean isNewIsland() {
	return newIsland;
    }

    /**
     * @param newIsland the newIsland to set
     */
    public void setNewIsland(boolean newIsland) {
	this.newIsland = newIsland;
    }

    /**
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     * 
     * @param l
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public static boolean isSafeLocation(final Location l) {
	if (l == null) {
	    return false;
	}
	final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
	final Block space1 = l.getBlock();
	final Block space2 = l.getBlock().getRelative(BlockFace.UP);
	if (ground.getType().equals(Material.AIR)) {
	    return false;
	}
	// In aSkyblock, liquid maybe unsafe
	if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
	    // Check if acid has no damage
	    if (Settings.acidDamage > 0D) {
		return false;
	    } else if (ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
		    || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
		    || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
		// Lava check only
		return false;
	    }
	}
	if (ground.getType().equals(Material.CACTUS)) {
	    return false;
	} // Ouch - prickly
	if (ground.getType().equals(Material.BOAT)) {
	    return false;
	} // No, I don't want to end up on the boat again
	// Check that the space is not solid
	// The isSolid function is not fully accurate (yet) so we have to check
	// a few other items
	// isSolid thinks that PLATEs and SIGNS are solid, but they are not
	if (space1.getType().isSolid()) {
	    // Do a few other checks
	    if (!(space1.getType().equals(Material.SIGN_POST)) && !(space1.getType().equals(Material.WALL_SIGN))) {
		return false;
	    }
	}
	if (space2.getType().isSolid()) {
	    // Do a few other checks
	    if (!(space2.getType().equals(Material.SIGN_POST)) && !(space2.getType().equals(Material.WALL_SIGN))) {
		return false;
	    }
	}
	// Safe
	return true;
    }


    /**
     * Saves a YAML file
     * 
     * @param yamlFile
     * @param fileLocation
     */
    public static void saveYamlFile(YamlConfiguration yamlFile, String fileLocation) {
	File dataFolder = plugin.getDataFolder();
	File file = new File(dataFolder, fileLocation);

	try {
	    yamlFile.save(file);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Loads a YAML file and if it does not exist it is looked for in the JAR
     * 
     * @param file
     * @return
     */
    public static YamlConfiguration loadYamlFile(String file) {
	File dataFolder = plugin.getDataFolder();
	File yamlFile = new File(dataFolder, file);

	YamlConfiguration config = null;
	if (yamlFile.exists()) {
	    try {
		config = new YamlConfiguration();
		config.load(yamlFile);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    // Create the missing file
	    config = new YamlConfiguration();
	    getPlugin().getLogger().info("No " + file + " found. Creating it...");
	    try {
		if (plugin.getResource(file) != null) {
		    getPlugin().getLogger().info("Using default found in jar file.");
		    plugin.saveResource(file, false);
		    config = new YamlConfiguration();
		    config.load(yamlFile);
		} else {
		    config.save(yamlFile);
		}
	    } catch (Exception e) {
		getPlugin().getLogger().severe("Could not create the " + file + " file!");
	    }
	}
	return config;
    }

    /**
     * Creates the warp list if it does not exist
     */
    public void loadWarpList() {
	getLogger().info("Loading warps...");
	// warpList.clear();
	welcomeWarps = loadYamlFile("warps.yml");
	if (welcomeWarps.getConfigurationSection("warps") == null) {
	    welcomeWarps.createSection("warps"); // This is only used to create
	    // the warp.yml file so forgive
	    // this code
	}
	HashMap<String,Object> temp = (HashMap<String, Object>) welcomeWarps.getConfigurationSection("warps").getValues(true);
	for (String s : temp.keySet()) {
	    try {
		UUID playerUUID = UUID.fromString(s);
		Location l = getLocationString((String)temp.get(s));
		Block b = l.getBlock();   
		// Check that a warp sign is still there
		if (b.getType().equals(Material.SIGN_POST)) {
		    warpList.put(playerUUID, temp.get(s));
		} else {
		    getLogger().warning("Warp at location "+ (String)temp.get(s) + " has no sign - removing.");
		}
	    } catch (Exception e) {
		getLogger().severe("Problem loading warp at location "+ (String)temp.get(s) + " - removing.");
	    }
	}
    }

    /**
     * Saves the warp lists to file
     */
    public void saveWarpList() {
	getLogger().info("Saving warps...");
	final HashMap<String,Object> warps = new HashMap<String,Object>();
	for (UUID p : warpList.keySet()) {
	    warps.put(p.toString(),warpList.get(p));
	}
	welcomeWarps.set("warps", warps);
	saveYamlFile(welcomeWarps, "warps.yml");
    }

    /**
     * Stores warps in the warp array
     * 
     * @param player
     * @param loc
     */
    public boolean addWarp(UUID player, Location loc) {
	final String locS = getStringLocation(loc);
	// Do not allow warps to be in the same location
	if (warpList.containsValue(locS)) {
	    return false;
	}
	// Remove the old warp if it existed
	if (warpList.containsKey(player)) {
	    warpList.remove(player);
	}
	warpList.put(player, locS);
	saveWarpList();
	return true;
    }

    /**
     * Removes a warp when the welcome sign is destroyed. Called by
     * WarpSigns.java.
     * 
     * @param uuid
     */
    public void removeWarp(UUID uuid) {
	if (warpList.containsKey(uuid)) {
	    popSign(getLocationString((String)warpList.get(uuid)));
	    warpList.remove(uuid);
	}
	saveWarpList();
    }

    private void popSign(Location loc) {
	Block b = loc.getBlock();
	if (b.getType().equals(Material.SIGN_POST)) {
	    Sign s = (Sign) b.getState();
	    if (s != null) {
		if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + Locale.warpswelcomeLine)) {
		    s.setLine(0, ChatColor.RED + Locale.warpswelcomeLine);
		    s.update();
		}
	    }
	}
    }
    /**
     * Removes a warp at a location. Called by WarpSigns.java.
     * 
     * @param loc
     */
    public void removeWarp(Location loc) {
	final String locS = getStringLocation(loc);
	getLogger().info("Asked to remove warp at " + locS);
	popSign(loc);
	if (warpList.containsValue(locS)) {
	    // Step through every key (sigh)
	    List<UUID> playerList = new ArrayList<UUID>();
	    for (UUID player : warpList.keySet()) {
		if (locS.equals(warpList.get(player))) {
		    playerList.add(player);
		}
	    }
	    for (UUID rp : playerList) {
		warpList.remove(rp);
		final Player p = getServer().getPlayer(rp);
		if (p != null) {
		    // Inform the player
		    p.sendMessage(ChatColor.RED + Locale.warpssignRemoved);
		}
		getLogger().warning(rp.toString() + "'s welcome sign at " + loc.toString() + " was removed by something.");
	    }
	} else {
	    getLogger().info("Not in the list which is:");
	    for (UUID player : warpList.keySet()) {
		getLogger().info(player.toString() + "," + warpList.get(player));
	    }

	}
	saveWarpList();
    }

    /**
     * Returns true if the location supplied is a warp location
     * 
     * @param loc
     * @return true if this location has a warp sign, false if not
     */
    public boolean checkWarp(Location loc) {
	final String locS = getStringLocation(loc);
	if (warpList.containsValue(locS)) {
	    return true;
	}
	return false;
    }

    /**
     * Lists all the known warps
     * 
     * @return String set of warps
     */
    public Set<UUID> listWarps() {
	//getLogger().info("DEBUG Warp list count = " + warpList.size());
	return warpList.keySet();
    }

    /**
     * Provides the location of the warp for player
     * 
     * @param player
     *            - the warp requested
     * @return Location of warp
     */
    public Location getWarp(UUID player) {
	if (warpList.containsKey(player)) {
	    return getLocationString((String) warpList.get(player));
	} else {
	    return null;
	}
    }

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public void loadPluginConfig() {
	try {
	    getConfig();
	} catch (final Exception e) {
	    e.printStackTrace();
	}
	// Get the challenges
	getChallengeConfig();
	// Get the localization strings
	getLocale();
	// Assign settings
	// Island reset commands
	Settings.resetCommands = getConfig().getStringList("general.resetcommands");
	Settings.useControlPanel = getConfig().getBoolean("general.usecontrolpanel", false);
	// Check if /island command is allowed when falling
	Settings.allowTeleportWhenFalling = getConfig().getBoolean("general.allowfallingteleport", true);
	// Max team size
	Settings.maxTeamSize = getConfig().getInt("island.maxteamsize",4);
	Settings.maxTeamSizeVIP = getConfig().getInt("island.maxteamsizeVIP",8);
	Settings.maxTeamSizeVIP2 = getConfig().getInt("island.maxteamsizeVIP2",12);
	// Settings from config.yml
	Settings.worldName = getConfig().getString("general.worldName");
	Settings.createNether = getConfig().getBoolean("general.createnether", true);
	if (!Settings.createNether) {
	    getLogger().info("The Nether is disabled");
	}

	Settings.islandDistance = getConfig().getInt("island.distance", 110);
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
	Settings.acidDamage = getConfig().getDouble("general.aciddamage", 5D);
	if (Settings.acidDamage > 100D) {
	    Settings.acidDamage = 100D;
	} else if (Settings.acidDamage < 0D) {
	    Settings.acidDamage = 0D;
	}
	Settings.mobAcidDamage = getConfig().getDouble("general.mobaciddamage", 10D);
	if (Settings.acidDamage > 100D) {
	    Settings.acidDamage = 100D;
	} else if (Settings.acidDamage < 0D) {
	    Settings.acidDamage = 0D;
	}
	Settings.rainDamage = getConfig().getDouble("general.raindamage", 0.5D);
	if (Settings.rainDamage > 100D) {
	    Settings.rainDamage = 100D;
	} else if (Settings.rainDamage < 0D) {
	    Settings.rainDamage = 0D;
	}
	// Damage Type
	List<String> acidDamageType = getConfig().getStringList("general.damagetype");
	Settings.acidDamageType.clear();
	if (acidDamageType != null) {
	    for (String effect : acidDamageType) {
		PotionEffectType newPotionType = PotionEffectType.getByName(effect);
		if (newPotionType != null) {
		    // Check if it is a valid addition
		    if (newPotionType.equals(PotionEffectType.BLINDNESS)
			    || newPotionType.equals(PotionEffectType.CONFUSION)
			    || newPotionType.equals(PotionEffectType.HUNGER)
			    || newPotionType.equals(PotionEffectType.POISON)
			    || newPotionType.equals(PotionEffectType.SLOW)
			    || newPotionType.equals(PotionEffectType.SLOW_DIGGING)
			    || newPotionType.equals(PotionEffectType.WEAKNESS)
			    ) {
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
	if (Settings.abandonedIslandLevel<0) {
	    Settings.abandonedIslandLevel = 0;
	}

	Settings.island_protectionRange = getConfig().getInt("island.protectionRange", 100);
	if (Settings.island_protectionRange > Settings.islandDistance) {
	    Settings.island_protectionRange = Settings.islandDistance;
	} else if (Settings.island_protectionRange < 0) {
	    Settings.island_protectionRange = 0;
	}
	Settings.resetChallenges = getConfig().getBoolean("general.resetchallenges", true);
	Settings.resetMoney = getConfig().getBoolean("general.resetmoney", true);
	Settings.clearInventory = getConfig().getBoolean("general.resetinventory", true);

	Settings.startingMoney = getConfig().getDouble("general.startingmoney", 0D);
	// Nether spawn protection radius
	Settings.netherSpawnRadius = getConfig().getInt("general.netherspawnradius",25);
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
	Settings.damageOps = getConfig().getBoolean("general.damageops", false);
	//Settings.ultraSafeBoats = getConfig().getBoolean("general.ultrasafeboats", true);
	Settings.logInRemoveMobs = getConfig().getBoolean("general.loginremovemobs", true);
	Settings.islandRemoveMobs = getConfig().getBoolean("general.islandremovemobs", false);
	// The island's center is actually 5 below sea level
	Settings.sea_level = getConfig().getInt("general.sealevel", 50);
	if (Settings.sea_level < 0) {
	    Settings.sea_level = 0;
	}
	Settings.island_level = getConfig().getInt("general.islandlevel", 50) - 5;
	if (Settings.island_level < 0) {
	    Settings.island_level = 0;
	}
	//getLogger().info("DEBUG: island level is " + Settings.island_level);
	// Get chest items
	final String[] chestItemString = getConfig().getString("island.chestItems").split(" ");
	final ItemStack[] tempChest = new ItemStack[chestItemString.length];
	for (int i = 0; i < tempChest.length; i++) {
	    try {
		String[] amountdata = chestItemString[i].split(":");
		if (amountdata[0].equals("POTION")) {
		    //getLogger().info("DEBUG: Potion length " + amountdata.length);
		    if (amountdata.length == 2) {
			final String chestPotionEffect = getConfig().getString("island.chestPotion","");
			if (!chestPotionEffect.isEmpty()) {
			    // Change the water bottle stack to a potion of some kind
			    Potion chestPotion = new Potion(PotionType.valueOf(chestPotionEffect));
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[1]));
			}
		    }
		    else if (amountdata.length == 3) {
			//getLogger().info("DEBUG: Potion type :" + amountdata[1]);
			Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1]));
			//getLogger().info("Potion in chest is :" + chestPotion.getType().toString() + " x " + amountdata[2]);
			tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[2]));
		    }
		    else if (amountdata.length == 4) {
			// Extended or splash potions
			if (amountdata[2].equals("EXTENDED")) {
			    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).extend();
			    //getLogger().info("Potion in chest is :" + chestPotion.getType().toString() + " extended duration x " + amountdata[3]);
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
			} else if (amountdata[2].equals("SPLASH")) {
			    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).splash();
			    //getLogger().info("Potion in chest is :" + chestPotion.getType().toString() + " splash x " + amountdata[3]);
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
			} else if (amountdata[2].equals("EXTENDEDSPLASH")) {
			    Potion chestPotion = new Potion(PotionType.valueOf(amountdata[1])).extend().splash();
			    //getLogger().info("Potion in chest is :" + chestPotion.getType().toString() + " splash, extended duration x " + amountdata[3]);
			    tempChest[i] = chestPotion.toItemStack(Integer.parseInt(amountdata[3]));
			}
		    }
		} else {
		    if (amountdata.length == 2) {
			tempChest[i] = new ItemStack(Material.getMaterial(amountdata[0]), Integer.parseInt(amountdata[1]));
		    } else if (amountdata.length == 3) {
			tempChest[i] = new ItemStack(Material.getMaterial(amountdata[0]), Integer.parseInt(amountdata[2]), Short.parseShort(amountdata[1]));
		    }
		}
	    } catch (java.lang.IllegalArgumentException ex) {
		getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
		getLogger().severe("Error is : " + ex.getMessage());
		getLogger().info("Potential potion types are: ");
		for (PotionType c : PotionType.values())
		    getLogger().info(c.name());
	    }
	    catch (Exception e) {
		getLogger().severe("Problem loading chest item from config.yml so skipping it: " + chestItemString[i]);
		getLogger().info("Potential material types are: ");
		for (Material c : Material.values())
		    getLogger().info(c.name());
		//e.printStackTrace();
	    }
	}
	Settings.chestItems = tempChest;
	Settings.allowPvP = getConfig().getString("island.allowPvP");
	if (!Settings.allowPvP.equalsIgnoreCase("allow")) {
	    Settings.allowPvP = "deny";
	}
	Settings.allowBreakBlocks = getConfig().getBoolean("island.allowbreakblocks", false);
	Settings.allowPlaceBlocks= getConfig().getBoolean("island.allowplaceblocks", false);
	Settings.allowBedUse= getConfig().getBoolean("island.allowbeduse", false);
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
	Settings.allowTNTDamage = getConfig().getBoolean("island.allowtntdamage", false);
	Settings.allowSpawnEggs = getConfig().getBoolean("island.allowspawneggs", false);
	Settings.allowBreeding = getConfig().getBoolean("island.allowbreeding", false);
	Settings.absorbLava = getConfig().getBoolean("sponge.absorbLava", false);
	Settings.absorbFire = getConfig().getBoolean("sponge.absorbFire", false);
	Settings.restoreWater = getConfig().getBoolean("sponge.restoreWater", true);
	Settings.canPlaceWater = getConfig().getBoolean("sponge.canPlaceWater", false);
	Settings.spongeRadius = getConfig().getInt("sponge.spongeRadius", 2);
	Settings.threadedSpongeSave = getConfig().getBoolean("sponge.threadedSpongeSave", true);
	Settings.flowTimeMult = getConfig().getInt("sponge.flowTimeMult", 600);
	Settings.attackFire = getConfig().getBoolean("sponge.attackFire", false);
	Settings.pistonMove = getConfig().getBoolean("sponge.pistonMove", true);
	Settings.spongeSaturation = getConfig().getBoolean("sponge.spongeSaturation", false);
	// SPONGE DEBUG
	debug = getConfig().getBoolean("sponge.debug", false);

	// Challenges
	final Set<String> challengeList = getChallengeConfig().getConfigurationSection("challenges.challengeList").getKeys(false);
	Settings.challengeList = challengeList;
	Settings.challengeLevels = Arrays.asList(getChallengeConfig().getString("challenges.levels").split(" "));
	Settings.waiverAmount = getChallengeConfig().getInt("challenges.waiveramount", 1);
	if (Settings.waiverAmount < 0) {
	    Settings.waiverAmount = 0;
	}
	// Challenge completion
	Settings.broadcastMessages = getConfig().getBoolean("general.broadcastmessages",true);

	// Levels
	// Get the blockvalues.yml file
	YamlConfiguration blockValuesConfig = ASkyBlock.loadYamlFile("blockvalues.yml");
	Settings.blockLimits = new HashMap<Material,Integer>();
	if (blockValuesConfig.isSet("limits")) {
	    for (String material : blockValuesConfig.getConfigurationSection("limits").getKeys(false)) {
		try {
		    Material mat = Material.valueOf(material);
		    Settings.blockLimits.put(mat, blockValuesConfig.getInt("limits." + material,0));
		    if (debug) {
			getLogger().info("Maximum number of " + mat.toString() + " will be " + Settings.blockLimits.get(mat));
		    }
		} catch (Exception e) {
		    getLogger().warning("Unknown material ("+material +") in blockvalues.yml Limits section. Skipping...");
		}
	    }
	}
	Settings.blockValues = new HashMap<Material,Integer>();
	if (blockValuesConfig.isSet("blocks")) {
	    for (String material : blockValuesConfig.getConfigurationSection("blocks").getKeys(false)) {
		try {
		    Material mat = Material.valueOf(material);
		    Settings.blockValues.put(mat, blockValuesConfig.getInt("blocks." + material,0));
		    if (debug) {
			getLogger().info(mat.toString() + " value is " + Settings.blockValues.get(mat));
		    }
		} catch (Exception e) {
		    //e.printStackTrace();
		    getLogger().warning("Unknown material ("+ material + ") in blockvalues.yml blocks section. Skipping...");
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
	// Localization Locale Setting
	Locale.changingObsidiantoLava = locale.getString("changingObsidiantoLava", "Changing obsidian back into lava. Be careful!");
	Locale.acidLore = locale.getString("acidLore","Poison!\nBeware!\nDo not drink!");
	Locale.acidBucket = locale.getString("acidBucket", "Acid Bucket");
	Locale.acidBottle = locale.getString("acidBottle", "Bottle O' Acid");
	Locale.drankAcidAndDied = locale.getString("drankAcidAndDied", "drank acid and died.");
	Locale.drankAcid = locale.getString("drankAcid", "drank acid.");
	Locale.errorUnknownPlayer = locale.getString("error.unknownPlayer","That player is unknown.");
	Locale.errorNoPermission = locale.getString("error.noPermission","You don't have permission to use that command!");
	Locale.errorNoIsland = locale.getString("error.noIsland","You do not have an island!");
	Locale.errorNoIslandOther = locale.getString("error.noIslandOther","That player does not have an island!");
	//"You must be on your island to use this command."
	Locale.errorCommandNotReady = locale.getString("error.commandNotReady","You can't use that command right now.");
	Locale.errorOfflinePlayer = locale.getString("error.offlinePlayer","That player is offline or doesn't exist.");
	Locale.errorUnknownCommand = locale.getString("error.unknownCommand","Unknown command.");
	Locale.errorNoTeam = locale.getString("error.noTeam","That player is not in a team.");
	Locale.islandProtected = locale.getString("islandProtected","Island protected.");
	Locale.lavaTip = locale.getString("lavaTip","Changing obsidian back into lava. Be careful!");
	Locale.warpswelcomeLine = locale.getString("warps.welcomeLine","[WELCOME]");
	Locale.warpswarpTip = locale.getString("warps.warpTip","Create a warp by placing a sign with [WELCOME] at the top.");
	Locale.warpssuccess = locale.getString("warps.success","Welcome sign placed successfully!");
	Locale.warpsremoved = locale.getString("warps.removed","Welcome sign removed!");
	Locale.warpssignRemoved = locale.getString("warps.signRemoved","Your welcome sign was removed!");
	Locale.warpsdeactivate = locale.getString("warps.deactivate","Deactivating old sign!");
	Locale.warpserrorNoRemove = locale.getString("warps.errorNoRemove","You can only remove your own Welcome Sign!");
	Locale.warpserrorNoPerm = locale.getString("warps.errorNoPerm","You do not have permission to place Welcome Signs yet!");
	Locale.warpserrorNoPlace = locale.getString("warps.errorNoPlace","You must be on your island to place a Welcome Sign!");
	Locale.warpserrorDuplicate = locale.getString("warps.errorDuplicate","Sorry! There is a sign already in that location!");
	Locale.warpserrorDoesNotExist = locale.getString("warps.errorDoesNotExist","That warp doesn't exist!");
	Locale.warpserrorNotReadyYet = locale.getString("warps.errorNotReadyYet","That warp is not ready yet. Try again later.");
	Locale.warpserrorNotSafe = locale.getString("warps.errorNotSafe","That warp is not safe right now. Try again later.");
	Locale.warpswarpToPlayersSign = locale.getString("warps.warpToPlayersSign","Warp to <player>'s welcome sign.");
	Locale.warpserrorNoWarpsYet = locale.getString("warps.errorNoWarpsYet","There are no warps available yet!");
	Locale.warpswarpsAvailable = locale.getString("warps.warpsAvailable","The following warps are available");
	Locale.topTenheader = locale.getString("topTen.header","These are the Top 10 islands:");
	Locale.topTenerrorNotReady = locale.getString("topTen.errorNotReady","Top ten list not generated yet!");
	Locale.levelislandLevel = locale.getString("level.islandLevel","Island level");
	Locale.levelerrornotYourIsland = locale.getString("level.errornotYourIsland", "Only the island owner can do that.");
	Locale.setHomehomeSet = locale.getString("sethome.homeSet","Your island home has been set to your current location.");
	Locale.setHomeerrorNotOnIsland = locale.getString("sethome.errorNotOnIsland","You must be within your island boundaries to set home!");
	Locale.setHomeerrorNoIsland = locale.getString("sethome.errorNoIsland","You are not part of an island. Returning you the spawn area!");
	Locale.challengesyouHaveCompleted = locale.getString("challenges.youHaveCompleted", "You have completed the [challenge] challenge!");
	Locale.challengesnameHasCompleted = locale.getString("challenges.nameHasCompleted", "[name] has completed the [challenge] challenge!");
	Locale.challengesyouRepeated = locale.getString("challenges.youRepeated", "You repeated the [challenge] challenge!");
	Locale.challengestoComplete = locale.getString("challenges.toComplete","Complete [challengesToDo] more [thisLevel] challenges to unlock this level!");
	Locale.challengeshelp1 = locale.getString("challenges.help1","Use /c <name> to view information about a challenge.");
	Locale.challengeshelp2 = locale.getString("challenges.help2","Use /c complete <name> to attempt to complete that challenge.");
	Locale.challengescolors = locale.getString("challenges.colors","Challenges will have different colors depending on if they are:");
	Locale.challengescomplete = locale.getString("challenges.complete", "Complete");
	Locale.challengesincomplete = locale.getString("challenges.incomplete","Incomplete");
	Locale.challengescompleteNotRepeatable = locale.getString("challenges.completeNotRepeatable","Completed(not repeatable)");
	Locale.challengescompleteRepeatable = locale.getString("challenges.completeRepeatable","Completed(repeatable)");
	Locale.challengesname = locale.getString("challenges.name","Challenge Name");
	Locale.challengeslevel = locale.getString("challenges.level","Level");
	Locale.challengesitemTakeWarning = locale.getString("challenges.itemTakeWarning","All required items are taken when you complete this challenge!");
	Locale.challengesnotRepeatable = locale.getString("challenges.notRepeatable","This Challenge is not repeatable!");
	Locale.challengesfirstTimeRewards = locale.getString("challenges.firstTimeRewards","First time reward(s)");
	Locale.challengesrepeatRewards = locale.getString("challenges.repeatRewards","Repeat reward(s)");
	Locale.challengesexpReward = locale.getString("challenges.expReward","Exp reward");
	Locale.challengesmoneyReward = locale.getString("challenges.moneyReward","Money reward");
	Locale.challengestoCompleteUse = locale.getString("challenges.toCompleteUse","To complete this challenge, use");
	Locale.challengesinvalidChallengeName = locale.getString("challenges.invalidChallengeName","Invalid challenge name! Use /c help for more information");
	Locale.challengesrewards = locale.getString("challenges.rewards","Reward(s)");
	Locale.challengesyouHaveNotUnlocked = locale.getString("challenges.youHaveNotUnlocked","You have not unlocked this challenge yet!");
	Locale.challengesunknownChallenge = locale.getString("challenges.unknownChallenge","Unknown challenge name (check spelling)!");
	Locale.challengeserrorNotEnoughItems = locale.getString("challenges.errorNotEnoughItems","You do not have enough of the required item(s)");
	Locale.challengeserrorNotOnIsland = locale.getString("challenges.errorNotOnIsland","You must be on your island to do that!");
	Locale.challengeserrorNotCloseEnough = locale.getString("challenges.errorNotCloseEnough","You must be standing within 10 blocks of all required items.");
	Locale.challengeserrorItemsNotThere = locale.getString("challenges.errorItemsNotThere","All required items must be close to you on your island!");
	Locale.challengeserrorIslandLevel = locale.getString("challenges.errorIslandLevel","Your island must be level [level] to complete this challenge!");
	Locale.challengesguiTitle = locale.getString("challenges.guititle", "Challenges");
	Locale.challengeserrorYouAreMissing = locale.getString("challenges.erroryouaremissing", "You are missing");
	Locale.islandteleport = locale.getString("island.teleport","Teleporting you to your island. (/island help for more info)");
	Locale.islandnew = locale.getString("island.new","Creating a new island for you...");
	Locale.islanderrorCouldNotCreateIsland = locale.getString("island.errorCouldNotCreateIsland","Could not create your Island. Please contact a server moderator.");
	Locale.islanderrorYouDoNotHavePermission = locale.getString("island.errorYouDoNotHavePermission", "You do not have permission to use that command!");
	Locale.islandresetOnlyOwner = locale.getString("island.resetOnlyOwner","Only the owner may restart this island. Leave this island in order to start your own (/island leave).");
	Locale.islandresetMustRemovePlayers = locale.getString("island.resetMustRemovePlayers","You must remove all players from your island before you can restart it (/island kick <player>). See a list of players currently part of your island using /island team.");
	Locale.islandresetPleaseWait = locale.getString("island.resetPleaseWait","Please wait, generating new island");
	Locale.islandresetWait = locale.getString("island.resetWait","You have to wait [time] seconds before you can do that again.");
	Locale.islandresetConfirm = locale.getString("island.resetConfirm", "Type /island confirm within 10 seconds to delete your island and restart!");
	Locale.islandhelpIsland = locale.getString("island.helpIsland","start an island, or teleport to your island.");
	Locale.islandhelpTeleport = locale.getString("island.helpTeleport", "teleport to your island.");
	Locale.islandhelpSpawn = locale.getString("island.helpIslandSpawn","go to ASkyBlock spawn.");
	Locale.islandhelpControlPanel = locale.getString("island.helpControlPanel","open the island GUI.");
	Locale.islandhelpRestart = locale.getString("island.helpRestart","restart your island and remove the old one.");
	Locale.islandDeletedLifeboats = locale.getString("island.islandDeletedLifeboats","Island deleted! Head to the lifeboats!");
	Locale.islandhelpSetHome = locale.getString("island.helpSetHome","set your teleport point for /island.");
	Locale.islandhelpLevel = locale.getString("island.helpLevel","calculate your island level");
	Locale.islandhelpLevelPlayer = locale.getString("island.helpLevelPlayer","see another player's island level.");
	Locale.islandhelpTop = locale.getString("island.helpTop","see the top ranked islands.");
	Locale.islandhelpWarps = locale.getString("island.helpWarps","Lists all available welcome-sign warps.");
	Locale.islandhelpWarp = locale.getString("island.helpWarp","Warp to <player>'s welcome sign.");
	Locale.islandhelpTeam = locale.getString("island.helpTeam","view your team information.");
	Locale.islandhelpInvite = locale.getString("island.helpInvite","invite a player to join your island.");
	Locale.islandhelpLeave = locale.getString("island.helpLeave","leave another player's island.");
	Locale.islandhelpKick = locale.getString("island.helpKick","remove a player from your island.");
	Locale.adminHelpHelp = locale.getString("adminHelp.help","Acid Admin Commands:");
	Locale.islandhelpAcceptReject = locale.getString("island.helpAcceptReject","accept or reject an invitation.");
	Locale.islandhelpMakeLeader = locale.getString("island.helpMakeLeader","transfer the island to <player>.");
	Locale.islanderrorLevelNotReady = locale.getString("island.errorLevelNotReady","Can't use that command right now! Try again in a few seconds.");
	Locale.islanderrorInvalidPlayer = locale.getString("island.errorInvalidPlayer","That player is invalid or does not have an island!");
	Locale.islandislandLevelis = locale.getString("island.islandLevelis","Island level is");
	Locale.invitehelp = locale.getString("invite.help","Use [/island invite <playername>] to invite a player to your island.");
	Locale.inviteyouCanInvite = locale.getString("invite.youCanInvite","You can invite [number] more players.");
	Locale.inviteyouCannotInvite = locale.getString("invite.youCannotInvite","You can't invite any more players.");
	Locale.inviteonlyIslandOwnerCanInvite = locale.getString("invite.onlyIslandOwnerCanInvite","Only the island's owner can invite!");
	Locale.inviteyouHaveJoinedAnIsland = locale.getString("invite.youHaveJoinedAnIsland","You have joined an island! Use /island team to see the other members.");
	Locale.invitehasJoinedYourIsland = locale.getString("invite.hasJoinedYourIsland","[name] has joined your island!");
	Locale.inviteerrorCantJoinIsland = locale.getString("invite.errorCantJoinIsland","You couldn't join the island, maybe it's full.");
	Locale.inviteerrorYouMustHaveIslandToInvite = locale.getString("invite.errorYouMustHaveIslandToInvite","You must have an island in order to invite people to it!");
	Locale.inviteerrorYouCannotInviteYourself = locale.getString("invite.errorYouCannotInviteYourself","You can not invite yourself!");
	Locale.inviteremovingInvite = locale.getString("invite.removingInvite","Removing your previous invite.");
	Locale.inviteinviteSentTo = locale.getString("invite.inviteSentTo","Invite sent to [name]");
	Locale.invitenameHasInvitedYou = locale.getString("invite.nameHasInvitedYou","[name] has invited you to join their island!");
	Locale.invitetoAcceptOrReject = locale.getString("invite.toAcceptOrReject","to accept or reject the invite.");
	Locale.invitewarningYouWillLoseIsland = locale.getString("invite.warningYouWillLoseIsland","WARNING: You will lose your current island if you accept!");
	Locale.inviteerrorYourIslandIsFull = locale.getString("invite.errorYourIslandIsFull","Your island is full, you can't invite anyone else.");
	Locale.inviteerrorThatPlayerIsAlreadyInATeam = locale.getString("invite.errorThatPlayerIsAlreadyInATeam","That player is already in a team.");
	Locale.rejectyouHaveRejectedInvitation = locale.getString("reject.youHaveRejectedInvitation","You have rejected the invitation to join an island.");
	Locale.rejectnameHasRejectedInvite = locale.getString("reject.nameHasRejectedInvite","[name] has rejected your island invite!");
	Locale.rejectyouHaveNotBeenInvited = locale.getString("reject.youHaveNotBeenInvited","You had not been invited to join a team.");
	Locale.leaveerrorYouAreTheLeader = locale.getString("leave.errorYouAreTheLeader","You are the leader, use /island remove <player> instead.");
	Locale.leaveyouHaveLeftTheIsland = locale.getString("leave.youHaveLeftTheIsland","You have left the island and returned to the player spawn.");
	Locale.leavenameHasLeftYourIsland = locale.getString("leave.nameHasLeftYourIsland","[name] has left your island!");
	Locale.leaveerrorYouCannotLeaveIsland = locale.getString("leave.errorYouCannotLeaveIsland","You can't leave your island if you are the only person. Try using /island restart if you want a new one!");
	Locale.leaveerrorYouMustBeInWorld = locale.getString("leave.errorYouMustBeInWorld","You must be in the ASkyBlock world to leave your team!");
	Locale.leaveerrorLeadersCannotLeave = locale.getString("leave.errorLeadersCannotLeave","Leaders cannot leave an island. Make someone else the leader fist using /island makeleader <player>");
	Locale.teamlistingMembers = locale.getString("team.listingMembers","Listing your island members");
	Locale.kickerrorPlayerNotInTeam = locale.getString("kick.errorPlayerNotInTeam","That player is not in your team!");
	Locale.kicknameRemovedYou = locale.getString("kick.nameRemovedYou","[name] has removed you from their island!");
	Locale.kicknameRemoved = locale.getString("kick.nameRemoved","[name] has been removed from the island.");
	Locale.kickerrorNotPartOfTeam = locale.getString("kick.errorNotPartOfTeam","That player is not part of your island team!");
	Locale.kickerrorOnlyLeaderCan = locale.getString("kick.errorOnlyLeaderCan","Only the island's owner may remove people from the island!");
	Locale.kickerrorNoTeam = locale.getString("kick.errorNoTeam","No one else is on your island, are you seeing things?");
	Locale.makeLeadererrorPlayerMustBeOnline = locale.getString("makeleader.errorPlayerMustBeOnline","That player must be online to transfer the island.");
	Locale.makeLeadererrorYouMustBeInTeam = locale.getString("makeleader.errorYouMustBeInTeam","You must be in a team to transfer your island.");
	Locale.makeLeadererrorRemoveAllPlayersFirst = locale.getString("makeleader.errorRemoveAllPlayersFirst","Remove all players from your team other than the player you are transferring to.");
	Locale.makeLeaderyouAreNowTheOwner = locale.getString("makeleader.youAreNowTheOwner","You are now the owner of your island.");
	Locale.makeLeadernameIsNowTheOwner = locale.getString("makeleader.nameIsNowTheOwner","[name] is now the owner of your island!");
	Locale.makeLeadererrorThatPlayerIsNotInTeam = locale.getString("makeleader.errorThatPlayerIsNotInTeam","That player is not part of your island team!");
	Locale.makeLeadererrorNotYourIsland = locale.getString("makeleader.errorNotYourIsland","This isn't your island, so you can't give it away!");
	Locale.makeLeadererrorGeneralError = locale.getString("makeleader.errorGeneralError","Acid Admin Commands:");
	Locale.adminHelpHelp = locale.getString("adminHelp.help","Could not change leaders.");
	Locale.adminHelpreload = locale.getString("adminHelp.reload","reload configuration from file.");
	Locale.adminHelptopTen = locale.getString("adminHelp.topTen","manually update the top 10 list");
	Locale.adminHelpregister = locale.getString("adminHelp.register","set a player's island to your location");
	Locale.adminHelpdelete = locale.getString("adminHelp.delete","delete an island (removes blocks).");
	Locale.adminHelpcompleteChallenge = locale.getString("adminHelp.completeChallenge","marks a challenge as complete");
	Locale.adminHelpresetChallenge = locale.getString("adminHelp.resetChallenge","marks a challenge as incomplete");
	Locale.adminHelpresetAllChallenges = locale.getString("adminHelp.resetAllChallenges","resets all of the player's challenges");
	Locale.adminHelppurge = locale.getString("adminHelp.purge","delete inactive islands older than [TimeInDays].");
	Locale.adminHelpinfo = locale.getString("adminHelp.info","check information on the given player.");
	Locale.adminHelpSetSpawn = locale.getString("adminHelp.setspawn","opens the spawn GUI for ASkyBlock world.");
	Locale.adminHelpinfoIsland = locale.getString("adminHelp.infoisland","provide info on the nearest island.");
	Locale.adminHelptp = locale.getString("adminHelp.tp", "Teleport to a player's island.");
	Locale.reloadconfigReloaded = locale.getString("reload.configReloaded","Configuration reloaded from file.");
	Locale.adminTopTengenerating = locale.getString("adminTopTen.generating","Generating the Top Ten list");
	Locale.adminTopTenfinished = locale.getString("adminTopTen.finished","Finished generation of the Top Ten list");
	Locale.purgealreadyRunning = locale.getString("purge.alreadyRunning","Purge is already running, please wait for it to finish!");
	Locale.purgeusage = locale.getString("purge.usage","Calculating which islands have been inactive for more than [time] days.");
	Locale.purgecalculating = locale.getString("purge.calculating","No inactive islands to remove.");
	Locale.purgenoneFound = locale.getString("purge.noneFound","This will remove [number] inactive islands!");
	Locale.purgethisWillRemove = locale.getString("purge.thisWillRemove","DANGER! Do not run this with players on the server! MAKE BACKUP OF WORLD!");
	Locale.purgewarning = locale.getString("purge.warning","Type /acid confirm to proceed within 10 seconds");
	Locale.purgetypeConfirm = locale.getString("purge.typeConfirm","Purge cancelled.");
	Locale.purgepurgeCancelled = locale.getString("purge.purgeCancelled","Finished purging of inactive islands.");
	Locale.purgefinished = locale.getString("purge.finished","Purge: Removing [name]'s island");
	Locale.purgeremovingName = locale.getString("purge.removingName","Time limit expired! Issue command again.");
	Locale.confirmerrorTimeLimitExpired = locale.getString("confirm.errorTimeLimitExpired","Removing [name]'s island.");
	Locale.deleteremoving = locale.getString("delete.removing","Set [name]'s island to the bedrock nearest you.");
	Locale.registersettingIsland = locale.getString("register.settingIsland","Error: unable to set the island!");
	Locale.registererrorBedrockNotFound = locale.getString("register.errorBedrockNotFound","Island Location");
	Locale.adminInfoislandLocation = locale.getString("adminInfo.islandLocation","That player is not a member of an island team.");
	Locale.adminInfoerrorNotPartOfTeam = locale.getString("adminInfo.errorNotPartOfTeam","Team leader should be null!");
	Locale.adminInfoerrorNullTeamLeader = locale.getString("adminInfo.errorNullTeamLeader","Player has team members, but shouldn't!");
	Locale.adminInfoerrorTeamMembersExist = locale.getString("adminInfo.errorTeamMembersExist","[name] has had all challenges reset.");
	Locale.resetChallengessuccess = locale.getString("resetallchallenges.success","Checking Team of [name]");
	Locale.checkTeamcheckingTeam = locale.getString("checkTeam.checkingTeam","Challenge doesn't exist or is already completed");
	Locale.completeChallengeerrorChallengeDoesNotExist = locale.getString("completechallenge.errorChallengeDoesNotExist","[challengename] has been completed for [name]");
	Locale.completeChallengechallangeCompleted = locale.getString("completechallenge.challangeCompleted","Challenge doesn't exist or isn't yet completed");
	Locale.resetChallengeerrorChallengeDoesNotExist = locale.getString("resetchallenge.errorChallengeDoesNotExist","[challengename] has been reset for [name]");
	Locale.confirmerrorTimeLimitExpired = locale.getString("confirm.errorTimeLimitExpired","Time limit expired! Issue command again.");
	Locale.deleteremoving = locale.getString("delete.removing","Removing [name]'s island.");
	Locale.registersettingIsland = locale.getString("register.settingIsland","Set [name]'s island to the bedrock nearest you.");
	Locale.registererrorBedrockNotFound = locale.getString("register.errorBedrockNotFound","Error: unable to set the island!");
	Locale.adminInfoislandLocation = locale.getString("adminInfo.islandLocation","Island Location");
	Locale.adminInfoerrorNotPartOfTeam = locale.getString("adminInfo.errorNotPartOfTeam","That player is not a member of an island team.");
	Locale.adminInfoerrorNullTeamLeader = locale.getString("adminInfo.errorNullTeamLeader","Team leader should be null!");
	Locale.adminInfoerrorTeamMembersExist = locale.getString("adminInfo.errorTeamMembersExist","Player has team members, but shouldn't!");
	Locale.resetChallengessuccess = locale.getString("resetallchallenges.success","[name] has had all challenges reset.");
	Locale.checkTeamcheckingTeam = locale.getString("checkTeam.checkingTeam","Checking Team of [name]");
	Locale.completeChallengeerrorChallengeDoesNotExist = locale.getString("completechallenge.errorChallengeDoesNotExist","Challenge doesn't exist or is already completed");
	Locale.completeChallengechallangeCompleted = locale.getString("completechallenge.challangeCompleted","[challengename] has been completed for [name]");
	Locale.resetChallengeerrorChallengeDoesNotExist = locale.getString("resetchallenge.errorChallengeDoesNotExist","Challenge doesn't exist or isn't yet completed");
	Locale.resetChallengechallengeReset = locale.getString("resetchallenge.challengeReset","[challengename] has been reset for [name]");
	Locale.newsHeadline = locale.getString("news.headline","[ASkyBlock News] While you were offline...");
	Locale.netherSpawnIsProtected = locale.getString("nether.spawnisprotected", "The Nether spawn area is protected.");
	Locale.islandhelpMiniShop = locale.getString("minishop.islandhelpMiniShop","Opens the MiniShop" );
	Locale.islandMiniShopTitle = locale.getString("minishop.title","MiniShop" );
	Locale.minishopBuy = locale.getString("minishop.buy","Buy(Left Click)");
	Locale.minishopSell = locale.getString("minishop.sell","Sell(Right Click)");
	Locale.minishopYouBought = locale.getString("minishop.youbought", "You bought [number] [description] for [price]");
	Locale.minishopSellProblem = locale.getString("minishop.sellproblem", "You do not have enough [description] to sell.");
	Locale.minishopYouSold = locale.getString("minishop.yousold","You sold [number] [description] for [price]");
	Locale.minishopBuyProblem  = locale.getString("minishop.buyproblem", "There was a problem purchasing [description]");
	Locale.minishopYouCannotAfford = locale.getString("minishop.youcannotafford", "You cannot afford [description]!");
	Locale.minishopOutOfStock = locale.getString("minishop.outofstock", "Out Of Stock");
	Locale.boatWarningItIsUnsafe = locale.getString("boats.warning", "It's unsafe to exit the boat right now...");
	Locale.adminHelpclearReset = locale.getString("general.clearreset", "resets the island reset limit for player.");
	Locale.resetYouHave = locale.getString("island.resetYouHave","You have [number] resets left.");
	Locale.islandResetNoMore = locale.getString("island.resetNoMore", "No more resets are allowed for your island!");
	Locale.clearedResetLimit = locale.getString("resetTo", "Cleared reset limit");
	Locale.signLine1 = locale.getString("sign.line1", "&1[A Skyblock]");
	Locale.signLine2 = locale.getString("sign.line2", "[player]");
	Locale.signLine3 = locale.getString("sign.line3", "Do not fall!");
	Locale.signLine4 = locale.getString("sign.line4", "Beware!");
	Locale.islandhelpBiome = locale.getString("biome.help","open the biome GUI.");
	Locale.biomeSet = locale.getString("biome.set","Island biome set to [biome]!");
	Locale.biomeUnknown = locale.getString("biome.unknown","Unknown biome!");
	Locale.biomeYouBought = locale.getString("biome.youbought","Purchased for [cost]!");
	Locale.biomePanelTitle = locale.getString("biome.paneltitle", "Select A Biome");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
	try {
	    // Remove players from memory
	    players.removeAllPlayers();
	    //saveConfig();
	    saveWarpList();
	    saveMessages();
	} catch (final Exception e) {
	    plugin.getLogger().severe("Something went wrong saving files!");
	    e.printStackTrace();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
	// instance of this plugin
	plugin = this;
	saveDefaultConfig();
	saveDefaultChallengeConfig();
	saveDefaultLocale();
	if (!VaultHelper.setupEconomy()) {
	    getLogger().severe("Could not set up economy!");
	    getServer().getPluginManager().disablePlugin(this);
	}
	if (!VaultHelper.setupPermissions()) {
	    getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
	    getServer().getPluginManager().disablePlugin(this);
	} 
	loadPluginConfig();
	getIslandWorld();

	// Set and make the player's directory if it does not exist and then load players into memory
	playersFolder = new File(getDataFolder() + File.separator + "players");
	if (!playersFolder.exists()) {
	    playersFolder.mkdir();
	}
	// Set up commands for this plugin

	getCommand("island").setExecutor(new IslandCmd(this));
	getCommand("asc").setExecutor(getChallenges());
	getCommand("asadmin").setExecutor(new AdminCmd(this));
	// Register events that this plugin uses
	//registerEvents();
	// Load warps
	loadWarpList();
	// Load messages
	loadMessages();
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
		// update the list
		//updateTopTen();
		// Minishop - must wait for economy to load before we can use econ 
		getServer().getPluginManager().registerEvents(new ControlPanel(plugin), plugin);
		if (getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
		    getLogger().info("Trying to register generator with Multiverse ");
		    try {
			getServer().dispatchCommand(getServer().getConsoleSender(), "mv modify set generator ASkyBlock " + Settings.worldName);
		    } catch (Exception e) {
			getLogger().info("Not successfull! Disabling ASkyBlock!");
			e.printStackTrace();
			getServer().getPluginManager().disablePlugin(plugin);
		    }
		}
	    }
	});

	// This part will kill monsters if they fall into the water because it
	// is acid
	if (Settings.mobAcidDamage > 0D) {
	    getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
		@Override
		public void run() {
		    List<Entity> entList = acidWorld.getEntities();
		    for (Entity current : entList) {
			if (current instanceof Monster) {
			    if ((current.getLocation().getBlock().getType() == Material.WATER)
				    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
				((Monster) current).damage(Settings.mobAcidDamage);
				//getLogger().info("Killing monster");
			    }
			}
		    }
		}
	    }, 0L, 20L);
	}
    }

    /**
     * Checks if an online player is on their island or on a team island
     * 
     * @param player
     *            - the player who is being checked
     * @return - true if they are on their island, otherwise false
     */
    public boolean playerIsOnIsland(final Player player) {
	Location islandTestLocation = null;
	if (players.hasIsland(player.getUniqueId())) {
	    islandTestLocation = players.getIslandLocation(player.getUniqueId());
	} else if (players.inTeam(player.getUniqueId())) {
	    islandTestLocation = players.get(player.getUniqueId()).getTeamIslandLocation();
	}
	if (islandTestLocation == null) {
	    return false;
	}
	if (player.getLocation().getX() > islandTestLocation.getX() - Settings.island_protectionRange / 2
		&& player.getLocation().getX() < islandTestLocation.getX() + Settings.island_protectionRange / 2
		&& player.getLocation().getZ() > islandTestLocation.getZ() - Settings.island_protectionRange / 2
		&& player.getLocation().getZ() < islandTestLocation.getZ() + Settings.island_protectionRange / 2) {
	    return true;
	}
	return false;
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
	//manager.registerEvents(new ControlPanel(), this);
	// Change names of inventory items
	manager.registerEvents(new AcidInventory(this), this);
	// Biomes
	// Load Biomes
	biomes = new Biomes(this);
	manager.registerEvents(biomes, this);
    }

    public void unregisterEvents() {
	HandlerList.unregisterAll(warpSignsListener);
	HandlerList.unregisterAll(lavaListener);	
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
     * Removes monsters around location l 
     * 
     * @param l
     */
    public void removeMobs(final Location l) {
	final int px = l.getBlockX();
	final int py = l.getBlockY();
	final int pz = l.getBlockZ();
	for (int x = -1; x <= 1; x++) {
	    for (int z = -1; z <= 1; z++) {
		final Chunk c = l.getWorld().getChunkAt(new Location(l.getWorld(), px + x * 16, py, pz + z * 16));
		for (final Entity e : c.getEntities()) {
		    if (e instanceof Monster) {
			e.remove();
		    }
		}
	    }
	}
    }

    /**
     * This removes the island at the location given. Removes any entities in
     * that area too
     * 
     * @param loc
     *            - a Location
     */
    public void removeIsland(final Location loc) {
	//getLogger().info("DEBUG: removeIsland");
	if (loc != null) {
	    // Place a temporary entity
	    //final World world = getIslandWorld();

	    Entity snowBall = loc.getWorld().spawnEntity(loc, EntityType.SNOWBALL);
	    // Remove any mobs if they just so happen to be around in the
	    // vicinity
	    final Iterator<Entity> ents = snowBall.getNearbyEntities((Settings.island_protectionRange / 2.0), 110.0D, (Settings.island_protectionRange / 2.0))
		    .iterator();
	    while (ents.hasNext()) {
		final Entity tempent = ents.next();
		// Remove anything except for a players
		if (tempent instanceof Player) {
		    // Player
		    Player pl = (Player)tempent;
		    if (pl.isInsideVehicle()) {
			pl.leaveVehicle();
		    }
		    if (!pl.isFlying()) {
			// Move player to spawn
			if (plugin.getSpawn().getSpawnLoc() != null) {
			    // go to aSkyblock spawn
			    pl.teleport(plugin.getSpawn().getSpawnLoc());
			    getLogger().warning("During island deletion player " + pl.getName() + " sent to spawn.");
			} else {
			    if (!pl.performCommand("spawn")) {
				getLogger().warning("During island deletion player " + pl.getName() + " could not be sent to spawn so was dropped, sorry.");	
			    } else {
				getLogger().warning("During island deletion player " + pl.getName() + " sent to spawn using /spawn.");
			    }
			}
		    } else {
			getLogger().warning("Not moving player " + pl.getName() + " because they are flying");
		    }
		} else {
		    tempent.remove();
		}
	    }
	    /*
	    for (int x = Settings.island_protectionRange / 2 * -1; x <= Settings.island_protectionRange / 2; x++) {
		for (int y = 255; y >= 0; y--) {
		    for (int z = Settings.island_protectionRange / 2 * -1; z <= Settings.island_protectionRange / 2; z++) {

			final Block b = new Location(l.getWorld(), px + x, y, pz + z).getBlock();
			final Material bt = new Location(l.getWorld(), px + x, y, pz + z).getBlock().getType();
			// Grab anything out of containers (do that it is
			// destroyed)
			switch (bt) {
			case CHEST:
			    //getLogger().info("DEBUG: Chest");
			case TRAPPED_CHEST:
			    //getLogger().info("DEBUG: Trapped Chest");
			    final Chest c = (Chest) b.getState();
			    final ItemStack[] items = new ItemStack[c.getInventory().getContents().length];
			    c.getInventory().setContents(items);
			    break;
			case FURNACE:
			    final Furnace f = (Furnace) b.getState();
			    final ItemStack[] i2 = new ItemStack[f.getInventory().getContents().length];
			    f.getInventory().setContents(i2);
			    break;
			case DISPENSER:
			    final Dispenser d = (Dispenser) b.getState();
			    final ItemStack[] i3 = new ItemStack[d.getInventory().getContents().length];
			    d.getInventory().setContents(i3);
			    break;
			case HOPPER:
			    final Hopper h = (Hopper) b.getState();
			    final ItemStack[] i4 = new ItemStack[h.getInventory().getContents().length];
			    h.getInventory().setContents(i4);
			    break;
			case SIGN_POST:
			case WALL_SIGN:
			case SIGN:
			    //getLogger().info("DEBUG: Sign");
			    b.setType(Material.AIR);
			    break;
			default:
			    break;
			}
			// Split depending on below or above water line
			if (y < Settings.sea_level + 5) {
			    if (!b.getType().equals(Material.STATIONARY_WATER))
				b.setType(Material.STATIONARY_WATER);
			} else {
			    if (!b.getType().equals(Material.AIR))
				b.setType(Material.AIR);
			}
		    }
		}
	    }*/
	}
    }

    /**
     * Transfers ownership of an island from one player to another
     * 
     * @param playerOne
     * @param playerTwo
     * @return
     */
    public boolean transferIsland(final UUID playerOne, final UUID playerTwo) {
	if (players.hasIsland(playerOne)) {
	    players.setHasIsland(playerTwo, true);
	    players.setIslandLocation(playerTwo, players.getIslandLocation(playerOne));
	    players.setIslandLevel(playerTwo, players.getIslandLevel(playerOne));
	    players.setTeamIslandLocation(playerTwo, null);
	    players.setHasIsland(playerOne, false);
	    players.setIslandLocation(playerOne, null);
	    players.setIslandLevel(playerOne, 0);
	    players.setTeamIslandLocation(playerOne, players.getIslandLocation(playerOne));
	    return true;
	}
	return false;
    }


    /**
     * Generates a sorted map of islands for the Top Ten list
     */
    public void updateTopTen() {
	Map<UUID, Integer> top = new HashMap<UUID, Integer>();
	for (final File f : playersFolder.listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
		    if (playerUUID == null) {
			getLogger().warning("Player file contains erroneous UUID data.");
			getLogger().info("Looking at " + fileName.substring(0, fileName.length() - 4));
		    }
		    Players player = new Players(this, playerUUID);    
		    if (player.getIslandLevel() > 0) {
			if (!player.inTeam()) {
			    top.put(player.getPlayerUUID(), player.getIslandLevel());
			} else if (player.getTeamLeader() != null) {
			    if (player.getTeamLeader().equals(player.getPlayerUUID())) {
				top.put(player.getPlayerUUID(), player.getIslandLevel());
			    }
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
	// Now sort the list
	top = MapUtil.sortByValue(top);
	topTenList = top;
    }

    /**
     * Saves the challenge.yml file if it does not exist
     */
    public void saveDefaultChallengeConfig() {
	if (challengeConfigFile == null) {
	    challengeConfigFile = new File(getDataFolder(), "challenges.yml");
	}
	if (!challengeConfigFile.exists()) {            
	    plugin.saveResource("challenges.yml", false);
	}
    }

    /**
     * Reloads the challenge config file
     */
    public void reloadChallengeConfig() {
	if (challengeConfigFile == null) {
	    challengeConfigFile = new File(getDataFolder(), "challenges.yml");
	}
	challengeFile = YamlConfiguration.loadConfiguration(challengeConfigFile);

	// Look for defaults in the jar

	InputStream defConfigStream = this.getResource("challenges.yml");
	if (defConfigStream != null) {
	    @SuppressWarnings("deprecation")
	    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	    challengeFile.setDefaults(defConfig);
	}
    }

    /**
     * @return challenges FileConfiguration object
     */
    public FileConfiguration getChallengeConfig() {
	if (challengeFile == null) {
	    reloadChallengeConfig();
	}
	return challengeFile;
    }

    /**
     * Saves challenges.yml
     */
    public void saveChallengeConfig() {
	if (challengeFile == null || challengeConfigFile == null) {
	    return;
	}
	try {
	    getChallengeConfig().save(challengeConfigFile);
	} catch (IOException ex) {
	    getLogger().severe("Could not save config to " + challengeConfigFile);
	}
    }

    // Localization
    /**
     * Saves the locale.yml file if it does not exist
     */
    public void saveDefaultLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	if (!localeFile.exists()) {            
	    plugin.saveResource("locale.yml", false);
	}
    }

    /**
     * Reloads the locale file
     */
    public void reloadLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	locale = YamlConfiguration.loadConfiguration(localeFile);

	// Look for defaults in the jar
	InputStream defLocaleStream = this.getResource("locale.yml");
	if (defLocaleStream != null) {
	    YamlConfiguration defLocale = YamlConfiguration.loadConfiguration(defLocaleStream);
	    locale.setDefaults(defLocale);
	}
    }

    /**
     * @return locale FileConfiguration object
     */
    public FileConfiguration getLocale() {
	if (locale == null) {
	    reloadLocale();
	}
	return locale;
    }

    /**
     * Saves challenges.yml
     */
    public void saveLocale() {
	if (locale == null || localeFile == null) {
	    return;
	}
	try {
	    getLocale().save(localeFile);
	} catch (IOException ex) {
	    getLogger().severe("Could not save config to " + localeFile);
	}
    }

    public void tellOfflineTeam(UUID playerUUID, String message) {
	//getLogger().info("DEBUG: tell offline team called");
	if (!players.inTeam(playerUUID)) {
	    //getLogger().info("DEBUG: player is not in a team");
	    return;
	}
	UUID teamLeader = players.getTeamLeader(playerUUID);
	List<UUID> teamMembers = players.getMembers(teamLeader);
	for (UUID member : teamMembers) {
	    //getLogger().info("DEBUG: trying UUID " + member.toString());
	    if (getServer().getPlayer(member) == null) {
		// Offline player
		setMessage(member, message);
	    }
	}
    }
    /**
     * Sets a message for the player to receive next time they login
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
	//getLogger().info("DEBUG: received message - " + message);
	Player player = getServer().getPlayer(playerUUID);
	// Check if player is online
	if (player != null) {
	    if (player.isOnline()) {
		//player.sendMessage(message);
		return false;
	    }
	}
	// Player is offline so store the message

	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    playerMessages.add(message);
	} else {
	    playerMessages = new ArrayList<String>(Arrays.asList(message));
	}
	messages.put(playerUUID, playerMessages);
	return true;
    }

    public List<String> getMessages(UUID playerUUID) {
	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    // Remove the messages
	    messages.remove(playerUUID);
	} else {
	    // No messages
	    playerMessages = new ArrayList<String>();
	}
	return playerMessages;
    }

    public boolean saveMessages() {
	plugin.getLogger().info("Saving offline messages...");
	try {
	    // Convert to a serialized string
	    final HashMap<String,Object> offlineMessages = new HashMap<String,Object>();
	    for (UUID p : messages.keySet()) {
		offlineMessages.put(p.toString(),messages.get(p));
	    }
	    // Convert to YAML
	    messageStore.set("messages", offlineMessages);
	    saveYamlFile(messageStore, "messages.yml");
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public boolean loadMessages() {
	getLogger().info("Loading offline messages...");
	try {
	    messageStore = loadYamlFile("messages.yml");
	    if (messageStore.getConfigurationSection("messages") == null) {
		messageStore.createSection("messages"); // This is only used to create
	    }
	    HashMap<String,Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
	    for (String s : temp.keySet()) {
		List<String> messageList = messageStore.getStringList("messages." + s);
		if (!messageList.isEmpty()) {
		    messages.put(UUID.fromString(s), messageList);
		}
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * Converts a name like IRON_INGOT into Iron Ingot to improve readability
     * 
     * @param ugly
     *            The string such as IRON_INGOT
     * @return A nicer version, such as Iron Ingot
     * 
     *         Credits to mikenon on GitHub!
     */
    public static String prettifyText(String ugly) {
	if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase())))
	    return ugly;
	String fin = "";
	ugly = ugly.toLowerCase();
	if (ugly.contains("_")) {
	    String[] splt = ugly.split("_");
	    int i = 0;
	    for (String s : splt) {
		i += 1;
		fin += Character.toUpperCase(s.charAt(0)) + s.substring(1);
		if (i < splt.length)
		    fin += " ";
	    }
	} else {
	    fin += Character.toUpperCase(ugly.charAt(0)) + ugly.substring(1);
	}
	return fin;
    }

    /**
     * Converts block face direction to radial degrees. Returns 0 if block face is not radial.
     * @param face
     * @return degrees
     */
    public static float blockFaceToFloat(BlockFace face) {
	switch (face) {
	case EAST:
	    return 90F;
	case EAST_NORTH_EAST:
	    return 67.5F;
	case EAST_SOUTH_EAST:
	    return 0F;
	case NORTH:
	    return 0F;
	case NORTH_EAST:
	    return 45F;
	case NORTH_NORTH_EAST:
	    return 22.5F;
	case NORTH_NORTH_WEST:
	    return 337.5F;
	case NORTH_WEST:
	    return 315F;
	case SOUTH:
	    return 180F;
	case SOUTH_EAST:
	    return 135F;
	case SOUTH_SOUTH_EAST:
	    return 157.5F;
	case SOUTH_SOUTH_WEST:
	    return 202.5F;
	case SOUTH_WEST:
	    return 225F;
	case WEST:
	    return 270F;
	case WEST_NORTH_WEST:
	    return 292.5F;
	case WEST_SOUTH_WEST:
	    return 247.5F;
	default:
	    return 0F;	
	}
    }

    /**
     * Resets a player's inventory, armor slots, equipment, enderchest and potion effects
     * @param player
     */
    public void resetPlayer(Player player) {
	if (Settings.clearInventory) {
	    // Clear their inventory and equipment and set them as survival
	    player.getInventory().clear(); // Javadocs are wrong - this does not
	    // clear armor slots! So...
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
	players.save(player.getUniqueId());
	updateTopTen();
	// Update the inventory
	player.updateInventory();
	/*
	if (Settings.resetEnderChest) {
	    // Clear any Enderchest contents
	    final ItemStack[] items = new ItemStack[player.getEnderChest().getContents().length];
	    player.getEnderChest().setContents(items);
	}*/
	// Clear any potion effects
	for (PotionEffect effect : player.getActivePotionEffects())
	    player.removePotionEffect(effect.getType());	
    }

    /**
     * @return the spawn
     */
    public Spawn getSpawn() {
	if (spawn == null) {
	    spawn = new Spawn(this);
	}
	return spawn;
    }

    /**
     * @param location
     * @return Name of warp owner
     */
    public String getWarpOwner(Location location) {
	for (UUID playerUUID : warpList.keySet()) {
	    Location l = getLocationString((String) warpList.get(playerUUID));
	    if (l.equals(location)) {
		return players.getName(playerUUID);
	    }
	}
	return "a player";
    }


    /**
     * Used to prevent teleporting when falling
     * @param uniqueId
     */
    public void setFalling(UUID uniqueId) {
	this.fallingPlayers.add(uniqueId);
    }

    /**
     * Used to prevent teleporting when falling
     * @param uniqueId
     */
    public void unsetFalling(UUID uniqueId) {
	this.fallingPlayers.remove(uniqueId);
    }

    /**
     * Used to prevent teleporting when falling
     * @param uniqueId
     * @return true or false
     */
    public boolean isFalling(UUID uniqueId) {
	return this.fallingPlayers.contains(uniqueId);
    }

    /**
     * Sets all blocks in an island to a specified biome type
     * @param islandLoc
     * @param biomeType
     */
    public boolean setIslandBiome(Location islandLoc, Biome biomeType) {
	final int islandX = islandLoc.getBlockX();
	final int islandZ = islandLoc.getBlockZ();
	final World world = islandLoc.getWorld();
	final int range = (int)Math.round((double)Settings.island_protectionRange / 2);
	List<Pair> chunks = new ArrayList<Pair>();
	try {
	    // Biomes only work in 2D, so there's no need to set every block in the island area
	    // However, we need to collect the chunks and push them out again
	    //getLogger().info("DEBUG: Protection range is = " + Settings.island_protectionRange);
	    for (int x = -range; x <= range; x++) {
		for (int z = -range; z <= range; z++) {
		    Location l = new Location(world,(islandX + x), 0, (islandZ + z));
		    final Pair chunkCoords = new Pair(l.getChunk().getX(),l.getChunk().getZ());
		    if (!chunks.contains(chunkCoords)) {
			chunks.add(chunkCoords);
		    }
		    //getLogger().info("DEBUG: Block   " + l.getBlockX() + "," + l.getBlockZ());
		    /*
		    // Weird stuff going on here. Sometimes the location does not get created.
		    if (l.getBlockX() != (islandX +x)) {
			getLogger().info("DEBUG: Setting " + (islandX + x) + "," + (islandZ + z));
			getLogger().info("DEBUG: disparity in x");
		    }
		    if (l.getBlockZ() != (islandZ +z)) {
			getLogger().info("DEBUG: Setting " + (islandX + x) + "," + (islandZ + z));
			getLogger().info("DEBUG: disparity in z");
		    }*/
		    l.getBlock().setBiome(biomeType);
		}
	    }
	} catch (Exception noBiome) {
	    return false;
	}
	// Now do some adjustments based on the Biome
	switch (biomeType) {
	case DESERT:
	case JUNGLE:
	case SAVANNA:
	case SWAMPLAND:
	    // No ice or snow allowed
	    for (int y = islandLoc.getWorld().getMaxHeight(); y >= 0; y--) {
		for (int x = islandX-range; x <= islandX + range; x++) {
		    for (int z = islandZ-range; z <= islandZ+range; z++) {
			switch(world.getBlockAt(x, y, z).getType()) {
			case ICE:
			case SNOW:
			    world.getBlockAt(x, y, z).setType(Material.AIR);
			    break;
			default:
			}
		    }
		}
	    }
	    break;
	case HELL:
	    // No water or ice allowed
	    for (int y = islandLoc.getWorld().getMaxHeight(); y >= 0; y--) {
		for (int x = islandX-range; x <= islandX + range; x++) {
		    for (int z = islandZ-range; z <= islandZ+range; z++) {
			switch(world.getBlockAt(x, y, z).getType()) {
			case ICE:
			case WATER:
			case STATIONARY_WATER:
			case SNOW:
			    world.getBlockAt(x, y, z).setType(Material.AIR);
			    break;
			default:
			}
		    }
		}
	    }
	    break;
	default:
	    break;

	}
	// Update chunks
	for (Pair p: chunks) {
	    islandLoc.getWorld().refreshChunk(p.getLeft(), p.getRight());
	    //plugin.getLogger().info("DEBUG: refreshing " + p.getLeft() + "," + p.getRight());
	}
	return true;
    }

    /**
     * @return the biomes
     */
    public Biomes getBiomes() {
	return biomes;
    }
}