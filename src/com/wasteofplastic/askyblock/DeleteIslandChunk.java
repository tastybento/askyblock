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
    private Set<Pair<Integer, Integer>> chunksToClear = new HashSet<>();
    //private Map<Location, Material> blocksToClear = new HashMap<>();
    private NMSAbstraction nms = null;

    private int nearest16(int x, boolean countUp) {
        while (x % 16 != 0) {
            if (countUp) {
                x++;
            } else {
                x--;
            }
        }
        if (!countUp) {
            return (int) Math.floor((double)(x-1) / 16);
        }
        return (int) Math.floor((double)x / 16);
    }

    public DeleteIslandChunk(final ASkyBlock plugin, final Island island) {
        if (island.getCenter().getWorld() == null)
            return;
        if (Settings.deleteProtectedOnly) {
            gatherProtected(island);
        } else {
            gatherWholeIsland(island);
        }

        // Remove from grid
        plugin.getGrid().deleteIsland(island.getCenter());
        // Clear up any chunks
        if (!Settings.useOwnGenerator && !chunksToClear.isEmpty()) {
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

    private void gatherProtected(Island island) {
        // Get the min and max whole chunks
        Pair<Integer, Integer> minWholeChunk = new Pair<>(nearest16(island.getMinProtectedX(), true), nearest16(island.getMinProtectedZ(), true) );
        Pair<Integer, Integer> maxWholeChunk = new Pair<>(nearest16(island.getMinProtectedX() + Settings.islandProtectionRange, false), nearest16(island.getMinProtectedZ() + Settings.islandProtectionRange, false));
        // Get the chunks of the whole island
        Pair<Integer, Integer> minChunk = new Pair<>((int) Math.floor((double)island.getMinProtectedX() / 16), (int) Math.floor((double)island.getMinProtectedZ() / 16));
        Pair<Integer, Integer> maxChunk = new Pair<>((int) Math.floor((double)(island.getMinProtectedX() + Settings.islandProtectionRange) / 16),
                (int) Math.floor((double)(island.getMinProtectedZ() + Settings.islandProtectionRange)  / 16));
        regenerate(island.getCenter().getWorld(), minWholeChunk, maxWholeChunk, minChunk, maxChunk);

    }

    private void gatherWholeIsland(Island island) {
        // Get the min and max whole chunks
        Pair<Integer, Integer> minWholeChunk = new Pair<>(nearest16(island.getMinX(), true), nearest16(island.getMinZ(), true) );
        Pair<Integer, Integer> maxWholeChunk = new Pair<>(nearest16(island.getMinX() + Settings.islandDistance, false), nearest16(island.getMinZ() + Settings.islandDistance, false));
        // Get the chunks of the whole island
        Pair<Integer, Integer> minChunk = new Pair<>((int) Math.floor((double)island.getMinX() / 16), (int) Math.floor((double)island.getMinZ() / 16));
        Pair<Integer, Integer> maxChunk = new Pair<>((int) Math.floor((double)(island.getMinX() + Settings.islandDistance) / 16),
                (int) Math.floor((double)(island.getMinZ() + Settings.islandDistance)  / 16));
        regenerate(island.getCenter().getWorld(), minWholeChunk, maxWholeChunk, minChunk, maxChunk);
    }

    private void regenerate(World world, Pair<Integer, Integer> minWholeChunk, Pair<Integer, Integer> maxWholeChunk,
            Pair<Integer, Integer> minChunk, Pair<Integer, Integer> maxChunk) {
        for (int i = minChunk.x; i <= maxChunk.x; i++) {
            for (int j = minChunk.z; j <= maxChunk.z; j++) {
                if (i >= minWholeChunk.x && i <= maxWholeChunk.x && j >= minWholeChunk.z && j <= maxWholeChunk.z) {
                    world.regenerateChunk(i, j);
                    if (Settings.newNether && Settings.createNether) {
                        if (world.equals(ASkyBlock.getIslandWorld())) {
                            ASkyBlock.getNetherWorld().regenerateChunk(i, j);
                        }
                        if (world.equals(ASkyBlock.getNetherWorld())) {
                            ASkyBlock.getIslandWorld().regenerateChunk(i, j);
                        }
                    }
                } else {
                    chunksToClear.add(new Pair<>(i, j));
                }
            }
        }

    }


}