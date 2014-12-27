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


import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Squid;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
import org.bukkit.util.Vector;


/**
 * @author ben
 * Provides protection to islands
 */
public class IslandGuard implements Listener {
    private final ASkyBlock plugin;
    private final boolean debug = false;

    public IslandGuard(final ASkyBlock plugin) {
	this.plugin = plugin;

    }

    // Vehicle damage
    @EventHandler(priority = EventPriority.LOW)
    void vehicleDamageEvent(VehicleDamageEvent e){
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getVehicle() instanceof Boat) {
	    // Boats can always be hit
	    return;
	}
	if (!(e.getAttacker() instanceof Player)) {
	    return;
	}

	Player p = (Player)e.getAttacker();
	// This permission bypasses protection
	if (VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
	    return;
	}
	if (!p.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (p.isOp()) {
	    // You can do anything if you are Op
	    return;
	}
	// Check if on island
	if (plugin.playerIsOnIsland(p)) {
	    return;
	}
	if (!Settings.allowBreakBlocks) {
	    e.setCancelled(true);
	    p.sendMessage(ChatColor.RED + Locale.islandProtected);
	}
    }



    /**
     * Handles coop inventory switching
     * @param e
     */
    /*
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent e) {
	Player player = e.getPlayer();
	UUID playerUUID = player.getUniqueId();
	if (!e.getFrom().getWorld().getName().equalsIgnoreCase(Settings.worldName) || !e.getTo().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// Find out if the player has any coop islands
	Set<Location> coopIslands = CoopPlay.getInstance().getCoopIslands(player);
	if (coopIslands.isEmpty()) {
	    return;
	}
	// Options are:
	// 1. Player entered a coop island
	// 2. Player left a coop island
	// 3. Player entered a coop island from another coop island (rare - more likely via teleport)
	Location from = plugin.locationIsOnIsland(coopIslands, e.getFrom());
	Location to = plugin.locationIsOnIsland(coopIslands, e.getTo());
	if (from == null && to != null) {
	    // Entering a coop island area
	    player.sendMessage(ChatColor.GREEN + "Entering coop island. Switching inventory.");
	    // Save and clear the visitor's inventory
	    // Save and clear the visitor's inventory
	    if (plugin.getPlayers().inTeam(playerUUID)) {
		InventorySave.getInstance().switchPlayerInventory(player, plugin.getPlayers().getTeamIslandLocation(playerUUID), to);
	    } else {
		InventorySave.getInstance().switchPlayerInventory(player, plugin.getPlayers().getIslandLocation(playerUUID), to);
	    }
	} else if (from != null && to == null) {
	    // Leaving a coop island area
	    e.getPlayer().sendMessage(ChatColor.GREEN + "Leaving coop island. Returning inventory.");
	    // Return the inventory to the island owners and swap in home inventory
	    if (plugin.getPlayers().inTeam(playerUUID)) {
		InventorySave.getInstance().switchPlayerInventory(player, from, plugin.getPlayers().getTeamIslandLocation(playerUUID));
	    } else {
		InventorySave.getInstance().switchPlayerInventory(player, from, plugin.getPlayers().getIslandLocation(playerUUID));
	    }
	} else if (from != null && to != null && !from.equals(to)) {
	    // Moving from one coop to another that is immediately adjacent (very unlikely)
	    e.getPlayer().sendMessage(ChatColor.GREEN + "Switching coop island. Switching inventory.");
	    InventorySave.getInstance().switchPlayerInventory(player, from, to);
	    CoopPlay.getInstance().saveAndClearInventory(e.getPlayer());
	}
	// Set the flag of whether they are on a coop island or not
	// This flag is used to clean up the inventory situation should the player teleport or die
	CoopPlay.getInstance().setOnCoopIsland(e.getPlayer().getUniqueId(), to);
    }*/

