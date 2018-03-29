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

package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;

public class SettingsPanel implements Listener {
    // Island Guard Settings Panel
    private ASkyBlock plugin;
    private static boolean hasChorusFruit;
    private static boolean hasArmorStand;
    private HashMap<UUID,Long> pvpCoolDown = new HashMap<UUID,Long>();
    private static final boolean DEBUG = false;

    /**
     * Lookup table of Material to SettingsFlag
     */
    private static BiMap<Material,SettingsFlag> lookup = HashBiMap.create();
    static {
        // Find out if these exist
        hasChorusFruit = (Material.getMaterial("CHORUS_FRUIT") != null);
        hasArmorStand = (Material.getMaterial("ARMOR_STAND") != null);
        // No icon or flag can be the same, they must all be unique because this is a bimap.
        // Developer - if you add a setting but don't see it appear in the GUI, make sure there's a locale settings for it!
        if (hasArmorStand)
            lookup.put(Material.ARMOR_STAND, SettingsFlag.ARMOR_STAND);
        if (hasChorusFruit)
            lookup.put(Material.CHORUS_FRUIT, SettingsFlag.CHORUS_FRUIT);
        lookup.put(Material.ANVIL, SettingsFlag.ANVIL);
        lookup.put(Material.ARROW, SettingsFlag.PVP);
        lookup.put(Material.BEACON, SettingsFlag.BEACON);
        lookup.put(Material.BED, SettingsFlag.BED);
        lookup.put(Material.BREWING_STAND_ITEM, SettingsFlag.BREWING);
        lookup.put(Material.BUCKET, SettingsFlag.BUCKET);
        lookup.put(Material.CARROT_ITEM, SettingsFlag.BREEDING);
        lookup.put(Material.CHEST, SettingsFlag.CHEST);
        lookup.put(Material.DIAMOND_BARDING, SettingsFlag.HORSE_RIDING);
        lookup.put(Material.DIAMOND, SettingsFlag.VISITOR_ITEM_PICKUP);    
        lookup.put(Material.DIRT, SettingsFlag.PLACE_BLOCKS);
        lookup.put(Material.EGG, SettingsFlag.EGGS);
        lookup.put(Material.EMERALD, SettingsFlag.VILLAGER_TRADING);
        lookup.put(Material.ENCHANTMENT_TABLE, SettingsFlag.ENCHANTING);
        lookup.put(Material.ENDER_PEARL, SettingsFlag.ENDER_PEARL);
        lookup.put(Material.FENCE_GATE, SettingsFlag.GATE);
        lookup.put(Material.FLINT_AND_STEEL, SettingsFlag.FIRE);
        lookup.put(Material.FURNACE, SettingsFlag.FURNACE);
        lookup.put(Material.GOLD_BARDING, SettingsFlag.HORSE_INVENTORY);
        lookup.put(Material.GOLD_INGOT, SettingsFlag.VISITOR_ITEM_DROP);
        lookup.put(Material.GOLD_PLATE, SettingsFlag.PRESSURE_PLATE);
        lookup.put(Material.ICE, SettingsFlag.FIRE_EXTINGUISH);
        lookup.put(Material.IRON_SWORD, SettingsFlag.HURT_MONSTERS);
        lookup.put(Material.JUKEBOX, SettingsFlag.MUSIC);
        lookup.put(Material.LAVA_BUCKET, SettingsFlag.COLLECT_LAVA);
        lookup.put(Material.LEASH, SettingsFlag.LEASH);
        lookup.put(Material.LEVER, SettingsFlag.LEVER_BUTTON);
        lookup.put(Material.MILK_BUCKET, SettingsFlag.MILKING);
        lookup.put(Material.MOB_SPAWNER, SettingsFlag.MONSTER_SPAWN);
        lookup.put(Material.MONSTER_EGG, SettingsFlag.SPAWN_EGGS);
        lookup.put(Material.NETHERRACK, SettingsFlag.NETHER_PVP);
        lookup.put(Material.OBSIDIAN, SettingsFlag.PORTAL);
        lookup.put(Material.POTATO_ITEM, SettingsFlag.MOB_SPAWN);
        lookup.put(Material.POTION, SettingsFlag.ACID_DAMAGE);
        lookup.put(Material.REDSTONE_COMPARATOR, SettingsFlag.REDSTONE);
        lookup.put(Material.SHEARS, SettingsFlag.SHEARING);
        lookup.put(Material.SIGN, SettingsFlag.ENTER_EXIT_MESSAGES);
        lookup.put(Material.STONE, SettingsFlag.BREAK_BLOCKS);
        lookup.put(Material.TNT, SettingsFlag.CREEPER_PAIN);
        lookup.put(Material.TORCH, SettingsFlag.FIRE_SPREAD);
        lookup.put(Material.WATER_BUCKET, SettingsFlag.COLLECT_WATER);
        lookup.put(Material.WHEAT, SettingsFlag.CROP_TRAMPLE);
        lookup.put(Material.WOOD_DOOR, SettingsFlag.DOOR);
        lookup.put(Material.WOOD_SWORD, SettingsFlag.HURT_MOBS);
        lookup.put(Material.WORKBENCH, SettingsFlag.CRAFTING);
    }

