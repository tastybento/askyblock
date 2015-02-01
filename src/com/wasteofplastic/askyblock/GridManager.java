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
	islandGrid.clear();
	//protectionGrid.clear();
	islandFile = new File(plugin.getDataFolder(),"islands.yml");
	if (!islandFile.exists()) {
	    // check if island folder exists
	    File islandFolder = new File(plugin.getDataFolder() + File.pathSeparator + "islands");
	    if (islandFolder.exists()) {
		plugin.getLogger().info("islands.yml does not exist. Converting...");
		convert();
	    }
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
	final File islandFolder = new File(plugin.getDataFolder() + File.separator + "islands");
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
		    Island newIsland = addIsland(x,z);
		    ownershipMap.put(newIsland.getOwner(), newIsland);
		    //plugin.getLogger().info("DEBUG: added island " + x + "," +z);
		} catch (Exception e) {
		    e.printStackTrace(); 
		}
	    }
	}
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

    /**
     * Returns the island at the location or null if there is none
     * @param location
     * @return Island object
     */
    protected Island getIslandAt(Location location) {
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
	Island island = getIslandAt(loc);
	int x = island.getMinX();
	int z = island.getMinZ();
	if (islandGrid.containsKey(x)) {
	    TreeMap<Integer,Island> zEntry = islandGrid.get(x);
	    if (zEntry.containsKey(z)) {
		// Island exists - delete it
		zEntry.remove(z);
		islandGrid.put(x, zEntry);
	    } 
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