    /*
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCoopTeleport(PlayerTeleportEvent e) {
	plugin.getLogger().info("DEBUG coop teleport to " + e.getTo());
	plugin.getLogger().info("DEBUG coop teleport from " + e.getFrom());
	// If both from and to are not in the island world return
	if (!e.getFrom().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    plugin.getLogger().info("DEBUG return - not in right world");
	    return;
	}

	Player player = e.getPlayer();
	UUID playerUUID = player.getUniqueId();
	// If to world is no island world then quit all coops
	if (!e.getTo().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // Clear any coop inventories
	    CoopPlay.getInstance().returnAllInventories(player);
	    // Remove any of the target's coop invitees and grab their stuff
	    CoopPlay.getInstance().clearMyCoops(player);
	}
	// Find out if the player is entering a coop area
	Location to = plugin.locationIsOnIsland(CoopPlay.getInstance().getCoopIslands(player),e.getTo());
	// Check they were not in a coop area
	Location from = CoopPlay.getInstance().getOnCoopIsland(playerUUID);
	// If this is nothing to do with coop return quickly
	if (to == null && from == null) {
	    return;
	}
	// If this is a teleport within the same island space then return
	if (to != null && from != null && to.equals(from)) {
	    return;
	}
	//plugin.getLogger().info("DEBUG coop teleport to coop island " + to);
	//plugin.getLogger().info("DEBUG coop teleport last coop island location = " + from);
	if (to != null) {
	    //plugin.getLogger().info("DEBUG coop is not null");
	    // Entering a coop area
	    if (from == null) {
		//plugin.getLogger().info("DEBUG lastcoop is null - entering island");
		player.sendMessage(ChatColor.GREEN + "Entering coop island. Switching inventory.");
		// Save and clear the visitor's inventory
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    InventorySave.getInstance().switchPlayerInventory(player, plugin.getPlayers().getTeamIslandLocation(playerUUID), to);
		} else {
		    InventorySave.getInstance().switchPlayerInventory(player, plugin.getPlayers().getIslandLocation(playerUUID),to);
		}
		CoopPlay.getInstance().setOnCoopIsland(e.getPlayer().getUniqueId(), to);
	    } else {
		//plugin.getLogger().info("DEBUG lastcoop is not null - switched to new coop island");
		// Player has teleported from one coop area to another
		player.sendMessage(ChatColor.GREEN + "Switched to new coop island. Switching inventory.");
		InventorySave.getInstance().switchPlayerInventory(player, from, to);
		CoopPlay.getInstance().setOnCoopIsland(e.getPlayer().getUniqueId(), to);
	    }
	} else {
	    //plugin.getLogger().info("DEBUG coop is null");
	    // Check they were already in a coop area
	    if (from != null) {	
		//plugin.getLogger().info("DEBUG lastcoop is not null - leaving island");
		player.sendMessage(ChatColor.GREEN + "Leaving coop island. Switching inventory.");
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    InventorySave.getInstance().switchPlayerInventory(player, from, plugin.getPlayers().getTeamIslandLocation(playerUUID));
		} else {
		    InventorySave.getInstance().switchPlayerInventory(player, from, plugin.getPlayers().getIslandLocation(playerUUID));
		}
	    }
	    CoopPlay.getInstance().setOnCoopIsland(e.getPlayer().getUniqueId(), null);
	    // Nothing to do, they were not in a coop area and teleported to another non-coop area
	}
    }*/

