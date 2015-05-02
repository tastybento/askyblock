/**
 * ****************************************************************************
 * This file is part of ASkyBlock.
 * <p/>
 * ASkyBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * ASkyBlock is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 */
package com.wasteofplastic.askyblock;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Deletes islands fast using chunk regeneration
 *
 * @author tastybento
 *
 */
public class DeleteIslandChunk {
    private ASkyBlock plugin;

    /**
     * Class dedicated to deleting islands
     *
     * @param plugin
     * @param loc
     */
    public DeleteIslandChunk(ASkyBlock plugin, final Location loc) {
        if (loc == null)
            return;
        World world = loc.getWorld();
        int range = Settings.island_protectionRange / 2 * +1;
        this.plugin = plugin;
        int minx = (loc.getBlockX() - range);
        int minz = (loc.getBlockZ() - range);
        int maxx = (loc.getBlockX() + range);
        int maxz = (loc.getBlockZ() + range);
        // plugin.getLogger().info("DEBUG: protection limits are: " + minx +
        // ", " + minz + " to " + maxx + ", " + maxz );
        int islandSpacing = Settings.islandDistance - Settings.island_protectionRange;
        int minxX = (loc.getBlockX() - range - islandSpacing);
        int minzZ = (loc.getBlockZ() - range - islandSpacing);
        int maxxX = (loc.getBlockX() + range + islandSpacing);
        int maxzZ = (loc.getBlockZ() + range + islandSpacing);
        // plugin.getLogger().info("DEBUG: absolute max limits are: " + minxX +
        // ", " + minzZ + " to " + maxxX + ", " + maxzZ );
        // get the chunks for these locations
        final Chunk minChunk = loc.getWorld().getChunkAt(new Location(world, minx, 0, minz));
        final Chunk maxChunk = loc.getWorld().getChunkAt(new Location(world, maxx, 0, maxz));

        // Find out what chunks are within the island protection range
        // plugin.getLogger().info("DEBUG: chunk limits are: " +
        // (minChunk.getBlock(0, 0, 0).getLocation().getBlockX()) + ", " +
        // (minChunk.getBlock(0, 0, 0).getLocation().getBlockZ())
        // + " to " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockX()) +
        // ", " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockZ()));

        for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
            for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
                boolean regen = true;
                if (loc.getWorld().getChunkAt(x, z).getBlock(0, 0, 0).getX() < minxX) {
                    // plugin.getLogger().info("DEBUG: min x coord is less than absolute min! "
                    // + minxX);
                    regen = false;
                }
                if (loc.getWorld().getChunkAt(x, z).getBlock(0, 0, 0).getZ() < minzZ) {
                    // plugin.getLogger().info("DEBUG: min z coord is less than absolute min! "
                    // + minzZ);
                    regen = false;
                }
                if (loc.getWorld().getChunkAt(x, z).getBlock(15, 0, 15).getX() > maxxX) {
                    // plugin.getLogger().info("DEBUG: max x coord is more than absolute max! "
                    // + maxxX);
                    regen = false;
                }
                if (loc.getWorld().getChunkAt(x, z).getBlock(15, 0, 15).getZ() > maxzZ) {
                    // plugin.getLogger().info("DEBUG: max z coord in chunk is more than absolute max! "
                    // + maxzZ);
                    regen = false;
                }
                if (regen) {
                    loc.getWorld().regenerateChunk(x, z);
                }
            }
        }
        // Remove from file system
    /*
	 * String checkName = loc.getBlockX() + "," + loc.getBlockZ() + ".yml";
	 * final File islandFile = new File(plugin.getDataFolder() +
	 * File.separator + "islands" + File.separator + checkName);
	 * if (islandFile.exists()) {
	 * //plugin.getLogger().info("File exists");
	 * if (!islandFile.delete()) {
	 * plugin.getLogger().severe("Could not delete island file " + checkName
	 * + "!");
	 * }
	 * }
	 */
        // Remove from grid
        plugin.getGrid().deleteIsland(loc);

	/*
	 * plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
	 * @Override
	 * public void run() {
	 * for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
	 * for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
	 * loc.getWorld().refreshChunk(x, z);
	 * }
	 * }
	 * }});
	 */
    }
}