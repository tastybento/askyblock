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

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * This tracks entities so they can be limited in number if required
 * @author tastybento
 *
 */
public class EntitySpawning implements Listener {
    private final ASkyBlock plugin;
    private final static boolean DEBUG = false;

    public EntitySpawning(ASkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: entity tracker " + e.getEventName());
        // Check world
        if (!e.getLocation().getWorld().toString().contains(Settings.worldName)) {
            return;
        }
        if (!Settings.entityLimits.containsKey(e.getEntityType())) {
            // Unknown entity limit or unlimited
            return;
        }
        boolean bypass = false;
        if (DEBUG)
            plugin.getLogger().info("DEBUG: spawn reason = " + e.getSpawnReason());
        // Check why it was spawned
        switch (e.getSpawnReason()) {
        // These reasons are due to a player being involved (usually)
        case BREEDING:
        case BUILD_IRONGOLEM:
        case BUILD_SNOWMAN:
        case BUILD_WITHER:
        case CURED:
        case EGG:
        case SPAWNER_EGG:
            // If someone in that area has the bypass permission, allow the spawning
            for (Entity entity : e.getLocation().getWorld().getNearbyEntities(e.getLocation(), 5, 5, 5)) {
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    if (player.isOp() || VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypass")) {
                        //plugin.getLogger().info("DEBUG: bypass");
                        bypass = true;
                        break;
                    }
                }
            }
            break;
        default:
            break;
        }
        // Tag the entity with the island spawn location
        LivingEntity creature = e.getEntity();
        Island island = plugin.getGrid().getIslandAt(e.getLocation());
        if (island == null) {
            // Only count island entities
            return;
        }
        // Ignore spawn
        if (island.isSpawn()) {
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Checking entity limits");
        // Check if the player is at the limit
        int count = 0;
        checkLimits:
            if (bypass || Settings.entityLimits.get(e.getEntityType()) > 0) {
                // If bypass, just tag the creature. If not, then we need to count creatures
                if (!bypass) {
                    // Run through all the current entities on this world
                    for (LivingEntity entity: e.getEntity().getWorld().getLivingEntities()) {
                        // If it is the right one
                        if (entity.getType().equals(e.getEntityType())) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: " + entity.getType() + " found");
                            // Check spawn location
                            if (entity.hasMetadata("spawnLoc")) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: has meta");
                                // Get the meta data
                                List<MetadataValue> values = entity.getMetadata("spawnLoc");
                                for (MetadataValue v : values) {
                                    // There is a chance another plugin also uses the meta data spawnLoc
                                    if (v.getOwningPlugin().equals(plugin)) {
                                        // Get the island spawn location
                                        Location spawnLoc = Util.getLocationString(v.asString());
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: entity spawnLoc = " + spawnLoc);
                                        if (spawnLoc != null && spawnLoc.equals(island.getCenter())) {
                                            // Entity is on this island
                                            count++;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: entity is on island. Number = " + count);
                                            if (count >= Settings.entityLimits.get(e.getEntityType())) {
                                                // No more allowed!
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: no more allowed! >=" + count);
                                                break checkLimits;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Okay to spawn, but tag it
                creature.setMetadata("spawnLoc", new FixedMetadataValue(plugin, Util.getStringLocation(island.getCenter())));
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: spawn okay");
                return;
            }
        // Cancel - no spawning - tell nearby players
        if (DEBUG)
            plugin.getLogger().info("DEBUG: spawn cancelled");
        e.setCancelled(true);
        for (Entity ent : e.getLocation().getWorld().getNearbyEntities(e.getLocation(), 5, 5, 5)) {
            if (ent instanceof Player) {
                Player player = (Player)ent; 
                Util.sendMessage(player, ChatColor.RED 
                        + (plugin.myLocale(player.getUniqueId()).entityLimitReached.replace("[entity]", 
                                Util.prettifyText(e.getEntityType().toString()))
                                .replace("[number]", String.valueOf(Settings.entityLimits.get(e.getEntityType())))));
            }
        }
    }
}

