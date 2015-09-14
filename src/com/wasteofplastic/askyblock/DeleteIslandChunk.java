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

import org.bukkit.Chunk;
import org.bukkit.World;

/**
 * Deletes islands fast using chunk regeneration
 * 
 * @author tastybento
 * 
 */
public class DeleteIslandChunk {

    /**
     * Class dedicated to deleting islands
     * 
     * @param plugin
     * @param loc
     */
    public DeleteIslandChunk(ASkyBlock plugin, final Island island) {
	World world = island.getCenter().getWorld();
	if (world == null)
	    return;
	int range = island.getProtectionSize() / 2 * +1;
	int minx = island.getMinProtectedX();
	int minz = island.getMinProtectedZ();
	int maxx = island.getMinProtectedX() + island.getProtectionSize();
	int maxz = island.getMinProtectedZ() + island.getProtectionSize();
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
		}
	    }
	}
	// Remove from grid
	plugin.getGrid().deleteIsland(island.getCenter());
    }
}