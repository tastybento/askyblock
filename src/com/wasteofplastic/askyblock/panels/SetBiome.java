package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Settings;

public class SetBiome {
    private static final int SPEED = 10000;
    private int xDone = 0;
    private int zDone = 0;
    private boolean inProgress = false;
    
    public SetBiome(final ASkyBlock plugin, final Island island, final Biome biomeType) {
        final World world = island.getCenter().getWorld();
        // Update the settings so they can be checked later
        island.setBiome(biomeType);
        xDone = island.getMinX();
        zDone = island.getMinZ();
        new BukkitRunnable() {                
            @Override
            public void run() {
                if (inProgress) {
                    return;
                }
                inProgress = true;
                int count = 0;
                //plugin.getLogger().info("DEBUG: Restart xDone = " + xDone + " zDone = " + zDone);
                //plugin.getLogger().info("DEBUG: max x = " + (island.getMinX() + island.getIslandDistance()));
                while (xDone < (island.getMinX() + island.getIslandDistance())) {                    
                    while(zDone < (island.getMinZ() + island.getIslandDistance())) {
                        world.setBiome(xDone, zDone, biomeType);
                        //plugin.getLogger().info("DEBUG: xDone = " + xDone + " zDone = " + zDone);
                        if (count++ > SPEED) {
                            //plugin.getLogger().info("DEBUG: set " + SPEED + " blocks");
                            inProgress = false;
                            return;
                        }
                        zDone++;
                    }
                    zDone = island.getMinZ();
                    xDone++;
                }
                //plugin.getLogger().info("DEBUG: END xDone = " + xDone + " zDone = " + zDone);
                this.cancel();               
            }
        }.runTaskTimer(plugin, 0L, 20L); // Work every second 

        // Get a snapshot of the island
        
        // If the biome is dry, then we need to remove the water, ice, snow, etc.
        switch (biomeType) {
        case MESA:
        case DESERT:
        case JUNGLE:
        case SAVANNA:
        case SWAMPLAND:
        case HELL:
            // Get the chunks
            //plugin.getLogger().info("DEBUG: get the chunks");
            List<ChunkSnapshot> chunkSnapshot = new ArrayList<ChunkSnapshot>();
            for (int x = island.getMinProtectedX() /16; x <= (island.getMinProtectedX() + island.getProtectionSize() - 1)/16; x++) {
                for (int z = island.getMinProtectedZ() /16; z <= (island.getMinProtectedZ() + island.getProtectionSize() - 1)/16; z++) {
                    boolean loaded = world.getChunkAt(x, z).isLoaded();
                    chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
                    if (!loaded) {
                        world.getChunkAt(x, z).unload();
                    }
                }  
            }
            //plugin.getLogger().info("DEBUG: size of chunk ss = " + chunkSnapshot.size());
            final List<ChunkSnapshot> finalChunk = chunkSnapshot;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    //System.out.println("DEBUG: running async task");
                    HashMap<Vector,Integer> blocksToRemove = new HashMap<Vector, Integer>();
                    // Go through island space and find the offending columns
                    for (ChunkSnapshot chunk: finalChunk) {
                        for (int x = 0; x< 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                // Check if it is snow, ice or water
                                for (int yy = world.getMaxHeight()-1; yy >= Settings.seaHeight; yy--) {
                                    int type = chunk.getBlockTypeId(x, yy, z);
                                    if (type == Material.ICE.getId() || type == Material.SNOW.getId() || type == Material.SNOW_BLOCK.getId()
                                            || type == Material.WATER.getId() || type == Material.STATIONARY_WATER.getId()) {
                                        //System.out.println("DEBUG: offending block found " + Material.getMaterial(type) + " @ " + (chunk.getX()*16 + x) + " " + yy + " " + (chunk.getZ()*16 + z));
                                        blocksToRemove.put(new Vector(chunk.getX()*16 + x,yy,chunk.getZ()*16 + z), type);
                                    } else if (type != Material.AIR.getId()){
                                        // Hit a non-offending block so break and store this column of vectors
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // Now get rid of the blocks
                    if (!blocksToRemove.isEmpty()) {
                        //plugin.getLogger().info("DEBUG: There are blocks to remove "  + blocksToRemove.size());
                        final HashMap<Vector, Integer> blocks = blocksToRemove;
                        // Kick of a sync task
                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                            @Override
                            public void run() {
                                //plugin.getLogger().info("DEBUG: Running sync task");
                                for (Entry<Vector, Integer> entry: blocks.entrySet()) {
                                    if (entry.getValue() == Material.WATER.getId() || entry.getValue() == Material.STATIONARY_WATER.getId()) {
                                        if (biomeType.equals(Biome.HELL)) {
                                            // Remove water from Hell   
                                            entry.getKey().toLocation(world).getBlock().setType(Material.AIR);
                                        }
                                    } else {
                                        entry.getKey().toLocation(world).getBlock().setType(Material.AIR);
                                    }
                                }
                            }});
                    }
                }});
        default:
        }
    }

}
