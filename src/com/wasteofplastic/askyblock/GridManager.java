/**
 * 
 */
package com.wasteofplastic.askyblock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.util.Util;

/**
 * This class manages the island islandGrid. It knows where every island is, and
 * where new
 * ones should go. It can handle any size of island or protection size
 * The islandGrid is stored in a YML file.
 * 
 * @author tastybento
 */
public class GridManager {
    private ASkyBlock plugin;
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<Integer, TreeMap<Integer, Island>>();
    // private TreeMap<Integer,TreeMap<Integer,PlayerIsland>> protectionGrid = new
    // TreeMap<Integer,TreeMap<Integer,PlayerIsland>>();
    // Reverse lookup for owner, if they exists
    private HashMap<UUID, Island> ownershipMap = new HashMap<UUID, Island>();
    private File islandFile;
    private Island spawn;

    /**
     * @param plugin
     */
    public GridManager(ASkyBlock plugin) {
	this.plugin = plugin;
	loadGrid();
    }

    private void loadGrid() {
	plugin.getLogger().info("Loading island grid...");
	islandGrid.clear();
	// protectionGrid.clear();
	islandFile = new File(plugin.getDataFolder(), "islands.yml");
	if (!islandFile.exists()) {
	    // check if island folder exists
	    plugin.getLogger().info("islands.yml does not exist. Creating...");
	    convert();
	    plugin.getLogger().info("islands.yml created.");
	} else {
	    plugin.getLogger().info("Loading islands.yml");
	    YamlConfiguration islandYaml = new YamlConfiguration();
	    try {
		islandYaml.load(islandFile);
		List<String> islandList = new ArrayList<String>();
		if (islandYaml.contains(Settings.worldName)) {
		    islandList = islandYaml.getStringList(Settings.worldName);
		    for (String island : islandList) {
			Island newIsland = addIsland(island);
			ownershipMap.put(newIsland.getOwner(), newIsland);
			if (newIsland.isSpawn()) {
			    spawn = newIsland;
			}
		    }
		} else {
		    plugin.getLogger().severe("Could not find any islands for this world. World name in config.yml is probably wrong.");
		    plugin.getLogger().severe("Making backup of islands.yml. Correct world name and then replace islands.yml");
		    File rename = new File(plugin.getDataFolder(), "islands_backup.yml");
		    islandFile.renameTo(rename);
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not load islands.yml");
	    }
	}
	// for (int x : protectionGrid.)
	// plugin.getLogger().info("Debug: protection grid is size " +
	// protectionGrid.size());
	// plugin.getLogger().info("Debug: Island grid is sized = " +
	// islandGrid.size());
    }

    /**
     * Provides confirmation that the island is on the grid lines
     * 
     * @param loc
     * @return
     */
    private boolean onGrid(Location loc) {
	int x = loc.getBlockX();
	int z = loc.getBlockZ();
	return onGrid(x, z);
    }

    private boolean onGrid(int x, int z) {
	if ((x - Settings.islandXOffset) % Settings.islandDistance != 0) {
	    return false;
	}
	if ((z - Settings.islandZOffset) % Settings.islandDistance != 0) {
	    return false;
	}
	return true;
    }

    /**
     * Converts from the old version where islands were stored in an island
     * folder.
     * Did not work for large installations.
     */
    private void convert() {
	// Read spawn file if it exists
	final File spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
	if (spawnFile.exists()) {
	    YamlConfiguration spawn = new YamlConfiguration();
	    try {
		spawn.load(spawnFile);
		int range = spawn.getInt("spawn.range");
		// plugin.getLogger().info("DEBUG:" + range + " " +
		// spawn.getString("spawn.bedrock",""));
		Location spawnLoc = Util.getLocationString(spawn.getString("spawn.bedrock", ""));
		if (spawnLoc != null && onGrid(spawnLoc)) {
		    Island newIsland = addIsland(spawnLoc.getBlockX(), spawnLoc.getBlockZ());
		    setSpawn(newIsland);
		    newIsland.setProtectionSize(range);
		} else {
		    plugin.getLogger().severe("Spawn could not be imported! Location " + spawnLoc);
		    plugin.getLogger().severe("Go to the spawn island and set it manually");
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Spawn could not be imported! File could not load.");
	    }
	}
	// Go through player folder
	final File playerFolder = new File(plugin.getDataFolder() + File.separator + "players");
	final File quarantineFolder = new File(plugin.getDataFolder() + File.separator + "quarantine");
	YamlConfiguration playerFile = new YamlConfiguration();
	int noisland = 0;
	int inTeam = 0;
	int count = 0;
	if (playerFolder.exists() && playerFolder.listFiles().length > 0) {
	    plugin.getLogger().warning("Reading player folder...");
	    if (playerFolder.listFiles().length > 5000) {
		plugin.getLogger().warning("This could take some time with a large number of islands...");
	    }
	    for (File f : playerFolder.listFiles()) {
		// Need to remove the .yml suffix
		String fileName = f.getName();
		if (fileName.endsWith(".yml")) {
		    try {
			playerFile.load(f);
			boolean hasIsland = playerFile.getBoolean("hasIsland", false);
			if (hasIsland) {
			    String islandLocation = playerFile.getString("islandLocation");
			    if (islandLocation.isEmpty()) {
				plugin.getLogger().severe("Problem with " + fileName);
				plugin.getLogger().severe("Owner :" + playerFile.getString("playerName", "Unknown"));
				plugin.getLogger().severe("Player file says they have an island, but there is no location.");
				// Move to quarantine
				if (!quarantineFolder.exists()) {
				    quarantineFolder.mkdir();
				}
				// Move the file
				plugin.getLogger().severe("Moving " + f.getName() + " to " + quarantineFolder.getName());
				File rename = new File(quarantineFolder, f.getName());
				f.renameTo(rename);
			    } else {
				// Location exists
				Location islandLoc = Util.getLocationString(islandLocation);
				if (islandLoc != null) {
				    // Check to see if this island is already loaded
				    Island island = getIslandAt(islandLoc);
				    if (island != null) {
					// PlayerIsland exists, compare creation dates
					plugin.getLogger().severe("Problem with " + fileName);
					plugin.getLogger().severe("Owner :" + playerFile.getString("playerName", "Unknown"));
					plugin.getLogger().severe("This island location already exists and is already imported");
					if (island.getUpdatedDate() > f.lastModified()) {
					    plugin.getLogger().severe("Previous file is more recent so keeping it.");
					    // Original file is more recent
					    // Move to quarantine
					    if (!quarantineFolder.exists()) {
						quarantineFolder.mkdir();
					    }
					    plugin.getLogger().severe(
						    "Moving " + (playerFile.getString("playerName", "Unknown")) + "'s file (" + f.getName() + ") to "
							    + quarantineFolder.getName());
					    File rename = new File(quarantineFolder, f.getName());
					    f.renameTo(rename);
					} else {
					    // New file is more recent
					    plugin.getLogger().severe(playerFile.getString("playerName", "Unknown") + "'s file is more recent");
					    File oldFile = new File(playerFolder, island.getOwner().toString() + ".yml");
					    File rename = new File(quarantineFolder, oldFile.getName());
					    // Move to quarantine
					    if (!quarantineFolder.exists()) {
						quarantineFolder.mkdir();
					    }
					    plugin.getLogger().severe("Moving previous file (" + oldFile.getName() + ") to " + quarantineFolder.getName());
					    oldFile.renameTo(rename);
					    deleteIsland(islandLoc);
					    island = null;
					}
				    }
				    if (island == null) {
					if (!onGrid(islandLoc)) {
					    plugin.getLogger().severe("Problem with " + fileName);
					    plugin.getLogger().severe("Owner :" + playerFile.getString("playerName", "Unknown"));
					    plugin.getLogger().severe("Island is not on grid lines! " + islandLoc);
					}
					String ownerString = fileName.substring(0, fileName.length() - 4);
					// Add the island
					UUID owner = UUID.fromString(ownerString);
					Island newIsland = addIsland(islandLoc.getBlockX(), islandLoc.getBlockZ(), owner);
					ownershipMap.put(owner, newIsland);
					// Grab when this was last updated
					newIsland.setUpdatedDate(f.lastModified());

					if ((count) % 1000 == 0) {
					    plugin.getLogger().info("Converted " + count + " islands");
					}
					count++;
					// plugin.getLogger().info("Converted island at "
					// + islandLoc);
					// Top ten

					int islandLevel = playerFile.getInt("islandLevel", 0);
					String teamLeaderUUID = playerFile.getString("teamLeader", "");
					if (islandLevel > 0) {
					    if (!playerFile.getBoolean("hasTeam")) {
						TopTen.topTenAddEntry(owner, islandLevel);
					    } else if (!teamLeaderUUID.isEmpty()) {
						if (teamLeaderUUID.equals(ownerString)) {
						    TopTen.topTenAddEntry(owner, islandLevel);
						}
					    }
					}
				    }
				} else {
				    plugin.getLogger().severe("Problem with " + fileName);
				    plugin.getLogger().severe("Owner :" + playerFile.getString("playerName", "Unknown"));
				    plugin.getLogger().severe("The world for this file does not exist!");
				}
			    }
			} else {
			    noisland++;
			    if (playerFile.getBoolean("hasTeam", false)) {
				inTeam++;
			    }
			}

		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + fileName);
			e.printStackTrace();
		    }
		}
	    }
	    plugin.getLogger().info("Converted " + count + " islands from player's folder");
	    plugin.getLogger().info(noisland + " have no island, of which " + inTeam + " are in a team.");
	    plugin.getLogger().info((noisland - inTeam) + " are in the system, but have no island or team");
	}
	TopTen.topTenSave();

	int count2 = 0;
	// Check island folder
	final File islandFolder = new File(plugin.getDataFolder() + File.separator + "islands");
	if (islandFolder.exists() && islandFolder.listFiles().length > 0) {
	    plugin.getLogger().warning("Reading island folder...");
	    if (islandFolder.listFiles().length > 5000) {
		plugin.getLogger().warning("This could take some time with a large number of islands...");
	    }
	    for (File f : islandFolder.listFiles()) {
		// Need to remove the .yml suffix
		String fileName = f.getName();
		int comma = fileName.indexOf(",");
		if (fileName.endsWith(".yml") && comma != -1) {
		    try {
			// Parse to an island value
			int x = Integer.parseInt(fileName.substring(0, comma));
			int z = Integer.parseInt(fileName.substring(comma + 1, fileName.indexOf(".")));
			if (!onGrid(x, z)) {
			    plugin.getLogger().severe("Island is not on grid lines! " + x + "," + z + " skipping...");
			} else {
			    // Note that this is the CENTER of the island
			    if (getIslandAt(x, z) == null) {
				addIsland(x, z);
				if (count2 % 1000 == 0) {
				    plugin.getLogger().info("Converted " + count + " islands");
				}
				count2++;
				// plugin.getLogger().info("Added island from island folder: "
				// + x + "," +z);
			    }
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	    plugin.getLogger().info("Converted " + count2 + " islands from island folder");
	    plugin.getLogger().info("Total " + (count + count2) + " islands converted.");
	}
	// Now save the islandGrid
	saveGrid();
    }

    public void saveGrid() {
	final File islandFile = new File(plugin.getDataFolder(), "islands.yml");
	YamlConfiguration islandYaml = new YamlConfiguration();
	List<String> islandList = new ArrayList<String>();
	for (int x : islandGrid.keySet()) {
	    for (int z : islandGrid.get(x).keySet()) {
		Island island = islandGrid.get(x).get(z);
		islandList.add(island.serialize());
	    }
	}
	islandYaml.set(Settings.worldName, islandList);
	// Save the file
	try {
	    islandYaml.save(islandFile);
	} catch (Exception e) {
	    plugin.getLogger().severe("Could not save islands.yml!");
	    e.printStackTrace();
	}
    }

    /**
     * Returns the island at the location or null if there is none
     * 
     * @param location
     * @return PlayerIsland object
     */
    public Island getIslandAt(Location location) {
	if (location == null) {
	    return null;
	}
	// Check if it is spawn
	if (spawn != null && spawn.onIsland(location)) {
	    return spawn;
	}
	return getIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Returns the island at the x,z location or null if there is none
     * 
     * @param x
     * @param z
     * @return PlayerIsland or null
     */
    public Island getIslandAt(int x, int z) {
	Entry<Integer, TreeMap<Integer, Island>> en = islandGrid.lowerEntry(x);
	if (en != null) {
	    Entry<Integer, Island> ent = en.getValue().lowerEntry(z);
	    if (ent != null) {
		// Check if in the island range
		Island island = ent.getValue();
		if (island.inIslandSpace(x, z)) {
		    // plugin.getLogger().info("DEBUG: In island space");
		    return island;
		}
		// plugin.getLogger().info("DEBUG: not in island space");
	    }
	}
	return null;
    }

    /**
     * Returns the island being public at the location or null if there is none
     * 
     * @param location
     * @return PlayerIsland object
     */
    public Island getProtectedIslandAt(Location location) {
	// Try spawn
	if (spawn != null && spawn.onIsland(location)) {
	    return spawn;
	}
	Island island = getIslandAt(location);
	if (island == null) {
	    return null;
	}
	if (island.onIsland(location)) {
	    return island;
	}
	return null;
    }

    /**
     * Returns the owner of the island at location
     * 
     * @param location
     * @return UUID of owner
     */
    public UUID getOwnerOfIslandAt(Location location) {
	Island island = getIslandAt(location);
	if (island != null) {
	    return island.getOwner();
	}
	return null;
    }

    // islandGrid manipulation methods
    /**
     * Adds an island to the islandGrid with the CENTER point x,z
     * 
     * @param x
     * @param z
     */
    public Island addIsland(int x, int z) {
	return addIsland(x, z, null);
    }

    /**
     * Adds an island to the islandGrid with the center point x,z owner UUID
     * 
     * @param x
     * @param z
     * @param owner
     */
    public Island addIsland(int x, int z, UUID owner) {
	// Check if this owner already has an island
	if (ownershipMap.containsKey(owner)) {
	    Island island = ownershipMap.get(owner);
	    plugin.getLogger().warning(
		    "Island at " + island.getCenter().getBlockX() + ", " + island.getCenter().getBlockZ()
		    + " is already owned by this player. Removing ownership of this island.");
	    island.setOwner(null);
	    ownershipMap.remove(owner);
	}
	// plugin.getLogger().info("DEBUG: adding island to grid at " + x + ", "
	// + z + " for " + owner.toString());
	Island newIsland = new Island(plugin, x, z, owner);
	// if (newIsland != null) {
	// plugin.getLogger().info("DEBUG: new island is good");
	// }
	addToGrids(newIsland);
	return newIsland;
    }

    /**
     * Adds island to the grid using the stored information
     * 
     * @param islandSerialized
     */
    public Island addIsland(String islandSerialized) {
	// plugin.getLogger().info("DEBUG: adding island " + islandSerialized);
	Island newIsland = new Island(plugin, islandSerialized);
	addToGrids(newIsland);
	return newIsland;
    }

    private void addToGrids(Island newIsland) {
	if (newIsland.getOwner() != null) {
	    ownershipMap.put(newIsland.getOwner(), newIsland);
	}
	// plugin.getLogger().info("DEBUG: adding island to grid");
	if (islandGrid.containsKey(newIsland.getMinX())) {
	    // plugin.getLogger().info("DEBUG: min x is in the grid :" +
	    // newIsland.getMinX());
	    TreeMap<Integer, Island> zEntry = islandGrid.get(newIsland.getMinX());
	    if (zEntry.containsKey(newIsland.getMinZ())) {
		// Island already exists
		Island conflict = islandGrid.get(newIsland.getMinX()).get(newIsland.getMinZ());
		plugin.getLogger().warning("*** Duplicate or overlapping islands! ***");
		plugin.getLogger().warning(
			"Island at (" + newIsland.getCenter().getBlockX() + ", " + newIsland.getCenter().getBlockZ() + ") conflicts with ("
				+ conflict.getCenter().getBlockX() + ", " + conflict.getCenter().getBlockZ() + ")");
		if (conflict.getOwner() != null) {
		    plugin.getLogger().warning("Accepted island is owned by " + plugin.getPlayers().getName(conflict.getOwner()));
		    plugin.getLogger().warning(conflict.getOwner().toString() + ".yml");
		} else {
		    plugin.getLogger().warning("Accepted island is unowned.");
		}
		if (newIsland.getOwner() != null) {
		    plugin.getLogger().warning("Denied island is owned by " + plugin.getPlayers().getName(newIsland.getOwner()));
		    plugin.getLogger().warning(newIsland.getOwner().toString() + ".yml");
		} else {
		    plugin.getLogger().warning("Denied island is unowned and was just found in the islands folder. Skipping it...");
		}
		plugin.getLogger().warning("Recommend that the denied player file is deleted otherwise weird things can happen.");
		return;
	    } else {
		// Add island
		// plugin.getLogger().info("DEBUG: min z is not in the grid :" +
		// newIsland.getMinZ());
		zEntry.put(newIsland.getMinZ(), newIsland);
		islandGrid.put(newIsland.getMinX(), zEntry);
		// plugin.getLogger().info("Debug: " + newIsland.toString());
	    }
	} else {
	    // Add island
	    // plugin.getLogger().info("DEBUG: min x is not in the grid :" +
	    // newIsland.getMinX());
	    // plugin.getLogger().info("DEBUG: min Z is not in the grid :" +
	    // newIsland.getMinZ());
	    TreeMap<Integer, Island> zEntry = new TreeMap<Integer, Island>();
	    zEntry.put(newIsland.getMinZ(), newIsland);
	    islandGrid.put(newIsland.getMinX(), zEntry);
	}
    }

    /**
     * Removes the island at location loc from the grid and removes the player
     * from the ownership map
     * 
     * @param loc
     */
    public void deleteIsland(Location loc) {
	// plugin.getLogger().info("DEBUG: deleting island at " + loc);
	Island island = getIslandAt(loc);
	if (island != null) {
	    UUID owner = island.getOwner();
	    int x = island.getMinX();
	    int z = island.getMinZ();
	    // plugin.getLogger().info("DEBUG: x = " + x + " z = " + z);
	    if (islandGrid.containsKey(x)) {
		// plugin.getLogger().info("DEBUG: x found");
		TreeMap<Integer, Island> zEntry = islandGrid.get(x);
		if (zEntry.containsKey(z)) {
		    // plugin.getLogger().info("DEBUG: z found - deleting the island");
		    // Island exists - delete it
		    Island deletedIsland = zEntry.get(z);
		    deletedIsland.setOwner(null);
		    deletedIsland.setLocked(false);
		    zEntry.remove(z);
		    islandGrid.put(x, zEntry);
		} // else {
		// plugin.getLogger().info("DEBUG: could not find z");
		// }
	    }
	    // Remove from the ownership map
	    // If the owner already has been given a new island, then this will
	    // not be needed
	    if (owner != null && ownershipMap.containsKey(owner)) {
		if (ownershipMap.get(owner).equals(island)) {
		    ownershipMap.remove(owner);
		}
	    }
	}

    }

    /**
     * Gets island by owner's UUID. Just because the island does not exist in
     * this map
     * does not mean it does not exist in this world, due to legacy island
     * support
     * Will return the island that this player is a member of if a team player
     * 
     * @param owner
     * @return island object or null if it does not exist in the list
     */
    public Island getIsland(UUID owner) {
	if (owner != null) {
	    if (ownershipMap.containsKey(owner)) {
		return ownershipMap.get(owner);
	    }
	    // Try and get team islands
	    UUID leader = plugin.getPlayers().getTeamLeader(owner);
	    if (leader != null && ownershipMap.containsKey(leader)) {
		return ownershipMap.get(leader);
	    }
	}
	return null;
    }

    /**
     * Sets an island to be owned by another player. If the new owner had an
     * island, that island is released to null ownership
     * 
     * @param island
     * @param newOwner
     */
    public void setIslandOwner(Island island, UUID newOwner) {
	// The old owner
	UUID oldOwner = island.getOwner();
	// If the island owner is being set to null - remove the old owner's
	// ownership
	if (newOwner == null && oldOwner != null) {
	    ownershipMap.remove(oldOwner);
	    island.setOwner(null);
	    return;
	}
	// Check if the new owner already has an island
	if (ownershipMap.containsKey(newOwner)) {
	    Island oldIsland = ownershipMap.get(newOwner);
	    // plugin.getLogger().warning("Island at " +
	    // oldIsland.getCenter().getBlockX() + ", " +
	    // oldIsland.getCenter().getBlockZ()
	    // +
	    // " is already owned by this player. Removing ownership of this island.");
	    oldIsland.setOwner(null);
	    ownershipMap.remove(newOwner);
	}
	// Make the new owner own the island
	if (newOwner != null && island != null) {
	    // See if this island has an owner already
	    island.setOwner(newOwner);
	    // If the old owner exists remove them from the map
	    if (oldOwner != null && ownershipMap.containsKey(oldOwner)) {
		// Remove the old entry
		ownershipMap.remove(oldOwner);
	    }
	    // Insert the new entry
	    ownershipMap.put(newOwner, island);
	}
    }

    /**
     * @return the ownershipMap
     */
    public HashMap<UUID, Island> getOwnershipMap() {
	return ownershipMap;
    }

    /**
     * @return the spawn
     */
    public Island getSpawn() {
	return spawn;
    }

    /**
     * @param spawn
     *            the spawn to set
     */
    public void setSpawn(Island spawn) {
	// plugin.getLogger().info("DEBUG: Spawn set");
	spawn.setSpawn(true);
	spawn.setProtectionSize(spawn.getIslandDistance());
	this.spawn = spawn;
    }

    public void deleteSpawn() {
	deleteIsland(spawn.getCenter());
	this.spawn = null;

    }

    /**
     * Indicates whether a player is at the island spawn or not
     * 
     * @param playerLoc
     * @return true if they are, false if they are not, or spawn does not exist
     */
    public boolean isAtSpawn(Location playerLoc) {
	if (spawn == null) {
	    return false;
	}
	return spawn.onIsland(playerLoc);
    }

    /**
     * Determines if an island is at a location in this area
     * location. Also checks if the spawn island is in this area.
     * Used for creating new islands ONLY
     * 
     * @param loc
     * @return true if found, otherwise false
     */
    public boolean islandAtLocation(final Location loc) {
	if (loc == null) {
	    return true;
	}
	// getLogger().info("DEBUG checking islandAtLocation for location " +
	// loc.toString());
	// Check the island grid
	if (getIslandAt(loc) != null) {
	    // This checks if loc is inside the island spawn radius too
	    return true;
	}
	final int px = loc.getBlockX();
	final int pz = loc.getBlockZ();
	// Extra spawn area check
	// If island protection distance is less than island distance then the
	// check above will cover it
	// Island edge must be > protection edge spawn
	Island spawn = getSpawn();
	if (spawn != null && spawn.getProtectionSize() > spawn.getIslandDistance()) {
	    if (Math.abs(px - spawn.getCenter().getBlockX()) < ((spawn.getProtectionSize() + Settings.islandDistance) / 2)
		    && Math.abs(pz - spawn.getCenter().getBlockZ()) < ((spawn.getProtectionSize() + Settings.islandDistance) / 2)) {
		// getLogger().info("DEBUG: island is within spawn space " + px
		// + " " + pz);
		return true;
	    }
	}

	// Bedrock check
	if (loc.getBlock().getType().equals(Material.BEDROCK)) {
	    plugin.getLogger().info("Found bedrock at island height - adding to islands.yml " + px + "," + pz);
	    addIsland(px, pz);
	    return true;
	}
	// Look around
	for (int x = -5; x <= 5; x++) {
	    for (int y = 10; y <= 255; y++) {
		for (int z = -5; z <= 5; z++) {
		    if (loc.getWorld().getBlockAt(x + px, y, z + pz).getType().equals(Material.BEDROCK)) {
			plugin.getLogger().info("Bedrock found during long search - adding to islands.yml " + px + "," + pz);
			addIsland(px, pz);
			return true;
		    }
		}
	    }
	}
	return false;
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     * 
     * @param location
     * @return
     */
    public Location getClosestIsland(Location location) {
	long x = Math.round((double) location.getBlockX() / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
	long z = Math.round((double) location.getBlockZ() / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;
	long y = Settings.island_level;
	return new Location(location.getWorld(), x, y, z);
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
	// TODO: improve the safe location finding.
	//Bukkit.getLogger().info("DEBUG: " + l.toString());
	final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
	final Block space1 = l.getBlock();
	final Block space2 = l.getBlock().getRelative(BlockFace.UP);
	//Bukkit.getLogger().info("DEBUG: ground = " + ground.getType());
	//Bukkit.getLogger().info("DEBUG: space 1 = " + space1.getType());
	//Bukkit.getLogger().info("DEBUG: space 2 = " + space2.getType());
	// Portals are not "safe"
	if (space1.getType() == Material.PORTAL || ground.getType() == Material.PORTAL || space2.getType() == Material.PORTAL
		|| space1.getType() == Material.ENDER_PORTAL || ground.getType() == Material.ENDER_PORTAL || space2.getType() == Material.ENDER_PORTAL) {
	    return false;
	}
	// If ground is AIR, then this is either not good, or they are on slab,
	// stair, etc.
	if (ground.getType() == Material.AIR) {
	    // Bukkit.getLogger().info("DEBUG: air");
	    return false;
	}
	// In aSkyblock, liquid may be unsafe
	if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
	    // Check if acid has no damage
	    if (Settings.acidDamage > 0D) {
		// Bukkit.getLogger().info("DEBUG: acid");
		return false;
	    } else if (ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
		    || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
		    || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
		// Lava check only
		// Bukkit.getLogger().info("DEBUG: lava");
		return false;
	    }
	}
	MaterialData md = ground.getState().getData();
	if (md instanceof SimpleAttachableMaterialData) {
	    //Bukkit.getLogger().info("DEBUG: trapdoor/button/tripwire hook etc.");
	    if (md instanceof TrapDoor) {
		TrapDoor trapDoor = (TrapDoor)md;
		if (trapDoor.isOpen()) {
		    //Bukkit.getLogger().info("DEBUG: trapdoor open");
		    return false;
		}
	    } else {
		return false;
	    }
	    //Bukkit.getLogger().info("DEBUG: trapdoor closed");
	}
	if (ground.getType().equals(Material.CACTUS) || ground.getType().equals(Material.BOAT) || ground.getType().equals(Material.FENCE)
		|| ground.getType().equals(Material.NETHER_FENCE) || ground.getType().equals(Material.SIGN_POST) || ground.getType().equals(Material.WALL_SIGN)) {
	    // Bukkit.getLogger().info("DEBUG: cactus");
	    return false;
	}
	// Check that the space is not solid
	// The isSolid function is not fully accurate (yet) so we have to
	// check
	// a few other items
	// isSolid thinks that PLATEs and SIGNS are solid, but they are not
	if (space1.getType().isSolid() && !space1.getType().equals(Material.SIGN_POST) && !space1.getType().equals(Material.WALL_SIGN)) {
	    return false;
	}
	if (space2.getType().isSolid()&& !space2.getType().equals(Material.SIGN_POST) && !space2.getType().equals(Material.WALL_SIGN)) {
	    return false;
	}
	// Safe
	//Bukkit.getLogger().info("DEBUG: safe!");
	return true;
    }

    /**
     * Determines a safe teleport spot on player's island or the team island
     * they belong to.
     * 
     * @param p UUID of player
     * @param number - starting home location e.g., 1
     * @return Location of a safe teleport spot or null if one cannot be found
     */
    public Location getSafeHomeLocation(final UUID p, int number) {
	// Try the numbered home location first
	Location l = plugin.getPlayers().getHomeLocation(p, number);
	if (l == null) {
	    // Get the default home, which may be null too, but that's okay
	    number = 1;
	    l = plugin.getPlayers().getHomeLocation(p, number);
	}
	// Check if it is safe
	//plugin.getLogger().info("DEBUG: Home location " + l);
	if (l != null) {
	    // Homes are stored as integers and need correcting to be more central
	    if (isSafeLocation(l)) {
		return l.clone().add(new Vector(0.5D,0,0.5D));
	    }
	    // To cover slabs, stairs and other half blocks, try one block above
	    Location lPlusOne = l.clone();
	    lPlusOne.add(new Vector(0, 1, 0));
	    if (lPlusOne != null) {
		if (isSafeLocation(lPlusOne)) {
		    // Adjust the home location accordingly
		    plugin.getPlayers().setHomeLocation(p, lPlusOne, number);
		    return lPlusOne.clone().add(new Vector(0.5D,0,0.5D));
		}
	    }
	}

	//plugin.getLogger().info("DEBUG: Home location either isn't safe, or does not exist so try the island");
	// Home location either isn't safe, or does not exist so try the island
	// location
	if (plugin.getPlayers().inTeam(p)) {
	    l = plugin.getPlayers().getTeamIslandLocation(p);
	    if (isSafeLocation(l)) {
		plugin.getPlayers().setHomeLocation(p, l, number);
		return l.clone().add(new Vector(0.5D,0,0.5D));
	    } else {
		// try team leader's home
		Location tlh = plugin.getPlayers().getHomeLocation(plugin.getPlayers().getTeamLeader(p));
		if (tlh != null) {
		    if (isSafeLocation(tlh)) {
			plugin.getPlayers().setHomeLocation(p, tlh, number);
			return tlh.clone().add(new Vector(0.5D,0,0.5D));
		    }
		}
	    }
	} else {
	    l = plugin.getPlayers().getIslandLocation(p);
	    if (isSafeLocation(l)) {
		plugin.getPlayers().setHomeLocation(p, l, number);
		return l.clone().add(new Vector(0.5D,0,0.5D));
	    }
	}
	if (l == null) {
	    plugin.getLogger().warning(plugin.getPlayers().getName(p) + " player has no island!");
	    return null;
	}
	//plugin.getLogger().info("DEBUG: If these island locations are not safe, then we need to get creative");
	// If these island locations are not safe, then we need to get creative
	// Try the default location
	//plugin.getLogger().info("DEBUG: default");
	Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
	if (isSafeLocation(dl)) {
	    plugin.getPlayers().setHomeLocation(p, dl, number);
	    return dl;
	}
	// Try just above the bedrock
	//plugin.getLogger().info("DEBUG: above bedrock");
	dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
	if (isSafeLocation(dl)) {
	    plugin.getPlayers().setHomeLocation(p, dl, number);
	    return dl;
	}
	// Try all the way up to the sky
	//plugin.getLogger().info("DEBUG: try all the way to the sky");
	for (int y = l.getBlockY(); y < 255; y++) {
	    final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
	    if (isSafeLocation(n)) {
		plugin.getPlayers().setHomeLocation(p, n, number);
		return n;
	    }
	}
	//plugin.getLogger().info("DEBUG: unsuccessful");
	// Unsuccessful
	return null;
    }

    /**
     * This is a generic scan that can work in the overworld or the nether
     * @param l - location around which to scan
     * @param i - the range to scan for a location < 0 means the full island.
     * @return - safe location, or null if none can be found
     */
    public Location bigScan(Location l, int i) {
	final int height;
	final int depth;
	if (i > 0) {
	    height = i;
	    depth = i;
	} else {
	    Island island = plugin.getGrid().getIslandAt(l);
	    if (island == null) {
		return null;
	    }
	    i = island.getProtectionSize();
	    height = l.getWorld().getMaxHeight() - l.getBlockY();
	    depth = l.getBlockY();
	}


	//plugin.getLogger().info("DEBUG: ranges i = " + i);
	//plugin.getLogger().info(" " + minX + "," + minZ + " " + maxX + " " + maxZ);
	//plugin.getLogger().info("DEBUG: height = " + height);
	//plugin.getLogger().info("DEBUG: depth = " + depth);
	//plugin.getLogger().info("DEBUG: trying to find a safe spot at " + l.toString());

	// Work outwards from l until the closest safe location is found.
	int minXradius = 0;
	int maxXradius = 0;
	int minZradius = 0;
	int maxZradius = 0;
	int minYradius = 0;
	int maxYradius = 0;

	do {
	    int minX = l.getBlockX()-minXradius;
	    int minZ = l.getBlockZ()-minZradius;
	    int minY = l.getBlockY()-minYradius;
	    int maxX = l.getBlockX()+maxXradius;
	    int maxZ = l.getBlockZ()+maxZradius;
	    int maxY = l.getBlockY()+maxYradius;
	    for (int x = minX; x<= maxX; x++) {
		for (int z = minZ; z <= maxZ; z++) {
		    for (int y = minY; y <= maxY; y++) {
			if (!((x > minX && x < maxX) && (z > minZ && z < maxZ) && (y > minY && y < maxY))) {
			    //plugin.getLogger().info("DEBUG: checking " + x + "," + y + "," + z);
			    Location ultimate = new Location(l.getWorld(), x + 0.5D, y, z + 0.5D);
			    if (isSafeLocation(ultimate)) {
				//plugin.getLogger().info("DEBUG: Found! " + ultimate);
				return ultimate;
			    }
			}
		    }
		}
	    }
	    if (minXradius < i) {
		minXradius++;
	    }
	    if (maxXradius < i) {
		maxXradius++;
	    }
	    if (minZradius < i) {
		minZradius++;
	    }
	    if (maxZradius < i) {
		maxZradius++;
	    }
	    if (minYradius < depth) {
		minYradius++;
	    }
	    if (maxYradius < height) {
		maxYradius++;
	    }
	    //plugin.getLogger().info("DEBUG: Radii " + minXradius + "," + minYradius + "," + minZradius + 
	    //    "," + maxXradius + "," + maxYradius + "," + maxZradius);
	} while (minXradius < i || maxXradius < i || minZradius < i || maxZradius < i || minYradius < depth 
		|| maxYradius < height);
	// Nothing worked
	return null;
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     * 
     * @param player
     * @return
     */
    public boolean homeTeleport(final Player player) {
	return homeTeleport(player, 1);
    }
    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     * @param player
     * @param number - home location to do to
     * @return true if successful, false if not
     */
    public boolean homeTeleport(final Player player, int number) {
	Location home = null;
	//plugin.getLogger().info("home teleport called for #" + number);
	home = getSafeHomeLocation(player.getUniqueId(), number);
	//plugin.getLogger().info("home get safe loc = " + home);
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
	    //plugin.getLogger().info("Fixing home location using safe spot teleport");
	    // Try to fix this teleport location and teleport the player if possible
	    new SafeSpotTeleport(plugin, player, plugin.getPlayers().getHomeLocation(player.getUniqueId(), number), number);
	    return true;
	}
	//plugin.getLogger().info("DEBUG: home loc = " + home + " teleporting");
	player.teleport(home);
	//player.sendBlockChange(home, Material.GLOWSTONE, (byte)0);
	if (number ==1 ) {
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).islandteleport);
	} else {
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).islandteleport + " #" + number);
	}
	return true;

    }

    /**
     * Sets the numbered home location based on where the player is now
     * @param player
     * @param number
     */
    public void homeSet(Player player, int number) {
	// Check if player is in their home world
	if (!player.getWorld().equals(plugin.getGrid().getIsland(player.getUniqueId()).getCenter().getWorld())) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNotOnIsland);
	    return; 
	}
	// Check if player is on island, ignore coops
	if (!plugin.getGrid().playerIsOnIsland(player, false)) {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNotOnIsland);
	    return;
	}
	plugin.getPlayers().setHomeLocation(player.getUniqueId(), player.getLocation(), number);
	if (number == 1) {
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).setHomehomeSet);
	} else {
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).setHomehomeSet + " #" + number);
	}
    }

    /**
     * Sets the home location based on where the player is now
     * 
     * @param player
     * @return
     */
    public void homeSet(final Player player) {
	homeSet(player, 1);
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * owned by the player
     * 
     * @param player
     * @param loc
     * @return
     */
    public boolean locationIsOnIsland(final Player player, final Location loc) {

	// Get the player's island from the grid if it exists
	Island island = getIslandAt(loc);
	if (island != null) {
	    //plugin.getLogger().info("DEBUG: island here is " + island.getCenter());
	    // On an island in the grid
	    if (island.onIsland(loc) && island.getMembers().contains(player.getUniqueId())) {
		//plugin.getLogger().info("DEBUG: allowed");
		// In a protected zone but is on the list of acceptable players
		return true;
	    } else {
		// Not allowed
		//plugin.getLogger().info("DEBUG: not allowed");
		return false;
	    }
	} else {
	    //plugin.getLogger().info("DEBUG: no island at this location");
	}
	// Not in the grid, so do it the old way
	// Make a list of test locations and test them
	Set<Location> islandTestLocations = new HashSet<Location>();
	if (plugin.getPlayers().hasIsland(player.getUniqueId())) {
	    islandTestLocations.add(plugin.getPlayers().getIslandLocation(player.getUniqueId()));
	} else if (plugin.getPlayers().inTeam(player.getUniqueId())) {
	    islandTestLocations.add(plugin.getPlayers().get(player.getUniqueId()).getTeamIslandLocation());
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
     * Finds out if location is within a set of island locations and returns the
     * one that is there or null if not
     * 
     * @param islandTestLocations
     * @param loc
     * @return
     */
    public Location locationIsOnIsland(final Set<Location> islandTestLocations, final Location loc) {
	// Run through all the locations
	for (Location islandTestLocation : islandTestLocations) {
	    if (loc.getWorld().equals(islandTestLocation.getWorld())) {
		if (getIslandAt(islandTestLocation) != null) {
		    // Get the protection range for this location if possible
		    Island island = getProtectedIslandAt(islandTestLocation);
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
     * Checks if an online player is on their island, on a team island or on a
     * coop island
     * 
     * @param player
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(final Player player) {
	return playerIsOnIsland(player, true);
    }

    /**
     * Checks if an online player is on their island, on a team island or on a
     * coop island
     * @param player
     * @param coop - if true, coop islands are included
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(final Player player, boolean coop) {
	return locationIsAtHome(player, coop, player.getLocation());
    }
    
    
    /**
     * Checks if a location is within the home boundaries of a player. If coop is true, this check includes coop players.
     * @param player
     * @param coop
     * @param loc
     * @return true if the location is within home boundaries
     */
    public boolean locationIsAtHome(final Player player, boolean coop, Location loc) {
	// Make a list of test locations and test them
	Set<Location> islandTestLocations = new HashSet<Location>();
	if (plugin.getPlayers().hasIsland(player.getUniqueId())) {
	    islandTestLocations.add(plugin.getPlayers().getIslandLocation(player.getUniqueId()));
	    // If new Nether
	    if (Settings.createNether && Settings.newNether) {
		islandTestLocations.add(netherIsland(plugin.getPlayers().getIslandLocation(player.getUniqueId())));
	    }
	} else if (plugin.getPlayers().inTeam(player.getUniqueId())) {
	    islandTestLocations.add(plugin.getPlayers().getTeamIslandLocation(player.getUniqueId()));
	    if (Settings.createNether && Settings.newNether) {
		islandTestLocations.add(netherIsland(plugin.getPlayers().getTeamIslandLocation(player.getUniqueId())));
	    }
	}
	// Check coop locations
	if (coop) {
	    islandTestLocations.addAll(CoopPlay.getInstance().getCoopIslands(player));
	}
	if (islandTestLocations.isEmpty()) {
	    return false;
	}
	// Run through all the locations
	for (Location islandTestLocation : islandTestLocations) {
	    // Must be in the same world as the locations being checked
	    // Note that getWorld can return null if a world has been deleted on the server
	    if (islandTestLocation != null && islandTestLocation.getWorld() != null && islandTestLocation.getWorld().equals(loc.getWorld())) {
		int protectionRange = Settings.island_protectionRange;
		if (getIslandAt(islandTestLocation) != null) {
		    // Get the protection range for this location if possible
		    Island island = getProtectedIslandAt(islandTestLocation);
		    if (island != null) {
			// We are in a protected island area.
			protectionRange = island.getProtectionSize();
		    }
		}
		if (loc.getX() > islandTestLocation.getX() - protectionRange / 2
			&& loc.getX() < islandTestLocation.getX() + protectionRange / 2
			&& loc.getZ() > islandTestLocation.getZ() - protectionRange / 2
			&& loc.getZ() < islandTestLocation.getZ() + protectionRange / 2) {
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Generates a Nether version of the locations
     * @param islandLocation
     * @return
     */
    private Location netherIsland(Location islandLocation) {
	//plugin.getLogger().info("DEBUG: netherworld = " + ASkyBlock.getNetherWorld());
	return islandLocation.toVector().toLocation(ASkyBlock.getNetherWorld());
    }

    /**
     * Checks to see if a player is trespassing on another player's island
     * Both players must be online.
     * 
     * @param owner
     *            - owner or team member of an island
     * @param target
     * @return true if they are on the island otherwise false.
     */
    public boolean isOnIsland(final Player owner, final Player target) {
	// Get the island location of owner
	Location islandTestLocation = null;
	if (plugin.getPlayers().inTeam(owner.getUniqueId())) {
	    // Is the target in the owner's team?
	    if (plugin.getPlayers().getMembers(plugin.getPlayers().getTeamLeader(owner.getUniqueId())).contains(target.getUniqueId())) {
		// Yes, so this is not a trespass for sure
		return false;
	    }
	    // No, so check where the target is now
	    islandTestLocation = plugin.getPlayers().getTeamIslandLocation(owner.getUniqueId());
	} else {
	    islandTestLocation = plugin.getPlayers().getIslandLocation(owner.getUniqueId());
	}
	// Check position of target
	if (islandTestLocation == null) {
	    return false;
	}
	if (target.getWorld().equals(islandTestLocation.getWorld())) {
	    int protectionRange = Settings.island_protectionRange;
	    if (getIslandAt(islandTestLocation) != null) {

		Island island = getProtectedIslandAt(islandTestLocation);
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
     * Transfers ownership of an island from one player to another
     * 
     * @param oldOwner
     * @param newOwner
     * @return
     */
    public boolean transferIsland(final UUID oldOwner, final UUID newOwner) {
	if (plugin.getPlayers().hasIsland(oldOwner)) {
	    Location islandLoc = plugin.getPlayers().getIslandLocation(oldOwner);
	    plugin.getPlayers().setHasIsland(newOwner, true);
	    plugin.getPlayers().setIslandLocation(newOwner, islandLoc);
	    // plugin.getPlayers().setIslandLevel(newOwner,
	    // plugin.getPlayers().getIslandLevel(oldOwner));
	    plugin.getPlayers().setTeamIslandLocation(newOwner, null);
	    plugin.getPlayers().setHasIsland(oldOwner, false);
	    plugin.getPlayers().setIslandLocation(oldOwner, null);
	    // plugin.getPlayers().setIslandLevel(oldOwner, 0);
	    plugin.getPlayers().setTeamIslandLocation(oldOwner, islandLoc);
	    // Update grid
	    Island island = getIslandAt(islandLoc);
	    if (island != null) {
		setIslandOwner(island, newOwner);
	    }
	    // Update top ten list
	    // Remove old owner score from top ten list
	    TopTen.remove(oldOwner);
	    return true;
	}
	return false;
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
		if (c.isLoaded()) {
		    for (final Entity e : c.getEntities()) {
			if (e instanceof Monster && !Settings.mobWhiteList.contains(e.getType())) {
			    e.remove();
			}
		    }
		}
	    }
	}
    }

    /**
     * This removes players from an island overworld and nether - used when reseting or deleting an island
     * Mobs are killed when the chunks are refreshed.
     * @param island to remove players from
     */
    public void removePlayersFromIsland(final Island island) {
	// Teleport players away
	for (Player player : plugin.getServer().getOnlinePlayers()) {
	    if (island.inIslandSpace(player.getLocation())) {
		// Teleport island players to their island home
		if (!player.getUniqueId().equals(island.getOwner()) && (plugin.getPlayers().hasIsland(player.getUniqueId()) || plugin.getPlayers().inTeam(player.getUniqueId()))) {
		    homeTeleport(player);
		} else {
		    // Move player to spawn
		    Island spawn = getSpawn();
		    if (spawn != null) {
			// go to island spawn
			player.teleport(ASkyBlock.getIslandWorld().getSpawnLocation());
			plugin.getLogger().warning("During island deletion player " + player.getName() + " sent to spawn.");
		    } else {
			if (!player.performCommand(Settings.SPAWNCOMMAND)) {
			    plugin.getLogger().warning(
				    "During island deletion player " + player.getName() + " could not be sent to spawn so was dropped, sorry.");
			} else {
			    plugin.getLogger().warning("During island deletion player " + player.getName() + " sent to spawn using /spawn.");
			}
		    }
		}
	    }
	}
    }

    /**
     * @return a list of unowned islands
     */
    public HashMap<String, Island> getUnownedIslands() {
	HashMap<String, Island> result = new HashMap<String,Island>();
	for (Entry<Integer, TreeMap<Integer, Island>> x : islandGrid.entrySet()) {
	    for (Island island : x.getValue().values()) {
		//plugin.getLogger().info("DEBUG: checking island at " + island.getCenter());
		if (island.getOwner() == null && !island.isSpawn() && !island.isPurgeProtected()) {
		    Location center = island.getCenter();
		    String serialized = island.getCenter().getWorld().getName() + ":" + center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ();
		    result.put(serialized,island);
		}
	    }
	}
	return result;
    }

    /**
     * Set the spawn point for the island world
     * @param location
     */
    public void setSpawnPoint(Location location) {
	ASkyBlock.getIslandWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	//plugin.getLogger().info("DEBUG: setting spawn point to " + location);
	spawn.setSpawnPoint(location);
    }

    /**
     * @return the spawnPoint
     */
    public Location getSpawnPoint() {
	//plugin.getLogger().info("DEBUG: getting spawn point : " + spawn.getSpawnPoint());
	return spawn.getSpawnPoint();
    }
}