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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands - handles newer events that may not
 *         exist in older servers
 */
public class IslandGuardNew implements Listener {
    private final ASkyBlock plugin;
    private final boolean debug = false;

    public IslandGuardNew(final ASkyBlock plugin) {
	this.plugin = plugin;

    }

    /**
     * Handle interaction with armor stands V1.8
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!IslandGuard.inWorld(e.getPlayer())) {
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
		&& !plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getRightClicked().getLocation())) {
	    // plugin.getLogger().info("DEBUG: Armor stand clicked off island");
	    if (!Settings.allowArmorStandUse) {
		e.setCancelled(true);
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
	    }
	}
    }

    /**
     * Handle V1.8 blocks that need special treatment
     * 
     * @param e
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
	    return;
	}
	if (!IslandGuard.inWorld(e.getPlayer())) {
	    return;
	}
	if (e.getPlayer().isOp()) {
	    return;
	}
	// This permission bypasses protection
	if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
	    return;
	}
	// Prevents tilling of coarse dirt into dirt
	ItemStack inHand = e.getPlayer().getItemInHand();
	if (inHand.getType() == Material.WOOD_HOE || inHand.getType() == Material.IRON_HOE || inHand.getType() == Material.GOLD_HOE
		|| inHand.getType() == Material.DIAMOND_HOE || inHand.getType() == Material.STONE_HOE) {
	    // plugin.getLogger().info("DEBUG: hoe in hand");
	    Block block = e.getClickedBlock();
	    // plugin.getLogger().info("DEBUG: block is " + block.getType() +
	    // ":" + block.getData());
	    // Check if coarse dirt
	    if (block.getType() == Material.DIRT && block.getData() == (byte) 1) {
		// plugin.getLogger().info("DEBUG: hitting coarse dirt!");
		e.setCancelled(true);
	    }
	}
    }

    // Armor stand events
    @EventHandler(priority = EventPriority.LOWEST)
    void placeArmorStandEvent(PlayerInteractEvent e) {
	Player p = e.getPlayer();
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!IslandGuard.inWorld(p)) {
	    return;
	}
	if (p.isOp()) {
	    // You can do anything if you are Op
	    return;
	}
	// Check if on island
	if (plugin.getGrid().playerIsOnIsland(e.getPlayer())) {
	    return;
	}
	// Check if they are holding armor stand
	ItemStack inHand = e.getPlayer().getItemInHand();
	if (inHand != null && inHand.getType().equals(Material.ARMOR_STAND)) {
	    // plugin.getLogger().info("DEBUG: stand place cancelled");
	    e.setCancelled(true);
	    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
	    e.getPlayer().updateInventory();
	}

    }

    @EventHandler(priority = EventPriority.LOW)
    public void ArmorStandDestroy(EntityDamageByEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info("IslandGuard New " + e.getEventName());
	}
	if (!(e.getEntity() instanceof LivingEntity)) {
	    return;
	}
	if (!IslandGuard.inWorld(e.getEntity())) {
	    return;
	}
	final LivingEntity livingEntity = (LivingEntity) e.getEntity();
	if (!livingEntity.getType().equals(EntityType.ARMOR_STAND)) {
	    return;
	}
	if (e.getDamager() instanceof Player) {
	    Player p = (Player) e.getDamager();
	    if (p.isOp()) {
		return;
	    }
	    // Check if on island
	    if (plugin.getGrid().playerIsOnIsland(p)) {
		return;
	    }
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
	    e.setCancelled(true);
	}
    }

}