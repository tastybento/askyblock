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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;
import com.wasteofplastic.askyblock.util.Pair;
import com.wasteofplastic.askyblock.util.Util;

//import com.wasteofplastic.askyblock.nms.NMSAbstraction;

/**
 * Deletes islands fast using chunk regeneration
 * 
 * @author tastybento
 * 
 */
public class DeleteIslandChunk {
    private Set<Pair<Integer, Integer>> chunksToClear = new HashSet<Pair<Integer, Integer>>();
    //private HashMap<Location, Material> blocksToClear = new HashMap<Location,Material>();
    private NMSAbstraction nms = null;

    public DeleteIslandChunk(final ASkyBlock plugin, final Island island) {
        final World world = island.getCenter().getWorld();
        if (world == null)
            return;
        // Determine if blocks need to be cleaned up or not
        boolean cleanUpBlocks = false;
        if (Settings.islandDistance - island.getProtectionSize() < 16) {
            cleanUpBlocks = true;
            // Never clear up more than the island size
            island.setProtectionSize(Settings.islandDistance);
        }
        int range = island.getProtectionSize() / 2 * +1;
        final int minx = island.getMinProtectedX();
        final int minz = island.getMinProtectedZ();
        final int maxx = island.getMinProtectedX() + island.getProtectionSize();
        final int maxz = island.getMinProtectedZ() + island.getProtectionSize();
        // plugin.getLogger().info("DEBUG: protection limits are: " + minx +
        // ", " + minz + " to " + maxx + ", " + maxz );
        int islandSpacing = Settings.islandDistance - island.getProtectionSize();
        int minxX = (island.getCenter().getBlockX() - range - islandSpacing);
        int minzZ = (island.getCenter().getBlockZ() - range - islandSpacing);
        int maxxX = (island.getCenter().getBlockX() + range + islandSpacing);
        int maxzZ = (island.getCenter().getBlockZ() + range + islandSpacing);
        // plugin.getLogger().info("DEBUG: absolute max limits are: " + minxX +
        // ", " + minzZ + " to " + maxxX + ", " + maxzZ );
        // get the chunks for these locations
        final Chunk minChunk = world.getBlockAt(minx,0,minz).getChunk();
        final Chunk maxChunk = world.getBlockAt(maxx, 0, maxz).getChunk();

        // Find out what chunks are within the island protection range
        // plugin.getLogger().info("DEBUG: chunk limits are: " +
        // (minChunk.getBlock(0, 0, 0).getLocation().getBlockX()) + ", " +
        // (minChunk.getBlock(0, 0, 0).getLocation().getBlockZ())
        // + " to " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockX()) +
        // ", " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockZ()));

        for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
            for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
                boolean regen = true;

                if (world.getChunkAt(x, z).getBlock(0, 0, 0).getX() < minxX) {
                    // plugin.getLogger().info("DEBUG: min x coord is less than absolute min! "
                    // + minxX);
                    regen = false;
                }
                if (world.getChunkAt(x, z).getBlock(0, 0, 0).getZ() < minzZ) {
                    // plugin.getLogger().info("DEBUG: min z coord is less than absolute min! "
                    // + minzZ);
                    regen = false;
                }
                if (world.getChunkAt(x, z).getBlock(15, 0, 15).getX() > maxxX) {
                    // plugin.getLogger().info("DEBUG: max x coord is more than absolute max! "
                    // + maxxX);
                    regen = false;
                }
                if (world.getChunkAt(x, z).getBlock(15, 0, 15).getZ() > maxzZ) {
                    // plugin.getLogger().info("DEBUG: max z coord in chunk is more than absolute max! "
                    // + maxzZ);
                    regen = false;
                }
                if (regen) {
                    world.regenerateChunk(x, z);
                    if (Settings.newNether && Settings.createNether) {
                        if (world.equals(ASkyBlock.getIslandWorld())) {
                            ASkyBlock.getNetherWorld().regenerateChunk(x, z);
                        }
                        if (world.equals(ASkyBlock.getNetherWorld())) {
                            ASkyBlock.getIslandWorld().regenerateChunk(x, z);
                        }
                    }
                } else {
                    // Add to clear up list if requested
                    if (cleanUpBlocks) {
                        chunksToClear.add(new Pair<Integer, Integer>(x,z));
                    }
                }
            }
        }
        // Remove from grid
        plugin.getGrid().deleteIsland(island.getCenter());
        // Clear up any chunks
        if (!chunksToClear.isEmpty()) {
            try {
                nms = Util.checkVersion();
            } catch (Exception ex) {
                plugin.getLogger().warning("Cannot clean up blocks because there is no NMS acceleration available");
                return;
            }
            plugin.getLogger().info("Island delete: There are " + chunksToClear.size() + " chunks that need to be cleared up.");
            plugin.getLogger().info("Clean rate is " + Settings.cleanRate + " chunks per second. Should take ~" + Math.round((float)chunksToClear.size()/Settings.cleanRate) + "s");
            new BukkitRunnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    Iterator<Pair<Integer, Integer>> it = chunksToClear.iterator();
                    int count = 0;
                    while (it.hasNext() && count++ < Settings.cleanRate) {                    
                        Pair<Integer, Integer> pair = it.next();
                        //plugin.getLogger().info("DEBUG: There are " + chunksToClear.size() + " chunks that need to be cleared up");
                        //plugin.getLogger().info("DEBUG: Deleting chunk " + pair.getLeft() + ", " + pair.getRight());                       
                        // Check if coords are in island space
                        for (int x = 0; x < 16; x ++) {
                            for (int z = 0; z < 16; z ++) {
                                int xCoord = pair.x * 16 + x;
                                int zCoord = pair.z * 16 + z;
                                if (island.inIslandSpace(xCoord, zCoord)) {                                 
                                    //plugin.getLogger().info(xCoord + "," + zCoord + " is in island space - deleting column");
                                    // Delete all the blocks here
                                    for (int y = 0; y < ASkyBlock.getIslandWorld().getMaxHeight(); y ++) {
                                        // Overworld
                                        Block block = ASkyBlock.getIslandWorld().getBlockAt(xCoord, y, zCoord);                                       
                                        Material blockType = block.getType();
                                        Material setTo = Material.AIR;
                                        // Split depending on below or above water line
                                        if (y < Settings.seaHeight) {
                                            setTo = Material.STATIONARY_WATER;
                                        }
                                        // Grab anything out of containers (do that it is
                                        // destroyed)                                  
                                        switch (blockType) {
                                        case CHEST:
                                        case TRAPPED_CHEST:                                           
                                        case FURNACE:
                                        case DISPENSER:
                                        case HOPPER:
                                            final InventoryHolder ih = ((InventoryHolder)block.getState());
                                            ih.getInventory().clear();                                            
                                            block.setType(setTo);
                                            break;
                                        case AIR:   
                                            if (setTo.equals(Material.STATIONARY_WATER)) {
                                                nms.setBlockSuperFast(block, setTo.getId(), (byte)0, false);
                                            }
                                            break;
                                        case STATIONARY_WATER:
                                            if (setTo.equals(Material.AIR)) {
                                                nms.setBlockSuperFast(block, setTo.getId(), (byte)0, false);
                                            }
                                            break;
                                        default:
                                            nms.setBlockSuperFast(block, setTo.getId(), (byte)0, false);
                                            break;
                                        }
                                        // Nether, if it exists
                                        if (Settings.newNether && Settings.createNether && y < ASkyBlock.getNetherWorld().getMaxHeight() - 8) {
                                            block = ASkyBlock.getNetherWorld().getBlockAt(xCoord, y, zCoord);                                       
                                            blockType = block.getType();
                                            if (!blockType.equals(Material.AIR)) {
                                                setTo = Material.AIR;                                            
                                                // Grab anything out of containers (do that it is
                                                // destroyed)                                  
                                                switch (blockType) {
                                                case CHEST:
                                                case TRAPPED_CHEST:                                           
                                                case FURNACE:
                                                case DISPENSER:
                                                case HOPPER:
                                                    final InventoryHolder ih = ((InventoryHolder)block.getState());
                                                    ih.getInventory().clear();                                            
                                                    block.setType(setTo);
                                                    break;
                                                 default:
                                                    nms.setBlockSuperFast(block, setTo.getId(), (byte)0, false);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }                                
                            }
                        }
                        it.remove();                        
                    } 
                    if (chunksToClear.isEmpty()){
                        plugin.getLogger().info("Finished island deletion");
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);

        }
    }

}