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
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.commands.IslandCmd;
import com.wasteofplastic.askyblock.events.IslandEnterEvent;
import com.wasteofplastic.askyblock.schematics.Schematic;
import com.wasteofplastic.askyblock.schematics.Schematic.PasteReason;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;
import com.wasteofplastic.askyblock.util.teleport.SafeTeleportBuilder;

public class NetherPortals implements Listener {
    private final ASkyBlock plugin;
    private boolean DEBUG2;
    private static final boolean DEBUG = false;

    public NetherPortals(ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * This handles non-player portal use
     * Currently disables portal use by entities
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPortal(final EntityPortalEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: nether portal entity " + event.getFrom().getBlock().getType());
        // If the nether is disabled then quit immediately
        if (!Settings.createNether || ASkyBlock.getNetherWorld() == null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Disabled nether: Settings create nether = " + Settings.createNether + " " + (ASkyBlock.getNetherWorld() == null ? "Nether world is null": "Nether world is not null"));
            return;
        }
        // Disable entity portal transfer due to dupe glitching
        event.setCancelled(true);
    } 

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortal(final PlayerPortalEvent event) {
        if (DEBUG)
            plugin.getLogger().info("Player portal event - reason =" + event.getCause());
        UUID playerUUID = event.getPlayer().getUniqueId();
        // If the nether is disabled then quit immediately
        if (!Settings.createNether || ASkyBlock.getNetherWorld() == null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Disabled nether: Settings create nether = " + Settings.createNether + " " + (ASkyBlock.getNetherWorld() == null ? "Nether world is null": "Nether world is not null"));
            return;
        }
        Location currentLocation = event.getFrom().clone();
        String currentWorld = currentLocation.getWorld().getName();
        if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")
                && !currentWorld.equalsIgnoreCase(Settings.worldName + "_the_end")) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: not island world");
            return;
        }
        // Check if player has permission
        Island island = plugin.getGrid().getIslandAt(currentLocation);
        if ((island == null && !Settings.defaultWorldSettings.get(SettingsFlag.PORTAL)) 
                || (island != null && !(island.getIgsFlag(SettingsFlag.PORTAL) || island.getMembers().contains(event.getPlayer().getUniqueId())))) {
            // Portals use is not allowed
            if (!event.getPlayer().isOp() && !VaultHelper.checkPerm(event.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                Util.sendMessage(event.getPlayer(), ChatColor.RED + plugin.myLocale(event.getPlayer().getUniqueId()).islandProtected);
                event.setCancelled(true);
                return;
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
                        Util.sendMessage(event.getPlayer(), ChatColor.RED + plugin.myLocale(event.getPlayer().getUniqueId()).warpserrorNotSafe);
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
                            new SafeTeleportBuilder(plugin)
                            .entity(event.getPlayer())
                            .location(plugin.getPlayers().getIslandLocation(playerUUID))
                            .portal()
                            .homeNumber(1)
                            .build();
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
                            // Fire entry event
                            Island islandTo = plugin.getGrid().getIslandAt(dest);
                            final IslandEnterEvent event2 = new IslandEnterEvent(event.getPlayer().getUniqueId(), islandTo, dest);
                            plugin.getServer().getPluginManager().callEvent(event2);
                        } else {
                            event.setCancelled(true);
                            new SafeTeleportBuilder(plugin)
                            .entity(event.getPlayer())
                            .location(plugin.getPlayers().getIslandLocation(playerUUID))
                            .portal()
                            .homeNumber(1)
                            .build();
                            // Fire entry event
                            Island islandTo = plugin.getGrid().getIslandAt(plugin.getPlayers().getIslandLocation(playerUUID));
                            final IslandEnterEvent event2 = new IslandEnterEvent(event.getPlayer().getUniqueId(), islandTo, plugin.getPlayers().getIslandLocation(playerUUID));
                            plugin.getServer().getPluginManager().callEvent(event2);
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
                Location overworldIsland = island.getCenter().toVector().toLocation(ASkyBlock.getIslandWorld());
                Location netherIsland = island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld());
                //Location dest = event.getFrom().toVector().toLocation(ASkyBlock.getIslandWorld());
                if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
                    // Going to Nether
                    // Check that there is a nether island there. Due to legacy reasons it may not exist
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: island center = " + island.getCenter());               
                    if (netherIsland.getBlock().getType() != Material.BEDROCK) {
                        // Check to see if there is anything there
                        if (plugin.getGrid().bigScan(netherIsland, 20) == null) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: big scan is null");
                            plugin.getLogger().warning("Creating nether island for " + event.getPlayer().getName() + " using default nether schematic");
                            Schematic nether = IslandCmd.getSchematics().get("nether");
                            if (nether != null) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: pasting at " + island.getCenter().toVector());
                                plugin.getIslandCmd().pasteSchematic(nether, netherIsland, event.getPlayer(), PasteReason.PARTNER);
                                if (nether.isPlayerSpawn()) {
                                    // Set partner home
                                    plugin.getPlayers().setHomeLocation(event.getPlayer().getUniqueId(), nether.getPlayerSpawn(netherIsland), -2);
                                }
                            } else {
                                plugin.getLogger().severe("Cannot teleport player to nether because there is no nether schematic");
                                event.setCancelled(true);
                                Util.sendMessage(event.getPlayer(), ChatColor.RED + plugin.myLocale(event.getPlayer().getUniqueId()).warpserrorNotSafe);
                                return;
                            }
                        }
                    }
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Teleporting to " + event.getFrom().toVector().toLocation(ASkyBlock.getNetherWorld()));
                    event.setCancelled(true);
                    // Teleport using the new safeSpot teleport
                    new SafeTeleportBuilder(plugin)
                    .entity(event.getPlayer())
                    .location(netherIsland)
                    .portal()
                    .build();
                    return;
                }
                // Going to the over world - if there isn't an island, do nothing
                event.setCancelled(true);
                // Teleport using the new safeSpot teleport
                new SafeTeleportBuilder(plugin)
                .entity(event.getPlayer())
                .location(overworldIsland)
                .portal()
                .build();
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
     * @return true if in the spawn area, false if not
     */
    private boolean awayFromSpawn(Player player) {
        Vector p = player.getLocation().toVector().multiply(new Vector(1, 0, 1));
        Vector spawn = player.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
        return !(spawn.distanceSquared(p) < (Settings.netherSpawnRadius
            * Settings.netherSpawnRadius));
    }

    /**
     * Prevents blocks from being broken
     * 
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (DEBUG2)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
        // plugin.getLogger().info("Block break");
        if ((e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether") && !Settings.newNether)
                || e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
            if (VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            if (DEBUG)
                plugin.getLogger().info("Block break in island nether");
            if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
                Util.sendMessage(e.getPlayer(), plugin.myLocale(e.getPlayer().getUniqueId()).netherSpawnIsProtected);
                e.setCancelled(true);
            }
        }

    }

    /**
     * Prevents placing of blocks
     * 
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
        if (DEBUG2)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
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
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
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
     * @param e - event
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
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeGrow(final StructureGrowEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());

        if (!Settings.netherTrees) {
            return;
        }
        if (!Settings.createNether || ASkyBlock.getNetherWorld() == null) {
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