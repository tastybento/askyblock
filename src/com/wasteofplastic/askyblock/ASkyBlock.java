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
import org.bukkit.entity.Animals;
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
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.NotSetup.Reason;

/**
 * @author tastybento
 * Main ASkyBlock class - provides an island minigame in a sea of acid
 */
public class ASkyBlock extends JavaPlugin {
    private boolean newIsland = false;
    // This plugin
    private static ASkyBlock plugin;
    // The ASkyBlock world
    private static World acidWorld = null;
    // Player YAMLs
    //public YamlConfiguration playerFile;
    private File playersFolder;
    // Where challenges are stored
    private FileConfiguration challengeFile = null;
    private File challengeConfigFile = null;
    private Challenges challenges;
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    // Where warps are stored
    private YamlConfiguration welcomeWarps;
    // Map of all warps stored as player, warp sign Location
    private HashMap<UUID, Object> warpList = new HashMap<UUID, Object>();
    // Top ten list of players
    private Map<UUID, Integer> topTenList = new HashMap<UUID, Integer>();
    // Players object
    private PlayerCache players;
    // Acid Damage Potion
    PotionEffectType acidPotion;
    // Listeners
    private Listener warpSignsListener;
    private Listener lavaListener;
    // A set of falling players
    HashSet<UUID> fallingPlayers = new HashSet<UUID>();
    // Biome chooser object
    Biomes biomes;
    // Island grid manager
    private GridManager grid;

    private boolean debug = false;
    //public boolean flag = false;

    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;

    // Level calc
    private boolean calculatingLevel = false;

    /**
     * @return ASkyBlock object instance
     */
    protected static ASkyBlock getPlugin() {
	return plugin;
    }

    /**
     * @return the challenges
     */
    protected Challenges getChallenges() {
	if (challenges == null) {
	    challenges = new Challenges(this, getPlayers());
	}
	return challenges;
    }

