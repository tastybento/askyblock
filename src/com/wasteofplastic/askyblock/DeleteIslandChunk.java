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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;
import com.wasteofplastic.askyblock.util.Pair;

/**
 * Deletes islands fast using chunk regeneration
 * 
 * @author tastybento
 * 
 */
public class DeleteIslandChunk {
    private final ASkyBlock plugin;
    private Set<Pair> chunksToClear = new HashSet<Pair>();
    private HashMap<Location, Material> blocksToClear = new HashMap<Location,Material>();
    private final NMSAbstraction nms;

    /**
     * Class dedicated to deleting islands
     * 
     * @param plugin
     * @param loc
     */
    public DeleteIslandChunk(final ASkyBlock plugin, final Island island) {
        this.plugin = plugin;
        nms = checkVersion();
        //List<Pair> chunkCoords = new ArrayList<Pair>();
        final World world = island.getCenter().getWorld();
        if (world == null || nms == null) {
            plugin.getLogger().severe("Cannot delete island, sorry - world is null");
            return;
        }
        final int range = island.getProtectionSize() / 2 * +1;
        final int minx = island.getMinProtectedX();
        final int minz = island.getMinProtectedZ();
        final int maxx = island.getMinProtectedX() + island.getProtectionSize();
        final int maxz = island.getMinProtectedZ() + island.getProtectionSize();
        //plugin.getLogger().info("DEBUG: protection limits are: " + minx + ", " + minz + " to " + maxx + ", " + maxz );
        final int islandSpacing = Settings.islandDistance - island.getProtectionSize();
        // Determine the safe delete area, which is the protection range plus the island spacing
        final int minxX = (island.getCenter().getBlockX() - range - islandSpacing);
        final int minzZ = (island.getCenter().getBlockZ() - range - islandSpacing);
        final int maxxX = (island.getCenter().getBlockX() + range + islandSpacing);
        final int maxzZ = (island.getCenter().getBlockZ() + range + islandSpacing);
        // plugin.getLogger().info("DEBUG: absolute max limits are: " + minxX +
        // ", " + minzZ + " to " + maxxX + ", " + maxzZ );
        // get the chunks for these locations
        //final Chunk minChunk = world.getBlockAt(minx,0,minz).getChunk();
        //final Chunk maxChunk = world.getBlockAt(maxx, 0, maxz).getChunk();

        // Store the chunks
        Set<ChunkSnapshot> chunkSnapshot = new HashSet<ChunkSnapshot>();
        Set<ChunkSnapshot> netherChunkSnapshot = new HashSet<ChunkSnapshot>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionSize() + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionSize() + 16); z += 16) {
                chunkSnapshot.add(ASkyBlock.getIslandWorld().getBlockAt(x, 0, z).getChunk().getChunkSnapshot());
                if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
                    netherChunkSnapshot.add(ASkyBlock.getNetherWorld().getBlockAt(x, 0, z).getChunk().getChunkSnapshot());
                }
            }
        }
        final Set<ChunkSnapshot> finalChunk = chunkSnapshot;
        final Set<ChunkSnapshot> finalNetherChunk = netherChunkSnapshot;
        //plugin.getLogger().info("DEBUG: Stored " + finalChunk.size() + " final chunks");
        //plugin.getLogger().info("DEBUG: Stored " + finalNetherChunk.size() + " nether chunks");
        // Go async to process data
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Deprecated
            @Override
            public void run() {
                // Store all the chunks that need regenerating
                // Overworld - always exists
                for (ChunkSnapshot snap : finalChunk) {
                    // Run through the x and z coords and determine if the chunk is within the safe area
                    if (snap.getX() * 16 >= minxX && snap.getZ() * 16 >= minzZ
                            && snap.getX() * 16 + 15 <= maxxX && snap.getZ() * 16 + 15 <= maxzZ) {
                        // Chunk is within the safe area.
                        // Okay to clear chunk using chunk regen
                        chunksToClear.add(new Pair(snap.getX(), snap.getZ())); 
                        //plugin.getLogger().info("DEBUG: Chunk " + snap.getX() + " " + snap.getZ() + " within the safe area");
                    } else {
                        //plugin.getLogger().info("DEBUG: Chunk " + snap.getX() + " " + snap.getZ() + " not within the safe area");
                        // We need to delete the blocks one by one
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z <16; z++) {
                                // Check if inside the protected area
                                if (snap.getX() * 16  + x >= minx && snap.getZ() * 16 + z >= minz
                                        && snap.getX() * 16 + x < maxx && snap.getZ() * 16 + z < maxz) {
                                    for (int y = 0; y < ASkyBlock.getIslandWorld().getMaxHeight(); y++) {                   
                                        if (y < Settings.sea_level) {
                                            // If the block is not already water, set it to water
                                            if (snap.getBlockTypeId(x, y, z) != Material.STATIONARY_WATER.getId()) {
                                                blocksToClear.put(new Location(ASkyBlock.getIslandWorld(),snap.getX() * 16 + x, y, snap.getZ() * 16 + z), Material.STATIONARY_WATER);
                                            }
                                        } else {
                                            // If the block is not already air, set it to air
                                            if (snap.getBlockTypeId(x, y, z) != Material.AIR.getId()) {
                                                blocksToClear.put(new Location(ASkyBlock.getIslandWorld(),snap.getX() * 16 + x, y, snap.getZ() * 16 + z), Material.AIR);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //plugin.getLogger().info("DEBUG: Blocks to clear = " + blocksToClear.size());
                    }
                }
                // Nether
                //plugin.getLogger().info("DEBUG: Adding nether blocks");
                if (Settings.createNether && Settings.newNether) {
                    for (ChunkSnapshot snap1 : finalNetherChunk) {
                        // Run through the x and z coords and determine if the chunk is within the safe area
                        if (!(snap1.getX() * 16 >= minxX && snap1.getZ() * 16 >= minzZ
                                && snap1.getX() * 16 + 15 <= maxxX && snap1.getZ() * 16 + 15 <= maxzZ)) {
                            // We need to delete the blocks one by one
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z <16; z++) {
                                    // Check if inside the protected area
                                    if (snap1.getX() * 16  + x >= minx && snap1.getZ() * 16 + z >= minz
                                            && snap1.getX() * 16 + x < maxx && snap1.getZ() * 16 + z < maxz) {
                                        // We cannot currently clean up the ceiling if it has to be done by blocks...
                                        for (int y = 0; y < ASkyBlock.getNetherWorld().getMaxHeight() - 16; y++) {                                        
                                            // If the block is not already air, set it to air
                                            // Only air in the nether
                                            if (snap1.getBlockTypeId(x, y, z) != Material.AIR.getId()) {
                                                blocksToClear.put(new Location(ASkyBlock.getNetherWorld(),snap1.getX() * 16 + x, y, snap1.getZ() * 16 + z), Material.AIR);
                                            }                                       
                                        }
                                    }
                                }
                            }
                            //plugin.getLogger().info("DEBUG: Blocks to clear (+nether) = " + blocksToClear.size());
                        }
                    }
                }
                // Now that we have determined what chunks and blocks need to be changed we need to enter bukkit sync space

                // ONLY do this if the settings requests it because it will cause lag and if NMS is not accelerated
                // it will cause BAD lag.
                if (Settings.cleanUpBlocks) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            //plugin.getLogger().info("DEBUG: Block clearing sync task started with " + blocksToClear.size() + " to delete");
                            Iterator<Entry<Location, Material>> it = blocksToClear.entrySet().iterator();
                            int count = 0; // Blocks
                            while (it.hasNext() && count < 500) {
                                Entry<Location, Material> entry = it.next();
                                // Clean if it's an inventory
                                if (entry.getKey().getBlock().getState() instanceof InventoryHolder) {
                                    ((InventoryHolder)entry.getKey().getBlock().getState()).getInventory().clear();
                                }
                                nms.setBlockSuperFast(entry.getKey().getBlock(), entry.getValue().getId(), (byte)0, false);
                                //entry.getKey().getBlock().setType(entry.getValue());
                                it.remove();
                                count++;
                            }
                            // If we're done, stop
                            if (blocksToClear.isEmpty()) {
                                //plugin.getLogger().info("DEBUG: Task cancelled");
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 10L, 5L);
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        //plugin.getLogger().info("DEBUG: Clearing chunks " + chunksToClear.size());
                        Iterator<Pair> it1 = chunksToClear.iterator();
                        //int count = 0; // Chunks
                        while (it1.hasNext()) {
                            Pair entry = it1.next();
                            //plugin.getLogger().info("DEBUG: regenerating chunk " + entry.getLeft() + "," + entry.getRight());
                            ASkyBlock.getIslandWorld().regenerateChunk(entry.getLeft(), entry.getRight());
                            if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
                                ASkyBlock.getNetherWorld().regenerateChunk(entry.getLeft(), entry.getRight());
                            }
                            it1.remove();
                            //count++;
                        }

                    }
                }.runTask(plugin);

            }
        });
        // Remove from grid
        //plugin.getLogger().info("DEBUG: Removing from grid");
        plugin.getGrid().deleteIsland(island.getCenter());
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return NMSAbstraction class
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private NMSAbstraction checkVersion() {
        String serverPackageName = plugin.getServer().getClass().getPackage().getName();
        String pluginPackageName = plugin.getClass().getPackage().getName();
        String version = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
        Class<?> clazz = null;
        try {
            //plugin.getLogger().info("Trying " + pluginPackageName + ".nms." + version + ".NMSHandler");
            clazz = Class.forName(pluginPackageName + ".nms." + version + ".NMSHandler");
        } catch (Exception e) {
            plugin.getLogger().warning("No NMS Handler found for " + version + ", falling back to Bukkit API.");
            if (Settings.cleanUpBlocks) {
                plugin.getLogger().severe("Island deletion will be very laggy! Recommend to not use block clean up setting!");
            }

            try {
                clazz = Class.forName(pluginPackageName + ".nms.fallback.NMSHandler");
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        //plugin.getLogger().info("DEBUG: " + serverPackageName);
        //plugin.getLogger().info("DEBUG: " + pluginPackageName);
        // Check if we have a NMSAbstraction implementing class at that location.
        if (clazz != null && NMSAbstraction.class.isAssignableFrom(clazz)) {
            try {
                return (NMSAbstraction) clazz.getConstructor().newInstance();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        } else {
            plugin.getLogger().severe("Class " + clazz.getName() + " does not implement NMSAbstraction");
        }
        return null;
    }
}