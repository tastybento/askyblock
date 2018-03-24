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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

public class WorldLoader implements Listener {
    private ASkyBlock plugin;
    private boolean worldLoaded = false;
    private static final boolean DEBUG = false;

    /**
     * Class to force world loading before plugins.
     * @param plugin - ASkyBlock plugin object
     */
    public WorldLoader(ASkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(final ChunkLoadEvent event) {
        if (worldLoaded) {
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + event.getEventName() + " : " + event.getWorld().getName());
        if (event.getWorld().getName().equals(Settings.worldName) || event.getWorld().getName().equals(Settings.worldName + "_nether")) {
            return;
        }
        // Load the world
        worldLoaded = true;
        ASkyBlock.getIslandWorld();
    }
}