    /**
     * @return the players
     */
    protected PlayerCache getPlayers() {
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
    protected static World getIslandWorld() {
	if (acidWorld == null) {
	    //Bukkit.getLogger().info("DEBUG worldName = " + Settings.worldName);
	    acidWorld = WorldCreator.name(Settings.worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL)
		    .generator(new ChunkGeneratorWorld()).createWorld();
	    // Make the nether if it does not exist
	    if (Settings.createNether) {
		if (plugin.getServer().getWorld(Settings.worldName + "_nether") == null) {
		    Bukkit.getLogger().info("Creating " + plugin.getName() + "'s Nether...");
		    if (!Settings.newNether) {
			WorldCreator.name(Settings.worldName + "_nether").type(WorldType.NORMAL).environment(World.Environment.NETHER).createWorld();
		    } else {
			WorldCreator.name(Settings.worldName + "_nether").type(WorldType.FLAT).generator(new ChunkGeneratorWorld()).environment(World.Environment.NETHER).createWorld();
		    }
		    //netherWorld.setMonsterSpawnLimit(Settings.monsterSpawnLimit);
		    // netherWorld.setAnimalSpawnLimit(Settings.animalSpawnLimit);
		}
	    }
	    // Multiverse configuration
	    if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
		Bukkit.getLogger().info("Trying to register generator with Multiverse ");
		try {
		    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "mv import " + Settings.worldName + " normal -g " + plugin.getName());
		    if (!Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "mv modify set generator " + plugin.getName() + " " + Settings.worldName)) {
			Bukkit.getLogger().severe("Multiverse is out of date! - Upgrade to latest version!");
		    }
		    if (Settings.createNether) {
			if (Settings.newNether) {
			    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "mv import " + Settings.worldName + "_nether nether -g " + plugin.getName());
			    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "mv modify set generator " + plugin.getName() + " " + Settings.worldName + "_nether");
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
	acidWorld.setWaterAnimalSpawnLimit(Settings.waterAnimalSpawnLimit);
	acidWorld.setMonsterSpawnLimit(Settings.monsterSpawnLimit);
	acidWorld.setAnimalSpawnLimit(Settings.animalSpawnLimit);

	return acidWorld;
    }

    /**
     * @return the playersFolder
     */
    public File getPlayersFolder() {
	return playersFolder;
    }

    /**
     * Delete Island
     * Called when an island is restarted or reset
     * @param player - player name String
     * @param removeBlocks
     */
    protected void deletePlayerIsland(final UUID player, boolean removeBlocks) {
	// Removes the island
	//getLogger().info("DEBUG: deleting player island");
	CoopPlay.getInstance().clearAllIslandCoops(player);
	removeWarp(player);
	if (removeBlocks) {
	    removeMobsFromIsland(players.getIslandLocation(player));
	    new DeleteIslandChunk(this,players.getIslandLocation(player));
	} else {
	    Island island = grid.getIsland(player);
	    if (island != null) {
		island.setLocked(false);
		grid.setIslandOwner(island, null);
	    }
	}
	players.zeroPlayerData(player);
    }

    /**
     * Displays the Top Ten list if it exists in chat
     * 
     * @param player
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    protected boolean topTenShow(final Player player) {
	player.sendMessage(ChatColor.GOLD + Locale.topTenheader);
	if (topTenList == null) {
	    topTenCreate();
	    //player.sendMessage(ChatColor.RED + Locale.topTenerrorNotReady);
	    //return true;
	}
	int i = 1;
	//getLogger().info("DEBUG: " + topTenList.toString());
	//getLogger().info("DEBUG: " + topTenList.values());
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
	return new ChunkGeneratorWorld();
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is empty
     * @param s - serialized location in format "world:x:y:z"
     * @return Location
     */
    static protected Location getLocationString(final String s) {
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
    static protected String getStringLocation(final Location l) {
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
    protected Location getSafeHomeLocation(final UUID p) {
	//getLogger().info("DEBUG: getSafeHomeLocation called for " + p.toString());
	// Try home location first
	Location l = players.getHomeLocation(p);
	//getLogger().info("DEBUG: Home location " + l.toString());
	if (l != null) {
	    if (isSafeLocation(l)) {
		return l;
	    }
	    // To cover slabs, stairs and other half blocks, try one block above
	    Location lPlusOne = l.clone();
	    lPlusOne.add(new Vector(0,1,0));
	    if (lPlusOne != null) {
		if (isSafeLocation(lPlusOne)) {
		    // Adjust the home location accordingly
		    l = lPlusOne;
		    return lPlusOne;
		}    
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
	//getLogger().info("DEBUG: default");
	Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
	if (isSafeLocation(dl)) {
	    players.setHomeLocation(p, dl);
	    return dl;
	}
	// Try just above the bedrock
	//getLogger().info("DEBUG: above bedrock");
	dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
	if (isSafeLocation(dl)) {
	    players.setHomeLocation(p, dl);
	    return dl;
	}

	// Try higher up - 25 blocks high and then move down
	//getLogger().info("DEBUG: Try higher up");
	for (int y = l.getBlockY() + 25; y > 0; y--) {
	    final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
	    if (isSafeLocation(n)) {
		return n;
	    }
	}
	// Try all the way up to the sky
	//getLogger().info("DEBUG: try all the way to the sky");
	for (int y = l.getBlockY(); y < 255; y++) {
	    final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
	    if (isSafeLocation(n)) {
		return n;
	    }
	}
	//getLogger().info("DEBUG: trying around the protected area");
	// Try anywhere in the protected island area
	//Be smart and start at the island level and above it
	for (int y = Settings.island_level; y<l.getWorld().getMaxHeight(); y++) {
	    for (int x = l.getBlockX(); x < l.getBlockX() + Settings.island_protectionRange/2; x++) {
		for (int z = l.getBlockZ(); z < l.getBlockZ() + Settings.island_protectionRange/2; z++) {
		    Location ultimate = new Location(l.getWorld(),(double)x+0.5D,y,(double)z+0.5D);
		    if (!ultimate.getBlock().equals(Material.AIR)) {
			if (isSafeLocation(ultimate)) {
			    players.setHomeLocation(p, ultimate);
			    return ultimate;
			}
		    }
		}
	    }
	}
	for (int y = Settings.island_level; y<l.getWorld().getMaxHeight(); y++) {
	    for (int x = l.getBlockX(); x > l.getBlockX() - Settings.island_protectionRange/2; x--) {
		for (int z = l.getBlockZ(); z < l.getBlockZ() - Settings.island_protectionRange/2; z--) {
		    Location ultimate = new Location(l.getWorld(),(double)x+0.5D,y,(double)z+0.5D);
		    if (!ultimate.getBlock().equals(Material.AIR)) {
			if (isSafeLocation(ultimate)) {
			    players.setHomeLocation(p, ultimate);
			    return ultimate;
			}
		    }
		}
	    }
	}
	// Try below the island level
	// Move away from the center and go to the positive extreme
	for (int y = Settings.island_level-1; y>0; y--) {
	    for (int x = l.getBlockX(); x < l.getBlockX() + Settings.island_protectionRange/2; x++) {
		for (int z = l.getBlockZ(); z < l.getBlockZ() + Settings.island_protectionRange/2; z++) {
		    Location ultimate = new Location(l.getWorld(),(double)x+0.5D,y,(double)z+0.5D);
		    if (!ultimate.getBlock().equals(Material.AIR)) {
			if (isSafeLocation(ultimate)) {
			    players.setHomeLocation(p, ultimate);
			    return ultimate;
			}
		    }
		}
	    }
	}
	// Go to the negative extreme
	for (int y = Settings.island_level-1; y>0; y--) {
	    for (int x = l.getBlockX(); x > l.getBlockX() - Settings.island_protectionRange/2; x--) {
		for (int z = l.getBlockZ(); z > l.getBlockZ() - Settings.island_protectionRange/2; z--) {
		    Location ultimate = new Location(l.getWorld(),(double)x+0.5D,y,(double)z+0.5D);
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
    protected void homeSet(final Player player) {
	// Make a list of test locations and test them
	Location islandTestLocation = null;
	if (players.hasIsland(player.getUniqueId())) {
	    islandTestLocation = players.getIslandLocation(player.getUniqueId());
	} else if (players.inTeam(player.getUniqueId())) {
	    islandTestLocation = players.get(player.getUniqueId()).getTeamIslandLocation();
	}
	if (islandTestLocation == null) {
	    player.sendMessage(ChatColor.RED + Locale.setHomeerrorNotOnIsland);
	    return;
	}
	// On island?
	if (player.getLocation().getX() > islandTestLocation.getX() - Settings.island_protectionRange / 2
		&& player.getLocation().getX() < islandTestLocation.getX() + Settings.island_protectionRange / 2
		&& player.getLocation().getZ() > islandTestLocation.getZ() - Settings.island_protectionRange / 2
		&& player.getLocation().getZ() < islandTestLocation.getZ() + Settings.island_protectionRange / 2) {
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
    protected boolean homeTeleport(final Player player) {
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
	    if (!player.performCommand(Settings.SPAWNCOMMAND)) {
		player.teleport(player.getWorld().getSpawnLocation());
		/*
		player.sendBlockChange(player.getWorld().getSpawnLocation()
			,player.getWorld().getSpawnLocation().getBlock().getType()
			,player.getWorld().getSpawnLocation().getBlock().getData());
		 */
	    }
	    player.sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
	    return true;
	}
	//home.getWorld().refreshChunk(home.getChunk().getX(), home.getChunk().getZ());
	// Removing this line because it appears to cause artifacts of hovering blocks
	//home.getWorld().loadChunk(home.getChunk());
	//getLogger().info("DEBUG: " + home.toString());
	// This next line should help players with long ping times
	// http://bukkit.org/threads/workaround-for-playing-falling-after-teleport-when-lagging.293035/
	//getLogger().info("DEBUG: home = " + home.toString());
	//player.sendBlockChange(home,home.getBlock().getType(),home.getBlock().getData());
	//player.sendBlockChange(home.getBlock().getRelative(BlockFace.DOWN).getLocation(),home.getBlock().getRelative(BlockFace.DOWN).getType(),home.getBlock().getRelative(BlockFace.DOWN).getData());
	//getLogger().info("DEBUG: " + home.getBlock().getType().toString());
	//getLogger().info("DEBUG: " + home.getBlock().getRelative(BlockFace.DOWN).getType());
	player.teleport(home);
	/*
	player.sendBlockChange(home,home.getBlock().getType(),home.getBlock().getData());
	player.sendBlockChange(home.getBlock().getRelative(BlockFace.DOWN).getLocation(),home.getBlock().getRelative(BlockFace.DOWN).getType(),home.getBlock().getRelative(BlockFace.DOWN).getData());
	 */
	player.sendMessage(ChatColor.GREEN + Locale.islandteleport);
	return true;
    }

    /**
     * Determines if an island is at a location in this area
     * location. Also checks if the spawn island is in this area.
     * Used for creating new islands ONLY
     * 
     * @param loc
     * @return true if found, otherwise false
     */
    protected boolean islandAtLocation(final Location loc) {	
	if (loc == null) {
	    return true;
	}
	//getLogger().info("DEBUG checking islandAtLocation for location " + loc.toString());
	// Check the island grid
	if (grid.getIslandAt(loc) != null) {
	    // This checks if loc is inside the island spawn radius too
	    return true;
	}
	final int px = loc.getBlockX();
	final int pz = loc.getBlockZ();
	// Extra spawn area check
	// If island protection distance is less than island distance then the check above will cover it
	// Island edge must be > protection edge spawn
	Island spawn = grid.getSpawn();
	if (spawn != null && spawn.getProtectionSize() > spawn.getIslandDistance()) {
	    if (Math.abs(px - spawn.getCenter().getBlockX()) < ((spawn.getProtectionSize() + Settings.islandDistance)/2)
		    && Math.abs(pz - spawn.getCenter().getBlockZ()) < ((spawn.getProtectionSize() + Settings.islandDistance)/2)) {
		//getLogger().info("DEBUG: island is within spawn space " + px + " " + pz);
		return true;
	    }
	}

	// Bedrock check
	if (loc.getBlock().getType().equals(Material.BEDROCK)) {
	    getLogger().info("Found bedrock at island height - adding to islands.yml " + px + "," + pz);
	    grid.addIsland(px,pz);
	    return true;
	}
	// Look around
	for (int x = -5; x <= 5; x++) {
	    for (int y = 10; y <= 255; y++) {
		for (int z = -5; z <= 5; z++) {
		    if (loc.getWorld().getBlockAt(x + px, y, z + pz).getType().equals(Material.BEDROCK)) {
			plugin.getLogger().info("Bedrock found during long search - adding to islands.yml " + px + "," + pz);
			grid.addIsland(px,pz);
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
    protected boolean isNewIsland() {
	return newIsland;
    }

    /**
     * @param newIsland the newIsland to set
     */
    protected void setNewIsland(boolean newIsland) {
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
    protected static boolean isSafeLocation(final Location l) {
	if (l == null) {
	    return false;
	}
	// TODO: improve the safe location finding.
	//Bukkit.getLogger().info("DEBUG: " + l.toString());
	final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
	final Block space1 = l.getBlock();
	final Block space2 = l.getBlock().getRelative(BlockFace.UP);
	//Bukkit.getLogger().info("DEBUG: ground = " + ground.getType());
	//Bukkit.getLogger().info("DEBUG: space 1 = " + space1.getType());
	//Bukkit.getLogger().info("DEBUG: space 2 = " + space2.getType());
	// If ground is AIR, then this is either not good, or they are on slab, stair, etc.
	if (ground.getType() == Material.AIR) {
	    //Bukkit.getLogger().info("DEBUG: air");
	    return false;
	}
	// In aSkyblock, liquid may be unsafe
	if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
	    // Check if acid has no damage
	    if (Settings.acidDamage > 0D) {
		//Bukkit.getLogger().info("DEBUG: acid");
		return false;
	    } else if (ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
		    || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
		    || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
		// Lava check only
		//Bukkit.getLogger().info("DEBUG: lava");
		return false;
	    }
	}
	if (ground.getType().equals(Material.CACTUS)) {
	    //Bukkit.getLogger().info("DEBUG: cactus");
	    return false;
	} // Ouch - prickly
	if (ground.getType().equals(Material.BOAT)) {
	    //Bukkit.getLogger().info("DEBUG: boat");
	    return false;
	} // No, I don't want to end up on the boat again
	// Check that the space is not solid
	// The isSolid function is not fully accurate (yet) so we have to check
	// a few other items
	// isSolid thinks that PLATEs and SIGNS are solid, but they are not
	if (space1.getType().isSolid()) {
	    //Bukkit.getLogger().info("DEBUG: space 1 is solid");	    
	    // Do a few other checks
	    if (!(space1.getType().equals(Material.SIGN_POST)) && !(space1.getType().equals(Material.WALL_SIGN))) {
		//Bukkit.getLogger().info("DEBUG: space 1 is a sign post or wall sign");
		return false;
	    }
	    /*
	    switch (space1.getType()) {
	    case ACACIA_STAIRS:
	    case BIRCH_WOOD_STAIRS:
	    case BRICK_STAIRS:
	    case COBBLESTONE_STAIRS:
	    case DARK_OAK_STAIRS:
	    case IRON_PLATE:
	    case JUNGLE_WOOD_STAIRS:
	    case NETHER_BRICK_STAIRS:
	    case PORTAL:
	    case QUARTZ_STAIRS:
	    case RED_SANDSTONE_STAIRS:
	    case SANDSTONE_STAIRS:
	    case SIGN_POST:
	    case SMOOTH_STAIRS:
	    case SPRUCE_WOOD_STAIRS:
	    case STEP:
	    case STONE_PLATE:
	    case STONE_SLAB2:
	    case WOOD_DOUBLE_STEP:
	    case WOOD_PLATE:
	    case WOOD_STAIRS:
	    case WOOD_STEP:
		Bukkit.getLogger().info("DEBUG: not solid");
		break;
	    default:
		Bukkit.getLogger().info("DEBUG: solid");
		return false;
	    }
	     */
	}
	if (space2.getType().isSolid()) {
	    //Bukkit.getLogger().info("DEBUG: space 2 is solid");
	    // Do a few other checks
	    if (!(space2.getType().equals(Material.SIGN_POST)) && !(space2.getType().equals(Material.WALL_SIGN))) {
		//Bukkit.getLogger().info("DEBUG: space 2 is a sign post or wall sign");
		return false;
	    }
	}
	// Safe
	//Bukkit.getLogger().info("DEBUG: safe!");
	return true;
    }


    /**
     * Saves a YAML file
     * 
     * @param yamlFile
     * @param fileLocation
     */
    protected static void saveYamlFile(YamlConfiguration yamlFile, String fileLocation) {
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
    protected static YamlConfiguration loadYamlFile(String file) {
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
    protected void loadWarpList() {
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
    protected void saveWarpList() {
	if (warpList == null || welcomeWarps == null) {
	    return;
	}
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
    protected boolean addWarp(UUID player, Location loc) {
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
    protected void removeWarp(UUID uuid) {
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
    protected void removeWarp(Location loc) {
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
    protected boolean checkWarp(Location loc) {
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
    protected Set<UUID> listWarps() {
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
    protected Location getWarp(UUID player) {
	if (warpList.containsKey(player)) {
	    return getLocationString((String) warpList.get(player));
	} else {
	    return null;
	}
    }

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    protected void loadPluginConfig() {
	//getLogger().info("*********************************************");
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
	// Load schematics
	if (getConfig().contains("general.schematics")) {
	    for(String key : getConfig().getConfigurationSection("general.schematics").getKeys(true)) {
		//getLogger().info(key);
		// Check the file exists
		String filename = getConfig().getString("general.schematics." + key);
		File schematicFile = new File(plugin.getDataFolder(), filename);
		if (schematicFile.exists()) {
		    Settings.schematics.put(key, filename);
		    getLogger().info("Found " + filename + " for perm " + key);
		}
	    }
	}
	// Use physics when pasting island block schematics
	Settings.usePhysics = getConfig().getBoolean("general.usephysics",false);
	// Run level calc at login
	Settings.loginLevel = getConfig().getBoolean("general.loginlevel",false);
	// Use economy or not
	// In future expand to include internal economy
	Settings.useEconomy = getConfig().getBoolean("general.useeconomy",true);
	// Island reset commands
	Settings.resetCommands = getConfig().getStringList("general.resetcommands");
	Settings.leaveCommands = getConfig().getStringList("general.leavecommands");
	Settings.useControlPanel = getConfig().getBoolean("general.usecontrolpanel", false);
	// Check if /island command is allowed when falling
	Settings.allowTeleportWhenFalling = getConfig().getBoolean("general.allowfallingteleport", true);
	Settings.fallingCommandBlockList = getConfig().getStringList("general.blockingcommands");
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

	Settings.islandDistance = getConfig().getInt("island.distance",200);
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
	if (!getConfig().getBoolean("island.overridelimit", false)) {
	    if (Settings.island_protectionRange > (Settings.islandDistance-16)) {
		Settings.island_protectionRange = Settings.islandDistance - 16;
		getLogger().warning("*** Island protection range must be " + (Settings.islandDistance-16) + " or less, (island range -16). Setting to: " + Settings.island_protectionRange);
	    }
	}
	if (Settings.island_protectionRange < 50) {
	    Settings.island_protectionRange = 50;
	}
	Settings.resetChallenges = getConfig().getBoolean("general.resetchallenges", true);
	Settings.resetMoney = getConfig().getBoolean("general.resetmoney", true);
	Settings.clearInventory = getConfig().getBoolean("general.resetinventory", true);
	Settings.resetEnderChest = getConfig().getBoolean("general.resetenderchest", false);
	
	Settings.startingMoney = getConfig().getDouble("general.startingmoney", 0D);

	Settings.newNether = getConfig().getBoolean("general.newnether", false);
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
	//Settings.ultraSafeBoats = getConfig().getBoolean("general.ultrasafeboats", true);
	Settings.logInRemoveMobs = getConfig().getBoolean("general.loginremovemobs", true);
	Settings.islandRemoveMobs = getConfig().getBoolean("general.islandremovemobs", false);

	//getLogger().info("DEBUG: island level is " + Settings.island_level);
	// Get chest items
	final String[] chestItemString = getConfig().getString("island.chestItems").split(" ");
	//getLogger().info("DEBUG: chest items = " + chestItemString);
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
	Settings.allowPvP = getConfig().getBoolean("island.allowPvP",false);
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
	Settings.allowMonsterEggs = getConfig().getBoolean("island.allowspawneggs", false);
	Settings.allowBreeding = getConfig().getBoolean("island.allowbreeding", false);
	Settings.allowFire = getConfig().getBoolean("island.allowfire", false);
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
	Settings.allowSpawnEnchanting = getConfig().getBoolean("spawn.allowenchanting",true);
	Settings.allowSpawnAnvilUse = getConfig().getBoolean("spawn.allowanviluse",true);
	Settings.allowSpawnBeaconAccess = getConfig().getBoolean("spawn.allowbeaconaccess",false);
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
	Settings.breedingLimit = getConfig().getInt("general.breedinglimit", 0);

	Settings.removeCompleteOntimeChallenges = getConfig().getBoolean("general.removecompleteonetimechallenges",false);
	Settings.addCompletedGlow = getConfig().getBoolean("general.addcompletedglow", true);

	// Localization Locale Setting
	// Command prefix - can be added to the beginning of any message
	Locale.prefix = ChatColor.translateAlternateColorCodes('&',ChatColor.translateAlternateColorCodes('&',locale.getString("prefix", "")));

	if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
	    Locale.signLine1 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line1", "&1[A Skyblock]"));
	    Locale.signLine2 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line2", "[player]"));
	    Locale.signLine3 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line3", "Do not fall!"));
	    Locale.signLine4 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line4", "Beware!"));
	    Locale.islandhelpSpawn = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpIslandSpawn","go to ASkyBlock spawn."));
	    Locale.newsHeadline = ChatColor.translateAlternateColorCodes('&',locale.getString("news.headline","[ASkyBlock News] While you were offline..."));

	} else {
	    // AcidIsland
	    Locale.signLine1 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line1", "&1[Acid Island]"));
	    Locale.signLine2 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line2", "[player]"));
	    Locale.signLine3 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line3", "Water is acid!"));
	    Locale.signLine4 = ChatColor.translateAlternateColorCodes('&',locale.getString("sign.line4", "Beware!"));
	    Locale.islandhelpSpawn = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpIslandSpawn","go to AcidIsland spawn."));
	    Locale.newsHeadline = ChatColor.translateAlternateColorCodes('&',locale.getString("news.headline","[AcidIsland News] While you were offline..."));

	}
	Locale.changingObsidiantoLava = ChatColor.translateAlternateColorCodes('&',locale.getString("changingObsidiantoLava", "Changing obsidian back into lava. Be careful!"));
	Locale.acidLore = ChatColor.translateAlternateColorCodes('&',locale.getString("acidLore","Poison!\nBeware!\nDo not drink!"));
	Locale.acidBucket = ChatColor.translateAlternateColorCodes('&',locale.getString("acidBucket", "Acid Bucket"));
	Locale.acidBottle = ChatColor.translateAlternateColorCodes('&',locale.getString("acidBottle", "Bottle O' Acid"));
	Locale.drankAcidAndDied = ChatColor.translateAlternateColorCodes('&',locale.getString("drankAcidAndDied", "drank acid and died."));
	Locale.drankAcid = ChatColor.translateAlternateColorCodes('&',locale.getString("drankAcid", "drank acid."));
	Locale.errorUnknownPlayer = ChatColor.translateAlternateColorCodes('&',locale.getString("error.unknownPlayer","That player is unknown."));
	Locale.errorNoPermission = ChatColor.translateAlternateColorCodes('&',locale.getString("error.noPermission","You don't have permission to use that command!"));
	Locale.errorNoIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("error.noIsland","You do not have an island!"));
	Locale.errorNoIslandOther = ChatColor.translateAlternateColorCodes('&',locale.getString("error.noIslandOther","That player does not have an island!"));
	//"You must be on your island to use this command."
	Locale.errorCommandNotReady = ChatColor.translateAlternateColorCodes('&',locale.getString("error.commandNotReady","You can't use that command right now."));
	Locale.errorOfflinePlayer = ChatColor.translateAlternateColorCodes('&',locale.getString("error.offlinePlayer","That player is offline or doesn't exist."));
	Locale.errorUnknownCommand = ChatColor.translateAlternateColorCodes('&',locale.getString("error.unknownCommand","Unknown command."));
	Locale.errorNoTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("error.noTeam","That player is not in a team."));
	Locale.errorWrongWorld = ChatColor.translateAlternateColorCodes('&',locale.getString("error.wrongWorld","You cannot do that in this world."));
	Locale.islandProtected = ChatColor.translateAlternateColorCodes('&',locale.getString("islandProtected","Island protected."));
	Locale.lavaTip = ChatColor.translateAlternateColorCodes('&',locale.getString("lavaTip","Changing obsidian back into lava. Be careful!"));
	Locale.warpswelcomeLine = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.welcomeLine","[WELCOME]"));
	Locale.warpswarpTip = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.warpTip","Create a warp by placing a sign with [WELCOME] at the top."));
	Locale.warpssuccess = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.success","Welcome sign placed successfully!"));
	Locale.warpsremoved = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.removed","Welcome sign removed!"));
	Locale.warpssignRemoved = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.signRemoved","Your welcome sign was removed!"));
	Locale.warpsdeactivate = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.deactivate","Deactivating old sign!"));
	Locale.warpserrorNoRemove = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorNoRemove","You can only remove your own Welcome Sign!"));
	Locale.warpserrorNoPerm = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorNoPerm","You do not have permission to place Welcome Signs yet!"));
	Locale.warpserrorNoPlace = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorNoPlace","You must be on your island to place a Welcome Sign!"));
	Locale.warpserrorDuplicate = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorDuplicate","Sorry! There is a sign already in that location!"));
	Locale.warpserrorDoesNotExist = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorDoesNotExist","That warp doesn't exist!"));
	Locale.warpserrorNotReadyYet = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorNotReadyYet","That warp is not ready yet. Try again later."));
	Locale.warpserrorNotSafe = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorNotSafe","That warp is not safe right now. Try again later."));
	Locale.warpswarpToPlayersSign = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.warpToPlayersSign","Warp to <player>'s welcome sign."));
	Locale.warpserrorNoWarpsYet = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.errorNoWarpsYet","There are no warps available yet!"));
	Locale.warpswarpsAvailable = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.warpsAvailable","The following warps are available"));
	Locale.warpsPlayerWarped = ChatColor.translateAlternateColorCodes('&',locale.getString("warps.playerWarped", "[name] &2warped to your island!"));
	Locale.topTenheader = ChatColor.translateAlternateColorCodes('&',locale.getString("topTen.header","These are the Top 10 islands:"));
	Locale.topTenerrorNotReady = ChatColor.translateAlternateColorCodes('&',locale.getString("topTen.errorNotReady","Top ten list not generated yet!"));
	Locale.levelislandLevel = ChatColor.translateAlternateColorCodes('&',locale.getString("level.islandLevel","Island level"));
	Locale.levelerrornotYourIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("level.errornotYourIsland", "Only the island owner can do that."));
	Locale.levelCalculating = ChatColor.translateAlternateColorCodes('&',locale.getString("level.calculating","Calculating island level. This will take a few seconds..."));
	Locale.setHomehomeSet = ChatColor.translateAlternateColorCodes('&',locale.getString("sethome.homeSet","Your island home has been set to your current location."));
	Locale.setHomeerrorNotOnIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("sethome.errorNotOnIsland","You must be within your island boundaries to set home!"));
	Locale.setHomeerrorNoIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("sethome.errorNoIsland","You are not part of an island. Returning you the spawn area!"));
	Locale.challengesyouHaveCompleted = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.youHaveCompleted", "You have completed the [challenge] challenge!"));
	Locale.challengesnameHasCompleted = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.nameHasCompleted", "[name] has completed the [challenge] challenge!"));
	Locale.challengesyouRepeated = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.youRepeated", "You repeated the [challenge] challenge!"));
	Locale.challengestoComplete = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.toComplete","Complete [challengesToDo] more [thisLevel] challenges to unlock this level!"));
	Locale.challengeshelp1 = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.help1","Use /c <name> to view information about a challenge."));
	Locale.challengeshelp2 = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.help2","Use /c complete <name> to attempt to complete that challenge."));
	Locale.challengescolors = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.colors","Challenges will have different colors depending on if they are:"));
	Locale.challengescomplete = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.complete", "Complete"));
	Locale.challengesincomplete = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.incomplete","Incomplete"));
	Locale.challengescompleteNotRepeatable = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.completeNotRepeatable","Completed(not repeatable)"));
	Locale.challengescompleteRepeatable = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.completeRepeatable","Completed(repeatable)"));
	Locale.challengesname = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.name","Challenge Name"));
	Locale.challengeslevel = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.level","Level"));
	Locale.challengesitemTakeWarning = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.itemTakeWarning","All required items are taken when you complete this challenge!"));
	Locale.challengesnotRepeatable = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.notRepeatable","This Challenge is not repeatable!"));
	Locale.challengesfirstTimeRewards = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.firstTimeRewards","First time reward(s)"));
	Locale.challengesrepeatRewards = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.repeatRewards","Repeat reward(s)"));
	Locale.challengesexpReward = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.expReward","Exp reward"));
	Locale.challengesmoneyReward = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.moneyReward","Money reward"));
	Locale.challengestoCompleteUse = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.toCompleteUse","To complete this challenge, use"));
	Locale.challengesinvalidChallengeName = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.invalidChallengeName","Invalid challenge name! Use /c help for more information"));
	Locale.challengesrewards = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.rewards","Reward(s)"));
	Locale.challengesyouHaveNotUnlocked = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.youHaveNotUnlocked","You have not unlocked this challenge yet!"));
	Locale.challengesunknownChallenge = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.unknownChallenge","Unknown challenge name (check spelling)!"));
	Locale.challengeserrorNotEnoughItems = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.errorNotEnoughItems","You do not have enough of the required item(s)"));
	Locale.challengeserrorNotOnIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.errorNotOnIsland","You must be on your island to do that!"));
	Locale.challengeserrorNotCloseEnough = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.errorNotCloseEnough","You must be standing within 10 blocks of all required items."));
	Locale.challengeserrorItemsNotThere = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.errorItemsNotThere","All required items must be close to you on your island!"));
	Locale.challengeserrorIslandLevel = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.errorIslandLevel","Your island must be level [level] to complete this challenge!"));
	Locale.challengeserrorRewardProblem = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.errorRewardProblem", "There was a problem giving your reward. Ask Admin to check log!"));
	Locale.challengesguiTitle = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.guititle", "Challenges"));
	Locale.challengeserrorYouAreMissing = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.erroryouaremissing", "You are missing"));
	Locale.challengesNavigation = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.navigation", "Click to see [level] challenges!"));
	Locale.challengescompletedtimes = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.completedtimes", "Completed [donetimes] out of [maxtimes]"));
	Locale.challengesmaxreached = ChatColor.translateAlternateColorCodes('&',locale.getString("challenges.maxreached", "Max reached [donetimes] out of [maxtimes]"));
	Locale.islandteleport = ChatColor.translateAlternateColorCodes('&',locale.getString("island.teleport","Teleporting you to your island. (/island help for more info)"));
	Locale.islandcannotTeleport = ChatColor.translateAlternateColorCodes('&',locale.getString("island.cannotTeleport","You cannot teleport when falling!"));
	Locale.islandnew = ChatColor.translateAlternateColorCodes('&',locale.getString("island.new","Creating a new island for you..."));
	Locale.islandSubTitle = locale.getString("island.subtitle","by tastybento");
	Locale.islandDonate = locale.getString("island.donate","ASkyBlock by tastybento, click here to donate via PayPal!");
	Locale.islandTitle = locale.getString("island.title","A SkyBlock");
	Locale.islandURL = locale.getString("island.url","https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ZSBJG5J2E3B7U");
	Locale.islanderrorCouldNotCreateIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("island.errorCouldNotCreateIsland","Could not create your Island. Please contact a server moderator."));
	Locale.islanderrorYouDoNotHavePermission = ChatColor.translateAlternateColorCodes('&',locale.getString("island.errorYouDoNotHavePermission", "You do not have permission to use that command!"));
	Locale.islandresetOnlyOwner = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetOnlyOwner","Only the owner may restart this island. Leave this island in order to start your own (/island leave)."));
	Locale.islandresetMustRemovePlayers = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetMustRemovePlayers","You must remove all players from your island before you can restart it (/island kick <player>). See a list of players currently part of your island using /island team."));
	Locale.islandresetPleaseWait = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetPleaseWait","Please wait, generating new island"));
	Locale.islandresetWait = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetWait","You have to wait [time] seconds before you can do that again."));
	Locale.islandresetConfirm = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetConfirm", "Type /island confirm within [seconds] seconds to delete your island and restart!"));
	Locale.islandhelpIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpIsland","start an island, or teleport to your island."));
	Locale.islandhelpTeleport = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpTeleport", "teleport to your island."));
	Locale.islandhelpControlPanel = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpControlPanel","open the island GUI."));
	Locale.islandhelpRestart = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpRestart","restart your island and remove the old one."));
	Locale.islandDeletedLifeboats = ChatColor.translateAlternateColorCodes('&',locale.getString("island.islandDeletedLifeboats","Island deleted! Head to the lifeboats!"));
	Locale.islandhelpSetHome = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpSetHome","set your teleport point for /island."));
	Locale.islandhelpLevel = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpLevel","calculate your island level"));
	Locale.islandhelpLevelPlayer = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpLevelPlayer","see another player's island level."));
	Locale.islandhelpTop = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpTop","see the top ranked islands."));
	Locale.islandhelpWarps = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpWarps","Lists all available welcome-sign warps."));
	Locale.islandhelpWarp = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpWarp","Warp to <player>'s welcome sign."));
	Locale.islandhelpTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpTeam","view your team information."));
	Locale.islandhelpInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpInvite","invite a player to join your island."));
	Locale.islandhelpLeave = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpLeave","leave another player's island."));
	Locale.islandhelpKick = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpKick","remove a team member from your island."));
	Locale.islandhelpExpel = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpExpel","force a player from your island."));
	Locale.islandHelpSettings = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpSettings","see island protection and game settings"));
	Locale.islandHelpChallenges = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpChallenges","/challenges: &fshow challenges"));
	Locale.adminHelpHelp = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.help","Acid Admin Commands:"));
	Locale.islandhelpAcceptReject = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpAcceptReject","accept or reject an invitation."));
	Locale.islandhelpMakeLeader = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpMakeLeader","transfer the island to <player>."));
	Locale.islanderrorLevelNotReady = ChatColor.translateAlternateColorCodes('&',locale.getString("island.errorLevelNotReady","Can't use that command right now! Try again in a few seconds."));
	Locale.islanderrorInvalidPlayer = ChatColor.translateAlternateColorCodes('&',locale.getString("island.errorInvalidPlayer","That player is invalid or does not have an island!"));
	Locale.islandislandLevelis = ChatColor.translateAlternateColorCodes('&',locale.getString("island.islandLevelis","Island level is"));
	Locale.invitehelp = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.help","Use [/island invite <playername>] to invite a player to your island."));
	Locale.inviteyouCanInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.youCanInvite","You can invite [number] more players."));
	Locale.inviteyouCannotInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.youCannotInvite","You can't invite any more players."));
	Locale.inviteonlyIslandOwnerCanInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.onlyIslandOwnerCanInvite","Only the island's owner can invite!"));
	Locale.inviteyouHaveJoinedAnIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.youHaveJoinedAnIsland","You have joined an island! Use /island team to see the other members."));
	Locale.invitehasJoinedYourIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.hasJoinedYourIsland","[name] has joined your island!"));
	Locale.inviteerrorCantJoinIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.errorCantJoinIsland","You couldn't join the island, maybe it's full."));
	Locale.inviteerrorYouMustHaveIslandToInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.errorYouMustHaveIslandToInvite","You must have an island in order to invite people to it!"));
	Locale.inviteerrorYouCannotInviteYourself = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.errorYouCannotInviteYourself","You can not invite yourself!"));
	Locale.inviteremovingInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.removingInvite","Removing your previous invite."));
	Locale.inviteinviteSentTo = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.inviteSentTo","Invite sent to [name]"));
	Locale.invitenameHasInvitedYou = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.nameHasInvitedYou","[name] has invited you to join their island!"));
	Locale.invitetoAcceptOrReject = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.toAcceptOrReject","to accept or reject the invite."));
	Locale.invitewarningYouWillLoseIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.warningYouWillLoseIsland","WARNING: You will lose your current island if you accept!"));
	Locale.inviteerrorYourIslandIsFull = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.errorYourIslandIsFull","Your island is full, you can't invite anyone else."));
	Locale.inviteerrorThatPlayerIsAlreadyInATeam = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.errorThatPlayerIsAlreadyInATeam","That player is already in a team."));
	Locale.inviteerrorCoolDown = ChatColor.translateAlternateColorCodes('&',locale.getString("invite.errorCoolDown","You can invite that player again in [time] minutes"));
	Locale.rejectyouHaveRejectedInvitation = ChatColor.translateAlternateColorCodes('&',locale.getString("reject.youHaveRejectedInvitation","You have rejected the invitation to join an island."));
	Locale.rejectnameHasRejectedInvite = ChatColor.translateAlternateColorCodes('&',locale.getString("reject.nameHasRejectedInvite","[name] has rejected your island invite!"));
	Locale.rejectyouHaveNotBeenInvited = ChatColor.translateAlternateColorCodes('&',locale.getString("reject.youHaveNotBeenInvited","You had not been invited to join a team."));
	Locale.leaveerrorYouAreTheLeader = ChatColor.translateAlternateColorCodes('&',locale.getString("leave.errorYouAreTheLeader","You are the leader, use /island remove <player> instead."));
	Locale.leaveyouHaveLeftTheIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("leave.youHaveLeftTheIsland","You have left the island and returned to the player spawn."));
	Locale.leavenameHasLeftYourIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("leave.nameHasLeftYourIsland","[name] has left your island!"));
	Locale.leaveerrorYouCannotLeaveIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("leave.errorYouCannotLeaveIsland","You can't leave your island if you are the only person. Try using /island restart if you want a new one!"));
	Locale.leaveerrorYouMustBeInWorld = ChatColor.translateAlternateColorCodes('&',locale.getString("leave.errorYouMustBeInWorld","You must be in the island world to leave your team!"));
	Locale.leaveerrorLeadersCannotLeave = ChatColor.translateAlternateColorCodes('&',locale.getString("leave.errorLeadersCannotLeave","Leaders cannot leave an island. Make someone else the leader fist using /island makeleader <player>"));
	Locale.teamlistingMembers = ChatColor.translateAlternateColorCodes('&',locale.getString("team.listingMembers","Listing your island members"));
	Locale.kickerrorPlayerNotInTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("kick.errorPlayerNotInTeam","That player is not in your team!"));
	Locale.kicknameRemovedYou = ChatColor.translateAlternateColorCodes('&',locale.getString("kick.nameRemovedYou","[name] has removed you from their island!"));
	Locale.kicknameRemoved = ChatColor.translateAlternateColorCodes('&',locale.getString("kick.nameRemoved","[name] has been removed from the island."));
	Locale.kickerrorNotPartOfTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("kick.errorNotPartOfTeam","That player is not part of your island team!"));
	Locale.kickerrorOnlyLeaderCan = ChatColor.translateAlternateColorCodes('&',locale.getString("kick.errorOnlyLeaderCan","Only the island's owner may remove people from the island!"));
	Locale.kickerrorNoTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("kick.errorNoTeam","No one else is on your island, are you seeing things?"));
	Locale.makeLeadererrorPlayerMustBeOnline = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.errorPlayerMustBeOnline","That player must be online to transfer the island."));
	Locale.makeLeadererrorYouMustBeInTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.errorYouMustBeInTeam","You must be in a team to transfer your island."));
	Locale.makeLeadererrorRemoveAllPlayersFirst = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.errorRemoveAllPlayersFirst","Remove all players from your team other than the player you are transferring to."));
	Locale.makeLeaderyouAreNowTheOwner = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.youAreNowTheOwner","You are now the owner of your island."));
	Locale.makeLeadernameIsNowTheOwner = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.nameIsNowTheOwner","[name] is now the owner of your island!"));
	Locale.makeLeadererrorThatPlayerIsNotInTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.errorThatPlayerIsNotInTeam","That player is not part of your island team!"));
	Locale.makeLeadererrorNotYourIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.errorNotYourIsland","This isn't your island, so you can't give it away!"));
	Locale.makeLeadererrorGeneralError = ChatColor.translateAlternateColorCodes('&',locale.getString("makeleader.errorGeneralError","Could not make leader!"));
	Locale.adminHelpHelp = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.help","Could not change leaders."));
	Locale.adminHelpreload = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.reload","reload configuration from file."));
	Locale.adminHelptopTen = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.topTen","manually update the top 10 list"));
	Locale.adminHelpregister = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.register","set a player's island to your location"));
	Locale.adminHelpunregister = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.unregister","deletes a player without deleting the island blocks"));
	Locale.adminHelpdelete = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.delete","delete an island (removes blocks)."));
	Locale.adminHelpcompleteChallenge = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.completeChallenge","marks a challenge as complete"));
	Locale.adminHelpresetChallenge = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.resetChallenge","marks a challenge as incomplete"));
	Locale.adminHelpresetAllChallenges = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.resetAllChallenges","resets all of the player's challenges"));
	Locale.adminHelppurge = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.purge","delete inactive islands older than [TimeInDays]."));
	Locale.adminHelppurgeholes = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.purgeholes","free up island holes for reuse."));
	Locale.adminHelpinfo = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.info","check information on the given player."));
	Locale.adminHelpSetSpawn = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.setspawn","sets the island world spawn to a location close to you."));
	Locale.adminHelpSetRange = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.setrange","changes the island's protection range."));
	Locale.adminHelpinfoIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.infoisland","provide info on the nearest island."));
	Locale.adminHelptp = ChatColor.translateAlternateColorCodes('&',locale.getString("adminHelp.tp", "Teleport to a player's island."));
	Locale.reloadconfigReloaded = ChatColor.translateAlternateColorCodes('&',locale.getString("reload.configReloaded","Configuration reloaded from file."));
	Locale.adminTopTengenerating = ChatColor.translateAlternateColorCodes('&',locale.getString("adminTopTen.generating","Generating the Top Ten list"));
	Locale.adminTopTenfinished = ChatColor.translateAlternateColorCodes('&',locale.getString("adminTopTen.finished","Finished generation of the Top Ten list"));
	Locale.purgealreadyRunning = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.alreadyRunning","Purge is already running, please wait for it to finish!"));
	Locale.purgeusage = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.usage","Usage: /[label] purge [TimeInDays]"));
	Locale.purgecalculating = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.calculating","Calculating which islands have been inactive for more than [time] days."));
	Locale.purgenoneFound = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.noneFound","No inactive islands to remove."));
	Locale.purgethisWillRemove = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.thisWillRemove","This will remove [number] inactive islands!"));
	Locale.purgewarning = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.warning","DANGER! Do not run this with players on the server! MAKE BACKUP OF WORLD!"));
	Locale.purgetypeConfirm = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.typeConfirm","Type [label] confirm to proceed within 10 seconds"));
	Locale.purgepurgeCancelled = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.purgeCancelled","Purge cancelled."));
	Locale.purgefinished = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.finished","Finished purging of inactive islands."));
	Locale.purgeremovingName = ChatColor.translateAlternateColorCodes('&',locale.getString("purge.removingName","Purge: Removing [name]'s island"));
	Locale.confirmerrorTimeLimitExpired = ChatColor.translateAlternateColorCodes('&',locale.getString("confirm.errorTimeLimitExpired","Time limit expired! Issue command again."));
	Locale.deleteremoving = ChatColor.translateAlternateColorCodes('&',locale.getString("delete.removing","Removing [name]'s island."));
	Locale.registersettingIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("register.settingIsland","Set [name]'s island to the bedrock nearest you."));
	Locale.registererrorBedrockNotFound = ChatColor.translateAlternateColorCodes('&',locale.getString("register.errorBedrockNotFound","Error: unable to set the island!"));
	Locale.adminInfoislandLocation = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.islandLocation","Island Location"));
	Locale.adminInfoerrorNotPartOfTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.errorNotPartOfTeam","That player is not a member of an island team."));
	Locale.adminInfoerrorNullTeamLeader = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.errorNullTeamLeader","Team leader should be null!"));
	Locale.adminInfoerrorTeamMembersExist = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.errorTeamMembersExist","Player has team members, but shouldn't!"));
	Locale.resetChallengessuccess = ChatColor.translateAlternateColorCodes('&',locale.getString("resetallchallenges.success","[name] has had all challenges reset."));
	Locale.checkTeamcheckingTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("checkTeam.checkingTeam","Checking Team of [name]"));
	Locale.completeChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',locale.getString("completechallenge.errorChallengeDoesNotExist","Challenge doesn't exist or is already completed"));
	Locale.completeChallengechallangeCompleted = ChatColor.translateAlternateColorCodes('&',locale.getString("completechallenge.challangeCompleted","[challengename] has been completed for [name]"));
	Locale.resetChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',locale.getString("resetchallenge.errorChallengeDoesNotExist","[challengename] has been reset for [name]"));
	Locale.confirmerrorTimeLimitExpired = ChatColor.translateAlternateColorCodes('&',locale.getString("confirm.errorTimeLimitExpired","Time limit expired! Issue command again."));
	Locale.deleteremoving = ChatColor.translateAlternateColorCodes('&',locale.getString("delete.removing","Removing [name]'s island."));
	Locale.registersettingIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("register.settingIsland","Set [name]'s island to the bedrock nearest you."));
	Locale.registererrorBedrockNotFound = ChatColor.translateAlternateColorCodes('&',locale.getString("register.errorBedrockNotFound","Error: unable to set the island!"));
	Locale.adminInfoislandLocation = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.islandLocation","Island Location"));
	Locale.adminInfoerrorNotPartOfTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.errorNotPartOfTeam","That player is not a member of an island team."));
	Locale.adminInfoerrorNullTeamLeader = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.errorNullTeamLeader","Team leader should be null!"));
	Locale.adminInfoerrorTeamMembersExist = ChatColor.translateAlternateColorCodes('&',locale.getString("adminInfo.errorTeamMembersExist","Player has team members, but shouldn't!"));
	Locale.resetChallengessuccess = ChatColor.translateAlternateColorCodes('&',locale.getString("resetallchallenges.success","[name] has had all challenges reset."));
	Locale.checkTeamcheckingTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("checkTeam.checkingTeam","Checking Team of [name]"));
	Locale.completeChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',locale.getString("completechallenge.errorChallengeDoesNotExist","Challenge doesn't exist or is already completed"));
	Locale.completeChallengechallangeCompleted = ChatColor.translateAlternateColorCodes('&',locale.getString("completechallenge.challangeCompleted","[challengename] has been completed for [name]"));
	Locale.resetChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',locale.getString("resetchallenge.errorChallengeDoesNotExist","Challenge doesn't exist or isn't yet completed"));
	Locale.resetChallengechallengeReset = ChatColor.translateAlternateColorCodes('&',locale.getString("resetchallenge.challengeReset","[challengename] has been reset for [name]"));
	Locale.netherSpawnIsProtected = ChatColor.translateAlternateColorCodes('&',locale.getString("nether.spawnisprotected", "The Nether spawn area is protected."));
	Locale.islandhelpMiniShop = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.islandhelpMiniShop","Opens the MiniShop" ));
	Locale.islandMiniShopTitle = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.title","MiniShop" ));
	Locale.minishopBuy = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.buy","Buy(Left Click)"));
	Locale.minishopSell = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.sell","Sell(Right Click)"));
	Locale.minishopYouBought = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.youbought", "You bought [number] [description] for [price]"));
	Locale.minishopSellProblem = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.sellproblem", "You do not have enough [description] to sell."));
	Locale.minishopYouSold = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.yousold","You sold [number] [description] for [price]"));
	Locale.minishopBuyProblem  = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.buyproblem", "There was a problem purchasing [description]"));
	Locale.minishopYouCannotAfford = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.youcannotafford", "You cannot afford [description]!"));
	Locale.minishopOutOfStock = ChatColor.translateAlternateColorCodes('&',locale.getString("minishop.outofstock", "Out Of Stock"));
	Locale.boatWarningItIsUnsafe = ChatColor.translateAlternateColorCodes('&',locale.getString("boats.warning", "It's unsafe to exit the boat right now..."));
	Locale.adminHelpclearReset = ChatColor.translateAlternateColorCodes('&',locale.getString("general.clearreset", "resets the island reset limit for player."));
	Locale.resetYouHave = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetYouHave","You have [number] resets left."));
	Locale.islandResetNoMore = ChatColor.translateAlternateColorCodes('&',locale.getString("island.resetNoMore", "No more resets are allowed for your island!"));
	Locale.clearedResetLimit = ChatColor.translateAlternateColorCodes('&',locale.getString("resetTo", "Cleared reset limit"));

	Locale.islandhelpBiome = ChatColor.translateAlternateColorCodes('&',locale.getString("biome.help","open the biome GUI."));
	Locale.biomeSet = ChatColor.translateAlternateColorCodes('&',locale.getString("biome.set","Island biome set to [biome]!"));
	Locale.biomeUnknown = ChatColor.translateAlternateColorCodes('&',locale.getString("biome.unknown","Unknown biome!"));
	Locale.biomeYouBought = ChatColor.translateAlternateColorCodes('&',locale.getString("biome.youbought","Purchased for [cost]!"));
	Locale.biomePanelTitle = ChatColor.translateAlternateColorCodes('&',locale.getString("biome.paneltitle", "Select A Biome"));
	Locale.expelNotOnIsland = ChatColor.translateAlternateColorCodes('&',locale.getString("expel.notonisland", "Player is not trespassing on your island!"));
	Locale.expelSuccess = ChatColor.translateAlternateColorCodes('&',locale.getString("expel.success", "You expelled [name]!"));
	Locale.expelExpelled = ChatColor.translateAlternateColorCodes('&',locale.getString("expel.expelled", "You were expelled from that island!"));
	Locale.expelFail = ChatColor.translateAlternateColorCodes('&',locale.getString("expel.fail", "[name] cannot be expelled!"));
	Locale.expelNotYourself = ChatColor.translateAlternateColorCodes('&',locale.getString("expel.notyourself", "You cannot expel yourself!"));
	Locale.moblimitsError = ChatColor.translateAlternateColorCodes('&',locale.getString("moblimits.error", "Island breeding limit of [number] reached!"));
	Locale.coopRemoved = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.removed", "[name] remove your coop status!"));
	Locale.coopRemoveSuccess = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.removesuccess", "[name] is no longer a coop player."));
	Locale.coopSuccess = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.success", "[name] is now a coop player until they log out or you expel them."));
	Locale.coopMadeYouCoop = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.madeyoucoopy", "[name] made you a coop player until you log out or they expel you."));
	Locale.coopOnYourTeam = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.onyourteam", "Player is already on your team!"));
	Locale.islandhelpCoop = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.help", "temporarily give a player full access to your island"));
	Locale.coopInvited = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.invited", "[name] made [player] a coop player!"));
	Locale.coopUseExpel = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.useexpel", "Use expel to remove."));
	Locale.lockIslandLocked = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.islandlocked", "Island is locked to visitors"));
	Locale.lockNowEntering = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.nowentering", "Now entering [name]'s island"));
	Locale.lockNowLeaving = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.nowleaving", "Now leaving [name]'s island"));
	Locale.lockLocking = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.locking", "Locking island"));
	Locale.lockUnlocking = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.unlocking", "Unlocking island"));
	Locale.islandHelpLock = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpLock", "Locks island so visitors cannot enter it"));
	Locale.helpColor = ChatColor.translateAlternateColorCodes('&',locale.getString("island.helpColor", "&e"));
	Locale.lockPlayerLocked = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.playerlocked", "[name] locked the island"));
	Locale.lockPlayerUnlocked = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.playerunlocked", "[name] unlocked the island"));
	Locale.lockEnteringSpawn = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.enteringspawn", "Entering Spawn"));
	Locale.lockLeavingSpawn = ChatColor.translateAlternateColorCodes('&',locale.getString("lock.leavingspawn", "Leaving Spawn"));

    }

    /*
     * (non-Javadoc)
     * 
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
	    saveWarpList();
	    saveMessages();
	    topTenSave();
	} catch (final Exception e) {
	    getLogger().severe("Something went wrong saving files!");
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
	// Check to see if island distance is set or not
	if (getConfig().getInt("island.distance",-1) < 1) {
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
	loadPluginConfig();
	if (Settings.useEconomy && !VaultHelper.setupEconomy()) {
	    getLogger().warning("Could not set up economy! - Running without an economy.");
	    Settings.useEconomy = false;
	}
	if (!VaultHelper.setupPermissions()) {
	    getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
	    getServer().getPluginManager().disablePlugin(this);
	    return;
	} 

	// This can no longer be run in onEnable because the plugin is loaded at startup and so key variables are
	// not known to the server. Instead it is run one tick after startup.
	//getIslandWorld();

	// Set and make the player's directory if it does not exist and then load players into memory
	playersFolder = new File(getDataFolder() + File.separator + "players");
	if (!playersFolder.exists()) {
	    playersFolder.mkdir();
	}
	// Set up commands for this plugin
	if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
	    getCommand("island").setExecutor(new IslandCmd(this));
	    getCommand("asc").setExecutor(getChallenges());
	    getCommand("asadmin").setExecutor(new AdminCmd(this));
	} else {
	    getCommand("ai").setExecutor(new IslandCmd(this));
	    getCommand("aic").setExecutor(getChallenges());
	    getCommand("acid").setExecutor(new AdminCmd(this));
	}
	// Register events that this plugin uses
	//registerEvents();
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
		// Create the world if it does not exist. This is run after the server starts.
		getIslandWorld();
		// Load warps
		loadWarpList();
		// Minishop - must wait for economy to load before we can use econ 
		getServer().getPluginManager().registerEvents(new ControlPanel(plugin), plugin);
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
			// load the list - order matters - grid first, then top ten to optimize upgrades
			// Load grid
			grid = new GridManager(plugin);
			topTenLoad();
			getLogger().info("All files loaded. Ready to play...");
		    }
		});
		// This part will kill monsters if they fall into the water because it
		// is acid
		if (Settings.mobAcidDamage > 0D || Settings.animalAcidDamage > 0D) {
		    getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
			    List<Entity> entList = acidWorld.getEntities();
			    for (Entity current : entList) {
				if ((current instanceof Monster) && Settings.mobAcidDamage > 0D) {
				    if ((current.getLocation().getBlock().getType() == Material.WATER)
					    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
					((Monster) current).damage(Settings.mobAcidDamage);
					//getLogger().info("Killing monster");
				    }
				} else if ((current instanceof Animals)  && Settings.animalAcidDamage > 0D) {
				    if ((current.getLocation().getBlock().getType() == Material.WATER)
					    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
					if (!current.getType().equals(EntityType.CHICKEN)) {
					    ((Animals) current).damage(Settings.animalAcidDamage);
					} else if (Settings.damageChickens) {
					    ((Animals) current).damage(Settings.animalAcidDamage);
					}
					//getLogger().info("Killing animal");			    
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
     * Checks if an online player is on their island, on a team island or on a coop island
     * 
     * @param player
     *            - the player who is being checked
     * @return - true if they are on an island they have rights to be on, otherwise false
     */
    protected boolean playerIsOnIsland(final Player player) {
	// Make a list of test locations and test them
	Set<Location> islandTestLocations = new HashSet<Location>();
	if (players.hasIsland(player.getUniqueId())) {
	    islandTestLocations.add(players.getIslandLocation(player.getUniqueId()));
	} else if (players.inTeam(player.getUniqueId())) {
	    islandTestLocations.add(players.get(player.getUniqueId()).getTeamIslandLocation());
	}
	// Check coop locations
	islandTestLocations.addAll(CoopPlay.getInstance().getCoopIslands(player));
	if (islandTestLocations.isEmpty()) {
	    return false;
	}
	// Run through all the locations
	for (Location islandTestLocation : islandTestLocations) {
	    if (islandTestLocation != null) {
		int protectionRange = Settings.island_protectionRange;
		if (grid.getIslandAt(islandTestLocation) != null) {
		    // Get the protection range for this location if possible
		    Island island = grid.getProtectedIslandAt(islandTestLocation);
		    if (island != null) {
			// We are in a protected island area.
			protectionRange = island.getProtectionSize();
		    }
		}
		if (player.getLocation().getX() > islandTestLocation.getX() - protectionRange / 2
			&& player.getLocation().getX() < islandTestLocation.getX() + protectionRange / 2
			&& player.getLocation().getZ() > islandTestLocation.getZ() - protectionRange / 2
			&& player.getLocation().getZ() < islandTestLocation.getZ() + protectionRange / 2) {
		    return true;
		}
	    }
	}
	return false;
    }
    /**
     * Checks to see if a player is trespassing on another player's island
     * Both players must be online.
     * @param owner - owner or team member of an island
     * @param target
     * @return true if they are on the island otherwise false.
     */
    protected boolean isOnIsland(final Player owner, final Player target) {
	// Get the island location of owner
	Location islandTestLocation = null;
	if (players.inTeam(owner.getUniqueId())) {
	    // Is the target in the owner's team?
	    if (players.getMembers(players.getTeamLeader(owner.getUniqueId())).contains(target.getUniqueId())) {
		// Yes, so this is not a trespass for sure
		return false;
	    }
	    // No, so check where the target is now
	    islandTestLocation = players.getTeamIslandLocation(owner.getUniqueId());
	} else {
	    islandTestLocation = players.getIslandLocation(owner.getUniqueId());
	}
	// Check position of target
	if (islandTestLocation == null) {
	    return false;
	}
	if (target.getWorld().equals(islandTestLocation.getWorld())) {
	    int protectionRange = Settings.island_protectionRange;
	    if (grid.getIslandAt(islandTestLocation) != null) {

		Island island = grid.getProtectedIslandAt(islandTestLocation);
		// Get the protection range for this location if possible
		if (island != null) {
		    // We are in a protected island area.
		    protectionRange = island.getProtectionSize();
		}
	    }
	    if (target.getLocation().getX() > islandTestLocation.getX() - protectionRange / 2
		    && target.getLocation().getX() < islandTestLocation.getX() + protectionRange / 2
		    && target.getLocation().getZ() > islandTestLocation.getZ() - protectionRange / 2
		    && target.getLocation().getZ() < islandTestLocation.getZ() + protectionRange / 2) {
		return true;
	    }
	}
	return false;	
    }

    /**
     * Checks if a specific location is within the protected range of an island owned by the player
     * @param player
     * @param loc
     * @return
     */
    protected boolean locationIsOnIsland(final Player player, final Location loc) {
	// Get the player's island from the grid if it exists
	Island island = grid.getIslandAt(loc);
	if (island != null) {
	    // On an island in the grid
	    if (island.onIsland(loc) && island.getMembers().contains(player.getUniqueId())) {
		// In a protected zone but is on the list of acceptable players
		return true;
	    } else {
		// Not allowed
		return false;
	    }
	}
	// Not in the grid, so do it the old way
	// Make a list of test locations and test them
	Set<Location> islandTestLocations = new HashSet<Location>();
	if (players.hasIsland(player.getUniqueId())) {
	    islandTestLocations.add(players.getIslandLocation(player.getUniqueId()));
	} else if (players.inTeam(player.getUniqueId())) {
	    islandTestLocations.add(players.get(player.getUniqueId()).getTeamIslandLocation());
	}
	// Check any coop locations
	islandTestLocations.addAll(CoopPlay.getInstance().getCoopIslands(player));
	if (islandTestLocations.isEmpty()) {
	    return false;
	}
	// Run through all the locations
	for (Location islandTestLocation : islandTestLocations) {
	    if (loc.getWorld().equals(islandTestLocation.getWorld())) {
		if (loc.getX() >= islandTestLocation.getX() - Settings.island_protectionRange / 2
			&& loc.getX() < islandTestLocation.getX() + Settings.island_protectionRange / 2
			&& loc.getZ() >= islandTestLocation.getZ() - Settings.island_protectionRange / 2
			&& loc.getZ() < islandTestLocation.getZ() + Settings.island_protectionRange / 2) {
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Finds out if location is within a set of island locations and returns the one that is there or null if not
     * @param islandTestLocations
     * @param loc
     * @return
     */
    protected Location locationIsOnIsland(final Set<Location> islandTestLocations, final Location loc) {
	// Run through all the locations
	for (Location islandTestLocation : islandTestLocations) {
	    if (loc.getWorld().equals(islandTestLocation.getWorld())) {
		if (grid.getIslandAt(islandTestLocation) != null) {
		    // Get the protection range for this location if possible
		    Island island = grid.getProtectedIslandAt(islandTestLocation);
		    if (island != null) {
			// We are in a protected island area.
			return island.getCenter();
		    }
		} else if (loc.getX() > islandTestLocation.getX() - Settings.island_protectionRange / 2
			&& loc.getX() < islandTestLocation.getX() + Settings.island_protectionRange / 2
			&& loc.getZ() > islandTestLocation.getZ() - Settings.island_protectionRange / 2
			&& loc.getZ() < islandTestLocation.getZ() + Settings.island_protectionRange / 2) {
		    return islandTestLocation;
		}
	    }
	}
	return null;
    }

    /**
     * Registers events
     */
    protected void registerEvents() {
	final PluginManager manager = getServer().getPluginManager();
	// Nether portal events
	manager.registerEvents(new NetherPortals(this), this);
	// Island Protection events
	manager.registerEvents(new IslandGuard(this), this);
	// New V1.8 events
	Class<?> clazz;
	try {
	    clazz = Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
	} catch (Exception e) {
	    getLogger().info("No PlayerInteractAtEntityEvent found.");
	    clazz = null;
	}
	if (clazz != null) {
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
	//manager.registerEvents(new ControlPanel(), this);
	// Change names of inventory items
	manager.registerEvents(new AcidInventory(this), this);
	// Biomes
	// Load Biomes
	biomes = new Biomes(this);
	manager.registerEvents(biomes, this);
    }

    protected void unregisterEvents() {
	HandlerList.unregisterAll(warpSignsListener);
	HandlerList.unregisterAll(lavaListener);	
    }

    protected void restartEvents() {
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
    protected void removeMobs(final Location l) {
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
     * This removes mobs from an island
     * 
     * @param loc
     *            - a Location
     */
    protected void removeMobsFromIsland(final Location loc) {
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
			Island spawn = grid.getSpawn();
			if (spawn != null) {
			    // go to island spawn
			    pl.teleport(acidWorld.getSpawnLocation());
			    getLogger().warning("During island deletion player " + pl.getName() + " sent to spawn.");
			} else {
			    if (!pl.performCommand(Settings.SPAWNCOMMAND)) {
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
	}
    }

    /**
     * Transfers ownership of an island from one player to another
     * 
     * @param oldOwner
     * @param newOwner
     * @return
     */
    protected boolean transferIsland(final UUID oldOwner, final UUID newOwner) {
	if (players.hasIsland(oldOwner)) {
	    Location islandLoc = players.getIslandLocation(oldOwner);
	    players.setHasIsland(newOwner, true);
	    players.setIslandLocation(newOwner, islandLoc);
	    //players.setIslandLevel(newOwner, players.getIslandLevel(oldOwner));
	    players.setTeamIslandLocation(newOwner, null);
	    players.setHasIsland(oldOwner, false);
	    players.setIslandLocation(oldOwner, null);
	    //players.setIslandLevel(oldOwner, 0);
	    players.setTeamIslandLocation(oldOwner, islandLoc);
	    // Update grid
	    Island island = grid.getIslandAt(islandLoc);
	    if (island != null) {
		grid.setIslandOwner(island, newOwner);
	    }
	    // Update top ten list
	    // Remove old owner score from top ten list
	    topTenList.remove(oldOwner);
	    return true;
	}
	return false;
    }

    /**
     * Loads the top ten from the file system topten.yml. If it does not exist then the top ten is created
     */
    protected void topTenLoad() {
	topTenList.clear();
	// Check to see if the top ten list exists
	File topTenFile = new File(getDataFolder(),"topten.yml");
	if (!topTenFile.exists()) {
	    getLogger().warning("Top ten file does not exist - creating it. This could take some time with a large number of players");
	    topTenCreate();
	    getLogger().warning("Completed top ten creation.");
	} else {
	    // Load the top ten
	    YamlConfiguration topTenConfig = loadYamlFile("topten.yml");
	    // Load the values
	    if (topTenConfig.isSet("topten")) {
		for (String playerUUID : topTenConfig.getConfigurationSection("topten").getKeys(false)) {
		    //getLogger().info(playerUUID);
		    try {
			UUID uuid = UUID.fromString(playerUUID);
			//getLogger().info(uuid.toString());
			int level = topTenConfig.getInt("topten." + playerUUID);
			//getLogger().info("Level = " + level);
			topTenAddEntry(uuid, level);
		    } catch (Exception e) {
			e.printStackTrace();
			getLogger().severe("Problem loading top ten list - recreating - this may take some time");
			topTenCreate();
		    }
		}
	    }
	}
    }

    /**
     * Adds a player to the top ten, if the level is good enough
     * @param ownerUUID
     * @param level
     */
    protected void topTenAddEntry(UUID ownerUUID, int level) {
	// Special case for removals
	if (level <1 ) {
	    if (topTenList.containsKey(ownerUUID)) {
		topTenList.remove(ownerUUID);
	    }
	    return;
	}
	// Only keep the top 20
	topTenList.put(ownerUUID, level);
	topTenList = MapUtil.sortByValue(topTenList);
	//getLogger().info("DEBUG: +" + level + ": " + topTenList.values().toString());
    }

    /**
     * Removes ownerUUID from the top ten list
     * @param ownerUUID
     */
    protected void topTenRemoveEntry(UUID ownerUUID) {
	topTenList.remove(ownerUUID);
    }
    /**
     * Generates a sorted map of islands for the Top Ten list from all player files
     */
    protected void topTenCreate() {
	// This map is a list of owner and island level
	YamlConfiguration player = new YamlConfiguration();
	int index = 1;
	for (final File f : playersFolder.listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    String playerUUIDString = fileName.substring(0, fileName.length() - 4);
		    final UUID playerUUID = UUID.fromString(playerUUIDString);
		    if (playerUUID == null) {
			getLogger().warning("Player file contains erroneous UUID data.");
			getLogger().info("Looking at " + playerUUIDString);
		    }
		    player.load(f);
		    index++;
		    if (index%1000 == 0) {
			getLogger().info("Processed " + index + " players");
		    }
		    //Players player = new Players(this, playerUUID);
		    int islandLevel = player.getInt("islandLevel",0);
		    String teamLeaderUUID = player.getString("teamLeader","");
		    if (islandLevel > 0) {
			if (!player.getBoolean("hasTeam")) {
			    topTenAddEntry(playerUUID, islandLevel);
			} else if (!teamLeaderUUID.isEmpty()) {
			    if (teamLeaderUUID.equals(playerUUIDString)) {
				topTenAddEntry(playerUUID, islandLevel);
			    }
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
	getLogger().info("Processed " + index + " players");
	// Save the top ten
	topTenSave();
    }

    protected void topTenSave() {
	if (topTenList == null) {
	    return;
	}
	getLogger().info("Saving top ten list");
	// Make file
	File topTenFile = new File(getDataFolder(),"topten.yml");
	// Make configuration
	YamlConfiguration config = new YamlConfiguration();
	// Save config

	int rank = 0;
	for (Map.Entry<UUID,Integer> m : topTenList.entrySet()) {
	    if (rank++ == 10) {
		break;
	    }
	    config.set("topten." + m.getKey().toString(), m.getValue());
	}
	try {
	    config.save(topTenFile);
	    getLogger().info("Saved top ten list");
	} catch (Exception e) {
	    getLogger().severe("Could not save top ten list!");
	    e.printStackTrace();
	}
    }

    /**
     * Saves the challenge.yml file if it does not exist
     */
    protected void saveDefaultChallengeConfig() {
	if (challengeConfigFile == null) {
	    challengeConfigFile = new File(getDataFolder(), "challenges.yml");
	}
	if (!challengeConfigFile.exists()) {            
	    saveResource("challenges.yml", false);
	}
    }

    /**
     * Reloads the challenge config file
     */
    protected void reloadChallengeConfig() {
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
    protected FileConfiguration getChallengeConfig() {
	if (challengeFile == null) {
	    reloadChallengeConfig();
	}
	return challengeFile;
    }

    /**
     * Saves challenges.yml
     */
    protected void saveChallengeConfig() {
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
    protected void saveDefaultLocale() {
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
    protected void reloadLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	locale = YamlConfiguration.loadConfiguration(localeFile);

	// Look for defaults in the jar
	InputStream defLocaleStream = this.getResource("locale.yml");
	if (defLocaleStream != null) {
	    YamlConfiguration defLocale = new YamlConfiguration().loadConfiguration(defLocaleStream);
	    locale.setDefaults(defLocale);
	}
    }

    /**
     * @return locale FileConfiguration object
     */
    protected FileConfiguration getLocale() {
	if (locale == null) {
	    reloadLocale();
	}
	return locale;
    }

    /**
     * Saves challenges.yml
     */
    protected void saveLocale() {
	if (locale == null || localeFile == null) {
	    return;
	}
	try {
	    getLocale().save(localeFile);
	} catch (IOException ex) {
	    getLogger().severe("Could not save config to " + localeFile);
	}
    }

    /**
     * Sends a message to every player in the team that is offline
     * @param playerUUID
     * @param message
     */
    protected void tellOfflineTeam(UUID playerUUID, String message) {
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
     * Tells all online team members something happened
     * @param playerUUID
     * @param message
     */
    protected void tellTeam(UUID playerUUID, String message) {
	//getLogger().info("DEBUG: tell offline team called");
	if (!players.inTeam(playerUUID)) {
	    //getLogger().info("DEBUG: player is not in a team");
	    return;
	}
	UUID teamLeader = players.getTeamLeader(playerUUID);
	List<UUID> teamMembers = players.getMembers(teamLeader);
	for (UUID member : teamMembers) {
	    //getLogger().info("DEBUG: trying UUID " + member.toString());
	    if (!member.equals(playerUUID) && getServer().getPlayer(member) != null) {
		// Online player
		getServer().getPlayer(member).sendMessage(message);
	    }
	}
    }
    /**
     * Sets a message for the player to receive next time they login
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    protected boolean setMessage(UUID playerUUID, String message) {
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
	//getLogger().info("DEBUG: player is offline - storing message");
	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    playerMessages.add(message);
	} else {
	    playerMessages = new ArrayList<String>(Arrays.asList(message));
	}
	messages.put(playerUUID, playerMessages);
	return true;
    }

    /**
     * Returns what messages are waiting for the player or null if none
     * @param playerUUID
     * @return
     */
    protected List<String> getMessages(UUID playerUUID) {
	List<String> playerMessages = messages.get(playerUUID);
	return playerMessages;
    }

    /**
     * Clears any messages for player
     * @param playerUUID
     */
    protected void clearMessages(UUID playerUUID) {
	messages.remove(playerUUID);
    }
    
    protected void saveMessages() {
	if (messageStore == null) {
	    return;
	}
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
	    return;
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
    }

    protected boolean loadMessages() {
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
    protected static String prettifyText(String ugly) {
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
    protected static float blockFaceToFloat(BlockFace face) {
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
    protected void resetPlayer(Player player) {
	//getLogger().info("DEBUG: clear inventory = " + Settings.clearInventory);
	if (Settings.clearInventory && (player.getWorld().getName().equalsIgnoreCase(Settings.worldName)
		|| player.getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether"))) {
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
	players.save(player.getUniqueId());
	topTenAddEntry(player.getUniqueId(),0);
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

    /**
     * @param location
     * @return Name of warp owner
     */
    protected String getWarpOwner(Location location) {
	for (UUID playerUUID : warpList.keySet()) {
	    Location l = getLocationString((String) warpList.get(playerUUID));
	    if (l.equals(location)) {
		return players.getName(playerUUID);
	    }
	}
	return "";
    }


    /**
     * Used to prevent teleporting when falling
     * @param uniqueId
     */
    protected void setFalling(UUID uniqueId) {
	this.fallingPlayers.add(uniqueId);
    }

    /**
     * Used to prevent teleporting when falling
     * @param uniqueId
     */
    protected void unsetFalling(UUID uniqueId) {
	//getLogger().info("DEBUG: unset falling");
	this.fallingPlayers.remove(uniqueId);
    }

    /**
     * Used to prevent teleporting when falling
     * @param uniqueId
     * @return true or false
     */
    protected boolean isFalling(UUID uniqueId) {
	return this.fallingPlayers.contains(uniqueId);
    }

    /**
     * Sets all blocks in an island to a specified biome type
     * @param islandLoc
     * @param biomeType
     */
    protected boolean setIslandBiome(Location islandLoc, Biome biomeType) {
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
		    //getLogger().info("DEBUG: Chunk saving  " + l.getChunk().getX() + "," + l.getChunk().getZ());
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
	case MESA:
	case MESA_BRYCE:
	case DESERT:
	case JUNGLE:
	case SAVANNA:
	case SAVANNA_MOUNTAINS:
	case SAVANNA_PLATEAU:
	case SAVANNA_PLATEAU_MOUNTAINS:
	case SWAMPLAND:
	    // No ice or snow allowed
	    for (int y = islandLoc.getWorld().getMaxHeight(); y >= Settings.sea_level; y--) {
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
	    for (int y = islandLoc.getWorld().getMaxHeight(); y >= Settings.sea_level; y--) {
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
	case BEACH:
	    break;
	case BIRCH_FOREST:
	    break;
	case BIRCH_FOREST_HILLS:
	    break;
	case BIRCH_FOREST_HILLS_MOUNTAINS:
	    break;
	case BIRCH_FOREST_MOUNTAINS:
	    break;
	case COLD_BEACH:
	    break;
	case COLD_TAIGA:
	    break;
	case COLD_TAIGA_HILLS:
	    break;
	case COLD_TAIGA_MOUNTAINS:
	    break;
	case DEEP_OCEAN:
	    break;
	case DESERT_HILLS:
	    break;
	case DESERT_MOUNTAINS:
	    break;
	case EXTREME_HILLS:
	    break;
	case EXTREME_HILLS_MOUNTAINS:
	    break;
	case EXTREME_HILLS_PLUS:
	    break;
	case EXTREME_HILLS_PLUS_MOUNTAINS:
	    break;
	case FLOWER_FOREST:
	    break;
	case FOREST:
	    break;
	case FOREST_HILLS:
	    break;
	case FROZEN_OCEAN:
	    break;
	case FROZEN_RIVER:
	    break;
	case ICE_MOUNTAINS:
	    break;
	case ICE_PLAINS:
	    break;
	case ICE_PLAINS_SPIKES:
	    break;
	case JUNGLE_EDGE:
	    break;
	case JUNGLE_EDGE_MOUNTAINS:
	    break;
	case JUNGLE_HILLS:
	    break;
	case JUNGLE_MOUNTAINS:
	    break;
	case MEGA_SPRUCE_TAIGA:
	    break;
	case MEGA_SPRUCE_TAIGA_HILLS:
	    break;
	case MEGA_TAIGA:
	    break;
	case MEGA_TAIGA_HILLS:
	    break;
	case MUSHROOM_ISLAND:
	    break;
	case MUSHROOM_SHORE:
	    break;
	case OCEAN:
	    break;
	case PLAINS:
	    break;
	case RIVER:
	    break;
	case ROOFED_FOREST:
	    break;
	case ROOFED_FOREST_MOUNTAINS:
	    break;
	case SKY:
	    break;
	case SMALL_MOUNTAINS:
	    break;
	case STONE_BEACH:
	    break;
	case SUNFLOWER_PLAINS:
	    break;
	case SWAMPLAND_MOUNTAINS:
	    break;
	case TAIGA:
	    break;
	case TAIGA_HILLS:
	    break;
	case TAIGA_MOUNTAINS:
	    break;
	case MESA_PLATEAU:
	    break;
	case MESA_PLATEAU_FOREST:
	    break;
	case MESA_PLATEAU_FOREST_MOUNTAINS:
	    break;
	case MESA_PLATEAU_MOUNTAINS:
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
    protected Biomes getBiomes() {
	return biomes;
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     * @param location
     * @return
     */
    protected Location getClosestIsland(Location location) {
	long x = Math.round((double)location.getBlockX() / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
	long z = Math.round((double)location.getBlockZ() / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;
	long y = Settings.island_level;
	return new Location(location.getWorld(),x,y,z);
    }

    /**
     * @return the calculatingLevel
     */
    public boolean isCalculatingLevel() {
	return calculatingLevel;
    }

    /**
     * @param calculatingLevel the calculatingLevel to set
     */
    public void setCalculatingLevel(boolean calculatingLevel) {
	this.calculatingLevel = calculatingLevel;
    }

    /**
     * @return the grid
     */
    public GridManager getGrid() {
	return grid;
    }

}