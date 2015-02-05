/**
 * 
 */
package com.wasteofplastic.askyblock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author tastybento
 * This class manages the island islandGrid. It knows where every island is, and where new
 * ones should go. It can handle any size of island or protection size
 * The islandGrid is stored in a YML file.
 */
public class GridManager {
    private ASkyBlock plugin = ASkyBlock.getPlugin();
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer,TreeMap<Integer,Island>> islandGrid = new TreeMap<Integer,TreeMap<Integer,Island>>();
    //private TreeMap<Integer,TreeMap<Integer,Island>> protectionGrid = new TreeMap<Integer,TreeMap<Integer,Island>>();
    // Reverse lookup for owner, if they exists
    private HashMap<UUID,Island> ownershipMap = new HashMap<UUID,Island>();
    private File islandFile;
    private Island spawn;

    /**
     * @param plugin
     */
    public GridManager(ASkyBlock plugin) {
	this.plugin = plugin;
	loadGrid();
    }

    protected void loadGrid() {
	plugin.getLogger().info("Loading island grid...");
	islandGrid.clear();
	//protectionGrid.clear();
	islandFile = new File(plugin.getDataFolder(),"islands.yml");
	if (!islandFile.exists()) {
	    // check if island folder exists
	    plugin.getLogger().info("islands.yml does not exist. Creating...");
	    convert();
	} else {
	    plugin.getLogger().info("Loading islands.yml");
	    YamlConfiguration islandYaml = ASkyBlock.loadYamlFile("islands.yml");
	    List <String> islandList = new ArrayList<String>();
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
		plugin.getLogger().severe("Could not find any islands for this world...");
	    } 
	}
	//for (int x : protectionGrid.)
	//plugin.getLogger().info("Debug: protection grid is size " + protectionGrid.size());
	//plugin.getLogger().info("Debug: Island grid is sized = " + islandGrid.size());
    }

    /**
     * TODO: Need to convert from player files too
     */
    private void convert() {
	// Go through player folder
	final File playerFolder = new File(plugin.getDataFolder() + File.separator + "players");
	YamlConfiguration playerFile = new YamlConfiguration();
	int count = 0;
	if (playerFolder.exists()) {
	    plugin.getLogger().warning("Reading player folder. This could time some time with a large number of players...");
	    for (File f: playerFolder.listFiles()) {
		// Need to remove the .yml suffix
		String fileName = f.getName();
		if (fileName.endsWith(".yml")) {
		    try {
			playerFile.load(f);
			// TODO: ADD TOP TEN INPUT HERE!!!!
			boolean hasIsland = playerFile.getBoolean("hasIsland",false);
			if (hasIsland) {
			    String islandLocation = playerFile.getString("islandLocation");
			    if (islandLocation.isEmpty()) {
				plugin.getLogger().severe("Problem with " + fileName);
				plugin.getLogger().severe("Player file says they have an island, but there is no location.");
			    } else {
				Location islandLoc = ASkyBlock.getLocationString(islandLocation);
				String ownerString = fileName.substring(0, fileName.length()-4);
				UUID owner = UUID.fromString(ownerString);
				Island newIsland = addIsland(islandLoc.getBlockX(),islandLoc.getBlockZ(),owner);
				ownershipMap.put(owner, newIsland);
				count++;
				//plugin.getLogger().info("Converted island at " + islandLoc);
				// Top ten
				int islandLevel = playerFile.getInt("islandLevel",0);
				String teamLeaderUUID = playerFile.getString("teamLeader","");
				if (islandLevel > 0) {
				    if (!playerFile.getBoolean("hasTeam")) {
					plugin.topTenAddEntry(owner, islandLevel);
				    } else if (!teamLeaderUUID.isEmpty()) {
					if (teamLeaderUUID.equals(ownerString)) {
					    plugin.topTenAddEntry(owner, islandLevel);
					}
				    }
				}
			    }
			}

		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + fileName);
			//e.printStackTrace(); 
		    }
		}
	    }
	}
	plugin.topTenSave();
	plugin.getLogger().info("Converted "+ count + " islands from player's folder");
	int count2 = 0;
	// Check island folder
	final File islandFolder = new File(plugin.getDataFolder() + File.separator + "islands");
	if (islandFolder.exists()) {
	    plugin.getLogger().warning("Reading island folder. This could time some time with a large number of islands...");
	    for (File f: islandFolder.listFiles()) {
		// Need to remove the .yml suffix
		String fileName = f.getName();
		int comma = fileName.indexOf(",");
		if (fileName.endsWith(".yml") && comma != -1) {
		    try {
			// Parse to an island value
			int x = Integer.parseInt(fileName.substring(0, comma));
			int z = Integer.parseInt(fileName.substring(comma +1 , fileName.indexOf(".")));
			// Note that this is the CENTER of the island
			if (getIslandAt(x,z) == null) {
			    addIsland(x,z);
			    count2++;
			    //plugin.getLogger().info("Added island from island folder: " + x + "," +z);
			}
		    } catch (Exception e) {
			e.printStackTrace(); 
		    }
		}
	    }
	}
	plugin.getLogger().info("Converted "+ count2 + " islands from island folder");
	plugin.getLogger().info("Total "+ (count+count2) + " islands converted.");
	// Now save the islandGrid
	saveGrid();
    }

    protected void saveGrid() {
	final File islandFile = new File(plugin.getDataFolder(), "islands.yml");
	YamlConfiguration islandYaml = new YamlConfiguration();
	List <String> islandList = new ArrayList<String>();
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

    protected Island getIslandAt(int x, int z) {
	Entry<Integer, TreeMap<Integer,Island>> en = islandGrid.lowerEntry(x);
	if (en != null) {
	    Entry<Integer, Island> ent = en.getValue().lowerEntry(z);
	    if (ent != null) {
		// Check if in the island range
		Island island = ent.getValue();
		if (island.inIslandSpace(x,z)) {
		    //plugin.getLogger().info("DEBUG: In island space");
		    return island;
		}
		//plugin.getLogger().info("DEBUG: not in island space");
	    }
	}
	return null;	
    }

    /**
     * Returns the island at the location or null if there is none
     * @param location
     * @return Island object
     */
    protected Island getIslandAt(Location location) {
	// Check if it is spawn
	if (spawn != null && spawn.onIsland(location)) {
	    return spawn;
	}
	int x = location.getBlockX();
	Entry<Integer, TreeMap<Integer,Island>> en = islandGrid.lowerEntry(x);
	if (en != null) {
	    int z = location.getBlockZ();
	    Entry<Integer, Island> ent = en.getValue().lowerEntry(z);
	    if (ent != null) {
		// Check if in the island range
		Island island = ent.getValue();
		if (island.inIslandSpace(location)) {
		    //plugin.getLogger().info("DEBUG: In island space");
		    return island;
		}
		//plugin.getLogger().info("DEBUG: not in island space");
	    }
	}
	return null;
    }

    /**
     * Returns the island being protected at the location or null if there is none
     * @param location
     * @return Island object
     */
    protected Island getProtectedIslandAt(Location location) {
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
     * @param location
     * @return UUID of owner
     */
    protected UUID getOwnerOfIslandAt(Location location) {
	Island island = getIslandAt(location);
	if (island != null) {
	    return island.getOwner();
	}
	return null;
    }

    // islandGrid manipulation methods
    /**
     * Adds an island to the islandGrid with the CENTER point x,z
     * @param x
     * @param z
     */
    protected Island addIsland(int x, int z) {
	return addIsland(x, z, null);
    }

    /**
     * Adds an island to the islandGrid with the center point x,z owner UUID
     * @param x
     * @param z
     * @param owner
     */
    protected Island addIsland(int x, int z, UUID owner) {
	Island newIsland = new Island(x,z, owner);
	addToGrids(newIsland);
	return newIsland;
    }

    /**
     * Adds island to the grid using the stored information
     * @param islandSerialized
     */
    protected Island addIsland(String islandSerialized) {
	//plugin.getLogger().info("DEBUG: adding island " + islandSerialized);
	Island newIsland = new Island(islandSerialized);
	addToGrids(newIsland);
	return newIsland;
    }

    private void addToGrids(Island newIsland) {
	//plugin.getLogger().info("DEBUG: adding island to grid");
	if (islandGrid.containsKey(newIsland.getMinX())) {
	    //plugin.getLogger().info("DEBUG: min x is in the grid :" + newIsland.getMinX()); 
	    TreeMap<Integer,Island> zEntry = islandGrid.get(newIsland.getMinX());
	    if (zEntry.containsKey(newIsland.getMinZ())) {
		// Island already exists
		Island conflict = islandGrid.get(newIsland.getMinX()).get(newIsland.getMinZ());
		plugin.getLogger().warning("Conflict in island grid");
		plugin.getLogger().warning("Previous island center = " + conflict.getCenter().toString());
		plugin.getLogger().warning("Duplicate island found in island.yml: " + newIsland.getCenter().toString());
		return;
	    } else {
		// Add island
		//plugin.getLogger().info("DEBUG: min z is not in the grid :" + newIsland.getMinZ());
		zEntry.put(newIsland.getMinZ(), newIsland);
		islandGrid.put(newIsland.getMinX(), zEntry);
		//plugin.getLogger().info("Debug: " + newIsland.toString());
	    }
	} else {
	    // Add island
	    //plugin.getLogger().info("DEBUG: min x is not in the grid :" + newIsland.getMinX());
	    //plugin.getLogger().info("DEBUG: min Z is not in the grid :" + newIsland.getMinZ());
	    TreeMap<Integer,Island> zEntry = new TreeMap<Integer,Island>();
	    zEntry.put(newIsland.getMinZ(), newIsland);
	    islandGrid.put(newIsland.getMinX(), zEntry);
	}
    }

    protected void deleteIsland(Location loc) {
	//plugin.getLogger().info("DEBUG: deleting island at " + loc);
	Island island = getIslandAt(loc);
	if (island != null) {
	    UUID owner = island.getOwner();
	    int x = island.getMinX();
	    int z = island.getMinZ();
	    //plugin.getLogger().info("DEBUG: x = " + x + " z = " + z);
	    if (islandGrid.containsKey(x)) {
		//plugin.getLogger().info("DEBUG: x found");
		TreeMap<Integer,Island> zEntry = islandGrid.get(x);
		if (zEntry.containsKey(z)) {
		    //plugin.getLogger().info("DEBUG: z found - deleting the island");
		    // Island exists - delete it
		    zEntry.remove(z);
		    islandGrid.put(x, zEntry);
		}  else {
		    //plugin.getLogger().info("DEBUG: could not find z");
		}
	    }
	    // Remove from the ownership map
	    ownershipMap.remove(owner);
	}


    }

    /**
     * Gets island by owner's UUID. Just because the island does not exist in this map
     * does not mean it does not exist in this world, due to legacy island support
     * Will return the island that this player is a member of if a team player
     * @param owner
     * @return island object or null if it does not exist in the list
     */
    protected Island getIsland(UUID owner) {
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
     * Sets an island to be owned by another player. If the new owner had an island, that island is released to null ownership
     * @param island
     * @param newOwner
     */
    protected void setIslandOwner(Island island, UUID newOwner) {
	if (newOwner != null && island != null) {
	    // See if this island has an owner already
	    UUID owner = island.getOwner();
	    island.setOwner(newOwner);
	    if (ownershipMap.containsKey(owner)) {
		// Remove the old entry
		ownershipMap.remove(owner);
	    }
	    if (ownershipMap.containsKey(newOwner)) {
		// new owner had an island - make owner null
		ownershipMap.get(newOwner).setOwner(null);
	    }
	    // Insert the new entry
	    ownershipMap.put(newOwner,island);
	}
    }
    /**
     * @return the spawn
     */
    public Island getSpawn() {
	return spawn;
    }

    /**
     * @param spawn the spawn to set
     */
    public void setSpawn(Island spawn) {
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
     * @param playerLoc
     * @return true if they are, false if they are not, or spawn does not exist
     */
    public boolean isAtSpawn(Location playerLoc) {
	if (spawn == null) {
	    return false;
	}
	return spawn.onIsland(playerLoc);
    }


}
