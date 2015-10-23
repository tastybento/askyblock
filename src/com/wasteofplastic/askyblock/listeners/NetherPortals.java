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
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.GridManager;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.Flags;
import com.wasteofplastic.askyblock.SafeSpotTeleport;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.commands.IslandCmd;
import com.wasteofplastic.askyblock.schematics.Schematic;
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
	//plugin.getLogger().info("DEBUG: nether portal entity " + event.getFrom().getBlock().getType());
	// If the nether is disabled then quit immediately
	if (!Settings.createNether) {
	    return;
	}
	if (event.getEntity() == null) {
	    return;
	}
	if (event.getFrom() != null && event.getFrom().getBlock().getType().equals(Material.ENDER_PORTAL)) {
	    event.setCancelled(true);
	    // Same action for all worlds except the end itself
	    if (!event.getFrom().getWorld().getEnvironment().equals(Environment.THE_END)) {
		if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
		    // The end exists
		    Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
		    boolean result = event.getEntity().teleport(end_place);
		    //plugin.getLogger().info("DEBUG: Result " + result + " teleported " + event.getEntityType() + " to " + end_place);
		    return;
		}
	    }
	    return;
	}
	Location currentLocation = event.getFrom().clone();
	String currentWorld = currentLocation.getWorld().getName();
	// Only operate if this is Island territory
	if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")) {
	    return;
	}
	// No entities may pass with the old nether
	if (!Settings.newNether) {
	    event.setCancelled(true);
	    return;
	}
	// New nether
	// Entities can pass only if there are adjoining portals
	Location dest = event.getFrom().toVector().toLocation(ASkyBlock.getIslandWorld());
	if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
	    dest = event.getFrom().toVector().toLocation(ASkyBlock.getNetherWorld());
	}
	// Vehicles
	if (event.getEntity() instanceof Vehicle) {
	    Vehicle vehicle = (Vehicle)event.getEntity();   
	    vehicle.eject();
	}
	new SafeSpotTeleport(plugin, event.getEntity(), dest);
	event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
	//plugin.getLogger().info("Player portal event - reason =" + event.getCause());
	UUID playerUUID = event.getPlayer().getUniqueId();
	// If the nether is disabled then quit immediately
	if (!Settings.createNether) {
	    return;
	}
	Location currentLocation = event.getFrom().clone();
	String currentWorld = currentLocation.getWorld().getName();
	if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")
		&& !currentWorld.equalsIgnoreCase(Settings.worldName + "_the_end")) {
	    return;
	}
	// Check if player has permission
	Island island = plugin.getGrid().getIslandAt(currentLocation);
	if ((island == null && !Settings.allowPortalUse) || (island != null && !island.getIgsFlag(Flags.allowPortalUse))) {
	    // Portal use is disallowed for visitors, but okay for ops or bypass
	    // mods
	    if (!event.getPlayer().isOp() && !VaultHelper.checkPerm(event.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
		// Portals use is always allowed around the spawn
		if (!plugin.getGrid().locationIsOnIsland(event.getPlayer(), event.getPlayer().getLocation())
			&& !plugin.getGrid().isAtSpawn(event.getPlayer().getLocation())) {
		    event.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(event.getPlayer().getUniqueId()).islandProtected);
		    event.setCancelled(true);
		    return;
		}
	    }
	}
	// Determine what portal it is
	switch (event.getCause()) {
	case END_PORTAL:
	    // Same action for all worlds except the end itself
	    if (!event.getFrom().getWorld().getEnvironment().equals(Environment.THE_END)) {
		if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
		    // The end exists
		    event.setCancelled(true);
		    Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
		    if (GridManager.isSafeLocation(end_place)) {
			event.getPlayer().teleport(end_place);
			// event.getPlayer().sendBlockChange(end_place,
			// end_place.getBlock().getType(),end_place.getBlock().getData());
			return;
		    } else {
			event.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(event.getPlayer().getUniqueId()).warpserrorNotSafe);
			plugin.getGrid().homeTeleport(event.getPlayer());
			return;
		    }
		}
	    } else {
		event.setCancelled(true);
		plugin.getGrid().homeTeleport(event.getPlayer());
	    }
	    break;
	case NETHER_PORTAL:
	    // Get the home world of this player
	    World homeWorld = ASkyBlock.getIslandWorld();
	    Location home = plugin.getPlayers().getHomeLocation(event.getPlayer().getUniqueId());
	    if (home != null) {
		homeWorld = home.getWorld();
	    }
	    if (!Settings.newNether) {
		// Legacy action
		if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
		    // Going to Nether
		    if (homeWorld.getEnvironment().equals(Environment.NORMAL)) {
			// Home world is over world
			event.setTo(ASkyBlock.getNetherWorld().getSpawnLocation());
			event.useTravelAgent(true); 
		    } else {
			// Home world is nether - going home
			event.useTravelAgent(false);
			Location dest = plugin.getGrid().getSafeHomeLocation(playerUUID,1);
			if (dest != null) {
			    event.setTo(dest);
			} else {
			    event.setCancelled(true);
			    new SafeSpotTeleport(plugin, event.getPlayer(), plugin.getPlayers().getIslandLocation(playerUUID), 1);
			}		
		    }
		} else {
		    // Going to Over world
		    if (homeWorld.getEnvironment().equals(Environment.NORMAL)) {
			// Home world is over world
			event.useTravelAgent(false);
			Location dest = plugin.getGrid().getSafeHomeLocation(playerUUID,1);
			if (dest != null) {
			    event.setTo(dest);
			} else {
			    event.setCancelled(true);
			    new SafeSpotTeleport(plugin, event.getPlayer(), plugin.getPlayers().getIslandLocation(playerUUID), 1);
			}
		    } else {
			// Home world is nether 
			event.setTo(ASkyBlock.getIslandWorld().getSpawnLocation());
			event.useTravelAgent(true); 
		    }
		}
	    } else {
		// New Nether
		// Get location of the island where the player is at
		if (island == null) {
		    event.setCancelled(true);
		    return;
		}
		// Can go both ways now
		//Location dest = island.getCenter().toVector().toLocation(ASkyBlock.getIslandWorld());
		Location dest = event.getFrom().toVector().toLocation(ASkyBlock.getIslandWorld());
		if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
		    // Going to Nether
		    dest = event.getFrom().toVector().toLocation(ASkyBlock.getNetherWorld());
		    // Check that there is a nether island there. Due to legacy reasons it may not exist
		    if (island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld()).getBlock().getType() != Material.BEDROCK) {
			// Check to see if there is anything there
			if (plugin.getGrid().bigScan(dest, 20) == null) {
			    plugin.getLogger().warning("Creating nether island for " + event.getPlayer().getName() + " using default nether schematic");
			    Schematic nether = IslandCmd.getSchematics().get("nether");
			    if (nether != null) {
				plugin.getIslandCmd().pasteSchematic(nether, island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld()), event.getPlayer());
			    } else {
				plugin.getLogger().severe("Cannot telelport player to nether because there is no nether schematic");
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(event.getPlayer().getUniqueId()).warpserrorNotSafe);
				return;
			    }
			}
		    }
		    //plugin.getLogger().info("DEBUG: Teleporting to " + event.getFrom().toVector().toLocation(ASkyBlock.getNetherWorld()));
		}
		event.setCancelled(true);
		// Teleport using the new safeSpot teleport
		new SafeSpotTeleport(plugin, event.getPlayer(), dest);
	    }
	    break;
	default:
	    break;
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
		e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).netherSpawnIsProtected);
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
	if (!Settings.newNether) {
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
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (!Settings.newNether) {
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
    }

    /**
     * Prevent the Nether spawn from being blown up
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
	if (Settings.newNether) {
	    // Not used in the new nether
	    return;
	}
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
	if (!Settings.newNether || !Settings.netherTrees) {
	    return;
	}
	// Check world
	if (!e.getLocation().getWorld().equals(ASkyBlock.getNetherWorld())) {
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