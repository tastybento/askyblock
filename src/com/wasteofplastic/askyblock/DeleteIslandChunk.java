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
import org.bukkit.Location;
import org.bukkit.World;


public class DeleteIslandChunk {
    private ASkyBlock plugin;

    /**
     * Class dedicated to deleting islands
     * @param plugin
     * @param loc
     */
    public DeleteIslandChunk(ASkyBlock plugin, final Location loc) {
	if (loc == null)
	    return;
	World world = loc.getWorld();
	int range = Settings.island_protectionRange / 2 * + 1;
	this.plugin = plugin;
	int minx = (loc.getBlockX() - range);
	int minz = (loc.getBlockZ() - range);
	int maxx = (loc.getBlockX() + range);
	int maxz = (loc.getBlockZ() + range);
	//plugin.getLogger().info("DEBUG: protection limits are: " + minx + ", " +  minz
		//+ " to " + maxx + ", " + maxz );
	// get the chunks for these locations
	final Chunk minChunk = loc.getWorld().getChunkAt(new Location(world,minx,0,minz));
	final Chunk maxChunk = loc.getWorld().getChunkAt(new Location(world,maxx,0,maxz));
	
	
	// Find out what chunks are within the island protection range
	//plugin.getLogger().info("DEBUG: chunk limits are: " + (minChunk.getBlock(0, 0, 0).getLocation().getBlockX()) + ", " + (minChunk.getBlock(0, 0, 0).getLocation().getBlockZ()) 
		//+ " to " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockX()) + ", " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockZ()));
	
	for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
	    for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
		loc.getWorld().regenerateChunk(x, z);
		//plugin.getLogger().info("DEBUG: 0,0 block = " + loc.getWorld().getChunkAt(x, z).getBlock(0, 0, 0).getLocation().getBlockX() + ","
			//+ loc.getWorld().getChunkAt(x, z).getBlock(0, 0, 0).getLocation().getBlockZ());
		//plugin.getLogger().info("DEBUG: 15,15 block = " + loc.getWorld().getChunkAt(x, z).getBlock(15, 0, 15).getLocation().getBlockX() + ","
			//+ loc.getWorld().getChunkAt(x, z).getBlock(15, 0, 15).getLocation().getBlockZ());
		//plugin.getLogger().info("DEBUG: regen chunk " + x + "," + z);
	    }  
	}
	plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

	    @Override
	    public void run() {
		for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
		    for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
			loc.getWorld().refreshChunk(x, z);
		    }
		}
	    }});
    }
}