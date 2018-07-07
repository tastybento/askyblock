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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
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

import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.events.IslandChangeOwnerEvent;
import com.wasteofplastic.askyblock.events.IslandPreTeleportEvent;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.teleport.SafeTeleportBuilder;

/**
 * This class manages the island islandGrid. It knows where every island is, and
 * where new
 * ones should go. It can handle any size of island or protection size
 * The islandGrid is stored in a YML file.
 *
 * @author tastybento
 */
public class GridManager {
    private static final String SETTINGS_KEY = "settingskey";
    private static final String ISLANDS_FILENAME = "islands.yml";
    private static final String ISLANDNAMES_FILENAME = "islandnames.yml";
    private final ASkyBlock plugin;
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<>();
    // Reverse lookup for owner, if they exists
    private final HashMap<UUID, Island> ownershipMap = new HashMap<>();
    private Island spawn;
    private final YamlConfiguration islandNames = new YamlConfiguration();

    /**
     * @param plugin - ASkyBlock plugin object
     */
    public GridManager(ASkyBlock plugin) {
        this.plugin = plugin;
        loadGrid();
    }

    private void loadGrid() {
        plugin.getLogger().info("Loading island grid...");
        islandGrid.clear();
        File islandNameFile = new File(plugin.getDataFolder(), ISLANDNAMES_FILENAME);
        if (!islandNameFile.exists()) {
            try {
                islandNameFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + ISLANDNAMES_FILENAME + "!");
            }
        }
        try {
            islandNames.load(islandNameFile);
        } catch (Exception e) {
            //e.printStackTrace();
            plugin.getLogger().severe("Could not load " + ISLANDNAMES_FILENAME);
        }
        File islandFile = new File(plugin.getDataFolder(), ISLANDS_FILENAME);
        if (!islandFile.exists()) {
            // check if island folder exists
            plugin.getLogger().info(ISLANDS_FILENAME + " does not exist. Creating...");
            convert();
            plugin.getLogger().info(ISLANDS_FILENAME + " created.");
        } else {
            plugin.getLogger().info("Loading " + ISLANDS_FILENAME);
            YamlConfiguration islandYaml = new YamlConfiguration();
            try {
                islandYaml.load(islandFile);
                List<String> islandList = new ArrayList<>();
                if (islandYaml.contains(Settings.worldName)) {
                    // Load the island settings key
                    List<String> settingsKey = islandYaml.getStringList(SETTINGS_KEY);
                    // Load spawn, if it exists - V3.0.6 onwards
                    if (islandYaml.contains("spawn")) {
                        Location spawnLoc = Util.getLocationString(islandYaml.getString("spawn.location"));
                        // Validate entries
                        if (spawnLoc != null && spawnLoc.getWorld() != null && spawnLoc.getWorld().equals(ASkyBlock.getIslandWorld())) {
                            Location spawnPoint = Util.getLocationString(islandYaml.getString("spawn.spawnpoint"));
                            int range = islandYaml.getInt("spawn.range", Settings.islandProtectionRange);
                            if (range < 0) {
                                range = Settings.islandProtectionRange;
                            }
                            String spawnSettings = islandYaml.getString("spawn.settings");
                            // Make the spawn
                            Island newSpawn = new Island(plugin, spawnLoc.getBlockX(), spawnLoc.getBlockZ());
                            newSpawn.setSpawn(true);
                            if (spawnPoint != null)
                                newSpawn.setSpawnPoint(spawnPoint);
                            newSpawn.setProtectionSize(range);
                            newSpawn.setSettings(spawnSettings, settingsKey);
                            spawn = newSpawn;
                        }

                    }
                    // Load the islands
                    islandList = islandYaml.getStringList(Settings.worldName);
                    for (String island : islandList) {
                        Island newIsland = addIsland(island, settingsKey);
                        if (newIsland.getOwner() != null) {
                            ownershipMap.put(newIsland.getOwner(), newIsland);
                        }
                        if (newIsland.isSpawn()) {
                            spawn = newIsland;
                        }
                    }
                } else {
                    plugin.getLogger().severe("Could not find any islands for this world. World name in config.yml is probably wrong.");
                    plugin.getLogger().severe("Making backup of " + ISLANDS_FILENAME + ". Correct world name and then replace " + ISLANDS_FILENAME);
                    File rename = new File(plugin.getDataFolder(), "islands_backup.yml");
                    islandFile.renameTo(rename);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                plugin.getLogger().severe("Could not load " + ISLANDS_FILENAME);
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
     * @param loc - location to check
     * @return true if on grid, false if not
     */
    public boolean onGrid(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return onGrid(x, z);
    }

    public boolean onGrid(int x, int z) {
        if ((x - Settings.islandXOffset) % Settings.islandDistance != 0) {
            return false;
        }
        return (z - Settings.islandZOffset) % Settings.islandDistance == 0;
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
                                                plugin.getTopTen().topTenAddEntry(owner, islandLevel);
                                            } else if (!teamLeaderUUID.isEmpty()) {
                                                if (teamLeaderUUID.equals(ownerString)) {
                                                    plugin.getTopTen().topTenAddEntry(owner, islandLevel);
                                                }
                                            }
                                        }

                                        // Check if there is an island info string and see if it jibes
                                        String islandInfo = playerFile.getString("islandInfo","");
                                        if (!islandInfo.isEmpty()) {
                                            String[] split = islandInfo.split(":");
                                            try {
                                                //int protectionRange = Integer.parseInt(split[3]);
                                                //int islandDistance = Integer.parseInt(split[4]);
                                                newIsland.setLocked(false);
                                                if (split.length > 6) {
                                                    // Get locked status
                                                    if (split[6].equalsIgnoreCase("true")) {
                                                        newIsland.setLocked(true);
                                                    }
                                                }
                                                // Check if deletable
                                                newIsland.setPurgeProtected(false);
                                                if (split.length > 7) {
                                                    if (split[7].equalsIgnoreCase("true")) {
                                                        newIsland.setPurgeProtected(true);
                                                    }
                                                }
                                                if (!split[5].equals("null")) {
                                                    if (split[5].equals("spawn")) {
                                                        newIsland.setSpawn(true);
                                                        // Try to get the spawn point
                                                        if (split.length > 8) {
                                                            //plugin.getLogger().info("DEBUG: " + serial.substring(serial.indexOf(":SP:") + 4));
                                                            Location spawnPoint = Util.getLocationString(islandInfo.substring(islandInfo.indexOf(":SP:") + 4));
                                                            newIsland.setSpawnPoint(spawnPoint);
                                                        }
                                                    }
                                                }
                                                // Check if protection options there
                                                if (!newIsland.isSpawn()) {
                                                    //plugin.getLogger().info("DEBUG: NOT SPAWN owner is " + owner + " location " + center);
                                                    if (split.length > 8 && split[8].length() == 29) {
                                                        // Parse the 8th string into island guard protection settings
                                                        int index = 0;
                                                        // Run through the enum and set
                                                        for (SettingsFlag flag : SettingsFlag.values()) {
                                                            if (index < split[8].length()) {
                                                                newIsland.setIgsFlag(flag, split[8].charAt(index++) == '1');
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
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
                        //e.printStackTrace();
                    }
                }
            }
            plugin.getLogger().info("Converted " + count + " islands from player's folder");
            plugin.getLogger().info(noisland + " have no island, of which " + inTeam + " are in a team.");
            plugin.getLogger().info((noisland - inTeam) + " are in the system, but have no island or team");
        }

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

    /**
     * Saves the grid asynchronously
     */
    public void saveGrid() {
        saveGrid(true);
    }

    /**
     * Saves the grid. Option to save sync or async.
     * Async cannot be used when disabling the plugin
     * @param async - true if saving should be done async
     */
    public void saveGrid(boolean async) {
        //final File islandFile = new File(plugin.getDataFolder(), ISLANDS_FILENAME);
        final YamlConfiguration islandYaml = new YamlConfiguration();
        // Save the settings config key
        List<String> islandSettings = new ArrayList<String>();
        for (SettingsFlag flag: SettingsFlag.values()) {
            islandSettings.add(flag.toString());
        }
        islandYaml.set(SETTINGS_KEY, islandSettings);
        // Save spawn
        if (getSpawn() != null) {
            islandYaml.set("spawn.location", Util.getStringLocation(getSpawn().getCenter()));
            islandYaml.set("spawn.spawnpoint", Util.getStringLocation(getSpawn().getSpawnPoint()));
            islandYaml.set("spawn.range", getSpawn().getProtectionSize());
            islandYaml.set("spawn.settings", getSpawn().getSettings());
        }
        // Save the regular islands
        List<String> islandList = new ArrayList<String>();
        Iterator<TreeMap<Integer, Island>> it = islandGrid.values().iterator();
        while (it.hasNext()) {
            Iterator<Island> islandIt = it.next().values().iterator();
            while (islandIt.hasNext()) {
                Island island = islandIt.next();
                if (!island.isSpawn()) {
                    islandList.add(island.save());
                }
            }
        }
        islandYaml.set(Settings.worldName, islandList);
        // Save the file
        Util.saveYamlFile(islandYaml, ISLANDS_FILENAME, async);
        // Save any island names
        if (islandNames != null) {
            Util.saveYamlFile(islandNames, ISLANDNAMES_FILENAME, async);
        }
    }

    /**
     * Returns the island at the location or null if there is none.
     * This includes the full island space, not just the protected area
     *
     * @param location - location to query
     * @return PlayerIsland object
     */
    public Island getIslandAt(Location location) {
        if (location == null) {
            return null;
        }
        // World check
        if (!inWorld(location)) {
            return null;
        }
        // Check if it is spawn
        if (spawn != null && spawn.onIsland(location)) {
            return spawn;
        }
        return getIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Determines if a location is in the island world or not or
     * in the new nether if it is activated
     * @param loc - location to query
     * @return true if in the island world
     */
    protected boolean inWorld(Location loc) {
        if (loc.getWorld().equals(ASkyBlock.getIslandWorld())) {
            return true;
        }
        return Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null
                && loc.getWorld().equals(ASkyBlock.getNetherWorld());
    }


    /**
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     *
     * @param x coord
     * @param z coord
     * @return PlayerIsland or null
     */
    public Island getIslandAt(int x, int z) {
        Entry<Integer, TreeMap<Integer, Island>> en = islandGrid.floorEntry(x);
        if (en != null) {
            Entry<Integer, Island> ent = en.getValue().floorEntry(z);
            if (ent != null) {
                // Check if in the island range
                Island island = ent.getValue();
                if (island.inIslandSpace(x, z)) {
                    // plugin.getLogger().info("DEBUG: In island space");
                    return island;
                }
                //plugin.getLogger().info("DEBUG: not in island space");
            }
        }
        return null;
    }

    /**
     * Returns the island being public at the location or null if there is none
     *
     * @param location - location to query
     * @return Island object
     */
    public Island getProtectedIslandAt(Location location) {
        //plugin.getLogger().info("DEBUG: getProtectedIslandAt " + location);
        // Try spawn
        if (spawn != null && spawn.onIsland(location)) {
            return spawn;
        }
        Island island = getIslandAt(location);
        if (island == null) {
            //plugin.getLogger().info("DEBUG: no island at this location");
            return null;
        }
        if (island.onIsland(location)) {
            return island;
        }
        return null;
    }

    /**
     * Returns the owner of the island at location or null if there is none
     *
     * @param location location to query
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
     * @param x cood
     * @param z cood
     * @return island object
     */
    public Island addIsland(int x, int z) {
        return addIsland(x, z, null);
    }

    /**
     * Adds an island to the islandGrid with the center point x,z owner UUID
     *
     * @param x coord
     * @param z coord
     * @param owner owner of island
     * @return island object
     */
    public Island addIsland(int x, int z, UUID owner) {
        // Check if this owner already has an island
        if (ownershipMap.containsKey(owner)) {
            // Island exists
            Island island = ownershipMap.get(owner);
            // Remove island if the player already has a different one
            if (island.getCenter().getBlockX() != x || island.getCenter().getBlockZ() != z) {
                //plugin.getLogger().warning(
                //"Island at " + island.getCenter().getBlockX() + ", " + island.getCenter().getBlockZ()
                //+ " is already owned by this player. Removing ownership of this island.");
                island.setOwner(null);
                ownershipMap.remove(owner);
            } else {
                // Player already has island
                addToGrids(island);
                return island;
            }
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
     * @param islandSerialized serialized version of the island information
     * @param settingsKey
     * @return island object
     */
    public Island addIsland(String islandSerialized, List<String> settingsKey) {
        // plugin.getLogger().info("DEBUG: adding island " + islandSerialized);
        Island newIsland = new Island(plugin, islandSerialized, settingsKey);
        addToGrids(newIsland);
        return newIsland;
    }

    /**
     * Adds an island to the grid register
     * @param newIsland new island object to register on the grid
     */
    private void addToGrids(Island newIsland) {
        //plugin.getLogger().info("DEBUG: adding island to grid at " + newIsland.getMinX() + "," + newIsland.getMinZ());
        if (newIsland.getOwner() != null) {
            ownershipMap.put(newIsland.getOwner(), newIsland);
        }
        if (islandGrid.containsKey(newIsland.getMinX())) {
            //plugin.getLogger().info("DEBUG: min x is in the grid :" + newIsland.getMinX());
            TreeMap<Integer, Island> zEntry = islandGrid.get(newIsland.getMinX());
            if (zEntry.containsKey(newIsland.getMinZ())) {
                //plugin.getLogger().info("DEBUG: min z is in the grid :" + newIsland.getMinZ());
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
            } else {
                // Add island
                //plugin.getLogger().info("DEBUG: added island to grid at " + newIsland.getMinX() + "," + newIsland.getMinZ());
                zEntry.put(newIsland.getMinZ(), newIsland);
                islandGrid.put(newIsland.getMinX(), zEntry);
                // plugin.getLogger().info("Debug: " + newIsland.toString());
            }
        } else {
            // Add island
            //plugin.getLogger().info("DEBUG: added island to grid at " + newIsland.getMinX() + "," + newIsland.getMinZ());
            TreeMap<Integer, Island> zEntry = new TreeMap<Integer, Island>();
            zEntry.put(newIsland.getMinZ(), newIsland);
            islandGrid.put(newIsland.getMinX(), zEntry);
        }
    }

    /**
     * Deletes any island owned by owner from the grid. Does not actually remove the island
     * from the world. Used for cleaning up issues such as mismatches between player files
     * and island.yml
     * @param owner UUID of owner
     */
    public void deleteIslandOwner(UUID owner) {
        if (owner != null && ownershipMap.containsKey(owner)) {
            Island island = ownershipMap.get(owner);
            if (island != null) {
                island.setOwner(null);
            }
            ownershipMap.remove(owner);
        }
    }
    /**
     * Removes the island at location loc from the grid and removes the player
     * from the ownership map
     *
     * @param loc location to remove
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
     * @param owner island owner's UUID
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
     * @param island island object
     * @param newOwner new owner's UUID
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
            if (oldOwner != null) {
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
     * @return the spawn or null if spawn does not exist
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
     * @param playerLoc - location to query
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
     * @param loc location to query
     * @return true if found, otherwise false
     */
    public boolean islandAtLocation(Location loc) {
        if (loc == null) {
            return true;
        }
        // Make sure location is on the grid
        loc = getClosestIsland(loc);
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
        if (!Settings.useOwnGenerator) {
            // Block check
            if (!loc.getBlock().isEmpty() && !loc.getBlock().isLiquid()) {
                // Get the closest island
                plugin.getLogger().info("Found solid block at island height - adding to " + ISLANDS_FILENAME + " " + px + "," + pz);
                addIsland(px, pz);
                return true;
            }
            // Look around

            for (int x = -5; x <= 5; x++) {
                for (int y = 10; y <= 255; y++) {
                    for (int z = -5; z <= 5; z++) {
                        if (!loc.getWorld().getBlockAt(x + px, y, z + pz).isEmpty() && !loc.getWorld().getBlockAt(x + px, y, z + pz).isLiquid()) {
                            plugin.getLogger().info("Solid block found during long search - adding to " + ISLANDS_FILENAME + " " + px + "," + pz);
                            addIsland(px, pz);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     *
     * @param location location to query
     * @return Location of closest island
     */
    public Location getClosestIsland(Location location) {
        long x = Math.round((double) location.getBlockX() / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
        long z = Math.round((double) location.getBlockZ() / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;
        long y = Settings.islandHeight;
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
        // In ASkyBlock, liquid may be unsafe
        if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
            if (Settings.acidDamage > 0D
                    || ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
                    || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
                    || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
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
        return !space2.getType().isSolid() || space2.getType().equals(Material.SIGN_POST) || space2
                .getType().equals(Material.WALL_SIGN);
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
     * @param i - the range to scan for a location less than 0 means the full island.
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
     * @param player player object
     * @return true if the home teleport is successful
     */
    public void homeTeleport(final Player player) {
        homeTeleport(player, 1);
    }
    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     * @param player player object
     * @param number - home location to do to
     * @return true if successful, false if not
     */
    @SuppressWarnings("deprecation")
    public void homeTeleport(final Player player, int number) {
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
            new SafeTeleportBuilder(plugin)
            .entity(player)
            .location(plugin.getPlayers().getHomeLocation(player.getUniqueId(), number))
            .homeNumber(number)
            .build();
            return;
        }
        //plugin.getLogger().info("DEBUG: home loc = " + home + " teleporting");
        //home.getChunk().load();
        IslandPreTeleportEvent event = new IslandPreTeleportEvent(player, IslandPreTeleportEvent.Type.HOME, home);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            player.teleport(event.getLocation());
            //player.sendBlockChange(home, Material.GLOWSTONE, (byte)0);
            if (number ==1 ) {
                Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).islandteleport);
            } else {
                Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).islandteleport + " #" + number);
            }
        }
        plugin.getPlayers().setInTeleport(player.getUniqueId(), false);
    }

    /**
     * Sets the numbered home location based on where the player is now
     * @param player player object
     * @param number home number
     */
    public void homeSet(Player player, int number) {
        // Check if player is in their home world
        if (!player.getWorld().equals(plugin.getPlayers().getIslandLocation(player.getUniqueId()).getWorld())) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNotOnIsland);
            return;
        }
        // Check if player is on island, ignore coops
        if (!plugin.getGrid().playerIsOnIsland(player, false)) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).setHomeerrorNotOnIsland);
            return;
        }
        plugin.getPlayers().setHomeLocation(player.getUniqueId(), player.getLocation(), number);
        if (number == 1) {
            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).setHomehomeSet);
        } else {
            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).setHomehomeSet + " #" + number);
        }
    }

    /**
     * Sets the home location based on where the player is now
     *
     * @param player player object
     */
    public void homeSet(final Player player) {
        homeSet(player, 1);
    }

    /**
     * Checks if a player is in their full island space
     * @param player
     * @return true if they are anywhere inside their island space (not just protected area)
     */
    public boolean inIslandSpace(Player player) {
        if (player == null) {
            return false;
        }
        Island island = getIslandAt(player.getLocation());
        if (island != null) {
            return island.inIslandSpace(player.getLocation()) && island.getMembers()
                    .contains(player.getUniqueId());
        }
        return false;
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * owned by the player
     *
     * @param player player object
     * @param loc location to query
     * @return true if location is on island of player
     */
    public boolean locationIsOnIsland(final Player player, final Location loc) {
        if (player == null) {
            return false;
        }
        // Get the player's island from the grid if it exists
        Island island = getIslandAt(loc);
        if (island != null) {
            //plugin.getLogger().info("DEBUG: island here is " + island.getCenter());
            // On an island in the grid
            //plugin.getLogger().info("DEBUG: onIsland = " + island.onIsland(loc));
            //plugin.getLogger().info("DEBUG: members = " + island.getMembers());
            //plugin.getLogger().info("DEBUG: player UUID = " + player.getUniqueId());

            //plugin.getLogger().info("DEBUG: allowed");
            // In a protected zone but is on the list of acceptable players
            // Not allowed
            //plugin.getLogger().info("DEBUG: not allowed");
            return island.onIsland(loc) && island.getMembers().contains(player.getUniqueId());
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
                if (loc.getX() >= islandTestLocation.getX() - Settings.islandProtectionRange / 2D
                        && loc.getX() < islandTestLocation.getX() + Settings.islandProtectionRange / 2D
                        && loc.getZ() >= islandTestLocation.getZ() - Settings.islandProtectionRange / 2D
                        && loc.getZ() < islandTestLocation.getZ() + Settings.islandProtectionRange / 2D) {
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
     * @param islandTestLocations set of test locations
     * @param loc location to query
     * @return Location found that is on the island
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
                } else if (loc.getX() > islandTestLocation.getX() - Settings.islandProtectionRange / 2D
                        && loc.getX() < islandTestLocation.getX() + Settings.islandProtectionRange / 2D
                        && loc.getZ() > islandTestLocation.getZ() - Settings.islandProtectionRange / 2D
                        && loc.getZ() < islandTestLocation.getZ() + Settings.islandProtectionRange / 2D) {
                    return islandTestLocation;
                }
            }
        }
        return null;
    }

    /**
     * Checks if an online player is in the protected area of their island, a team island or a
     * coop island
     *
     * @param player playe object
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(final Player player) {
        return playerIsOnIsland(player, true);
    }

    /**
     * Checks if an online player is in the protected area of their island, a team island or a
     * coop island
     * @param player playe object
     * @param coop - if true, coop islands are included
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(final Player player, boolean coop) {
        return locationIsAtHome(player, coop, player.getLocation());
    }


    /**
     * Checks if a location is within the home boundaries of a player. If coop is true, this check includes coop players.
     * @param player player object
     * @param coop true if coops should be included
     * @param loc location to query
     * @return true if the location is within home boundaries
     */
    public boolean locationIsAtHome(final Player player, boolean coop, Location loc) {
        // Make a list of test locations and test them
        Set<Location> islandTestLocations = new HashSet<Location>();
        if (plugin.getPlayers().hasIsland(player.getUniqueId())) {
            islandTestLocations.add(plugin.getPlayers().getIslandLocation(player.getUniqueId()));
            // If new Nether
            if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
                islandTestLocations.add(netherIsland(plugin.getPlayers().getIslandLocation(player.getUniqueId())));
            }
        } else if (plugin.getPlayers().inTeam(player.getUniqueId())) {
            islandTestLocations.add(plugin.getPlayers().getTeamIslandLocation(player.getUniqueId()));
            if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
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
                int protectionRange = Settings.islandProtectionRange;
                if (getIslandAt(islandTestLocation) != null) {
                    // Get the protection range for this location if possible
                    Island island = getProtectedIslandAt(islandTestLocation);
                    if (island != null) {
                        // We are in a protected island area.
                        protectionRange = island.getProtectionSize();
                    }
                }
                if (loc.getX() > islandTestLocation.getX() - protectionRange / 2D
                        && loc.getX() < islandTestLocation.getX() + protectionRange / 2D
                        && loc.getZ() > islandTestLocation.getZ() - protectionRange / 2D
                        && loc.getZ() < islandTestLocation.getZ() + protectionRange / 2D) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates a Nether version of the locations
     * @param islandLocation - location to generate
     * @return location of nether version
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
     * @param target target of the query
     * @return true if they are on the island otherwise false.
     */
    public boolean isOnIsland(final Player owner, final Player target) {
        // Check world
        if (target.getWorld().equals(ASkyBlock.getIslandWorld()) || (Settings.newNether && Settings.createNether && target.getWorld().equals(ASkyBlock.getNetherWorld()))) {

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

            int protectionRange = Settings.islandProtectionRange;
            if (getIslandAt(islandTestLocation) != null) {

                Island island = getProtectedIslandAt(islandTestLocation);
                // Get the protection range for this location if possible
                if (island != null) {
                    // We are in a protected island area.
                    protectionRange = island.getProtectionSize();
                }
            }
            return target.getLocation().getX() > islandTestLocation.getX() - protectionRange / 2D
                    && target.getLocation().getX() < islandTestLocation.getX() + protectionRange / 2D
                    && target.getLocation().getZ() > islandTestLocation.getZ() - protectionRange / 2D
                    && target.getLocation().getZ() < islandTestLocation.getZ() + protectionRange / 2D;

        }
        return false;
    }

    /**
     * Transfers ownership of an island from one player to another
     *
     * @param oldOwner old owner UUID
     * @param newOwner new owner UUID
     * @return true if successful
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
            plugin.getTopTen().remove(oldOwner);
            // Fire event
            plugin.getServer().getPluginManager().callEvent(new IslandChangeOwnerEvent(island, oldOwner, newOwner));
            return true;
        }
        return false;
    }

    /**
     * Removes monsters around location l
     *
     * @param l location
     */
    public void removeMobs(final Location l) {
        if (!inWorld(l)) {
            return;
        }
        //plugin.getLogger().info("DEBUG: removing mobs");
        // Don't remove mobs if at spawn
        if (this.isAtSpawn(l)) {
            //plugin.getLogger().info("DEBUG: at spawn!");
            return;
        }

        final int px = l.getBlockX();
        final int py = l.getBlockY();
        final int pz = l.getBlockZ();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                final Chunk c = l.getWorld().getChunkAt(new Location(l.getWorld(), px + x * 16, py, pz + z * 16));
                if (c.isLoaded()) {
                    for (final Entity e : c.getEntities()) {
                        //plugin.getLogger().info("DEBUG: " + e.getType());
                        // Don't remove if the entity is an NPC or has a name tag
                        if (e.getCustomName() != null || e.hasMetadata("NPC"))
                            continue;
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
     * @param uuid - uuid to ignore
     */
    public void removePlayersFromIsland(final Island island, UUID uuid) {
        // Teleport players away
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (island.inIslandSpace(player.getLocation())) {
                //plugin.getLogger().info("DEBUG: in island space");
                // Teleport island players to their island home
                if (!player.getUniqueId().equals(uuid) && (plugin.getPlayers().hasIsland(player.getUniqueId()) || plugin.getPlayers().inTeam(player.getUniqueId()))) {
                    //plugin.getLogger().info("DEBUG: home teleport");
                    homeTeleport(player);
                } else {
                    //plugin.getLogger().info("DEBUG: move player to spawn");
                    // Move player to spawn
                    Island spawn = getSpawn();
                    if (spawn != null) {
                        // go to island spawn
                        player.teleport(ASkyBlock.getIslandWorld().getSpawnLocation());
                        //plugin.getLogger().warning("During island deletion player " + player.getName() + " sent to spawn.");
                    } else {
                        if (!player.performCommand(Settings.SPAWNCOMMAND)) {
                            plugin.getLogger().warning(
                                    "During island deletion player " + player.getName() + " could not be sent to spawn so was dropped, sorry.");
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
     * @param location island location
     */
    public void setSpawnPoint(Location location) {
        ASkyBlock.getIslandWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        //plugin.getLogger().info("DEBUG: setting spawn point to " + location);
        spawn.setSpawnPoint(location);
    }

    /**
     * @return the spawnPoint or null if spawn does not exist
     */
    public Location getSpawnPoint() {
        //plugin.getLogger().info("DEBUG: getting spawn point : " + spawn.getSpawnPoint());
        if (spawn == null)
            return null;
        return spawn.getSpawnPoint();
    }

    /**
     * @return how many islands are in the grid
     */
    public int getIslandCount() {
        return ownershipMap.size();
    }

    /**
     * Get the ownership map of islands
     * @return Hashmap of owned islands with owner UUID as a key
     *
     */
    public HashMap<UUID, Island> getOwnedIslands() {
        return ownershipMap;
    }

    /**
     * Get name of the island owned by owner
     * @param owner island owner UUID
     * @return Returns the name of owner's island, or the owner's name if there is none.
     */
    public String getIslandName(UUID owner) {
        if (owner == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', islandNames.getString(owner.toString(), plugin.getPlayers().getName(owner))) + ChatColor.RESET;
    }

    /**
     * Set the island name
     * @param owner island owner UUID
     * @param name name to set
     */
    public void setIslandName(UUID owner, String name) {
        islandNames.set(owner.toString(), name);
        Util.saveYamlFile(islandNames, ISLANDNAMES_FILENAME, true);
    }

}
