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

package com.wasteofplastic.askyblock.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * Cleans superflat chunks if they exist
 * Used to recover from a failed generator
 * 
 * @author tastybento
 */
public class CleanSuperFlat implements Listener {
    private static final boolean DEBUG = false;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (ASkyBlock.getIslandWorld() == null || e.getWorld() != ASkyBlock.getIslandWorld()) {
            if (DEBUG)
                Bukkit.getLogger().info("DEBUG: not right world");
            return;
        }
        if (e.getChunk().getBlock(0, 0, 0).getType().equals(Material.BEDROCK)) {
            e.getWorld().regenerateChunk(e.getChunk().getX(), e.getChunk().getZ());
            Bukkit.getLogger().warning("Regenerating superflat chunk at " + (e.getChunk().getX() * 16) + "," + (e.getChunk().getZ() * 16));
        }
    }


}