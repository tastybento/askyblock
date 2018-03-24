package com.wasteofplastic.askyblock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Monster;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AcidTask {
    private ASkyBlock plugin;
    private Set<UUID> itemsInWater;
    private BukkitTask task;

    /**
     * Runs repeating tasks to deliver acid damage to mobs, etc.
     * @param plugin - ASkyBlock plugin object - ASkyBlock plugin
     */
    public AcidTask(final ASkyBlock plugin) {
        this.plugin = plugin;
        // Initialize water item list
        itemsInWater = new HashSet<UUID>();
        // This part will kill monsters if they fall into the water
        // because it
        // is acid
        if (Settings.mobAcidDamage > 0D || Settings.animalAcidDamage > 0D) {
            plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    List<Entity> entList = ASkyBlock.getIslandWorld().getEntities();
                    for (Entity current : entList) {
                        if (plugin.isOnePointEight() && current instanceof Guardian) {
                            // Guardians are immune to acid too
                            continue;
                        }
                        if ((current instanceof Monster) && Settings.mobAcidDamage > 0D) {
                            if ((current.getLocation().getBlock().getType() == Material.WATER)
                                    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
                                ((Monster) current).damage(Settings.mobAcidDamage);
                            }
                        } else if ((current instanceof Animals) && Settings.animalAcidDamage > 0D) {
                            if ((current.getLocation().getBlock().getType() == Material.WATER)
                                    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
                                if (!current.getType().equals(EntityType.CHICKEN) || Settings.damageChickens) {
                                    ((Animals) current).damage(Settings.animalAcidDamage);
                                }
                            }
                        }
                    }
                }
            }, 0L, 20L);
        }
        runAcidItemRemovalTask();
    }

    public void runAcidItemRemovalTask() {
        if (task != null)
            task.cancel();
        // If items must be removed when dropped in acid
        if (Settings.acidItemDestroyTime > 0) {
            task = new BukkitRunnable() {
                public void run() {
                    //plugin.getLogger().info("DEBUG: running task every " + Settings.acidItemDestroyTime);
                    List<Entity> entList = ASkyBlock.getIslandWorld().getEntities();
                    // Clean up the itemsInWater list
                    Set<UUID> newItemsInWater = new HashSet<UUID>();
                    for (Entity current: entList) {
                        if (current.getType() != null && current.getType().equals(EntityType.DROPPED_ITEM)) {
                            if ((current.getLocation().getBlock().getType() == Material.WATER)
                                    || (current.getLocation().getBlock().getType() == Material.STATIONARY_WATER)) {
                                //plugin.getLogger().info("DEBUG: Item in water " + current.toString());
                                // Check if this item was in the list last time
                                if (itemsInWater.contains(current.getUniqueId())) {
                                    // Remove item
                                    if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                        current.getWorld().playSound(current.getLocation(), Sound.valueOf("FIZZ"), 3F, 3F);
                                    } else {
                                        current.getWorld().playSound(current.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 3F, 3F);
                                    }
                                    current.remove();
                                } else {
                                    // Add to list
                                    newItemsInWater.add(current.getUniqueId());
                                }
                            }
                        }
                    }
                    // Clean up any items from the list that do not exist anymore
                    itemsInWater = newItemsInWater;
                    //plugin.getLogger().info("DEBUG: items in water size = " + itemsInWater.size());
                }
            }.runTaskTimer(plugin, Settings.acidItemDestroyTime, Settings.acidItemDestroyTime);
        }
    }


}
