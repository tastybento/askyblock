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
    private TreeMap<Integer,TreeMap<Integer,Island>> protectionGrid = new TreeMap<Integer,TreeMap<Integer,Island>>();
    // Reverse lookup for owner, if they exists
    private HashMap<UUID,Island> ownershipMap = new HashMap<UUID,Island>();
    private File islandFile;


    /**
     * @param plugin
     */
    public GridManager(ASkyBlock plugin) {
	this.plugin = plugin;
	loadGrid();
    }

    protected void loadGrid() {
	islandGrid.clear();
	protectionGrid.clear();
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
		    addIsland(island);
		}
	    } else {
		plugin.getLogger().severe("Could not find any islands for this world...");
	    } 
	}
	plugin.getLogger().info("Debug: protection grid is size " + protectionGrid.size());
	plugin.getLogger().info("Debug: Island grid is sized = " + islandGrid.size());
    }

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
		    addIsland(x,z);
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
	int x = location.getBlockX();
	Entry<Integer, TreeMap<Integer,Island>> en = protectionGrid.lowerEntry(x);
	if (en != null) {
	    int z = location.getBlockZ();
	    Entry<Integer, Island> ent = en.getValue().lowerEntry(z);
	    if (ent != null) {
		// Check if in the island protected space
		Island island = ent.getValue();
		if (island.onIsland(location)) {
		    //plugin.getLogger().info("DEBUG: In island protected space");
		    return island;
		}
		//plugin.getLogger().info("DEBUG: not in island protected space");
	    }
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
    protected void addIsland(int x, int z) {
	addIsland(x, z, null);
    }

    /**
     * Adds an island to the islandGrid with the center point x,z owner UUID
     * @param x
     * @param z
     * @param owner
     */
    protected void addIsland(int x, int z, UUID owner) {
	Island newIsland = new Island(x,z, owner);
	addToGrids(newIsland);
    }

    /**
     * Adds island to the grid using the stored information
     * @param islandSerialized
     */
    protected void addIsland(String islandSerialized) {
	Island newIsland = new Island(islandSerialized);
	addToGrids(newIsland);
    }

    private void addToGrids(Island newIsland) {
	if (islandGrid.containsKey(newIsland.getMinX())) {
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
		zEntry.put(newIsland.getMinZ(), newIsland);
		islandGrid.put(newIsland.getMinX(), zEntry);
		//plugin.getLogger().info("Debug: " + newIsland.toString());
	    }
	} else {
	    // Add island
	    TreeMap<Integer,Island> zEntry = new TreeMap<Integer,Island>();
	    zEntry.put(newIsland.getMinZ(), newIsland);
	    islandGrid.put(newIsland.getMinX(), zEntry);
	}
	// Now add to protection grid
	if (protectionGrid.containsKey(newIsland.getMinProtectedX())) {
	    TreeMap<Integer,Island> zEntry = protectionGrid.get(newIsland.getMinProtectedX());
	    if (zEntry.containsKey(newIsland.getMinProtectedZ())) {
		Island conflict = islandGrid.get(newIsland.getMinProtectedX()).get(newIsland.getMinProtectedZ());
		plugin.getLogger().warning("Conflict in protetion grid");
		plugin.getLogger().warning("Previous island center = " + conflict.getCenter().toString());
		plugin.getLogger().warning("Duplicate island found in island.yml: " + newIsland.getCenter().toString());
		return;
	    } else {
		// Add island
		zEntry.put(newIsland.getMinProtectedZ(), newIsland);
		protectionGrid.put(newIsland.getMinProtectedX(), zEntry);
		//plugin.getLogger().info("Debug: " + newIsland.toString());
	    }
	} else {
	    // Add island
	    TreeMap<Integer,Island> zEntry = new TreeMap<Integer,Island>();
	    zEntry.put(newIsland.getMinProtectedZ(), newIsland);
	    protectionGrid.put(newIsland.getMinProtectedX(), zEntry);
	}
	// Add reverse look up for owner
	if (newIsland.getOwner() != null) {
	    ownershipMap.put(newIsland.getOwner(), newIsland);
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
	// Remove the protection grid
	x = island.getMinProtectedX();
	z = island.getMinProtectedZ();
	if (protectionGrid.containsKey(x)) {
	    TreeMap<Integer,Island> zEntry = protectionGrid.get(x);
	    if (zEntry.containsKey(z)) {
		// Island exists - delete it
		zEntry.remove(z);
		protectionGrid.put(x, zEntry);
	    } 
	}	
    }
    
    /**
     * Gets island by owner's UUID. Just because the island does not exist in this map
     * does not mean it does not exist in this world, due to legacy island support
     * @param owner
     * @return island object or null if it does not exist in the list
     */
    protected Island getIsland(UUID owner) {
	if (owner != null) {
	    if (ownershipMap.containsKey(owner)) {
		return ownershipMap.get(owner);
	    }
	}
	return null;
    }
}
