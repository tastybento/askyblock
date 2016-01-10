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

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;

/**
 * This class handles the Wither. Spawning withers is a reality on some servers as it is the way
 * to obtain a nether star. This class enables players to spawn a Wither without it adversely affecting
 * other players or their islands. 
 * 
 * @author tastybento
 *
 */
public class WitherEvents implements Listener {
    private final ASkyBlock plugin;
    private final static boolean DEBUG = false;
    private HashMap<UUID, Island> witherSpawnInfo;
    /**
     * @param plugin
     */
    public WitherEvents(ASkyBlock plugin) {
        this.plugin = plugin;
        this.witherSpawnInfo = new HashMap<UUID, Island>();
    }

    /**
     * Track where the wither was created. This will determine its allowable attack zone.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void WitherSpawn(CreatureSpawnEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover withers in the island world
        if (e.getEntityType() != EntityType.WITHER || !IslandGuard.inWorld(e.getEntity()) ) {
            return;
        }
        // Store where this wither originated
        Island island = plugin.getGrid().getIslandAt(e.getLocation());
        if (island != null) {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: Wither spawned on known island - id = " + e.getEntity().getUniqueId());
            }
            witherSpawnInfo.put(e.getEntity().getUniqueId(),island);
        } // Else do nothing - maybe an Op spawned it? If so, on their head be it!
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void WitherExplosion(EntityExplodeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover withers in the island world
        if (e.getEntity() == null || !IslandGuard.inWorld(e.getEntity())) {
            return;
        }
        // The wither or wither skulls can both blow up
        if (e.getEntityType() == EntityType.WITHER || e.getEntityType() == EntityType.WITHER_SKULL) {
            // Check the location
            //plugin.getLogger().info("DEBUG: Wither or wither skull");
            if (witherSpawnInfo.containsKey(e.getEntity().getUniqueId())) {
                // We know about this wither
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: We know about this wither");
                }
                if (!witherSpawnInfo.get(e.getEntity().getUniqueId()).inIslandSpace(e.getLocation())) {
                    // Cancel the explosion and block damage
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: cancel wither explosion");
                    }
                    e.blockList().clear();
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Deal with pre-explosions
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void WitherExplode(ExplosionPrimeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover withers in the island world
        if (!IslandGuard.inWorld(e.getEntity()) || e.getEntity() == null) {
            return;
        }
        // The wither or wither skulls can both blow up
        if (e.getEntityType() == EntityType.WITHER) {
            //plugin.getLogger().info("DEBUG: Wither");
            // Check the location
            if (witherSpawnInfo.containsKey(e.getEntity().getUniqueId())) {
                // We know about this wither
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: We know about this wither");
                }
                if (!witherSpawnInfo.get(e.getEntity().getUniqueId()).inIslandSpace(e.getEntity().getLocation())) {
                    // Cancel the explosion
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: cancelling wither pre-explosion");
                    }
                    e.setCancelled(true);
                }
            }
            // Testing only e.setCancelled(true);
        }
        if (e.getEntityType() == EntityType.WITHER_SKULL) {
            //plugin.getLogger().info("DEBUG: Wither skull");
            // Get shooter
            Projectile projectile = (Projectile)e.getEntity();
            if (projectile.getShooter() instanceof Wither) {
                //plugin.getLogger().info("DEBUG: shooter is wither");
                Wither wither = (Wither)projectile.getShooter();
                // Check the location
                if (witherSpawnInfo.containsKey(wither.getUniqueId())) {
                    // We know about this wither
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: We know about this wither");
                    }
                    if (!witherSpawnInfo.get(wither.getUniqueId()).inIslandSpace(e.getEntity().getLocation())) {
                        // Cancel the explosion
                        if (DEBUG) {
                            plugin.getLogger().info("DEBUG: cancel wither skull explosion");
                        }
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    /**
     * Withers change blocks to air after they are hit (don't know why)
     * This prevents this when the wither has been spawned by a visitor
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void WitherChangeBlocks(EntityChangeBlockEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover withers in the island world
        if (e.getEntityType() != EntityType.WITHER || !IslandGuard.inWorld(e.getEntity()) ) {
            return;
        }
        if (witherSpawnInfo.containsKey(e.getEntity().getUniqueId())) {
            // We know about this wither
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: We know about this wither");
            }
            if (!witherSpawnInfo.get(e.getEntity().getUniqueId()).inIslandSpace(e.getEntity().getLocation())) {
                // Cancel the block changes
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: cancelled wither block change");
                }
                e.setCancelled(true);
            }
        }
    }
    /**
     * Clean up the hashmap. It's probably not needed, but just in case.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void WitherDeath(EntityDeathEvent e) {
        if (e.getEntityType() == EntityType.WITHER) {
            witherSpawnInfo.remove(e.getEntity().getUniqueId());
        }
    }

}
