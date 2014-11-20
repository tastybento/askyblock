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
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
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
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class NetherPortals implements Listener {
    private final ASkyBlock plugin;

    public NetherPortals(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPlayerPortal(PlayerPortalEvent event) {
	// If the nether is disabled then quit immediately
	if (!Settings.createNether) {
	    return;
	}
	if (event.isCancelled()) {
	    //plugin.getLogger().info("PlayerPortalEvent was cancelled! ASkyBlock NOT teleporting!");
	    return;
	}
	Location currentLocation = event.getFrom().clone();
	String currentWorld = currentLocation.getWorld().getName();
	if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")
		&& !currentWorld.equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    return;
	}
	//plugin.getLogger().info(event.getCause().toString());
	//plugin.getLogger().info("Get from is " + currentLocation.toString());
	// Check that we know this player (they could have come from another world)
	Location destination = plugin.getSafeHomeLocation(event.getPlayer().getUniqueId());
	if (destination == null) {
	    event.getPlayer().sendMessage(ChatColor.YELLOW + "Type /island to start an island.");
	    event.setCancelled(true);
	    return;
	}
	if (currentWorld.equalsIgnoreCase(Settings.worldName)) {
	    // Going to the end
	    // TODO Need safe teleport and protection around the end spawn point
	    if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
		//plugin.getLogger().info("End world exists");
		if (event.getCause().equals(TeleportCause.END_PORTAL)) {
		    //plugin.getLogger().info("PlayerPortalEvent End Portal!");
		    //event.useTravelAgent(true);
		    event.setCancelled(true);
		    Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
		    if (ASkyBlock.isSafeLocation(end_place)) {
			event.getPlayer().teleport(end_place);
		    	return;
		    } else {
			event.getPlayer().sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
			plugin.homeTeleport(event.getPlayer());
			return;
		    }
		}
	    }
	    // Going to the nether
	    event.setTo(plugin.getServer().getWorld(Settings.worldName + "_nether").getSpawnLocation());
	    event.useTravelAgent(true);
	} else {
	    // Returning to island
	    event.setTo(destination); 
	    event.useTravelAgent(false);
	}
    }

    // Nether portal spawn protection

    // Function to check proximity to nether spawn location
    private boolean awayFromSpawn(Player player) {
	Location spawn = player.getWorld().getSpawnLocation();
	Location loc = player.getLocation();
	if (spawn.distance(loc) < Settings.netherSpawnRadius) {
	    player.sendMessage(Locale.netherSpawnIsProtected);
	    return false;
	} else {
	    return true;
	}
    }
    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(final BlockBreakEvent e) {
	//plugin.getLogger().info("Block break");
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    //plugin.getLogger().info("Block break in acid island nether");
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.setCancelled(true);
	    }
	}

    }

    /**
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {		   
		e.setCancelled(true);
	    }
	}

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
		|| e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.setCancelled(true);
	    }
	}

    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
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
	if (Settings.allowPvP) {
	    return;
	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		// Arrow shot by a player at another player
		if (Settings.allowPvP) {
		    return;
		} else {
		    e.setCancelled(true);
		    return;
		}
	    }
	}
	return;
    }

    /**
     * Prevent the Nether spawn from being blown up
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
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

}
