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

import java.util.Random;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

/**
 * This makes wither skeletons spawn from spawners in the nether again in 1.11
 * @author tastybento
 *
 */
public class NetherSpawning implements Listener {
    private final ASkyBlock plugin;
    private final static boolean DEBUG = false;
    private final static double WITHER_SKELETON_SPAWN_CHANCE = 0.1;
    private final Random random = new Random();
    private boolean hasWitherSkeleton = false;

    public NetherSpawning(ASkyBlock plugin) {
        this.plugin = plugin;
        for (EntityType type: EntityType.values()) {
            if (type.toString().equals("WITHER_SKELETON")) {
                this.hasWitherSkeleton = true;
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Wither Skeleton exists");
                break;
            }    
        }
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSkeletonSpawn(final CreatureSpawnEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
        if (!Settings.hackSkeletonSpawners) {
            return;
        }
        if (!hasWitherSkeleton) {
            // Only if this type of Entity exists
            return;
        }
        // Check for spawn reason
        if (e.getSpawnReason().equals(SpawnReason.SPAWNER) && e.getEntityType().equals(EntityType.SKELETON)) {
            if (!Settings.createNether || !Settings.newNether || ASkyBlock.getNetherWorld() == null) {
                return;
            }
            // Check world
            if (!e.getLocation().getWorld().equals(ASkyBlock.getNetherWorld())) {
                return;
            }
            if (random.nextDouble() < WITHER_SKELETON_SPAWN_CHANCE) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Wither Skelly spawned");
                e.setCancelled(true);
                e.getLocation().getWorld().spawnEntity(e.getLocation(), EntityType.WITHER_SKELETON);
            } else {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Standard Skelly spawned");
            }
        }
    }
}