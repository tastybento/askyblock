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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Squid;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
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
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.InventorySave;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.SafeBoat;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands
 */
public class IslandGuard implements Listener {
    private final ASkyBlock plugin;
    private final boolean debug = false;
    // A set of falling players
    private static HashSet<UUID> fallingPlayers = new HashSet<UUID>();

    public IslandGuard(final ASkyBlock plugin) {
	this.plugin = plugin;

    }

    /**
     * Determines if the player is in the island world or not or
     * in the new nether if it is activated
     * @param player
     * @return
     */
    protected static boolean inWorld(Entity entity) {
	return inWorld(entity.getLocation());
    }

    /**
     * Determines if a location is in the island world or not or
     * in the new nether if it is activated
     * @param loc
     * @return
     */
    protected static boolean inWorld(Location loc) {
	if (loc.getWorld().equals(ASkyBlock.getIslandWorld())) {
	    return true;
	}
	if (Settings.createNether && Settings.newNether && loc.getWorld().equals(ASkyBlock.getNetherWorld())) {
	    return true;
	}
	return false;
    }

    /*
     * For testing only
     * @EventHandler()
     * void testEvent(ChallengeLevelCompleteEvent e) {
     * plugin.getLogger().info(e.getEventName());
     * plugin.getLogger().info("DEBUG: challenge level complete!");
     * }
     * @EventHandler()
     * void testEvent(ChallengeCompleteEvent e) {
     * plugin.getLogger().info(e.getEventName());
     * plugin.getLogger().info("DEBUG: challenge complete!");
     * }
     */
    // Vehicle damage
    @EventHandler(priority = EventPriority.LOW)
    public void onVehicleDamageEvent(VehicleDamageEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	    plugin.getLogger().info(e.getAttacker().getType().toString());
	}
	if (inWorld(e.getVehicle())) {
	    if (!(e.getAttacker() instanceof Player)) {
		return;
	    }
	    Player p = (Player) e.getAttacker();
	    // This permission bypasses protection
	    if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowSpawnBreakBlocks && plugin.getGrid().isAtSpawn(e.getVehicle().getLocation())) {
		p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
		e.setCancelled(true);
	    }
	    if (!Settings.allowBreakBlocks && !plugin.getGrid().locationIsOnIsland(p, e.getVehicle().getLocation())) {
		p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
		e.setCancelled(true);
	    }
	}
    }

    /*
     * Prevent dropping items if player dies on another island
     * This option helps reduce the down side of dying due to traps, etc.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onVistorDeath(final PlayerDeathEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getEntity())) {
	    return;
	}
	// If the player is on their island then they die and lose everything -
	// sorry :-(
	if (plugin.getGrid().playerIsOnIsland(e.getEntity())) {
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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
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
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorPickup(final PlayerPickupItemEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getPlayer())) {
	    return;
	}
	if (Settings.allowVisitorItemPickup || e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
		|| plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getItem().getLocation())) {
	    return;
	}
	e.setCancelled(true);
    }

    /*
     * Prevent item drop by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorDrop(final PlayerDropItemEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getPlayer())) {
	    return;
	}
	if (Settings.allowVisitorItemPickup || e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
		|| plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getItemDrop().getLocation())) {
	    return;
	}
	e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
	e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(final VehicleMoveEvent e) {
	if (!inWorld(e.getVehicle())) {
	    return;
	}
	Entity passenger = e.getVehicle().getPassenger();
	if (passenger == null || !(passenger instanceof Player)) {
	    return;
	}
	Player player = (Player)passenger;
	if (plugin.getGrid() == null) {
	    return;
	}
	Island islandTo = plugin.getGrid().getProtectedIslandAt(e.getTo());
	// Announcement entering
	Island islandFrom = plugin.getGrid().getProtectedIslandAt(e.getFrom());
	// Only says something if there is a change in islands
	/*
	 * Situations:
	 * islandTo == null && islandFrom != null - exit
	 * islandTo == null && islandFrom == null - nothing
	 * islandTo != null && islandFrom == null - enter
	 * islandTo != null && islandFrom != null - same PlayerIsland or teleport?
	 * islandTo == islandFrom
	 */
	// plugin.getLogger().info("islandTo = " + islandTo);
	// plugin.getLogger().info("islandFrom = " + islandFrom);
	if (islandTo != null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
	    // Lock check
	    if (islandTo.isLocked()) {
		if (!islandTo.getMembers().contains(player.getUniqueId()) && !player.isOp()
			&& !VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).lockIslandLocked);
		    // Get the closest border
		    // plugin.getLogger().info("DEBUG: minx = " +
		    // islandTo.getMinProtectedX());
		    // plugin.getLogger().info("DEBUG: minz = " +
		    // islandTo.getMinProtectedZ());
		    // plugin.getLogger().info("DEBUG: maxx = " +
		    // (islandTo.getMinProtectedX() +
		    // islandTo.getProtectionSize()));
		    // plugin.getLogger().info("DEBUG: maxz = " +
		    // (islandTo.getMinProtectedZ() +
		    // islandTo.getProtectionSize()));
		    // Distance from x
		    int xTeleport = islandTo.getMinProtectedX() - 1;
		    int distanceX = Math.abs(islandTo.getMinProtectedX() - e.getTo().getBlockX());
		    // plugin.getLogger().info("DEBUG: distance from min X = " +
		    // distanceX);
		    int distfromMaxX = Math.abs(islandTo.getMinProtectedX() + islandTo.getProtectionSize() - e.getTo().getBlockX());
		    // plugin.getLogger().info("DEBUG: distance from max X = " +
		    // distfromMaxX);
		    int xdiff = Math.min(distanceX, distfromMaxX);
		    if (distanceX > distfromMaxX) {
			xTeleport = islandTo.getMinProtectedX() + islandTo.getProtectionSize() + 1;
		    }
		    // plugin.getLogger().info("DEBUG: X teleport location = " +
		    // xTeleport);

		    int zTeleport = islandTo.getMinProtectedZ() - 1;
		    int distanceZ = Math.abs(islandTo.getMinProtectedZ() - e.getTo().getBlockZ());
		    // plugin.getLogger().info("DEBUG: distance from min Z = " +
		    // distanceZ);
		    int distfromMaxZ = Math.abs(islandTo.getMinProtectedZ() + islandTo.getProtectionSize() - e.getTo().getBlockZ());
		    // plugin.getLogger().info("DEBUG: distance from max Z = " +
		    // distfromMaxZ);
		    if (distanceZ > distfromMaxZ) {
			zTeleport = islandTo.getMinProtectedZ() + islandTo.getProtectionSize() + 1;
		    }
		    // plugin.getLogger().info("DEBUG: Z teleport location = " +
		    // zTeleport);
		    int zdiff = Math.min(distanceZ, distfromMaxZ);
		    Location diff = new Location(e.getFrom().getWorld(), xTeleport, e.getFrom().getBlockY(), zTeleport);
		    if (xdiff < zdiff) {
			diff = new Location(e.getFrom().getWorld(), xTeleport, e.getFrom().getBlockY(), e.getFrom().getZ());
		    } else if (zdiff < xdiff) {
			diff = new Location(e.getFrom().getWorld(), e.getFrom().getX(), e.getFrom().getBlockY(), zTeleport);
		    }
		    // plugin.getLogger().info("DEBUG: " + diff.toString());
		    // Set velocities

		    Vector velocity = player.getVelocity();
		    velocity.multiply(new Vector(-1D, 1D, -1D));
		    player.setVelocity(velocity);
		    Entity vehicle = e.getVehicle();
		    float pitch = player.getLocation().getPitch();
		    float yaw = (player.getLocation().getYaw()+180) % 360;
		    //plugin.getLogger().info("DEBUG: " + yaw);
		    diff = new Location(diff.getWorld(), diff.getX(), vehicle.getLocation().getY(), diff.getZ(), yaw, pitch);
		    SafeBoat.setIgnore(player.getUniqueId());
		    player.teleport(diff);
		    vehicle.teleport(diff);
		    vehicle.setPassenger(player);
		    vehicle.setVelocity(velocity);
		    SafeBoat.setIgnore(player.getUniqueId());
		    return;
		}
	    }
	}
	if (islandTo !=null && islandFrom == null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
	    // Entering
	    if (islandTo.isSpawn()) {
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockEnteringSpawn);
	    } else {
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockNowEntering.replace("[name]", plugin.getPlayers().getName(islandTo.getOwner())));
	    }
	} else if (islandTo == null && islandFrom != null && (islandFrom.getOwner() != null || islandFrom.isSpawn())) {
	    // Leaving
	    if (islandFrom.isSpawn()) {
		// Leaving
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockLeavingSpawn);
	    } else {
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockNowLeaving.replace("[name]", plugin.getPlayers().getName(islandFrom.getOwner())));
	    }
	} else if (islandTo != null && islandFrom !=null && !islandTo.equals(islandFrom)) {
	    // Adjacent islands or overlapping protections
	    if (islandFrom.isSpawn()) {
		// Leaving
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockLeavingSpawn);
	    } else if (islandFrom.getOwner() != null){
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockNowLeaving.replace("[name]", plugin.getPlayers().getName(islandFrom.getOwner())));
	    }
	    if (islandTo.isSpawn()) {
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockEnteringSpawn);
	    } else if (islandTo.getOwner() != null){
		player.sendMessage(plugin.myLocale(player.getUniqueId()).lockNowEntering.replace("[name]", plugin.getPlayers().getName(islandTo.getOwner())));
	    }    
	}	
    }


    /**
     * Adds island lock function
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent e) {
	if (e.getPlayer().isDead()) {
	    return;
	}
	if (!inWorld(e.getPlayer())) {
	    return;
	}
	if (plugin.getGrid() == null) {
	    return;
	}
	if (e.getPlayer().isInsideVehicle()) {
	    return;
	}
	Island islandTo = plugin.getGrid().getProtectedIslandAt(e.getTo());
	// Announcement entering
	Island islandFrom = plugin.getGrid().getProtectedIslandAt(e.getFrom());
	// Only says something if there is a change in islands
	/*
	 * Situations:
	 * islandTo == null && islandFrom != null - exit
	 * islandTo == null && islandFrom == null - nothing
	 * islandTo != null && islandFrom == null - enter
	 * islandTo != null && islandFrom != null - same PlayerIsland or teleport?
	 * islandTo == islandFrom
	 */
	// plugin.getLogger().info("islandTo = " + islandTo);
	// plugin.getLogger().info("islandFrom = " + islandFrom);
	if (islandTo != null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
	    // Lock check
	    if (islandTo.isLocked()) {
		if (!islandTo.getMembers().contains(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()
			&& !VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).lockIslandLocked);
		    // Get the closest border
		    // plugin.getLogger().info("DEBUG: minx = " +
		    // islandTo.getMinProtectedX());
		    // plugin.getLogger().info("DEBUG: minz = " +
		    // islandTo.getMinProtectedZ());
		    // plugin.getLogger().info("DEBUG: maxx = " +
		    // (islandTo.getMinProtectedX() +
		    // islandTo.getProtectionSize()));
		    // plugin.getLogger().info("DEBUG: maxz = " +
		    // (islandTo.getMinProtectedZ() +
		    // islandTo.getProtectionSize()));
		    // Distance from x
		    int xTeleport = islandTo.getMinProtectedX() - 1;
		    int distanceX = Math.abs(islandTo.getMinProtectedX() - e.getTo().getBlockX());
		    // plugin.getLogger().info("DEBUG: distance from min X = " +
		    // distanceX);
		    int distfromMaxX = Math.abs(islandTo.getMinProtectedX() + islandTo.getProtectionSize() - e.getTo().getBlockX());
		    // plugin.getLogger().info("DEBUG: distance from max X = " +
		    // distfromMaxX);
		    int xdiff = Math.min(distanceX, distfromMaxX);
		    if (distanceX > distfromMaxX) {
			xTeleport = islandTo.getMinProtectedX() + islandTo.getProtectionSize() + 1;
		    }
		    // plugin.getLogger().info("DEBUG: X teleport location = " +
		    // xTeleport);

		    int zTeleport = islandTo.getMinProtectedZ() - 1;
		    int distanceZ = Math.abs(islandTo.getMinProtectedZ() - e.getTo().getBlockZ());
		    // plugin.getLogger().info("DEBUG: distance from min Z = " +
		    // distanceZ);
		    int distfromMaxZ = Math.abs(islandTo.getMinProtectedZ() + islandTo.getProtectionSize() - e.getTo().getBlockZ());
		    // plugin.getLogger().info("DEBUG: distance from max Z = " +
		    // distfromMaxZ);
		    if (distanceZ > distfromMaxZ) {
			zTeleport = islandTo.getMinProtectedZ() + islandTo.getProtectionSize() + 1;
		    }
		    // plugin.getLogger().info("DEBUG: Z teleport location = " +
		    // zTeleport);
		    int zdiff = Math.min(distanceZ, distfromMaxZ);
		    Location diff = new Location(e.getFrom().getWorld(), xTeleport, e.getFrom().getBlockY(), zTeleport);
		    if (xdiff < zdiff) {
			diff = new Location(e.getFrom().getWorld(), xTeleport, e.getFrom().getBlockY(), e.getFrom().getZ());
		    } else if (zdiff < xdiff) {
			diff = new Location(e.getFrom().getWorld(), e.getFrom().getX(), e.getFrom().getBlockY(), zTeleport);
		    }
		    // plugin.getLogger().info("DEBUG: " + diff.toString());
		    // Set velocities

		    Vector velocity = e.getPlayer().getVelocity();
		    velocity.multiply(new Vector(-1.5D, 1D, -1.5D));
		    e.getPlayer().setVelocity(velocity);
		    if (e.getPlayer().isInsideVehicle()) {
			Entity vehicle = e.getPlayer().getVehicle();
			if (vehicle.getType() != EntityType.BOAT) {
			    // THis doesn't work for boats.
			    diff = new Location(diff.getWorld(), diff.getX(), vehicle.getLocation().getY(), diff.getZ());
			    e.getPlayer().teleport(diff);
			    vehicle.teleport(diff);
			    vehicle.setPassenger(e.getPlayer());
			}
			vehicle.setVelocity(velocity);
		    } else {
			e.getPlayer().teleport(diff);
		    }
		    e.setCancelled(true);
		    return;
		}
	    }
	}

	if (islandTo != null && islandFrom == null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
	    // Entering
	    if (islandTo.isLocked()) {
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).lockIslandLocked);
	    }
	    if (islandTo.isSpawn()) {
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockEnteringSpawn);
	    } else {
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowEntering.replace("[name]", plugin.getPlayers().getName(islandTo.getOwner())));
	    }
	} else if (islandTo == null && islandFrom != null && (islandFrom.getOwner() != null || islandFrom.isSpawn())) {
	    // Leaving
	    if (islandFrom.isSpawn()) {
		// Leaving
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockLeavingSpawn);
	    } else {
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowLeaving.replace("[name]", plugin.getPlayers().getName(islandFrom.getOwner())));
	    }
	} else if (islandTo != null && islandFrom != null && !islandTo.equals(islandFrom)) {
	    // Adjacent islands or overlapping protections
	    if (islandFrom.isSpawn()) {
		// Leaving
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockLeavingSpawn);
	    } else if (islandFrom.getOwner() != null) {
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowLeaving.replace("[name]", plugin.getPlayers().getName(islandFrom.getOwner())));
	    }
	    if (islandTo.isSpawn()) {
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockEnteringSpawn);
	    } else if (islandTo.getOwner() != null) {
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowEntering.replace("[name]", plugin.getPlayers().getName(islandTo.getOwner())));
	    }
	}
    }

    /*
     * Prevent typing /island if falling - hard core
     * Checked if player teleports
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerFall(final PlayerMoveEvent e) {
	if (e.getPlayer().isDead()) {
	    return;
	}
	/*
	 * too spammy
	 * if (debug) {
	 * plugin.getLogger().info(e.getEventName());
	 * }
	 */
	if (!inWorld(e.getPlayer())) {
	    return;
	}
	if (Settings.allowTeleportWhenFalling) {
	    return;
	}
	if (!e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) || e.getPlayer().isOp()) {
	    return;
	}
	// Check if air below player
	// plugin.getLogger().info("DEBUG:" +
	// Math.round(e.getPlayer().getVelocity().getY()));
	if ((Math.round(e.getPlayer().getVelocity().getY()) < 0L)
		&& e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR
		&& e.getPlayer().getLocation().getBlock().getType() == Material.AIR) {
	    // plugin.getLogger().info("DEBUG: falling");
	    setFalling(e.getPlayer().getUniqueId());
	} else {
	    // plugin.getLogger().info("DEBUG: not falling");
	    unsetFalling(e.getPlayer().getUniqueId());
	}
    }

    /**
     * Prevents teleporting when falling based on setting by stopping commands
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerCommandPreprocessEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getPlayer()) || Settings.allowTeleportWhenFalling || e.getPlayer().isOp()
		|| !e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
	    return;
	}
	// Check commands
	// plugin.getLogger().info("DEBUG: falling command: '" +
	// e.getMessage().substring(1).toLowerCase() + "'");
	if (isFalling(e.getPlayer().getUniqueId()) && Settings.fallingCommandBlockList.contains(e.getMessage().substring(1).toLowerCase())) {
	    // Sorry you are going to die
	    e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).islandcannotTeleport);
	    e.setCancelled(true);
	}
    }

    /**
     * Prevents teleporting when falling based on setting
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getFrom()) || e.getTo() == null) {
	    return;
	}
	// Check if ready
	if (plugin.getGrid() == null) {
	    return;
	}
	// Teleporting while falling check
	if (!Settings.allowTeleportWhenFalling && e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !e.getPlayer().isOp()) {
	    if (isFalling(e.getPlayer().getUniqueId())) {
		// Sorry you are going to die
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).islandcannotTeleport);
		e.setCancelled(true);
		// Check if the player is in the void and kill them just in case
		if (e.getPlayer().getLocation().getBlockY() < 0) {
		    e.getPlayer().setHealth(0D);
		    unsetFalling(e.getPlayer().getUniqueId());
		}
		return;
	    }
	}
	//plugin.getLogger().info("DEBUG: From : " + e.getFrom());
	//plugin.getLogger().info("DEBUG: To : " + e.getTo());
	// Teleporting to a locked island
	Island islandTo = plugin.getGrid().getProtectedIslandAt(e.getTo());
	// Announcement entering
	Island islandFrom = plugin.getGrid().getProtectedIslandAt(e.getFrom());
	// Only says something if there is a change in islands
	/*
	 * Teleport Situations:
	 * islandTo == null && islandFrom != null - exit
	 * islandTo == null && islandFrom == null - nothing
	 * islandTo != null && islandFrom == null - enter
	 * islandTo != null && islandFrom != null - same PlayerIsland or teleport?
	 * islandTo == islandFrom
	 */
	if (islandTo != null && islandTo.getOwner() != null) {
	    if (islandTo != islandFrom) {
		// Entering
		if (islandTo.isLocked()) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).lockIslandLocked);
		    if (!plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getTo()) && !e.getPlayer().isOp()
			    && !VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
			e.setCancelled(true);
			return;
		    }
		}
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowEntering.replace("[name]", plugin.getPlayers().getName(islandTo.getOwner())));
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAnimalSpawn(final CreatureSpawnEvent e) {
	if (debug) {
	    plugin.getLogger().info("Animal spawn event! " + e.getEventName());
	    // plugin.getLogger().info(e.getSpawnReason().toString());
	    // plugin.getLogger().info(e.getCreatureType().toString());
	}
	// If not an animal
	if (!(e.getEntity() instanceof Animals)) {
	    return;
	}
	// If grid is not loaded yet, return
	if (plugin.getGrid() == null) {
	    return;
	}
	// If there's no limit - leave it
	if (Settings.breedingLimit <= 0) {
	    return;
	}
	// We only care about spawning and breeding
	if (e.getSpawnReason() != SpawnReason.SPAWNER && e.getSpawnReason() != SpawnReason.BREEDING && e.getSpawnReason() != SpawnReason.EGG
		&& e.getSpawnReason() != SpawnReason.DISPENSE_EGG && e.getSpawnReason() != SpawnReason.SPAWNER_EGG) {
	    return;
	}
	Animals animal = (Animals) e.getEntity();
	World world = animal.getWorld();
	// If not in the right world, return
	// Only cover overworld, not nether
	if (!animal.getWorld().equals(ASkyBlock.getIslandWorld())) {
	    return;
	}
	Location islandLoc = plugin.getGrid().getClosestIsland(animal.getLocation());
	Entity snowball = islandLoc.getWorld().spawnEntity(new Location(world, islandLoc.getBlockX(), 128, islandLoc.getBlockZ()), EntityType.SNOWBALL);
	if (snowball == null)
	    return;
	int animals = 0;
	// All for the island space is checked
	List<Entity> islandEntities = snowball.getNearbyEntities(Settings.islandDistance / 2, 128, Settings.islandDistance / 2);
	// An optimization - don't bother looping unless the number of all
	// entities is greater than the breeding limit
	// plugin.getLogger().info("DEBUG: islandEntities total = "+islandEntities.size());
	if (islandEntities.size() >= Settings.breedingLimit) {
	    //plugin.getLogger().info("DEBUG: breeding limit breached " + Settings.breedingLimit);
	    // Count how many animals there are and who the most likely spawner was if it was a player
	    List<Player> culprits = new ArrayList<Player>();
	    boolean overLimit = false;
	    for (Entity entity : islandEntities) {
		//plugin.getLogger().info("DEBUG: Entity is " + entity.getType());
		if (entity instanceof Animals) {
		    // plugin.getLogger().info("DEBUG: Animal count is " +
		    // animals);
		    animals++;
		    if (animals >= Settings.breedingLimit) {
			// Delete any extra animals
			overLimit = true;
			animal.remove();
			e.setCancelled(true);
		    }
		} else if (entity instanceof Player && e.getSpawnReason() != SpawnReason.SPAWNER && e.getSpawnReason() != SpawnReason.DISPENSE_EGG) {
		    ItemStack itemInHand = ((Player) entity).getItemInHand();
		    if (itemInHand != null) {
			Material type = itemInHand.getType();
			if (type == Material.EGG || type == Material.MONSTER_EGG || type == Material.WHEAT || type == Material.CARROT_ITEM
				|| type == Material.SEEDS) {
			    culprits.add(((Player) entity));
			}
		    }
		}
		if (overLimit) {
		    if (e.getSpawnReason() != SpawnReason.SPAWNER) {
			plugin.getLogger().warning(
				"Island at " + islandLoc.getBlockX() + "," + islandLoc.getBlockZ() + " hit the island animal breeding limit of "
					+ Settings.breedingLimit);
			for (Player player : culprits) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).moblimitsError.replace("[number]", String.valueOf(Settings.breedingLimit)));
			    plugin.getLogger().warning(player.getName() + " was trying to use a " + Util.prettifyText(player.getItemInHand().getType().toString()));
			}
		    }
		}
	    }
	}
	snowball.remove();

	// plugin.getLogger().info("DEBUG: Animal count is " + animals);
    }

    /**
     * Prevents mobs spawning at spawn
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(final CreatureSpawnEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// If not in the right world, return
	if (!e.getEntity().getWorld().equals(ASkyBlock.getIslandWorld())) {
	    return;
	}
	// If not at spawn, return, or if grid is not loaded yet.
	if (plugin.getGrid() == null || !plugin.getGrid().isAtSpawn(e.getLocation())) {
	    return;
	}

	// Deal with mobs
	if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime) {
	    if (e.getSpawnReason() == SpawnReason.SPAWNER_EGG && !Settings.allowSpawnMonsterEggs) {
		e.setCancelled(true);
		return;
	    }
	    if (!Settings.allowSpawnMobSpawn) {
		// Mobs not allowed to spawn
		e.setCancelled(true);
		return;
	    }
	}

	// If animals can spawn, check if the spawning is natural, or
	// egg-induced
	if (e.getEntity() instanceof Animals) {
	    if (e.getSpawnReason() == SpawnReason.SPAWNER_EGG && !Settings.allowSpawnMonsterEggs) {
		e.setCancelled(true);
		return;
	    }
	    if (e.getSpawnReason() == SpawnReason.EGG && !Settings.allowSpawnEggs) {
		e.setCancelled(true);
	    }
	    if (!Settings.allowSpawnAnimalSpawn) {
		// Animals are not allowed to spawn
		e.setCancelled(true);
		return;
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	    plugin.getLogger().info("Entity exploding is " + e.getEntity());
	}
	// Find out what is exploding
	Entity expl = e.getEntity();
	if (expl == null) {
	    // This allows beds to explode or other null entities, but still curtail the damage
	    // Note player can still die from beds exploding in the nether.
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
	    return;
	}
	if (!inWorld(e.getEntity())) {
	    return;
	}
	// prevent at spawn
	if (plugin.getGrid().isAtSpawn(e.getLocation())) {
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
		// plugin.getLogger().info("Creeper block damage prevented");
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
		// plugin.getLogger().info("TNT block damage prevented");
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
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndermanGrief(final EntityChangeBlockEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getEntity())) {
	    return;
	}
	// prevent at spawn
	if (plugin.getGrid().isAtSpawn(e.getEntity().getLocation())) {
	    e.setCancelled(true);
	}
	if (Settings.allowEndermanGriefing)
	    return;
	if (!(e.getEntity() instanceof Enderman)) {
	    return;
	}
	// Stop the Enderman from griefing
	// plugin.getLogger().info("Enderman stopped from griefing");
	e.setCancelled(true);
    }

    /**
     * Drops the Enderman's block when he dies if he has one
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndermanDeath(final EntityDeathEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!Settings.endermanDeathDrop)
	    return;
	if (!inWorld(e.getEntity())) {
	    return;
	}
	if (!(e.getEntity() instanceof Enderman)) {
	    // plugin.getLogger().info("Not an Enderman!");
	    return;
	}
	// Get the block the enderman is holding
	Enderman ender = (Enderman) e.getEntity();
	MaterialData m = ender.getCarriedMaterial();
	if (m != null && !m.getItemType().equals(Material.AIR)) {
	    // Drop the item
	    // plugin.getLogger().info("Dropping item " + m.toString());
	    e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), m.toItemStack(1));
	}
    }

    /**
     * Prevents blocks from being broken
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (plugin.getGrid().isAtSpawn(e.getBlock().getLocation())) {
		if (!Settings.allowSpawnBreakBlocks) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    } else if (!Settings.allowBreakBlocks && !plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getBlock().getLocation())) {
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		e.setCancelled(true);
	    }
	}
    }

    /**
     * This method protects players from PVP if it is not allowed and from
     * arrows fired by other players
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	    plugin.getLogger().info(e.getDamager().toString());
	}
	// We do not care about any EnderPearl damage in this method
	if (e.getDamager() instanceof EnderPearl) {
	    return;
	}
	// Check world
	if (!inWorld(e.getEntity())) {
	    return;
	}
	// Stop TNT damage if it is disallowed
	if (!Settings.allowTNTDamage && e.getDamager().getType().equals(EntityType.PRIMED_TNT)) {
	    e.setCancelled(true);
	    return;
	}
	// Stop Creeper damager if it is disallowed
	if (!Settings.allowCreeperDamage && e.getDamager().getType().equals(EntityType.CREEPER) && !(e.getEntity() instanceof Player)) {
	    e.setCancelled(true);
	    return;
	}
	// plugin.getLogger().info(e.getEventName());
	// Ops can do anything
	if (e.getDamager() instanceof Player) {
	    Player p = (Player) e.getDamager();
	    if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    // plugin.getLogger().info("Item frame being damaged");
	    if (!Settings.allowSpawnBreakBlocks && plugin.getGrid().isAtSpawn(e.getEntity().getLocation())) {
		Player player = (Player) e.getDamager();
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
		e.setCancelled(true);
		return;
	    }
	    if (Settings.allowBreakBlocks || (Settings.allowSpawnBreakBlocks && plugin.getGrid().isAtSpawn(e.getEntity().getLocation()))) {
		return;
	    }
	    // plugin.getLogger().info("Damager is = " +
	    // e.getDamager().toString());
	    if (e.getDamager() instanceof Player) {
		if (!plugin.getGrid().locationIsOnIsland((Player) e.getDamager(), e.getEntity().getLocation())) {
		    Player player = (Player) e.getDamager();
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
	    } else if (e.getDamager() instanceof Projectile) {
		// Find out who fired the arrow
		Projectile p = (Projectile) e.getDamager();
		// plugin.getLogger().info("Shooter is " +
		// p.getShooter().toString());
		if (p.getShooter() instanceof Player) {
		    // Is the item frame on the shooter's island?
		    if (!plugin.getGrid().locationIsOnIsland((Player) p.getShooter(), e.getEntity().getLocation())) {
			Player player = (Player) p.getShooter();
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		}
	    } else if ((e.getDamager() instanceof TNTPrimed) && !Settings.allowTNTDamage) {
		e.setCancelled(true);
		return;
	    }

	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}

	// plugin.getLogger().info("Entity is " + e.getEntity().toString());
	// Check for player initiated damage
	if (e.getDamager() instanceof Player) {
	    // plugin.getLogger().info("Damager is " +
	    // ((Player)e.getDamager()).getName());
	    // If the target is not a player check if mobs or animals can be
	    // hurt
	    if (!(e.getEntity() instanceof Player)) {
		Location targetLoc = e.getEntity().getLocation();
		// Check monsters
		if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime || e.getEntity() instanceof Squid) {
		    // plugin.getLogger().info("Entity is a monster - ok to hurt");
		    // At spawn?
		    if (plugin.getGrid().isAtSpawn(targetLoc)) {
			if (!Settings.allowSpawnMobKilling) {
			    Player player = (Player) e.getDamager();
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			    e.setCancelled(true);
			    return;
			}
			return;
		    }
		    // Monster has to be on player's island.
		    if (!plugin.getGrid().locationIsOnIsland((Player) e.getDamager(), e.getEntity().getLocation())) {
			if (!Settings.allowHurtMonsters) {
			    Player player = (Player) e.getDamager();
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			    e.setCancelled(true);
			    return;
			}
		    }
		    return;
		}
		if (e.getEntity() instanceof Animals) {
		    // plugin.getLogger().info("Entity is a non-monster - check if ok to hurt");
		    // At spawn?
		    if (plugin.getGrid().isAtSpawn(e.getEntity().getLocation())) {
			if (!Settings.allowSpawnAnimalKilling) {
			    Player player = (Player) e.getDamager();
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			    e.setCancelled(true);
			    return;
			}
			return;
		    }
		    if (!Settings.allowHurtMobs) {
			// Mob has to be on damager's island
			if (!plugin.getGrid().locationIsOnIsland((Player) e.getDamager(), e.getEntity().getLocation())) {
			    Player player = (Player) e.getDamager();
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			    e.setCancelled(true);
			    return;
			}
		    }
		    return;
		}
		// Other entities

		switch (e.getEntityType()) {
		case IRON_GOLEM:
		case SNOWMAN:
		case VILLAGER:
		    if (!Settings.allowHurtMobs) {
			if (!plugin.getGrid().locationIsOnIsland((Player) e.getDamager(), e.getEntity().getLocation())) {
			    Player player = (Player) e.getDamager();
			    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			    e.setCancelled(true);
			    return;
			}
		    }
		    break;
		default:
		    break;

		}
		return;
	    } else {
		// PVP
		// If PVP is okay then return
		if (Settings.allowPvP) {
		    //plugin.getLogger().info("DEBUG: PVP allowed");
		    return;
		}
		//plugin.getLogger().info("PVP not allowed");

	    }

	}
	// Check for projectiles
	//plugin.getLogger().info("DEBUG: projectile");
	// Only damagers who are players or arrows are left
	// Handle splash potions separately.
	if (e.getDamager() instanceof Projectile) {
	    //plugin.getLogger().info("DEBUG: Projectile attack");
	    Projectile projectile = (Projectile) e.getDamager();
	    // It really is a projectile
	    if (projectile.getShooter() instanceof Player) {
		Player shooter = (Player) projectile.getShooter();
		// plugin.getLogger().info("Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    // plugin.getLogger().info("Player vs Player!");
		    // If this is self-inflicted damage, e.g., harming thrown potions then it is ok
		    if (shooter.equals((Player)e.getEntity())) {
			//plugin.getLogger().info("Self damage!");
			return;
		    }
		    // Projectile shot by a player at another player
		    if (!Settings.allowPvP) {
			// plugin.getLogger().info("Target player is in a no-PVP area!");
			shooter.sendMessage(ChatColor.RED + plugin.myLocale(shooter.getUniqueId()).targetInNoPVPArea);
			e.setCancelled(true);
			return;
		    }
		} else {
		    // Damaged entity is NOT a player, but player is the shooter
		    if (!(e.getEntity() instanceof Monster) && !(e.getEntity() instanceof Slime) && !(e.getEntity() instanceof Squid)) {
			// plugin.getLogger().info("Entity is a non-monster - check if ok to hurt");
			if (!Settings.allowHurtMobs) {
			    if (!plugin.getGrid().locationIsOnIsland((Player) projectile.getShooter(), e.getEntity().getLocation())) {
				shooter.sendMessage(ChatColor.RED + plugin.myLocale(shooter.getUniqueId()).islandProtected);
				e.setCancelled(true);
				return;
			    }
			}
			return;
		    } else {
			if (!Settings.allowHurtMonsters) {
			    if (!plugin.getGrid().locationIsOnIsland(shooter, e.getEntity().getLocation())) {
				shooter.sendMessage(ChatColor.RED + plugin.myLocale(shooter.getUniqueId()).islandProtected);
				e.setCancelled(true);
				return;
			    }
			}
		    }
		}
	    }
	} else if (e.getDamager() instanceof Player) {
	    //plugin.getLogger().info("DEBUG: Player attack");
	    // Just a player attack
	    if (!Settings.allowPvP) {
		Player player = (Player) e.getDamager();
		player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).targetInNoPVPArea);
		e.setCancelled(true);
		return;
	    }
	}
	return;
    }

    /**
     * Prevents placing of blocks
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	//plugin.getLogger().info(e.getEventName());
	if (inWorld(e.getPlayer())) {
	    //plugin.getLogger().info("DEBUG: in world");
	    // This permission bypasses protection
	    if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    //plugin.getLogger().info("DEBUG: not op or bypass");
	    if (plugin.getGrid().isAtSpawn(e.getBlock().getLocation())) {
		//plugin.getLogger().info("DEBUG: at spawn");
		if (!Settings.allowSpawnPlaceBlocks) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    } else if (!Settings.allowPlaceBlocks && !plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getBlock().getLocation())) {
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		e.setCancelled(true);
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBlockPlace(final BlockMultiPlaceEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// plugin.getLogger().info(e.getEventName());
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (plugin.getGrid().isAtSpawn(e.getBlock().getLocation())) {
		if (!Settings.allowSpawnPlaceBlocks) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    } else if (!Settings.allowPlaceBlocks && !plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getBlock().getLocation())) {
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		e.setCancelled(true);
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBlockPlace(final HangingPlaceEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// plugin.getLogger().info(e.getEventName());
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (plugin.getGrid().isAtSpawn(e.getBlock().getLocation())) {
		if (!Settings.allowSpawnPlaceBlocks) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    } else if (!Settings.allowPlaceBlocks && !plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getBlock().getLocation())) {
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		e.setCancelled(true);
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
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBedUse) {
		if (!plugin.getGrid().playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Prevents the breakage of hanging items
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	    plugin.getLogger().info(e.getRemover().toString());
	}
	if (inWorld(e.getEntity())) {
	    if ((e.getRemover() instanceof Creeper) && !Settings.allowCreeperDamage) {
		e.setCancelled(true);
		return;
	    }
	    if (e.getRemover() instanceof Player) {
		Player p = (Player) e.getRemover();
		// This permission bypasses protection
		if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
		    return;
		}
		// Check spawn
		if (!Settings.allowSpawnBreakBlocks && plugin.getGrid().isAtSpawn(e.getEntity().getLocation())) {
		    p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
		// Check home island
		if (!Settings.allowBreakBlocks && !plugin.getGrid().locationIsOnIsland(p, e.getEntity().getLocation())) {
		    p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Prevents the leash use
     * 
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
	if (inWorld(e.getEntity())) {
	    if (!Settings.allowLeashUse) {
		if (e.getPlayer() != null) {
		    Player player = e.getPlayer();
		    // This permission bypasses protection
		    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
			return;
		    }
		    if (!plugin.getGrid().locationIsOnIsland(player, e.getEntity().getLocation()) && !player.isOp()) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
			e.setCancelled(true);
		    }
		}
	    }
	}
    }

    /**
     * Prevents the leash use
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerUnleashEntityEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	// plugin.getLogger().info(e.getEventName());
	if (inWorld(e.getEntity())) {
	    if (!Settings.allowLeashUse) {
		if (e.getPlayer() != null) {
		    Player player = e.getPlayer();
		    // This permission bypasses protection
		    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
			return;
		    }
		    if (!plugin.getGrid().locationIsOnIsland(player, e.getEntity().getLocation()) && !player.isOp()) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).islandProtected);
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
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBucketUse) {
		if (e.getBlockClicked() != null) {
		    if (!plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getBlockClicked().getLocation()) && !e.getPlayer().isOp()) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		}
	    }
	    // Check if biome is Nether and then stop water placement
	    if (e.getBlockClicked() != null && e.getBlockClicked().getBiome().equals(Biome.HELL)
		    && e.getPlayer().getItemInHand().getType().equals(Material.WATER_BUCKET)) {
		e.setCancelled(true);
		e.getPlayer().getItemInHand().setType(Material.BUCKET);
		e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.FIZZ, 1F, 2F);
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).biomeSet.replace("[biome]", "Nether"));
	    }
	}
    }

    /**
     * Prevents water from being dispensed in hell biomes
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNetherDispenser(final BlockDispenseEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getBlock().getLocation()) || !e.getBlock().getBiome().equals(Biome.HELL)) {
	    return;
	}
	// plugin.getLogger().info("DEBUG: Item being dispensed is " +
	// e.getItem().getType().toString());
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
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowBucketUse) {
		if (!plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getBlockClicked().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
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
	if (inWorld(e.getPlayer())) {
	    // This permission bypasses protection
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!Settings.allowShearing) {
		if (!plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getEntity().getLocation()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Handles interaction with objects
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (debug) {
	    plugin.getLogger().info(e.getEventName());
	}
	if (!inWorld(e.getPlayer())) {
	    return;
	}
	if (e.getPlayer().isOp()) {
	    return;
	}
	if ((e.getClickedBlock() != null && plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getClickedBlock().getLocation()))) {
	    // You can do anything on your island or if you are Op
	    return;
	}
	// Player is not clicking a block, they are clicking a material so this
	// is driven by where the player is
	if (e.getClickedBlock() == null && (e.getMaterial() != null && plugin.getGrid().playerIsOnIsland(e.getPlayer()))) {
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
	if (plugin.getGrid().isAtSpawn(e.getPlayer().getLocation())) {
	    // plugin.getLogger().info("DEBUG: Player is at spawn");
	    playerAtSpawn = true;
	}

	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    // plugin.getLogger().info("DEBUG: clicked block " +
	    // e.getClickedBlock());
	    // plugin.getLogger().info("DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
	    case WOODEN_DOOR:
	    case SPRUCE_DOOR:
	    case ACACIA_DOOR:
	    case DARK_OAK_DOOR:
	    case BIRCH_DOOR:
	    case JUNGLE_DOOR:
	    case TRAP_DOOR:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnDoorUse) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowDoorUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
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
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnGateUse) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowGateUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
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
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnChestAccess) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowChestAccess) {
		    // if (!Settings.allowChestAccess || !(playerAtSpawn &&
		    // Settings.allowSpawnChestAccess)) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case SOIL:
		if (!Settings.allowCropTrample) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnBrewing) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowBrewing) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
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
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnRedStone) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowRedStone) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case ENCHANTMENT_TABLE:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnEnchanting) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowEnchanting) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnFurnaceUse) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowFurnaceUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
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
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnMusic) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowMusic) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
	    case WOOD_BUTTON:
	    case LEVER:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnLeverButtonUse) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowLeverButtonUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnCrafting) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowCrafting) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case ANVIL:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnAnvilUse) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowAnvilUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    case RAILS:
	    case POWERED_RAIL:
	    case DETECTOR_RAIL:
	    case ACTIVATOR_RAIL:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnPlaceBlocks) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			e.getPlayer().updateInventory();
			return;
		    }
		} else if (!Settings.allowPlaceBlocks) {
		    if (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.STORAGE_MINECART || e.getMaterial() == Material.HOPPER_MINECART
			    || e.getMaterial() == Material.EXPLOSIVE_MINECART || e.getMaterial() == Material.POWERED_MINECART) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.getPlayer().updateInventory();
			return;
		    }
		}
	    case BEACON:
		if (playerAtSpawn) {
		    if (!Settings.allowSpawnBeaconAccess) {
			e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
			e.setCancelled(true);
			return;
		    }
		} else if (!Settings.allowBeaconAccess) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		    return;
		}
		break;
	    default:
		break;
	    }
	}
	// Check for disallowed in-hand items
	// plugin.getLogger().info("Material = " + e.getMaterial());
	// plugin.getLogger().info("in hand = " +
	// e.getPlayer().getItemInHand().toString());
	if (e.getMaterial() != null) {
	    // This check protects against an exploit in 1.7.9 against cactus
	    // and sugar cane
	    if (e.getMaterial() == Material.WOOD_DOOR) {
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		e.setCancelled(true);
		e.getPlayer().updateInventory();
		return;
	    } else if (e.getMaterial().equals(Material.BOAT) && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
		e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		e.setCancelled(true);
		return;
	    } else if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		if (!Settings.allowEnderPearls) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.FLINT_AND_STEEL)) {
		if (!Settings.allowFire) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.MONSTER_EGG)) {
		// plugin.getLogger().info("DEBUG: allowMonsterEggs = " +
		// Settings.allowMonsterEggs);
		if (!Settings.allowMonsterEggs) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
		// Potion
		// plugin.getLogger().info("DEBUG: potion");
		try {
		    Potion p = Potion.fromItemStack(e.getItem());
		    if (!p.isSplash()) {
			// plugin.getLogger().info("DEBUG: not a splash potion");
			return;
		    } else {
			// Splash potions are allowed only if PVP is allowed
			if (!Settings.allowPvP) {
			    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
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
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCraft(CraftItemEvent event) {
	if (debug) {
	    plugin.getLogger().info(event.getEventName());
	}
	Player player = (Player) event.getWhoClicked();
	if (inWorld(player) || player.getWorld().equals(ASkyBlock.getNetherWorld())) {
	    if (event.getRecipe().getResult().getType() == Material.ENDER_CHEST) {
		if (!(player.hasPermission(Settings.PERMPREFIX + "craft.enderchest"))) {
		    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
		    event.setCancelled(true);
		}
	    }
	}
    }

    /**
     * Prevents usage of an Ender Chest
     * 
     * @param event
     */

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnderChestEvent(PlayerInteractEvent event) {
	if (debug) {
	    plugin.getLogger().info("Ender chest " + event.getEventName());
	}
	Player player = (Player) event.getPlayer();
	if (inWorld(player) || player.getWorld().equals(ASkyBlock.getNetherWorld())) {
	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
		if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
		    if (!(event.getPlayer().hasPermission(Settings.PERMPREFIX + "craft.enderchest"))) {
			player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
			event.setCancelled(true);
		    }
		}
	    }
	}
    }


    /**
     * Handles hitting minecarts
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHitEntity(PlayerInteractEntityEvent e) {
	Player p = e.getPlayer();
	if (debug) {
	    plugin.getLogger().info("Hit entity event " + e.getEventName());
	}
	if (!inWorld(p)) {
	    return;
	}
	if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
	    // You can do anything if you are Op of have the bypass
	    return;
	}
	// Check limit of animals on island
	if (!plugin.getGrid().playerIsOnIsland(e.getPlayer())) {
	    // Not on island
	    // Minecarts and other storage entities
	    // plugin.getLogger().info("DEBUG: " +
	    // e.getRightClicked().getType().toString());
	    switch (e.getRightClicked().getType()) {
	    case HORSE:
	    case ITEM_FRAME:
	    case MINECART_CHEST:
	    case MINECART_FURNACE:
	    case MINECART_HOPPER:
	    case MINECART_TNT:
		if (!Settings.allowChestAccess) {
		    e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
		    e.setCancelled(true);
		}
	    default:
		break;
	    }
	}
    }

    /**
     * Used to prevent teleporting when falling
     * 
     * @param uniqueId
     * @return true or false
     */
    public static boolean isFalling(UUID uniqueId) {
	return fallingPlayers.contains(uniqueId);
    }

    /**
     * Used to prevent teleporting when falling
     * 
     * @param uniqueId
     */
    public static void setFalling(UUID uniqueId) {
	fallingPlayers.add(uniqueId);
    }

    /**
     * Used to prevent teleporting when falling
     * 
     * @param uniqueId
     */
    public static void unsetFalling(UUID uniqueId) {
	// getLogger().info("DEBUG: unset falling");
	fallingPlayers.remove(uniqueId);
    }
}