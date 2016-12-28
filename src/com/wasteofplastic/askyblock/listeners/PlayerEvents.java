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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.InventorySave;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.Flags;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.events.IslandEnterEvent;
import com.wasteofplastic.askyblock.events.IslandExitEvent;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands
 */
public class PlayerEvents implements Listener {
    private final ASkyBlock plugin;
    private static final boolean DEBUG = false;
    // A set of falling players
    private static HashSet<UUID> fallingPlayers = new HashSet<UUID>();
    private List<UUID> respawn;

    public PlayerEvents(final ASkyBlock plugin) {
        this.plugin = plugin;
        respawn = new ArrayList<UUID>();
    }

    /**
     * Prevents changing of hunger while having a special permission and being on your island
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHungerChange(final FoodLevelChangeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName() + " food level = " + ((Player)e.getEntity()).getFoodLevel() + " new food level = " + e.getFoodLevel());

        }
        // Allow food increases
        if (e.getFoodLevel() - ((Player)e.getEntity()).getFoodLevel() > 0) {
            return;
        }
        if (e.getEntity().hasPermission(Settings.PERMPREFIX + "nohunger")) {
            if(plugin.getGrid().playerIsOnIsland((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Places player back on their island if the setting is true
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerRespawn(final PlayerRespawnEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Settings.respawnOnIsland) {
            return;
        }
        if (respawn.contains(e.getPlayer().getUniqueId())) {
            respawn.remove(e.getPlayer().getUniqueId());
            Location respawnLocation = plugin.getGrid().getSafeHomeLocation(e.getPlayer().getUniqueId(), 1);
            if (respawnLocation != null) {
                //plugin.getLogger().info("DEBUG: Setting respawn location to " + respawnLocation);
                e.setRespawnLocation(respawnLocation);
            }
        }
    }

    /**
     * Registers death of player.
     * Places the player on the island respawn list if set
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onPlayerDeath(final PlayerDeathEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Died in island space?
        if (!IslandGuard.inWorld(e.getEntity())) {
            return;
        }
        UUID playerUUID = e.getEntity().getUniqueId();
        // Check if player has an island
        if (plugin.getPlayers().hasIsland(playerUUID) || plugin.getPlayers().inTeam(playerUUID)) {
            if (Settings.respawnOnIsland) {
                // Add them to the list to be respawned on their island
                respawn.add(playerUUID);
            }
            // Add death to death count
            plugin.getPlayers().addDeath(playerUUID);
            if (Settings.deathpenalty != 0) {
                if (plugin.getPlayers().inTeam(playerUUID)) {
                    // Tell team
                    plugin.getMessages().tellOfflineTeam(playerUUID, ChatColor.GREEN + "(" + String.valueOf(plugin.getPlayers().getDeaths(playerUUID)) + " " + plugin.myLocale(playerUUID).deathsDied + ")");
                }
            }
        }
    } 

    /*
     * Prevent dropping items if player dies on another island
     * This option helps reduce the down side of dying due to traps, etc.
     * Also handles muting of death messages
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onVistorDeath(final PlayerDeathEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!IslandGuard.inWorld(e.getEntity())) {
            return;
        }
        // Mute death messages
        if (Settings.muteDeathMessages) {
            e.setDeathMessage(null);
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
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // This will override any global settings
        if (Settings.allowVisitorKeepInvOnDeath) {
            InventorySave.getInstance().loadPlayerInventory(e.getPlayer());
            InventorySave.getInstance().clearSavedInventory(e.getPlayer());
        }
    }
    /*
     * Prevent item pickup by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorPickup(final PlayerPickupItemEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!IslandGuard.inWorld(e.getPlayer())) {
            return;
        }
        if (plugin.getGrid().isAtSpawn(e.getItem().getLocation())) {
            if (Settings.allowSpawnVisitorItemPickup || e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                    || plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getItem().getLocation())) {
                return;
            }
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
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!IslandGuard.inWorld(e.getPlayer())) {
            return;
        }
        if (plugin.getGrid().isAtSpawn(e.getItemDrop().getLocation())) {
            if (Settings.allowSpawnVisitorItemDrop || e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                    || plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getItemDrop().getLocation())) {
                return;
            }
        }
        if (Settings.allowVisitorItemDrop || e.getPlayer().isOp() || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getItemDrop().getLocation())) {
            return;
        }
        e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
        e.setCancelled(true);
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
        if (!IslandGuard.inWorld(e.getPlayer()) 
                || Settings.allowTeleportWhenFalling 
                || !e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) 
                || e.getPlayer().isOp() 
                || e.getPlayer().isFlying()) {
            fallingPlayers.remove(e.getPlayer().getUniqueId());
            return;
        }
        // Check if air below player
        // plugin.getLogger().info("DEBUG:" +
        // Math.round(e.getPlayer().getVelocity().getY()));
        if (e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerTeleport(final PlayerCommandPreprocessEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!IslandGuard.inWorld(e.getPlayer()) || Settings.allowTeleportWhenFalling || e.getPlayer().isOp()
                || !e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)
                || plugin.getPlayers().isInTeleport(e.getPlayer().getUniqueId())) {
            return;
        }
        // Check commands
        // plugin.getLogger().info("DEBUG: falling command: '" +
        // e.getMessage().substring(1).toLowerCase() + "'");
        if (isFalling(e.getPlayer().getUniqueId()) && (Settings.fallingCommandBlockList.contains("*") || Settings.fallingCommandBlockList.contains(e.getMessage().substring(1).toLowerCase()))) {
            // Sorry you are going to die
            e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).errorNoPermission); 
            e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).islandcannotTeleport);
            e.setCancelled(true);
        }
    }

    /**
     * Prevents teleporting when falling based on setting and teleporting to locked islands
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerTeleport(final PlayerTeleportEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("DEBUG: to = " + e.getTo());
            plugin.getLogger().info("DEBUG: from = " + e.getFrom());
        }
        // Options - 
        // Player is in an island world and trying to teleport out - handle
        // Player is in an island world and trying to teleport within - handle
        // Player is not in an island world and trying to teleport in - handle
        // Player is not in an island world and teleporting not in - skip
        if (e.getTo() == null || e.getFrom() == null) {
            return;
        }
        if (!IslandGuard.inWorld(e.getTo()) && !IslandGuard.inWorld(e.getFrom())) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: to or from are not in this world - returning ");
            return;
        }
        // Check if ready
        if (plugin.getGrid() == null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: grid is not ready");
            return;
        }
        // Teleporting while falling check
        if (!Settings.allowTeleportWhenFalling && e.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !e.getPlayer().isOp()) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Teleporting while falling check");
            // If the player is allowed to teleport excuse them
            if (plugin.getPlayers().isInTeleport(e.getPlayer().getUniqueId())) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: player is allowed to teleport excuse them");
                unsetFalling(e.getPlayer().getUniqueId());
            } else if (isFalling(e.getPlayer().getUniqueId())) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: player is falling");
                // Sorry you are going to die
                e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).islandcannotTeleport);
                e.setCancelled(true);
                // Check if the player is in the void and kill them just in case
                if (e.getPlayer().getLocation().getBlockY() < 0) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: player is in the void");
                    e.getPlayer().setHealth(0D);
                    unsetFalling(e.getPlayer().getUniqueId());
                }
                return;
            }
        }
        //plugin.getLogger().info("DEBUG: From : " + e.getFrom());
        //plugin.getLogger().info("DEBUG: To : " + e.getTo());
        // Teleporting to a locked island
        if (DEBUG)
            plugin.getLogger().info("DEBUG: getting islands for to from");
        Island islandFrom = plugin.getGrid().getProtectedIslandAt(e.getFrom());
        if (DEBUG && islandFrom != null)
            plugin.getLogger().info("DEBUG: islandFrom is not null");
        Island islandTo = plugin.getGrid().getProtectedIslandAt(e.getTo());
        if (DEBUG && islandTo != null)
            plugin.getLogger().info("DEBUG: islandTo is not null");

        // Ender pearl and chorus fruit teleport checks
        if (e.getCause() != null) {
            if (e.getCause().equals(TeleportCause.ENDER_PEARL)) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Enderpearl");

                if (islandTo == null) {
                    if (Settings.allowEnderPearls) {
                        return;
                    }
                } else {
                    if (DEBUG )
                        plugin.getLogger().info("DEBUG: islandTo is not null enderpearl");

                    if (islandTo.isSpawn()) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: islandTo is spawn");

                        if (Settings.allowSpawnEnderPearls) {
                            if (DEBUG )
                                plugin.getLogger().info("DEBUG: spawn enderpearl allowed");
                            return;
                        }
                    } else {
                        if (DEBUG )
                            plugin.getLogger().info("DEBUG: islandTo is regular island");
                        // Regular island
                        if (islandTo.getIgsFlag(Flags.allowEnderPearls) || islandTo.getMembers().contains(e.getPlayer().getUniqueId())) {
                            if (DEBUG )
                                plugin.getLogger().info("DEBUG: enderpearl allowed");
                            return;
                        }
                    }
                }
                if (DEBUG )
                    plugin.getLogger().info("DEBUG: enderpearl not allowed");
                e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                e.setCancelled(true);
                return;
            } else if (!plugin.getServer().getVersion().contains("(MC: 1.8")
                    && !plugin.getServer().getVersion().contains("(MC: 1.7")) {
                if (DEBUG )
                    plugin.getLogger().info("DEBUG: chorus fruit check");
                // Chorus fruit only exist in 1.9 and above
                if (e.getCause().equals(TeleportCause.CHORUS_FRUIT)) {
                    if (DEBUG )
                        plugin.getLogger().info("DEBUG: chorus fruit");
                    boolean cancel = false;
                    // Check both from and to islands
                    if (islandTo == null) {
                        if (!Settings.allowChorusFruit) {
                            cancel = true;
                        }
                    } else {
                        if (islandTo.isSpawn()) {
                            if (!Settings.allowSpawnChorusFruit) {
                                cancel = true;
                            }
                        } else {
                            // Regular island
                            if (!islandTo.getIgsFlag(Flags.allowChorusFruit) && !islandTo.getMembers().contains(e.getPlayer().getUniqueId())) {
                                cancel = true;
                            }
                        }
                    }
                    if (islandFrom == null) {
                        if (!Settings.allowChorusFruit) {
                            cancel = true;
                        }
                    } else {
                        if (islandFrom.isSpawn()) {
                            if (!Settings.allowSpawnChorusFruit) {
                                cancel = true;
                            }
                        } else {
                            // Regular island
                            if (!islandFrom.getIgsFlag(Flags.allowChorusFruit) && !islandFrom.getMembers().contains(e.getPlayer().getUniqueId())) {
                                cancel = true;
                            }
                        }
                    }
                    if (cancel) {
                        e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
                        e.setCancelled(true);
                    }
                    return;
                }
            }
        }

        // Announcement entering
        // Only says something if there is a change in islands
        /*
         * Teleport Situations:
         * islandTo == null && islandFrom != null - exit
         * islandTo == null && islandFrom == null - nothing
         * islandTo != null && islandFrom == null - enter
         * islandTo != null && islandFrom != null - same PlayerIsland or teleport?
         * islandTo == islandFrom
         */
        if (DEBUG )
            plugin.getLogger().info("DEBUG: announcements");
        if (islandTo != null && islandFrom == null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
            if (DEBUG )
                plugin.getLogger().info("DEBUG: entering");
            // Entering
            if (islandTo.isLocked() || plugin.getPlayers().isBanned(islandTo.getOwner(),e.getPlayer().getUniqueId())) {
                e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).lockIslandLocked);
                if (!plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getTo()) && !e.getPlayer().isOp()
                        && !VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                        && !VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypasslock")) {
                    if (DEBUG )
                        plugin.getLogger().info("DEBUG: not allowed to enter");
                    e.setCancelled(true);
                    return;
                }
            }
            if (islandTo.isSpawn()) {
                if (DEBUG )
                    plugin.getLogger().info("DEBUG: islandTo is locked spawn");
                if (!plugin.myLocale(e.getPlayer().getUniqueId()).lockEnteringSpawn.isEmpty()) {
                    e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockEnteringSpawn);
                }
            } else {
                if (DEBUG )
                    plugin.getLogger().info("DEBUG: islandTo is locked regular");
                if (!plugin.myLocale(e.getPlayer().getUniqueId()).lockNowEntering.isEmpty()) {
                    e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowEntering.replace("[name]", plugin.getGrid().getIslandName(islandTo.getOwner())));
                }
            }
            // Fire entry event
            final IslandEnterEvent event = new IslandEnterEvent(e.getPlayer().getUniqueId(), islandTo, e.getTo());
            plugin.getServer().getPluginManager().callEvent(event);
        } else if (islandTo == null && islandFrom != null && (islandFrom.getOwner() != null || islandFrom.isSpawn())) {
            if (DEBUG )
                plugin.getLogger().info("DEBUG: Leaving");
            // Leaving
            if (islandFrom.isSpawn()) {
                if (DEBUG )
                    plugin.getLogger().info("DEBUG: leaving spawn");
                // Leaving
                if (!plugin.myLocale(e.getPlayer().getUniqueId()).lockLeavingSpawn.isEmpty()) {
                    e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockLeavingSpawn);
                }
            } else {
                if (DEBUG )
                    plugin.getLogger().info("DEBUG: leaving locked");
                if (!plugin.myLocale(e.getPlayer().getUniqueId()).lockNowLeaving.isEmpty()) {
                    e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowLeaving.replace("[name]", plugin.getGrid().getIslandName(islandFrom.getOwner())));
                }
            }
            // Fire exit event
            final IslandExitEvent event = new IslandExitEvent(e.getPlayer().getUniqueId(), islandFrom, e.getFrom());
            plugin.getServer().getPluginManager().callEvent(event);
        } else if (islandTo != null && islandFrom != null && !islandTo.equals(islandFrom)) {
            if (DEBUG )
                plugin.getLogger().info("DEBUG: jumping from one island to another - adjacent islands");
            // Teleporting from one islands to another
            // Entering
            if (islandTo.isLocked() || plugin.getPlayers().isBanned(islandTo.getOwner(),e.getPlayer().getUniqueId())) {
                e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).lockIslandLocked);
                if (!plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getTo()) && !e.getPlayer().isOp()
                        && !VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                        && !VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypasslock")) {
                    if (DEBUG )
                        plugin.getLogger().info("DEBUG: cannot enter");
                    e.setCancelled(true);
                    return;
                }
            }            
            if (islandFrom.isSpawn()) {
                // Leaving
                e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockLeavingSpawn);
            } else if (islandFrom.getOwner() != null) {
                e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowLeaving.replace("[name]", plugin.getGrid().getIslandName(islandFrom.getOwner())));
            }
            if (islandTo.isSpawn()) {
                e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockEnteringSpawn);
            } else if (islandTo.getOwner() != null) {
                e.getPlayer().sendMessage(plugin.myLocale(e.getPlayer().getUniqueId()).lockNowEntering.replace("[name]", plugin.getGrid().getIslandName(islandTo.getOwner())));
            }
            // Fire exit event
            final IslandExitEvent event = new IslandExitEvent(e.getPlayer().getUniqueId(), islandTo, e.getTo());
            plugin.getServer().getPluginManager().callEvent(event);
            // Fire entry event
            final IslandEnterEvent event2 = new IslandEnterEvent(e.getPlayer().getUniqueId(), islandFrom, e.getFrom());
            plugin.getServer().getPluginManager().callEvent(event2);
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
     * Unset the falling flag
     * 
     * @param uniqueId
     */
    public static void unsetFalling(UUID uniqueId) {
        // getLogger().info("DEBUG: unset falling");
        fallingPlayers.remove(uniqueId);
    }

    /**
     * Prevents visitors from using commands on islands, like /spawner
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVisitorCommand(final PlayerCommandPreprocessEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("Visitor command " + e.getEventName() + ": " + e.getMessage());
        }
        if (!IslandGuard.inWorld(e.getPlayer()) || e.getPlayer().isOp()
                || VaultHelper.checkPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || plugin.getGrid().locationIsOnIsland(e.getPlayer(), e.getPlayer().getLocation())) {
            //plugin.getLogger().info("player is not in world or op etc.");
            return;
        }
        // Check banned commands
        //plugin.getLogger().info(Settings.visitorCommandBlockList.toString());
        String[] args = e.getMessage().substring(1).toLowerCase().split(" ");
        if (Settings.visitorCommandBlockList.contains(args[0])) {
            e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
            e.setCancelled(true);
        }
    }

    /**
     * Prevents visitors from getting damage if invinciblevisitors option is set to TRUE
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVisitorGetDamage(EntityDamageEvent e){
        if(!Settings.invincibleVisitors) return;
        if(!(e.getEntity() instanceof Player)) return;

        Player p = (Player) e.getEntity();
        if (!IslandGuard.inWorld(p) || plugin.getGrid().locationIsOnIsland(p, p.getLocation())) return;

        if (Settings.visitorDamagePrevention.contains(e.getCause())) e.setCancelled(true);

        else if(e.getCause().equals(DamageCause.VOID)) {
            if(plugin.getPlayers().hasIsland(p.getUniqueId())) {
                Location safePlace = plugin.getGrid().getSafeHomeLocation(p.getUniqueId(), 1);
                if (safePlace != null) {
                    p.teleport(safePlace);
                    // Set their fall distance to zero otherwise they crash onto their island and die
                    p.setFallDistance(0);
                    e.setCancelled(true);
                    return;
                } 
            }
            // No island, or no safe spot on island
            if (plugin.getGrid().getSpawnPoint() != null) {
                p.teleport(plugin.getGrid().getSpawnPoint());
                // Set their fall distance to zero otherwise they crash onto their island and die
                p.setFallDistance(0);
                e.setCancelled(true);
                return;
            }
            // No island spawn, try regular spawn
            if (!p.performCommand("spawn")) {
                // If this command doesn't work, let them die otherwise they may get trapped in the void forever
                return;
            }
            // Set their fall distance to zero otherwise they crash onto their island and die
            p.setFallDistance(0);
            e.setCancelled(true);
        }
    }
}