    public SettingsPanel(ASkyBlock plugin) {
        this.plugin = plugin;
        //plugin.getLogger().info("DEBUG: Settings Panel loaded");
    }

    /**
     * Presents a GUI for toggling or viewing settings
     * @param player
     * @return
     */
    public Inventory islandGuardPanel(Player player) {
        UUID uuid = player.getUniqueId();
        // Get the island settings for this player's location
        Island island = plugin.getGrid().getProtectedIslandAt(player.getLocation());
        List<IPItem> ip = new ArrayList<IPItem>();
        Inventory newPanel = null;
        if (island == null) {
            //plugin.getLogger().info("DEBUG: default world settings");
            ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsGeneralTitle, plugin.myLocale(uuid).igsSettingsGeneralDesc));
            // General settings all enum            
            for (SettingsFlag flag : SettingsFlag.values()) {
                //plugin.getLogger().info("DEBUG: flag = " + flag.name());
                //plugin.getLogger().info("DEBUG: default setting = " + Settings.defaultWorldSettings.get(flag));
                if (flag.equals(SettingsFlag.ACID_DAMAGE) && Settings.acidDamage == 0)
                    continue;
                if (Settings.defaultWorldSettings.containsKey(flag) && lookup.inverse().containsKey(flag) && plugin.myLocale(uuid).igs.containsKey(flag)) {
                    ip.add(new IPItem(Settings.defaultWorldSettings.get(flag), lookup.inverse().get(flag), plugin.myLocale(uuid).igs.get(flag),uuid));
                }
            }
            // System settings that are visible to users
            ip.add(new IPItem(Settings.allowChestDamage, Material.CHEST, plugin.myLocale(uuid).igsChestDamage, uuid));
            ip.add(new IPItem(Settings.allowCreeperDamage, Material.SKULL_ITEM, 4, plugin.myLocale(uuid).igsCreeperDamage, uuid));
            ip.add(new IPItem(Settings.allowCreeperGriefing, Material.SKULL_ITEM, 4, plugin.myLocale(uuid).igsCreeperGriefing, uuid));
            ip.add(new IPItem(!Settings.restrictWither, Material.SKULL_ITEM, 1, plugin.myLocale(uuid).igsWitherDamage, uuid));
            ip.add(new IPItem(Settings.allowTNTDamage, Material.TNT, plugin.myLocale(uuid).igsTNT, uuid));
            ip.add(new IPItem(Settings.allowVisitorKeepInvOnDeath, Material.IRON_CHESTPLATE, plugin.myLocale(uuid).igsVisitorKeep, uuid));
        } else if (island.isSpawn()) {
            ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsSpawnTitle, plugin.myLocale(uuid).igsSettingsSpawnDesc));
            // Spawn settings
            for (SettingsFlag flag : Settings.defaultSpawnSettings.keySet()) {
                //plugin.getLogger().info("DEBUG: " + flag.toString());
                if (flag.equals(SettingsFlag.ACID_DAMAGE) && Settings.acidDamage == 0)
                    continue;
                if (lookup.inverse().containsKey(flag) && plugin.myLocale(uuid).igs.containsKey(flag)) {
                    ip.add(new IPItem(island.getIgsFlag(flag), lookup.inverse().get(flag), plugin.myLocale(uuid).igs.get(flag), uuid));
                }
            }
        } else {
            // Standard island
            //plugin.getLogger().info("DEBUG: Standard island");
            ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsIslandTitle, plugin.myLocale(uuid).igsSettingsIslandDesc));
            for (SettingsFlag flag : Settings.visitorSettings.keySet()) {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: visitor flag = " + flag);
                    plugin.getLogger().info("DEBUG: setting for island = " + island.getIgsFlag(flag));
                }
                if (flag.equals(SettingsFlag.ACID_DAMAGE) && Settings.acidDamage == 0)
                    continue;
                if (lookup.inverse().get(flag) != null) {
                    if (plugin.myLocale(uuid).igs.containsKey(flag)) {
                        //plugin.getLogger().info("DEBUG: Adding flag");
                        ip.add(new IPItem(island.getIgsFlag(flag), lookup.inverse().get(flag), plugin.myLocale(uuid).igs.get(flag), uuid));
                    }
                } else if (DEBUG) {
                    plugin.getLogger().severe("DEBUG: " + flag + " is missing an icon");
                }
            }
        }
        if (ip.size() > 0) {
            // Make sure size is a multiple of 9
            int size = ip.size() + 8;
            size -= (size % 9);
            String title = plugin.myLocale(uuid).igsTitle;
            if (title.length() > 32) {
                title = title.substring(0, 31);
            }
            newPanel = Bukkit.createInventory(null, size, title);
            // Fill the inventory and return
            int slot = 0;
            for (IPItem i : ip) {
                i.setSlot(slot);
                newPanel.addItem(i.getItem());
            }
        }	    
        return newPanel;
    }

    /**
     * Handle clicks to the Settings panel
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked(); // The player that clicked the item
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        if (inventory.getName() == null) {
            return;
        }
        int slot = event.getRawSlot();
        // Check this is the right panel
        if (!inventory.getName().equals(plugin.myLocale(player.getUniqueId()).igsTitle)) {
            return;
        }
        // Stop removal of items
        event.setCancelled(true);
        if (event.getSlotType() == SlotType.OUTSIDE) {
            player.closeInventory();
            inventory.clear();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            inventory.clear();
            player.updateInventory();
            return;
        }
        // Check world
        if (!player.getLocation().getWorld().equals(ASkyBlock.getIslandWorld()) && !player.getLocation().getWorld().equals(ASkyBlock.getNetherWorld())) {
            return;
        }
        // 1.7.x server
        if (!hasArmorStand && slot > lookup.size()) {
            return;
        }
        // 1.8.x server
        if (slot > (lookup.size() +1)) {
            return;
        }
        // Get the flag
        SettingsFlag flag = null;
        if (lookup.containsKey(event.getCurrentItem().getType())) {
            // All other items
            flag = lookup.get(event.getCurrentItem().getType());
        } else if (hasArmorStand && event.getCurrentItem().getType() == Material.ARMOR_STAND) {
            // Special handling to avoid errors on 1.7.x servers
            flag = SettingsFlag.ARMOR_STAND;
        }
        //plugin.getLogger().info("DEBUG: flag is " + flag);
        // If flag is null, do nothing
        if (flag == null) {
            return;
        }
        // Players can only do something if they own the island or are op
        Island island = plugin.getGrid().getIslandAt(player.getLocation());
        if (island != null && (player.isOp() || (island.getOwner() != null && island.getOwner().equals(player.getUniqueId())))) {
            //plugin.getLogger().info("DEBUG: Check perm " + flag.toString());
            // Check perms
            if (player.isOp() || player.hasPermission(Settings.PERMPREFIX + "settings." + flag.toString())) {
                //plugin.getLogger().info("DEBUG: Player has perm " + flag.toString());
                if (flag.equals(SettingsFlag.PVP) || flag.equals(SettingsFlag.NETHER_PVP)) {
                    // PVP always results in an inventory closure
                    player.closeInventory();
                    inventory.clear();
                    // Check if the player is allowed to toggle
                    // PVP activation
                    if (!island.getIgsFlag(flag)) {
                        // Attempt to activate PVP
                        //plugin.getLogger().info("DEBUG: attempt to activate PVP");
                        if (pvpCoolDown.containsKey(player.getUniqueId())) {
                            //plugin.getLogger().info("DEBUG: player is in the cooldown list");
                            long setTime = pvpCoolDown.get(player.getUniqueId());
                            //plugin.getLogger().info("DEBUG: set time is " + setTime);
                            long secondsLeft = Settings.pvpRestartCooldown - (System.currentTimeMillis() - setTime) / 1000;
                            //plugin.getLogger().info("DEBUG: seconds left = " + secondsLeft);
                            if (secondsLeft > 0) {
                                Util.sendMessage(player, ChatColor.RED + "You must wait " + secondsLeft + " seconds until you can do that again!");
                                return;
                            }
                            // Tidy up
                            pvpCoolDown.remove(player.getUniqueId());
                        }
                        // Warn players on the island
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            if (island.onIsland(p.getLocation())) {
                                if (flag.equals(SettingsFlag.NETHER_PVP)) {
                                    Util.sendMessage(p, ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(p.getUniqueId()).igs.get(SettingsFlag.NETHER_PVP) + " " + plugin.myLocale(p.getUniqueId()).igsAllowed);
                                } else {
                                    Util.sendMessage(p, ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(p.getUniqueId()).igs.get(SettingsFlag.PVP) + " " + plugin.myLocale(p.getUniqueId()).igsAllowed);
                                }

                                if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                    player.getWorld().playSound(player.getLocation(), Sound.valueOf("ARROW_HIT"), 1F, 1F);
                                } else {
                                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
                                }
                            }
                        }
                        // Toggle the flag
                        island.toggleIgs(flag); 
                        // Update warp signs
                        final List<UUID> members = island.getMembers();
                        // Run one tick later because text gets updated at the end of tick
                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                            @Override
                            public void run() {
                                for (UUID playerUUID : members) {
                                    plugin.getWarpPanel().updateWarp(playerUUID);
                                }

                            }});

                        return;
                    } else {
                        // PVP deactivation
                        // Store this deactivation time
                        pvpCoolDown.put(player.getUniqueId(), System.currentTimeMillis());
                        // Immediately toggle the setting
                        island.toggleIgs(flag);
                        // Update warp signs
                        final List<UUID> members = island.getMembers();
                        // Run one tick later because text gets updated at the end of tick
                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                            @Override
                            public void run() {
                                for (UUID playerUUID : members) {
                                    plugin.getWarpPanel().updateWarp(playerUUID);
                                }

                            }});
                        // Warn players of change
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            if (island.onIsland(p.getLocation())) {
                                // Deactivate PVP
                                if (flag.equals(SettingsFlag.NETHER_PVP)) {
                                    Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).igs.get(SettingsFlag.NETHER_PVP) + " " + plugin.myLocale(p.getUniqueId()).igsDisallowed);
                                } else {
                                    Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).igs.get(SettingsFlag.PVP) + " " + plugin.myLocale(p.getUniqueId()).igsDisallowed);
                                }
                                if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                    p.getWorld().playSound(p.getLocation(), Sound.valueOf("FIREWORK_TWINKLE"), 1F, 1F);
                                } else {
                                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_TWINKLE, 1F, 1F);
                                }

                            }
                        }
                    }
                } else {
                    island.toggleIgs(flag);
                }
            }
            //player.closeInventory();
            inventory.clear();
            player.openInventory(islandGuardPanel(player));
        }
    }
}
