/**
 * 
 */
package com.wasteofplastic.askyblock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author tastybento
 * This class manages the island grid. It knows where every island is, and where new
 * ones should go. It can handle any size of island or protection size
 * The grid is stored in a YML file.
 */
public class GridManager {
    private ASkyBlock plugin = ASkyBlock.getPlugin();
    // 2D grid of islands, x,z
    private TreeMap<Integer,TreeMap<Integer,Island>> grid = new TreeMap<Integer,TreeMap<Integer,Island>>();
    private File islandFile;


    /**
     * @param plugin
     */
    public GridManager(ASkyBlock plugin) {
	this.plugin = plugin;
	loadGrid();
    }

    protected void loadGrid() {
	islandFile = new File(plugin.getDataFolder(),"islands.yml");
	if (!islandFile.exists()) {
	    plugin.getLogger().info("islands.yml does not exist. Converting...");
	    convert();
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
	// Now save the grid
	saveGrid();

    }

    protected void saveGrid() {
	final File islandFile = new File(plugin.getDataFolder(), "islands.yml");
	YamlConfiguration islandYaml = new YamlConfiguration();
	List <String> islandList = new ArrayList<String>();
	for (int x : grid.keySet()) {
	    for (int z : grid.get(x).keySet()) {
		Island island = grid.get(x).get(z);
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
	Entry<Integer, TreeMap<Integer,Island>> en = grid.lowerEntry(x);
	if (en != null) {
	    int z = location.getBlockZ();
	    Entry<Integer, Island> ent = en.getValue().lowerEntry(z);
	    if (ent != null) {
		return ent.getValue();
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

    // grid manipulation methods
    /**
     * Adds an island to the grid
     * @param x
     * @param z
     * @return true if successful, false if the spot is already taken
     */
    protected boolean addIsland(int x, int z) {
	return addIsland(x, z, null);
    }
    
    /**
     * Adds an island to the grid with owner UUID
     * @param x
     * @param z
     * @param owner
     * @return true if successful, false if the spot is already taken
     */
    protected boolean addIsland(int x, int z, UUID owner) {
	if (grid.containsKey(x)) {
	    TreeMap<Integer,Island> zEntry = grid.get(x);
	    if (zEntry.containsKey(z)) {
		// Island already exists
		return false;
	    } else {
		// Add island
		zEntry.put(z, new Island(x,z, owner));
		grid.put(x, zEntry);
		return true;
	    }
	} else {
	    // Add island
	    TreeMap<Integer,Island> zEntry = new TreeMap<Integer,Island>();
	    zEntry.put(z,new Island(x,z, owner));
	    grid.put(x, zEntry);
	    return true;
	}
    }
    protected boolean addIsland(String islandSerialized) {
	Island newIsland = new Island(islandSerialized);
	if (grid.containsKey(newIsland.getMinX())) {
	    TreeMap<Integer,Island> zEntry = grid.get(newIsland.getMinX());
	    if (zEntry.containsKey(newIsland.getMinZ())) {
		// Island already exists
		plugin.getLogger().warning("Duplicate island found in island.yml: " + newIsland.getMinX() + "," + newIsland.getMinZ());
		return false;
	    } else {
		// Add island
		zEntry.put(newIsland.getMinZ(), newIsland);
		grid.put(newIsland.getMinX(), zEntry);
		//plugin.getLogger().info("Debug: " + newIsland.toString());
		return true;
	    }
	} else {
	    // Add island
	    TreeMap<Integer,Island> zEntry = new TreeMap<Integer,Island>();
	    zEntry.put(newIsland.getMinZ(), newIsland);
	    grid.put(newIsland.getMinX(), zEntry);
	    //plugin.getLogger().info("Debug: " + newIsland.toString());
	    return true;
	}
    }
    /**
     * Delete island from the grid
     * @param x
     * @param z
     * @return true if successful, false if there was no island to delete
     */
    protected boolean deleteIsland(int x, int z) {
	if (grid.containsKey(x)) {
	    TreeMap<Integer,Island> zEntry = grid.get(x);
	    if (zEntry.containsKey(z)) {
		// Island exists - delete it
		zEntry.remove(z);
		grid.put(x, zEntry);
		return true;
	    } 
	}
	return false;
    }
}
