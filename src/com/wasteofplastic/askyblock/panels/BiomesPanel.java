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

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class BiomesPanel implements Listener {
    private ASkyBlock plugin;
    private HashMap<UUID, List<BiomeItem>> biomeItems = new HashMap<UUID, List<BiomeItem>>();

    /**
     * @param plugin - ASkyBlock plugin object
     */
    public BiomesPanel(ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns a customized panel of available Biomes for the player
     * 
     * @param player
     * @return custom Inventory object
     */
    public Inventory getBiomePanel(Player player) {
        // Go through the available biomes and check permission

        int slot = 0;
        List<BiomeItem> items = new ArrayList<BiomeItem>();
        for (String biomeName : plugin.getConfig().getConfigurationSection("biomes").getKeys(false)) {
            // Check the biome is actually real
            try {
                Biome biome = Biome.valueOf(biomeName);
                // Check permission
                String permission = plugin.getConfig().getString("biomes." + biomeName + ".permission", "");
                if (permission.isEmpty() || VaultHelper.permission.has(player, permission)) {
                    // Build inventory item
                    // Get icon
                    String icon = plugin.getConfig().getString("biomes." + biomeName + ".icon", "SAPLING");
                    Material material = null;
                    try {
                        if (StringUtils.isNumeric(icon)) {
                            material = Material.getMaterial(Integer.parseInt(icon));
                        } else {
                            material = Material.valueOf(icon.toUpperCase());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error parsing biome icon value " + icon + ". Using default SAPLING.");
                        material = Material.SAPLING;
                    }
                    // Get cost
                    double cost = plugin.getConfig().getDouble("biomes." + biomeName + ".cost", Settings.biomeCost);
                    // Get friendly name
                    String name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("biomes." + biomeName + ".friendlyname", Util.prettifyText(biomeName)));
                    // Get description
                    String description = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("biomes." + biomeName + ".description", ""));
                    // Get confirmation or not
                    boolean confirm = plugin.getConfig().getBoolean("biomes." + biomeName + ".confirm", false);
                    // Add item to list
                    // plugin.getLogger().info("DEBUG: " + description + name +
                    // confirm);
                    BiomeItem item = new BiomeItem(material, slot++, cost, description, name, confirm, biome);
                    // Add to item list
                    items.add(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().severe("Could not recognize " + biomeName + " as valid Biome! Skipping...");
                plugin.getLogger().severe("For V1.9, some biome names do not exist anymore. Change config.yml to the latest.");
            }
        }
        // Now create the inventory panel
        if (items.size() > 0) {
            // Save the items for later retrieval when the player clicks on them
            biomeItems.put(player.getUniqueId(), items);
            // Make sure size is a multiple of 9
            int size = items.size() + 8;
            size -= (size % 9);
            Inventory newPanel = Bukkit.createInventory(null, size, plugin.myLocale().biomePanelTitle);
            // Fill the inventory and return
            for (BiomeItem i : items) {
                newPanel.addItem(i.getItem());
            }
            return newPanel;
        } else {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
            plugin.getLogger().warning("There are no biomes in config.yml so /island biomes will not work!");
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked(); // The player that
        // clicked the item
        UUID playerUUID = player.getUniqueId();
        Inventory inventory = event.getInventory(); // The inventory that was
        // clicked in
        int slot = event.getRawSlot();
        // Check this is the right panel
        if (inventory.getName() == null || !inventory.getName().equals(plugin.myLocale().biomePanelTitle)) {
            return;
        }
        if (slot == -999) {
            inventory.clear();
            player.closeInventory();
            event.setCancelled(true);
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            event.setCancelled(true);
            inventory.clear();
            player.closeInventory();
            player.updateInventory();
            return;
        }

        // Get the list of items for this player
        List<BiomeItem> thisPanel = biomeItems.get(player.getUniqueId());
        if (thisPanel == null) {
            inventory.clear();
            player.closeInventory();
            event.setCancelled(true);
            return;
        }
        if (slot >= 0 && slot < thisPanel.size()) {
            event.setCancelled(true);
            // plugin.getLogger().info("DEBUG: slot is " + slot);
            // Do something
            // Check this player has an island
            Island island = plugin.getGrid().getIsland(playerUUID);
            if (island == null) {
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
                return;
            }
            // Check ownership
            if (!island.getOwner().equals(playerUUID)) {
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).levelerrornotYourIsland);
                return; 
            }
            if (!plugin.getGrid().playerIsOnIsland(player)) {
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotOnIsland);
                return;
            }
            Biome biome = thisPanel.get(slot).getBiome();
            if (biome != null) {
                event.setCancelled(true);
                if (Settings.useEconomy) {
                    // Check cost
                    double cost = thisPanel.get(slot).getPrice();
                    if (cost > 0D) {
                        if (!VaultHelper.econ.has(player, Settings.worldName, cost)) {
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).minishopYouCannotAfford.replace("[description]", VaultHelper.econ.format(cost)));
                            return;
                        } else {
                            VaultHelper.econ.withdrawPlayer(player, Settings.worldName, cost);
                            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).biomeYouBought.replace("[cost]", VaultHelper.econ.format(cost)));
                        }
                    }
                }
            }
            inventory.clear();
            player.closeInventory(); // Closes the inventory
            // Actually set the biome
            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).biomePleaseWait);
            new SetBiome(plugin, island, biome, player);
        }
        return;
    }

}