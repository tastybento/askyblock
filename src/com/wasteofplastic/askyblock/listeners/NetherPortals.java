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

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.GridManager;
import com.wasteofplastic.askyblock.Locale;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class NetherPortals implements Listener {
    private final ASkyBlock plugin;

    public NetherPortals(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * This handles non-player portal use
     * Currently disables portal use by entities
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onEntityPortal(EntityPortalEvent event) {
	Location currentLocation = event.getFrom().clone();
	String currentWorld = currentLocation.getWorld().getName();
	// Only operate if this is Island territory
	if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")
		&& !currentWorld.equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    return;
	}
	// If the nether is disabled then quit immediately
	if (!Settings.createNether) {
	    return;
	}

	if (event.getEntityType() != null) {
	    // plugin.getLogger().info("DEBUG : Entity going through portal " +
	    // event.getEntityType());
	    // plugin.getLogger().info("DEBUG: From : " + event.getFrom());
	    // plugin.getLogger().info("DEBUG: To : " + event.getTo());
	    if (!(event.getEntity() instanceof Player)) {
		// This event should never be called as a player, but just in
		// case
		// Cancel the event because entities cannot go through portals
		event.setCancelled(true);
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerPortal(PlayerPortalEvent event) {
	// If the nether is disabled then quit immediately
	if (!Settings.createNether) {
	    return;
	}
	if (event.isCancelled()) {
	    // plugin.getLogger().info("PlayerPortalEvent was cancelled! ASkyBlock NOT teleporting!");
	    return;
	}
	Location currentLocation = event.getFrom().clone();
	String currentWorld = currentLocation.getWorld().getName();
	if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")
		&& !currentWorld.equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    return;
	}
	// Check if player has permission
	if (!Settings.allowPortalUse && currentWorld.equalsIgnoreCase(Settings.worldName)) {
	    // Portal use is disallowed for visitors, but okay for ops or bypass
	    // mods
	    if (!event.getPlayer().isOp() && !VaultHelper.checkPerm(event.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		// Portals use is always allowed around the spawn
		if (!plugin.getGrid().locationIsOnIsland(event.getPlayer(), event.getPlayer().getLocation())
			&& !plugin.getGrid().isAtSpawn(event.getPlayer().getLocation())) {
		    event.getPlayer().sendMessage(ChatColor.RED + Locale.islandProtected);
		    event.setCancelled(true);
		    return;
		}
	    }
	}
	// plugin.getLogger().info(event.getCause().toString());
	// plugin.getLogger().info("Get from is " + currentLocation.toString());
	// Check that we know this player (they could have come from another
	// world)
	Location destination = plugin.getGrid().getSafeHomeLocation(event.getPlayer().getUniqueId(),1);
	if (destination == null) {
	    event.getPlayer().sendMessage(ChatColor.YELLOW + "Type /" + Settings.ISLANDCOMMAND + " to start an island.");
	    event.setCancelled(true);
	    return;
	}
	if (currentWorld.equalsIgnoreCase(Settings.worldName)) {
	    // Going to the end
	    // TODO Need safe teleport and protection around the end spawn point
	    if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
		// plugin.getLogger().info("End world exists");
		if (event.getCause().equals(TeleportCause.END_PORTAL)) {
		    // plugin.getLogger().info("PlayerPortalEvent End Portal!");
		    // event.useTravelAgent(true);
		    event.setCancelled(true);
		    Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
		    if (GridManager.isSafeLocation(end_place)) {
			event.getPlayer().teleport(end_place);
			// event.getPlayer().sendBlockChange(end_place,
			// end_place.getBlock().getType(),end_place.getBlock().getData());
			return;
		    } else {
			event.getPlayer().sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
			plugin.getGrid().homeTeleport(event.getPlayer());
			return;
		    }
		}
	    }
	    // Going to the nether
	    // event.setTo(plugin.getServer().getWorld(Settings.worldName +
	    // "_nether").getSpawnLocation());
	    UUID playerUUID = event.getPlayer().getUniqueId();
	    World world = ASkyBlock.getNetherWorld();
	    if (Settings.newNether) {
		Location netherHome = null;
		if (plugin.getPlayers().inTeam(playerUUID)) {
		    netherHome = plugin.getPlayers().getTeamIslandLocation(playerUUID).toVector().toLocation(world);
		} else {
		    netherHome = plugin.getPlayers().getIslandLocation(playerUUID).toVector().toLocation(world);
		}
		if (netherHome == null) {
		    event.setCancelled(true);
		    return;
		}
		if (!GridManager.isSafeLocation(netherHome)) {
		    netherHome = plugin.getGrid().bigScan(netherHome, playerUUID, 32);
		    //plugin.getLogger().info("DEBUG: Found netherhome at " + netherHome);
		    if (netherHome == null) {
			plugin.getLogger().info("Could not find a safe spot to port " + event.getPlayer().getName() + " to Nether island");
			event.getPlayer().sendMessage(Locale.warpserrorNotSafe);
			event.setCancelled(true);
			return;
		    }
		}
		event.getPlayer().teleport(netherHome);
		event.setCancelled(true);
		return;
		//event.setTo(netherHome);
		//event.useTravelAgent(false);
	    } else {
		// plugin.getLogger().info("DEBUG: transporting to nether spawn : "
		// + plugin.getServer().getWorld(Settings.worldName +
		// "_nether").getSpawnLocation().toString());
		event.setTo(plugin.getServer().getWorld(Settings.worldName + "_nether").getSpawnLocation());
		event.useTravelAgent(true);
	    }
	    // if (!Settings.newNether) {
	    // event.useTravelAgent(true);
	    // } else {
	    // Use the portal for now
	    
	    // }
	} else {
	    // Going to the end
	    // TODO Need safe teleport and protection around the end spawn point
	    if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
		// plugin.getLogger().info("End world exists");
		if (event.getCause().equals(TeleportCause.END_PORTAL)) {
		    // plugin.getLogger().info("PlayerPortalEvent End Portal!");
		    // event.useTravelAgent(true);
		    event.setCancelled(true);
		    Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
		    if (GridManager.isSafeLocation(end_place)) {
			event.getPlayer().teleport(end_place);
			// event.getPlayer().sendBlockChange(end_place,
			// end_place.getBlock().getType(),end_place.getBlock().getData());
			return;
		    } else {
			event.getPlayer().sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
			plugin.getGrid().homeTeleport(event.getPlayer());
			return;
		    }
		}
	    }

	    // Returning to island
	    event.setTo(destination);
	    event.useTravelAgent(false);
	}
    }

    // Nether portal spawn protection

    /**
     * Function to check proximity to nether spawn location
     * 
     * @param player
     * @return
     */
    private boolean awayFromSpawn(Player player) {
	Vector p = player.getLocation().toVector().multiply(new Vector(1, 0, 1));
	Vector spawn = player.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
	if (spawn.distanceSquared(p) < (Settings.netherSpawnRadius * Settings.netherSpawnRadius)) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * Prevents blocks from being broken
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(final BlockBreakEvent e) {
	// plugin.getLogger().info("Block break");
	if ((e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether") && !Settings.newNether)
		|| e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    // plugin.getLogger().info("Block break in acid island nether");
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(Locale.netherSpawnIsProtected);
		e.setCancelled(true);
	    }
	}

    }

    /**
     * Prevents placing of blocks
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.setCancelled(true);
	    }
	}

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		return;
	    }
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
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
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	// Check world
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    return;
	}
	// If the target is not a player return
	if (!(e.getEntity() instanceof Player)) {
	    return;
	}
	// If PVP is okay then return
	if (Settings.allowNetherPvP) {
	    return;
	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it
	// in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Projectile) {
	    Projectile arrow = (Projectile) e.getDamager();
	    // It really is an Projectile
	    if (arrow.getShooter() instanceof Player) {
		// Arrow shot by a player at another player
		if (Settings.allowNetherPvP) {
		    return;
		} else {
		    if (!Settings.allowNetherPvP) {
			// plugin.getLogger().info("Target player is in a no-PVP area!");
			((Player) arrow.getShooter()).sendMessage(Locale.targetInNoPVPArea);
			e.setCancelled(true);
			return;
		    }
		    return;
		}
	    }
	} else if (e.getDamager() instanceof Player) {
	    //plugin.getLogger().info("DEBUG: Player attack");
	    // Just a player attack
	    if (!Settings.allowNetherPvP) {
		((Player) e.getDamager()).sendMessage(Locale.targetInNoPVPArea);
		e.setCancelled(true);
		return;
	    }
	}
	return;
    }

    /**
     * Prevent the Nether spawn from being blown up
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
	// Find out what is exploding
	Entity expl = e.getEntity();
	if (expl == null) {
	    return;
	}
	// Check world
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    return;
	}
	Location spawn = e.getLocation().getWorld().getSpawnLocation();
	Location loc = e.getLocation();
	if (spawn.distance(loc) < Settings.netherSpawnRadius) {
	    e.blockList().clear();
	}
    }

    /**
     * Converts trees to gravel and glowstone
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeGrow(final StructureGrowEvent e) {
	if (!Settings.newNether) {
	    return;
	}
	// Check world
	if (!e.getLocation().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    return;
	}
	for (BlockState b : e.getBlocks()) {
	    if (b.getType() == Material.LOG || b.getType() == Material.LOG_2) {
		b.setType(Material.GRAVEL);
	    } else if (b.getType() == Material.LEAVES || b.getType() == Material.LEAVES_2) {
		b.setType(Material.GLOWSTONE);
	    }
	}
    }
}