    /*
     * Prevent dropping items if player dies on another island
     * This option helps reduce the down side of dying due to traps, etc.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=false)
    public void onVistorDeath(final PlayerDeathEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the player is on their island then they die and lose everything - sorry :-(
	if (plugin.playerIsOnIsland(e.getEntity())) {
	    return;
	}
	// If visitors will keep items and their level on death 
	// This will override any global settings
	if (Settings.allowVisitorKeepInvOnDeath) {
	    InventorySave.getInstance().savePlayerInventory(e.getEntity());
	    e.getDrops().clear();
	    e.setKeepLevel(true);
	    e.setDroppedExp(0);
	}
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVistorSpawn(final PlayerRespawnEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// This will override any global settings
	if (Settings.allowVisitorKeepInvOnDeath) {
	    InventorySave.getInstance().loadPlayerInventory(e.getPlayer());
	}
    }
    /*
     * Prevent item pickup by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVisitorPickup(final PlayerPickupItemEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (Settings.allowVisitorItemPickup || e.getPlayer().isOp()
		|| VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect") || plugin.locationIsOnIsland(e.getPlayer(), e.getItem().getLocation())) {
	    return;
	}
	e.setCancelled(true);
    }

    /*
     * Prevent item drop by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVisitorDrop(final PlayerDropItemEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (Settings.allowVisitorItemPickup || e.getPlayer().isOp()
		|| VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect") || plugin.locationIsOnIsland(e.getPlayer(),e.getItemDrop().getLocation())) {
	    return;
	}
	e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
	e.setCancelled(true);
    }

    /*
     * Prevent typing /island if falling - hard core
     * Checked if player teleports
     * 
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerFall(final PlayerMoveEvent e) {
	if (e.getPlayer().isDead()) {
	    return;
	}
	/* too spammy
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}*/
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (Settings.allowTeleportWhenFalling) {
	    return;
	}
	if (!e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) || e.getPlayer().isOp()) {
	    return;
	}
	// Check if air below player
	//plugin.getLogger().info("DEBUG:" + Math.round(e.getPlayer().getVelocity().getY()));
	if ((Math.round(e.getPlayer().getVelocity().getY())<0L) && e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR
		&& e.getPlayer().getLocation().getBlock().getType() == Material.AIR) {
	    //plugin.getLogger().info("DEBUG: falling");
	    plugin.setFalling(e.getPlayer().getUniqueId());
	} else {
	    //plugin.getLogger().info("DEBUG: not falling");
	    plugin.unsetFalling(e.getPlayer().getUniqueId());
	}
    }

    /**
     * Prevents teleporting when falling based on setting
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerTeleport(final PlayerTeleportEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (Settings.allowTeleportWhenFalling) {
	    return;
	}
	if (!e.getFrom().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (!e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) || e.getPlayer().isOp()) {
	    return;
	}
	if (plugin.isFalling(e.getPlayer().getUniqueId())) {
	    // Sorry you are going to die
	    e.getPlayer().sendMessage(Locale.islandcannotTeleport);
	    e.setCancelled(true);
	    // Check if the player is in the void and kill them just in case
	    if (e.getPlayer().getLocation().getBlockY() < 0) {
		e.getPlayer().setHealth(0D);
		plugin.unsetFalling(e.getPlayer().getUniqueId());
	    }
	}
    }


    /**
     * Prevents mobs spawning at spawn
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onMobSpawn(final CreatureSpawnEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (Settings.allowSpawnMobSpawn) {
	    return;
	}
	//plugin.getLogger().info("DEBUG: mob spawn");
	// prevent at spawn
	/*
	if (plugin.getSpawn().getBedrock() != null) {
	    plugin.getLogger().info("DEBUG: spawn loc exists");
	plugin.getLogger().info("DEBUG: distance sq = " + e.getLocation().distanceSquared(plugin.getSpawn().getBedrock()));
	plugin.getLogger().info("DEBUG: range sq = " + (plugin.getSpawn().getRange()*plugin.getSpawn().getRange()));
	} else {
	    plugin.getLogger().info("DEBUG: spawn loc does not exist");
	}*/
	if (plugin.getSpawn().getBedrock() != null && plugin.getSpawn().isAtSpawn(e.getLocation())) {
	    //plugin.getLogger().info("DEBUG: prevented mob spawn at spawn");
	    e.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplosion(final EntityExplodeEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// Find out what is exploding
	Entity expl = e.getEntity();
	if (expl == null) {
	    return;
	}
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// prevent at spawn
	if (plugin.getSpawn().getBedrock() != null && plugin.getSpawn().isAtSpawn(e.getLocation())) {
	    e.setCancelled(true);
	}
	// Find out what is exploding
	EntityType exploding = e.getEntityType();
	if (exploding == null) {
	    return;
	}
	switch (exploding) {
	case CREEPER:
	    if (!Settings.allowCreeperDamage) {
		//plugin.getLogger().info("Creeper block damage prevented");
		e.blockList().clear();
	    } else {
		if (!Settings.allowChestDamage) {
		    List<Block> toberemoved = new ArrayList<Block>();
		    // Save the chest blocks in a list
		    for (Block b : e.blockList()) {
			switch (b.getType()) {
			case CHEST:
			case ENDER_CHEST:
			case STORAGE_MINECART:
			case TRAPPED_CHEST:
			    toberemoved.add(b);
			    break;
			default:
			    break;
			}
		    }
		    // Now delete them
		    for (Block b : toberemoved) {
			e.blockList().remove(b);
		    }
		}
	    }
	    break;
	case PRIMED_TNT:
	case MINECART_TNT:
	    if (!Settings.allowTNTDamage) {
		//plugin.getLogger().info("TNT block damage prevented");
		e.blockList().clear();
	    } else {
		if (!Settings.allowChestDamage) {
		    List<Block> toberemoved = new ArrayList<Block>();
		    // Save the chest blocks in a list
		    for (Block b : e.blockList()) {
			switch (b.getType()) {
			case CHEST:
			case ENDER_CHEST:
			case STORAGE_MINECART:
			case TRAPPED_CHEST:
			    toberemoved.add(b);
			    break;
			default:
			    break;
			}
		    }
		    // Now delete them
		    for (Block b : toberemoved) {
			e.blockList().remove(b);
		    }
		}
	    }
	    break;
	default:
	    break;
	}
    }




    /**
     * Allows or prevents enderman griefing
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onEndermanGrief(final EntityChangeBlockEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// prevent at spawn
	if (plugin.getSpawn().getBedrock() != null && plugin.getSpawn().isAtSpawn(e.getEntity().getLocation())) {
	    e.setCancelled(true);
	}
	if (Settings.allowEndermanGriefing)
	    return;
	if (!(e.getEntity() instanceof Enderman)) {
	    return;
	}
	// Stop the Enderman from griefing
	//plugin.getLogger().info("Enderman stopped from griefing");
	e.setCancelled(true);
    }



    /**
     * Drops the Enderman's block when he dies if he has one
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onEndermanDeath(final EntityDeathEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!Settings.endermanDeathDrop)
	    return;
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (!(e.getEntity() instanceof Enderman)) {
	    //plugin.getLogger().info("Not an Enderman!");
	    return;
	}
	// Get the block the enderman is holding
	Enderman ender = (Enderman)e.getEntity();
	MaterialData m = ender.getCarriedMaterial();
	if (m != null && !m.getItemType().equals(Material.AIR)) {
	    // Drop the item
	    //plugin.getLogger().info("Dropping item " + m.toString());
	    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), m.toItemStack(1));
	}
    }


    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBreakBlocks) {
		if (!plugin.locationIsOnIsland(e.getPlayer(),e.getBlock().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// Check world
	if (!Settings.worldName.equalsIgnoreCase(e.getEntity().getWorld().getName())) {
	    return;
	}
	//plugin.getLogger().info(e.getEventName());
	// Ops can do anything
	if (e.getDamager() instanceof Player) {
	    if (((Player)e.getDamager()).isOp()) {
		return;
	    }
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm((Player)e.getDamager(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    //plugin.getLogger().info("Item frame being dmaged");
	    if (!Settings.allowBreakBlocks) {
		// Try and protect against all player instigated damage
		//plugin.getLogger().info("Damager is = " + e.getDamager().toString());
		if (e.getDamager() instanceof Player) {
		    if (!plugin.locationIsOnIsland((Player)e.getDamager(),e.getEntity().getLocation())) {
			((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		} else if (e.getDamager() instanceof Projectile) {
		    // Find out who threw the arrow
		    Projectile p = (Projectile)e.getDamager();
		    //plugin.getLogger().info("Shooter is " + p.getShooter().toString());
		    if (p.getShooter() instanceof Player) {
			((Player)p.getShooter()).sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		} 
	    }
	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}

	//plugin.getLogger().info("Entity is " + e.getEntity().toString());
	// Check for player initiated damage
	if (e.getDamager() instanceof Player) {
	    //plugin.getLogger().info("Damager is " + ((Player)e.getDamager()).getName());
	    // If the target is not a player check if mobs can be hurt
	    if (!(e.getEntity() instanceof Player)) {
		if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime || e.getEntity() instanceof Squid) {
		    //plugin.getLogger().info("Entity is a monster - ok to hurt"); 
		    // Monster has to be on player's island.
		    if (!Settings.allowHurtMonsters) {
			if (!plugin.locationIsOnIsland((Player)e.getDamager(),e.getEntity().getLocation())) {
			    ((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.islandProtected);
			    e.setCancelled(true);
			    return;
			}
		    }
		    return;
		} else {
		    //plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
		    //UUID playerUUID = e.getDamager().getUniqueId();
		    //if (playerUUID == null) {
		    //plugin.getLogger().info("player ID is null");
		    //}
		    if (!Settings.allowHurtMobs) {
			// Mob has to be on damager's island
			if (!plugin.locationIsOnIsland((Player)e.getDamager(),e.getEntity().getLocation())) {
			    ((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.islandProtected);
			    e.setCancelled(true);
			    return;
			}
		    }
		    return;
		}
	    } else {
		// PVP
		// If PVP is okay then return
		if (Settings.allowPvP) {
		    //plugin.getLogger().info("PVP allowed");
		    return;
		}
		//plugin.getLogger().info("PVP not allowed");

	    }
	}

	//plugin.getLogger().info("Player attack (or arrow)");
	// Only damagers who are players or arrows are left
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    //plugin.getLogger().info("Arrow attack");
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		Player shooter = (Player)arrow.getShooter();
		//plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    //plugin.getLogger().info("Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!Settings.allowPvP) {
			//plugin.getLogger().info("Target player is in a no-PVP area!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP area!");
			e.setCancelled(true);
			return;
		    } 
		} else {
		    if (!(e.getEntity() instanceof Monster) && !(e.getEntity() instanceof Slime) && !(e.getEntity() instanceof Squid)) {
			//plugin.getLogger().info("Entity is a non-monster - check if ok to hurt"); 
			if (!Settings.allowHurtMobs) {
			    if (!plugin.locationIsOnIsland((Player)arrow.getShooter(),e.getEntity().getLocation())) {
				shooter.sendMessage(ChatColor.RED + Locale.islandProtected);
				e.setCancelled(true);
				return;
			    }
			}
			return;
		    } else {
			if (!Settings.allowHurtMonsters) {
			    if (!plugin.locationIsOnIsland((Player)e.getDamager(),e.getEntity().getLocation())) {
				((Player)e.getDamager()).sendMessage(ChatColor.RED + Locale.islandProtected);
				e.setCancelled(true);
				return;
			    }
			}
		    }
		}
	    }
	} else if (e.getDamager() instanceof Player){
	    //plugin.getLogger().info("Player attack");
	    // Just a player attack
	    if (!Settings.allowPvP) {
		((Player)e.getDamager()).sendMessage("Target is in a no-PVP area!");
		e.setCancelled(true);
		return;
	    } 
	}
	return;
    }


    /**
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	//plugin.getLogger().info(e.getEventName());
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowPlaceBlocks) {
		if (!plugin.locationIsOnIsland(e.getPlayer(),e.getBlock().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final HangingPlaceEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	//plugin.getLogger().info(e.getEventName());
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowPlaceBlocks) {
		if (!plugin.locationIsOnIsland(e.getPlayer(),e.getBlock().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// Check world
	if (Settings.worldName.equalsIgnoreCase(e.getPlayer().getWorld().getName())) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBedUse) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }
    /**
     * Prevents the breakage of hanging items
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!Settings.allowBreakBlocks) {
		if (e.getRemover() instanceof Player) {
		    Player p = (Player)e.getRemover();
		    // This permission bypasses protection
		    if (VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
			return;
		    }
		    if (!plugin.locationIsOnIsland(p,e.getEntity().getLocation()) && !p.isOp()) {
			p.sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		}
	    }
	}
    }

    /**
     * Prevents the leash use
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerLeashEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!Settings.allowLeashUse) {
		if (e.getPlayer() != null) {
		    Player player = e.getPlayer();
		    // This permission bypasses protection
		    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
			return;
		    }
		    if (!plugin.locationIsOnIsland(player,e.getEntity().getLocation()) && !player.isOp()) {
			player.sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		}
	    }
	}
    }

    /**
     * Prevents the leash use
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerUnleashEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	//plugin.getLogger().info(e.getEventName());
	if (e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!Settings.allowLeashUse) {
		if (e.getPlayer() != null) {
		    Player player = e.getPlayer();
		    // This permission bypasses protection
		    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
			return;
		    }
		    if (!plugin.locationIsOnIsland(player,e.getEntity().getLocation()) && !player.isOp()) {
			player.sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
		    }
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBucketUse) {
		if (e.getBlockClicked() != null) {
		    if (!plugin.locationIsOnIsland(e.getPlayer(),e.getBlockClicked().getLocation()) && !e.getPlayer().isOp()) {
			e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
			e.setCancelled(true);
			return;
		    }
		}
	    }
	    // Check if biome is Nether and then stop water placement
	    if (e.getBlockClicked() != null && e.getBlockClicked().getBiome().equals(Biome.HELL) &&
		    e.getPlayer().getItemInHand().getType().equals(Material.WATER_BUCKET)) {
		e.setCancelled(true);
		e.getPlayer().getItemInHand().setType(Material.BUCKET);
		e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.FIZZ, 1F, 2F);
		e.getPlayer().sendMessage(ChatColor.RED + Locale.biomeSet.replace("[biome]", "Nether"));
	    }
	}
    }

    /**
     * Prevents water from being dispensed in hell biomes
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNetherDispenser(final BlockDispenseEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName) ||
		!e.getBlock().getBiome().equals(Biome.HELL)) {
	    return;
	}
	//plugin.getLogger().info("DEBUG: Item being dispensed is " + e.getItem().getType().toString());
	if (e.getItem().getType().equals(Material.WATER_BUCKET)) {
	    e.setCancelled(true);
	    e.getBlock().getWorld().playSound(e.getBlock().getLocation(), Sound.FIZZ, 1F, 2F);
	}
    }



    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBucketUse) {
		if (!plugin.locationIsOnIsland(e.getPlayer(),e.getBlockClicked().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowShearing) {	
		if (!plugin.locationIsOnIsland(e.getPlayer(),e.getEntity().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }



    /**
     * Handles interaction with objects
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (e.getPlayer().isOp()) {
	    return;
	}
	if ((e.getClickedBlock() != null && plugin.locationIsOnIsland(e.getPlayer(),e.getClickedBlock().getLocation()))) {
	    // You can do anything on your island or if you are Op
	    return;
	}
	// Player is not clicking a block, they are clicking a material so this is driven by where the player is
	if (e.getClickedBlock() == null && (e.getMaterial() != null && plugin.playerIsOnIsland(e.getPlayer()))) {
	    return;
	}
	// This permission bypasses protection
	if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
	    return;
	}
	// Player is off island
	// Check if player is at spawn
	// prevent at spawn
	boolean playerAtSpawn = false;
	if (plugin.getSpawn().getBedrock() != null && plugin.getSpawn().isAtSpawn(e.getPlayer().getLocation())) {
	    playerAtSpawn = true;
	}

	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    //plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
	    //plugin.getLogger().info("DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
	    case WOODEN_DOOR:
	    case SPRUCE_DOOR:
	    case ACACIA_DOOR:
	    case DARK_OAK_DOOR:
	    case BIRCH_DOOR:
	    case JUNGLE_DOOR:
	    case TRAP_DOOR:
		if (!Settings.allowDoorUse && !(playerAtSpawn && Settings.allowSpawnDoorUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case FENCE_GATE:
	    case SPRUCE_FENCE_GATE:
	    case ACACIA_FENCE_GATE:
	    case DARK_OAK_FENCE_GATE:
	    case BIRCH_FENCE_GATE:
	    case JUNGLE_FENCE_GATE:
		if (!Settings.allowGateUse && !(playerAtSpawn && Settings.allowSpawnGateUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return;  
		}
		break;
	    case CHEST:
	    case TRAPPED_CHEST:
	    case ENDER_CHEST:
	    case DISPENSER:
	    case DROPPER:
	    case HOPPER:
	    case HOPPER_MINECART:
	    case STORAGE_MINECART:
		if (!Settings.allowChestAccess && !(playerAtSpawn && Settings.allowSpawnChestAccess)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case SOIL:
		if (!Settings.allowCropTrample) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!Settings.allowBrewing && !(playerAtSpawn && Settings.allowSpawnBrewing)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case CAKE_BLOCK:
		break;
	    case DIODE:
	    case DIODE_BLOCK_OFF:
	    case DIODE_BLOCK_ON:
	    case REDSTONE_COMPARATOR_ON:
	    case REDSTONE_COMPARATOR_OFF:
		if (!Settings.allowRedStone && !(playerAtSpawn && Settings.allowSpawnRedStone)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ENCHANTMENT_TABLE:
		if (!Settings.allowEnchanting && !(playerAtSpawn && Settings.allowSpawnEnchanting)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}		
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (!Settings.allowFurnaceUse && !(playerAtSpawn && Settings.allowSpawnFurnaceUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ICE:
		break;
	    case ITEM_FRAME:
		break;
	    case JUKEBOX:
	    case NOTE_BLOCK:
		if (!Settings.allowMusic && !(playerAtSpawn && Settings.allowSpawnMusic)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
	    case WOOD_BUTTON:
	    case LEVER:
		if (!Settings.allowLeverButtonUse && !(playerAtSpawn && Settings.allowSpawnLeverButtonUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (!Settings.allowCrafting && !(playerAtSpawn && Settings.allowSpawnCrafting)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ANVIL:
		if (!Settings.allowAnvilUse && !(playerAtSpawn && Settings.allowSpawnAnvilUse)) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case RAILS:
	    case POWERED_RAIL:
	    case DETECTOR_RAIL:
	    case ACTIVATOR_RAIL:
		if (!Settings.allowPlaceBlocks) {
		    if (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.STORAGE_MINECART
			    || e.getMaterial() == Material.HOPPER_MINECART || e.getMaterial() == Material.EXPLOSIVE_MINECART
			    || e.getMaterial() == Material.POWERED_MINECART) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
			e.getPlayer().updateInventory();
			return;
		    }
		}
	    default:
		break;
	    }
	}
	// Check for disallowed in-hand items
	if (e.getMaterial() != null) {
	    if (e.getMaterial().equals(Material.BOAT) && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
		e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		e.setCancelled(true);
		return;
	    } else if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		if (!Settings.allowEnderPearls) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.FLINT_AND_STEEL)) {
		if (!Settings.allowFire) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.MONSTER_EGG)) {
		if (!Settings.allowSpawnEggs) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
		// Potion
		//plugin.getLogger().info("DEBUG: potion");
		try {
		    Potion p = Potion.fromItemStack(e.getItem());
		    if (!p.isSplash()) {
			//plugin.getLogger().info("DEBUG: not a splash potion");
			return;
		    } else {
			// Splash potions are allowed only if PVP is allowed
			if (!Settings.allowPvP) {
			    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
			    e.setCancelled(true);
			}
		    }
		} catch (Exception ex) {
		}
	    }
	    // Everything else is okay
	}
    }


    /**
     * Prevents crafting of EnderChest unless the player has permission
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCraft(CraftItemEvent event) {
	if (debug) {
	    plugin.getLogger().info(event.getEventName());
	}
	Player player = (Player) event.getWhoClicked();
	if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || 
		player.getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    if(event.getRecipe().getResult().getType() == Material.ENDER_CHEST) {
		if(!(player.hasPermission(Settings.PERMPREFIX + "craft.enderchest"))) {
		    event.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Prevents usage of an Ender Chest
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    void PlayerInteractEvent(PlayerInteractEvent event){
	if (debug) {
	    plugin.getLogger().info(event.getEventName());
	}
	Player player = (Player) event.getPlayer();
	if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName) || 
		player.getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK ){
		if (event.getClickedBlock().getType() == Material.ENDER_CHEST){
		    if(!(event.getPlayer().hasPermission(Settings.PERMPREFIX + "craft.enderchest"))) {
			event.setCancelled(true);
		    }
		}
	    }
	}
    }

    /**
     * This prevents breeding of animals off-island
     * Adds a limit to how many animals can be bred by a player
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST)
    void PlayerInteractEntityEvent(PlayerInteractEntityEvent e){
	Player p = e.getPlayer();
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!p.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (p.isOp()) {
	    // You can do anything if you are Op
	    return;
	}
	// Check limit of animals on island
	if (plugin.playerIsOnIsland(e.getPlayer())) {
	    if (Settings.breedingLimit > 0) {
		// Check if they are holding food
		ItemStack inHand = e.getPlayer().getItemInHand();
		//if (inHand != null)
		//    plugin.getLogger().info("DEBUG: in hand = " + inHand.getType().toString());
		if (inHand == null || !(inHand.getType().equals(Material.WHEAT) || inHand.getType().equals(Material.CARROT)
			|| inHand.getType().equals(Material.SEEDS))) {
		    //plugin.getLogger().info("DEBUG: no food in hand");
		    return;
		}
		// Approach # 1 - try the whole island
		// Get the animal spawn limit
		//int limit = Settings.island_protectionRange /16 * Settings.island_protectionRange / 16 * plugin.getServer().getAnimalSpawnLimit();
		//plugin.getLogger().info("DEBUG: Limit is " + Settings.breedingLimit);
		// Check if this player is at the limit of mobs
		// Spawn snowball in island
		Location islandLoc = plugin.getPlayers().getIslandLocation(p.getUniqueId());
		Entity snowball = p.getWorld().spawnEntity(new Location(p.getWorld(),islandLoc.getBlockX(),128,islandLoc.getBlockZ()), EntityType.SNOWBALL);
		if (snowball == null)
		    return;
		int animals = 0;
		for (Entity entity : snowball.getNearbyEntities(Settings.island_protectionRange/2, 128, Settings.island_protectionRange/2)) {
		    if (entity instanceof Animals) {
			animals++;
			if (animals > Settings.breedingLimit) {
			    p.sendMessage(ChatColor.RED + Locale.moblimitsError.replace("[number]",String.valueOf(Settings.breedingLimit)));
			    //plugin.getLogger().warning(p.getName() + " hit the island animal breeding limit of " + Settings.breedingLimit);
			    e.setCancelled(true);
			    snowball.remove();
			    return;
			}
		    }
		}
		snowball.remove();
		//plugin.getLogger().info("DEBUG: Animal count is " + animals);
		/* 
	    // Approach 2 - just check around player for concentrations - not accurate enough
	    int limit = 100;
	    int animals = 0;
	    for (Entity entity : p.getNearbyEntities(16, 128, 16)) {
		if (entity instanceof Animals) {
		    animals++;
		    if (animals > limit) {
			p.sendMessage(ChatColor.RED + "Island animal limit of " + limit + " reached!");
			plugin.getLogger().warning(p.getName() + " hit the island animal breeding limit of " + limit);
			e.setCancelled(true);
			return;
		    }
		}
	    }
	    // Approach 3 - check everywhere, but include all mobs
	    // Get the animal spawn limit
	    int limit = 100;
	    plugin.getLogger().info("DEBUG: Limit is " + limit);
	    // Check if this player is at the limit of mobs
	    // Spawn snowball in island
	    Location islandLoc = plugin.getPlayers().getIslandLocation(p.getUniqueId());
	    Entity snowball = p.getWorld().spawnEntity(new Location(p.getWorld(),islandLoc.getBlockX(),128,islandLoc.getBlockZ()), EntityType.SNOWBALL);
	    if (snowball == null) {
		plugin.getLogger().info("DEBUG: could not spawn snowball!");
		return;
	    }
	    int animals = snowball.getNearbyEntities(Settings.island_protectionRange/2, 128, Settings.island_protectionRange/2).size();
	    plugin.getLogger().info("DEBUG: Animal count is " + animals);
	    if (animals > limit) {
		p.sendMessage(ChatColor.RED + "Island animal limit of " + limit + " reached!");
		plugin.getLogger().warning(p.getName() + " hit the island animal breeding limit of " + limit);
		e.setCancelled(true);
		snowball.remove();
		return;
	    }
	    snowball.remove();
		 */
	    }
	} else {
	    // Not on island
	    if (!Settings.allowBreeding) {
		// Player is off island
		if (e.getRightClicked() instanceof Animals) {
		    //plugin.getLogger().info("You right clicked on an animal");
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true); 
		}
	    }
	    // Check for other entities
	    //Minecarts and other storage entities
	    switch (e.getRightClicked().getType()) {
	    case ITEM_FRAME:
	    case MINECART_CHEST:
	    case MINECART_FURNACE:
	    case MINECART_HOPPER:
	    case MINECART_TNT:
		if (!Settings.allowChestAccess) {
		    e.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    e.setCancelled(true); 
		}
	    default:
		break;
	    }
	}
    }

}


