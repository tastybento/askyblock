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
import org.bukkit.event.player.PlayerPickupItemEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands
 */
public class PlayerEvents2 implements Listener {
    private final ASkyBlock plugin;
    private static final boolean DEBUG = false;

    public PlayerEvents2(final ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /*
     * Prevent item pickup by visitors for servers before 1.12.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorPickup(final PlayerPickupItemEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!IslandGuard.inWorld(e.getPlayer())) {
            return;
        }
        Island island = plugin.getGrid().getIslandAt(e.getItem().getLocation());
        if ((island != null && island.getIgsFlag(SettingsFlag.VISITOR_ITEM_PICKUP)) 
                || e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getItem().getLocation())) {
            return;
        }
        e.setCancelled(true);
    }
}
