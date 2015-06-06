package com.wasteofplastic.askyblock.listeners;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Settings;

public class BiomeChunk implements Listener {
    private final ASkyBlock plugin;
    private final Set<Material> bannedBlocks = new HashSet<Material>();

    /**
     * Ensures that any block when loaded will match the biome of the center column of the island
     * if it exists.
     * @param plugin
     */
    public BiomeChunk(ASkyBlock plugin) {
	this.plugin = plugin;
	bannedBlocks.add(Material.ICE);
	bannedBlocks.add(Material.SNOW);
	bannedBlocks.add(Material.SNOW_BLOCK);
	bannedBlocks.add(Material.WATER);
	bannedBlocks.add(Material.STATIONARY_WATER);
    }


    /**
     * Ensures that any block when loaded will match the biome of the center column of the island
     * if it exists. Does not apply to spawn.
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChunkLoad(ChunkLoadEvent e) {
	// Only affects overworld
	if (!e.getWorld().equals(ASkyBlock.getIslandWorld())) {
	    return;
	}
	Island island = plugin.getGrid().getIslandAt(e.getChunk().getX()*16, e.getChunk().getZ()*16);
	if (island != null && !island.isSpawn()) {
	    Biome biome = island.getCenter().getBlock().getBiome();
	    for (int x = 0; x< 16; x++) {
		for (int z = 0; z< 16; z++) {
		    // Set biome
		    e.getChunk().getBlock(x, 0, z).setBiome(biome);
		    // Check y down for snow etc.
		    switch (biome) {
		    case MESA:
		    case MESA_BRYCE:
		    case DESERT:
		    case JUNGLE:
		    case SAVANNA:
		    case SAVANNA_MOUNTAINS:
		    case SAVANNA_PLATEAU:
		    case SAVANNA_PLATEAU_MOUNTAINS:
		    case SWAMPLAND:
			boolean topBlockFound = false;
			for (int y = e.getWorld().getMaxHeight(); y >= Settings.sea_level; y--) {
			    Block b = e.getChunk().getBlock(x, y, z);
			    if (!b.getType().equals(Material.AIR)) {
				topBlockFound = true;
			    }
			    if (topBlockFound) {
				if (b.getType() == Material.ICE || b.getType() == Material.SNOW || b.getType() == Material.SNOW_BLOCK) {
				    b.setType(Material.AIR);
				} else {
				    // Finished with the removals once we hit non-offending blocks
				    break;
				}
			    }
			}
			break;
		    case HELL:
			topBlockFound = false;
			for (int y = e.getWorld().getMaxHeight(); y >= Settings.sea_level; y--) {
			    Block b = e.getChunk().getBlock(x, y, z);
			    if (!b.getType().equals(Material.AIR)) {
				topBlockFound = true;
			    }
			    if (topBlockFound) {
				if (b.getType() == Material.ICE || b.getType() == Material.SNOW || b.getType() == Material.SNOW_BLOCK
					|| b.getType() == Material.WATER || b.getType() == Material.STATIONARY_WATER) {
				    b.setType(Material.AIR);
				} else {
				    // Finished with the removals once we hit non-offending blocks
				    break;
				}
			    }
			}
			break;

		    default:
		    }
		}
	    }
	}
    }
}
