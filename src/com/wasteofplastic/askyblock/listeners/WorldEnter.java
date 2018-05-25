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

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

public class WorldEnter implements Listener {
    private final ASkyBlock plugin;
    private final static boolean DEBUG = false;

    public WorldEnter(ASkyBlock aSkyBlock) {
        this.plugin = aSkyBlock;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG " + event.getEventName());
        if (!event.getPlayer().getWorld().equals(ASkyBlock.getIslandWorld()) &&
                !event.getPlayer().getWorld().equals(ASkyBlock.getNetherWorld())) {
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG correct world");
        Location islandLoc = plugin.getPlayers().getIslandLocation(event.getPlayer().getUniqueId());
        if (islandLoc == null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG  no island");
            // They have no island
            if (Settings.makeIslandIfNone || Settings.immediateTeleport) {
                event.getPlayer().performCommand(Settings.ISLANDCOMMAND);
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG Make island");
        } else {
            // They have an island and are going to their own world
            if (Settings.immediateTeleport && islandLoc.getWorld().equals(event.getPlayer().getWorld())) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG teleport");
                event.getPlayer().performCommand(Settings.ISLANDCOMMAND + " go");
            }
        }
    }
}
