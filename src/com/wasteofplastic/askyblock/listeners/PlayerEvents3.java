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

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands
 */
public class PlayerEvents3 implements Listener {
    private final ASkyBlock plugin;
    private static final boolean DEBUG = false;

    public PlayerEvents3(final ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /*
     * Prevent item pickup by visitors for 1.12+.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorPickup(final EntityPickupItemEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (e.getEntity() instanceof Player) {
            Player player = (Player)e.getEntity();
            if (!IslandGuard.inWorld(player)) {
                return;
            }
            Island island = plugin.getGrid().getIslandAt(e.getItem().getLocation());
            if ((island != null && island.getIgsFlag(SettingsFlag.VISITOR_ITEM_PICKUP)) 
                    || player.isOp() || VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")
                    || plugin.getGrid().locationIsOnIsland(player, e.getItem().getLocation())) {
                return;
            }
            e.setCancelled(true);
        }
    }
}
