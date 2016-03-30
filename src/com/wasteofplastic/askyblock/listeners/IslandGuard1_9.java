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
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.Flags;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands - handles newer events that may not
 *         exist in older servers
 */
public class IslandGuard1_9 implements Listener {
    private final ASkyBlock plugin;
    private final static boolean DEBUG = false;
    private Scoreboard scoreboard;
    private Team pushTeam;

    public IslandGuard1_9(final ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles Frost Walking on visitor's islands
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockForm(EntityBlockFormEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (e.getEntity() instanceof Player && e.getNewState().getType().equals(Material.FROSTED_ICE)) {
            Player player= (Player) e.getEntity();
            if (!IslandGuard.inWorld(player)) {
                return;
            }
            if (player.isOp()) {
                return;
            }
            // This permission bypasses protection
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            // Check island
            Island island = plugin.getGrid().getIslandAt(player.getLocation());
            if (island !=null) {
                if (island.isSpawn()) {
                    if (Settings.allowSpawnPlaceBlocks) {
                        return;
                    }
                } else {
                    if (island.getMembers().contains(player.getUniqueId()) || island.getIgsFlag(Flags.allowPlaceBlocks)) {
                        return;
                    }
                }
            }
            // Silently cancel the event
            e.setCancelled(true);
        }
    }


    /**
     * Handle interaction with end crystals 1.9
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHitEndCrystal(final PlayerInteractAtEntityEvent e) {
        if (DEBUG) {
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
        if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityType.ENDER_CRYSTAL)) {
            // Check island
            Island island = plugin.getGrid().getIslandAt(e.getRightClicked().getLocation());
            if (island !=null) {
                if (island.isSpawn()) {
                    if (Settings.allowSpawnBreakBlocks) {
                        return;
                    }
                } else {
                    if (island.getMembers().contains(e.getPlayer().getUniqueId()) || island.getIgsFlag(Flags.allowBreakBlocks)) {
                        return;
                    }
                }
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
        }
    }

    // End crystal
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    void placeEndCrystalEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (DEBUG) {
            plugin.getLogger().info("End crystal place " + e.getEventName());
        }
        if (!IslandGuard.inWorld(p)) {
            return;
        }
        if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
            // You can do anything if you are Op
            return;
        }

        // Check if they are holding armor stand
        ItemStack inHand = e.getPlayer().getItemInHand();
        if (inHand != null && inHand.getType().equals(Material.END_CRYSTAL)) {
            // Check island
            Island island = plugin.getGrid().getIslandAt(e.getPlayer().getLocation());
            if (island !=null && (island.getMembers().contains(p.getUniqueId()) || island.getIgsFlag(Flags.allowPlaceBlocks))) {
                //plugin.getLogger().info("DEBUG: armor stand place check");
                if (Settings.limitedBlocks.containsKey("END_CRYSTAL") && Settings.limitedBlocks.get("END_CRYSTAL") > -1) {
                    //plugin.getLogger().info("DEBUG: count armor stands");
                    int count = island.getTileEntityCount(Material.END_CRYSTAL);
                    //plugin.getLogger().info("DEBUG: count is " + count + " limit is " + Settings.limitedBlocks.get("ARMOR_STAND"));
                    if (Settings.limitedBlocks.get("END_CRYSTAL") <= count) {
                        e.getPlayer().sendMessage(ChatColor.RED + (plugin.myLocale(e.getPlayer().getUniqueId()).entityLimitReached.replace("[entity]",
                                Util.prettifyText(Material.END_CRYSTAL.toString()))).replace("[number]", String.valueOf(Settings.limitedBlocks.get("END_CRYSTAL"))));
                        e.setCancelled(true);
                        return;
                    }
                }
                return;
            }
            // plugin.getLogger().info("DEBUG: stand place cancelled");
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + plugin.myLocale(e.getPlayer().getUniqueId()).islandProtected);
            e.getPlayer().updateInventory();
        }

    }

    /**
     * Handle end crystal damage by visitors
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void EndCrystalDamage(EntityDamageByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("IslandGuard 1_9 " + e.getEventName());
            plugin.getLogger().info("Entity is " + e.getEntityType());
        }
        if (e.getEntity() == null || !IslandGuard.inWorld(e.getEntity())) {
            return;
        }
        if (!(e.getEntity() instanceof EnderCrystal)) {
            if (DEBUG) {
                plugin.getLogger().info("Entity is not End crystal it is " + e.getEntityType());
            }
            return;
        }
        if (DEBUG) {
            plugin.getLogger().info("Damager is " + e.getDamager());
        }
        Player p = null;
        if (e.getDamager() instanceof Player) {
            p = (Player) e.getDamager();
            if (DEBUG) {
                plugin.getLogger().info("Damager is a player");
            }
        } else if (e.getDamager() instanceof Projectile) {
            // Get the shooter
            Projectile projectile = (Projectile)e.getDamager();
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                p = (Player)shooter;
            }
            if (DEBUG) {
                plugin.getLogger().info("Damager is a projectile shot by " + p.getName());
            }
        }
        if (p != null) {  
            if (p.isOp() || VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
                if (DEBUG) {
                    plugin.getLogger().info("Bypassing protection");
                }
                return;
            }
            // Check if on island
            if (plugin.getGrid().playerIsOnIsland(p)) {
                if (DEBUG) {
                    plugin.getLogger().info("Player is on their own island");
                }
                return;
            }
            // Check island
            Island island = plugin.getGrid().getIslandAt(e.getEntity().getLocation());
            if (island != null && island.isSpawn() && Settings.allowSpawnBreakBlocks) {
                if (DEBUG) {
                    plugin.getLogger().info("Spawn and breaking blocks is allowed");
                }
                return;
            }
            if (island != null && island.getIgsFlag(Flags.allowBreakBlocks)) {
                if (DEBUG) {
                    plugin.getLogger().info("Visitor is allowed to break blocks");
                }
                return;
            }
            p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).islandProtected);
            e.setCancelled(true);
        }

    }

    /**
     * Handles end crystal explosions
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("Entity exploding is " + e.getEntity());
        }
        if (e.getEntity() == null || !e.getEntityType().equals(EntityType.ENDER_CRYSTAL)) {
            if (DEBUG) {
                plugin.getLogger().info("Entity is not an END CRYSTAL");
            }
            return;
        }

        if (!IslandGuard.inWorld(e.getLocation())) {
            return;
        }
        // General settings irrespective of whether this is allowed or not
        if (!Settings.allowTNTDamage) {
            plugin.getLogger().info("TNT block damage prevented");
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
        // prevent at spawn
        if (plugin.getGrid().isAtSpawn(e.getLocation())) {
            e.blockList().clear();
            e.setCancelled(true);
        }

    }

    /**
     * Triggers a push protection change or not
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        setPush(e.getPlayer());
    }

    /**
     * Handles push protection
     * @param player
     */
    public void setPush(Player player) {
        scoreboard = player.getScoreboard();
        if (scoreboard == null) {
            //plugin.getLogger().info("DEBUG: initializing scoreboard");
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        if (Settings.allowPushing) {
            if (scoreboard.getTeam("ASkyBlockNP") != null) {
                //plugin.getLogger().info("DEBUG: unregistering the team");
                scoreboard.getTeam("ASkyBlockNP").unregister();
            }
            return;
        }
        // Try and get what team the player is on right now
        pushTeam = scoreboard.getEntryTeam(player.getName());
        if (pushTeam == null) {
            // It doesn't exist yet, so make it
            pushTeam = scoreboard.getTeam("ASkyBlockNP");
            if (pushTeam == null) {
                pushTeam = scoreboard.registerNewTeam("ASkyBlockNP");
            }
            // Add the player to the team
            pushTeam.addEntry(player.getName()); 
        }
        if (pushTeam.getName().equals("ASkyBlockNP")) {
            //plugin.getLogger().info("DEBUG: pushing not allowed");
            pushTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);               
        } else {
            //plugin.getLogger().info("DEBUG: player is already in another team");
        }
    }
}