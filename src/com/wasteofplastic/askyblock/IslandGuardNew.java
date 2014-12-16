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


import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;


/**
 * @author ben
 * Provides protection to islands - handles newer events that may not exist in older servers
 */
public class IslandGuardNew implements Listener {
    private final ASkyBlock plugin;
    private final boolean debug = false;

    public IslandGuardNew(final ASkyBlock plugin) {
	this.plugin = plugin;

    }

    /**
     * Handle interaction with armor stands V1.8
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (e.getPlayer().isOp()) {
	    return;
	}
	// This permission bypasses protection
	if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
	    return;
	}
	if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityType.ARMOR_STAND)
		&& !plugin.locationIsOnIsland(e.getPlayer(),e.getRightClicked().getLocation())) {
	    //plugin.getLogger().info("DEBUG: Armor stand clicked off island");
	    if (!Settings.allowArmorStandUse) {
		e.setCancelled(true);
		e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
	    }
	}
    }

}


