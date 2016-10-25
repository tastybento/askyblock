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

import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.events.MiniShopEvent;
import com.wasteofplastic.askyblock.events.MiniShopEvent.TransactionType;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides a handy control panel and minishop
 */
public class ControlPanel implements Listener {

    private static YamlConfiguration miniShopFile;
    private static HashMap<Integer, MiniShopItem> store = new HashMap<Integer, MiniShopItem>();
    private static YamlConfiguration cpFile;
    private ASkyBlock plugin;
    private static boolean allowSelling;
    private static String defaultPanelName;
    private static final boolean DEBUG = false;

    /**
     * @param plugin
     */
    public ControlPanel(ASkyBlock plugin) {
        this.plugin = plugin;
        if (Settings.useEconomy) {
            loadShop();
        }
        loadControlPanel();
    }

    /**
     * Map of panel contents by name
     */
    private static HashMap<String, HashMap<Integer, CPItem>> panels = new HashMap<String, HashMap<Integer, CPItem>>();
    // public static final Inventory challenges = Bukkit.createInventory(null,
    // 9, ChatColor.YELLOW + "Challenges");

    /**
     * Map of CP inventories by name
     */
    public static HashMap<String, Inventory> controlPanel = new HashMap<String, Inventory>();

    public static Inventory miniShop;

    // The first parameter, is the inventory owner. I make it null to let
    // everyone use it.
    // The second parameter, is the slots in a inventory. Must be a multiple of
    // 9. Can be up to 54.
    // The third parameter, is the inventory name. This will accept chat colors.

    /**
     * This loads the minishop from the minishop.yml file
     */
    public static void loadShop() {
        // The first parameter is the Material, then the durability (if wanted),
        // slot, descriptions
        // Minishop
        store.clear();
        miniShopFile = Util.loadYamlFile("minishop.yml");
        allowSelling = miniShopFile.getBoolean("config.allowselling", false);
        ConfigurationSection items = miniShopFile.getConfigurationSection("items");
        ASkyBlock plugin = ASkyBlock.getPlugin();
        if (DEBUG)
            plugin.getLogger().info("DEBUG: loading the shop. items = " + items.toString());
        if (items != null) {
            // Create the store
            // Get how many the store should be
            int size = items.getKeys(false).size() + 8;
            size -= (size % 9);
            miniShop = Bukkit.createInventory(null, size, plugin.myLocale().islandMiniShopTitle);
            // Run through items
            int slot = 0;
            for (String item : items.getKeys(false)) {
                try {
                    String m = items.getString(item + ".material");
                    // plugin.getLogger().info("Material = " + m);
                    Material material = Material.matchMaterial(m);
                    int quantity = items.getInt(item + ".quantity", 0);
                    String extra = items.getString(item + ".extra", "");
                    double price = items.getDouble(item + ".price", -1D);
                    double sellPrice = items.getDouble(item + ".sellprice", -1D);
                    if (!allowSelling) {
                        sellPrice = -1;
                    }
                    String description = ChatColor.translateAlternateColorCodes('&',items.getString(item + ".description",""));
                    MiniShopItem shopItem = new MiniShopItem(material, extra, slot, description, quantity, price, sellPrice);
                    store.put(slot, shopItem);
                    miniShop.setItem(slot, shopItem.getItem());
                    slot++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Problem loading minishop item #" + slot);
                    plugin.getLogger().warning(e.getMessage());
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * This loads the control panel from the controlpanel.yml file
     */
    public static void loadControlPanel() {
        ASkyBlock plugin = ASkyBlock.getPlugin();
        // Map of known panel contents by name
        panels.clear();
        // Map of panel inventories by name
        controlPanel.clear();
        cpFile = Util.loadYamlFile("controlpanel.yml");
        ConfigurationSection controlPanels = cpFile.getRoot();
        if (controlPanels == null) {
            plugin.getLogger().severe("Controlpanel.yml is corrupted! Delete so it can be regenerated or fix!");
            return;
        }
        // Go through the yml file and create inventories and panel maps
        for (String panel : controlPanels.getKeys(false)) {
            // plugin.getLogger().info("DEBUG: Panel " + panel);
            ConfigurationSection panelConf = cpFile.getConfigurationSection(panel);
            // New panel map
            HashMap<Integer, CPItem> cp = new HashMap<Integer, CPItem>();
            String panelName = ChatColor.translateAlternateColorCodes('&', panelConf.getString("panelname", "Commands"));
            if (panel.equalsIgnoreCase("default")) {
                defaultPanelName = panelName;
            }
            // plugin.getLogger().info("DEBUG: Panel section " + panelName);
            // plugin.getLogger().info("DEBUG: putting panel " +
            // newPanel.getName());
            ConfigurationSection buttons = cpFile.getConfigurationSection(panel + ".buttons");
            if (buttons != null) {
                // Get how many buttons can be in the CP
                int size = buttons.getKeys(false).size() + 8;
                size -= (size % 9);
                // Add inventory to map of inventories
                controlPanel.put(panelName, Bukkit.createInventory(null, size, panelName));
                // Run through buttons
                int slot = 0;
                for (String item : buttons.getKeys(false)) {
                    try {
                        String m = buttons.getString(item + ".material", "BOOK");
                        // Split off damage
                        String[] icon = m.split(":");
                        // plugin.getLogger().info("Material = " + m);
                        Material material = Material.matchMaterial(icon[0]);
                        String description = ChatColor.translateAlternateColorCodes('&',buttons.getString(item + ".description", ""));
                        String command = buttons.getString(item + ".command", "").replace("[island]", Settings.ISLANDCOMMAND);
                        String nextSection = buttons.getString(item + ".nextsection", "");
                        ItemStack i = new ItemStack(material);
                        if (icon.length == 2) {
                            i.setDurability(Short.parseShort(icon[1]));
                        }
                        CPItem cpItem = new CPItem(i, description, command, nextSection);
                        cp.put(slot, cpItem);
                        controlPanel.get(panelName).setItem(slot, cpItem.getItem());
                        slot++;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Problem loading control panel " + panel + " item #" + slot);
                        plugin.getLogger().warning(e.getMessage());
                        e.printStackTrace();
                    }
                }
                // Add overall control panel
                panels.put(panelName, cp);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG:" + event.getEventName());
        Player player = (Player) event.getWhoClicked(); // The player that
        // clicked the item
        ItemStack clicked = event.getCurrentItem(); // The item that was clicked
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        if (inventory.getName() == null) {
            return;
        }
        // ASkyBlock plugin = ASkyBlock.getPlugin();
        int slot = event.getRawSlot();
        // Challenges
        if (inventory.getName().equals(plugin.myLocale(player.getUniqueId()).challengesguiTitle)) {
            event.setCancelled(true);
            if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
                inventory.clear();
                player.closeInventory();
                player.updateInventory();
                return;
            }
            if (event.getSlotType() == SlotType.OUTSIDE) {
                inventory.clear();
                player.closeInventory();
                return;
            }

            // Get the list of items in this inventory
            // plugin.getLogger().info("DEBUG: You clicked on slot " + slot);
            List<CPItem> challenges = plugin.getChallenges().getCP(player);
            if (challenges == null) {
                plugin.getLogger().warning("Player was accessing Challenge Inventory, but it had lost state - was server restarted?");
                inventory.clear();
                player.closeInventory();
                player.performCommand(Settings.CHALLENGECOMMAND);
                return;
            }
            // plugin.getLogger().info("DEBUG: Challenges size = " +
            // challenges.size());
            if (slot >= 0 && slot < challenges.size()) {
                CPItem item = challenges.get(slot);
                // TEST
                /*
                ItemStack icon = new ItemStack(Material.POTION);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName("Name");
                List<String> lore = new ArrayList<String>();
                lore.add("Lore 1");
                lore.add("Lore 2");
                meta.setLore(lore);
                icon.setItemMeta(meta);
                ItemStack click = new ItemStack(Material.POTION);
                plugin.getLogger().info("DEBUG: icon is " + icon.toString());
                plugin.getLogger().info("DEBUG: click is " + click.toString());
                if (click.equals(icon)) {
                    plugin.getLogger().info("DEBUG same");
                } else {
                    plugin.getLogger().info("DEBUG not same");
                   
                }*/
                // END TEST
                //plugin.getLogger().info("DEBUG: CP Item is " + item.getItem().toString());
                //plugin.getLogger().info("DEBUG: Clicked is " + clicked.toString());
                // Check that it is the top items that are being clicked on
                // These two should be identical because it is made before
                if (clicked.equals(item.getItem())) {
                    // plugin.getLogger().info("DEBUG: You clicked on a challenge item");
                    // plugin.getLogger().info("DEBUG: performing  /" +
                    // item.getCommand());
                    // plugin.getLogger().info("DEBUG: going to " +
                    // item.getNextSection());
                    // Next section indicates the level of panel to open
                    if (item.getNextSection() != null) {
                        inventory.clear();
                        player.closeInventory();
                        player.openInventory(plugin.getChallenges().challengePanel(player, item.getNextSection()));
                    } else if (item.getCommand() != null) {
                        player.performCommand(item.getCommand());
                        inventory.clear();
                        player.closeInventory();
                        player.openInventory(plugin.getChallenges().challengePanel(player));
                    }
                }
            }
            return;
        }
        /*
         * Minishop section
         */
        if (miniShop != null && inventory.getName().equals(miniShop.getName())) { // The
            // inventory
            // is
            // our
            // custom
            // Inventory
            String message = "";
            // plugin.getLogger().info("You clicked on slot " + slot);
            event.setCancelled(true); // Don't let them pick it up
            if (!Settings.useEconomy || slot == -999) {
                player.closeInventory();
                return;
            }
            if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {                    
                player.closeInventory();
                player.updateInventory();
                return;
            }
            if (store.containsKey(slot)) {
                // We have a winner!
                MiniShopItem item = store.get(slot);
                if (clicked.equals(item.getItem())) {
                    // Check what type of click - LEFT = BUY, RIGHT = sell
                    if (event.getClick().equals(ClickType.LEFT)) {
                        // Check if item is for sale
                        if (item.getPrice() > 0D) {
                            // Check they can afford it
                            if (!VaultHelper.econ.has(player, Settings.worldName, item.getPrice())) {
                                // message = "You cannot afford that item!";
                                message = (plugin.myLocale().minishopYouCannotAfford).replace("[description]", item.getDescription());
                            } else {
                                EconomyResponse r = VaultHelper.econ.withdrawPlayer(player, Settings.worldName, item.getPrice());
                                if (r.transactionSuccess()) {
                                    // message = "You bought " +
                                    // item.getQuantity() + " " +
                                    // item.getDescription() + " for " +
                                    // VaultHelper.econ.format(item.getPrice());
                                    message = plugin.myLocale().minishopYouBought.replace("[number]", Integer.toString(item.getQuantity()));
                                    message = message.replace("[description]", item.getDescription());
                                    message = message.replace("[price]", VaultHelper.econ.format(item.getPrice()));
                                    player.getInventory().addItem(item.getItemClean());
                                    // Fire event
                                    MiniShopEvent shopEvent = new MiniShopEvent(player.getUniqueId(), item, TransactionType.BUY);
                                    plugin.getServer().getPluginManager().callEvent(shopEvent);
                                } else {
                                    // message =
                                    // "There was a problem puchasing that item: "
                                    // + r.errorMessage;
                                    message = (plugin.myLocale().minishopBuyProblem).replace("[description]", item.getDescription());
                                }
                            }
                        }
                    } else if (event.getClick().equals(ClickType.RIGHT) && allowSelling && item.getSellPrice() > 0D) {
                        // Check if they have the item
                        if (player.getInventory().containsAtLeast(item.getItemClean(), item.getQuantity())) {
                            player.getInventory().removeItem(item.getItemClean());
                            VaultHelper.econ.depositPlayer(player, Settings.worldName, item.getSellPrice());
                            // message = "You sold " + item.getQuantity() + " "
                            // + item.getDescription() + " for " +
                            // VaultHelper.econ.format(item.getSellPrice());
                            message = plugin.myLocale().minishopYouSold.replace("[number]", Integer.toString(item.getQuantity()));
                            message = message.replace("[description]", item.getDescription());
                            message = message.replace("[price]", VaultHelper.econ.format(item.getSellPrice()));
                         // Fire event
                            MiniShopEvent shopEvent = new MiniShopEvent(player.getUniqueId(), item, TransactionType.SELL);
                            plugin.getServer().getPluginManager().callEvent(shopEvent);
                        } else {
                            // message =
                            // "You do not have enough of that item to sell it.";
                            message = (plugin.myLocale().minishopSellProblem).replace("[description]", item.getDescription());
                            ;
                        }
                    }
                    // player.closeInventory(); // Closes the inventory
                    if (!message.isEmpty()) {
                        player.sendMessage(message);
                    }
                }
            }
            return;
        }
        // Check control panels
        for (String panelName : controlPanel.keySet()) {
            if (inventory.getName().equals(panelName)) {
                event.setCancelled(true);
                // plugin.getLogger().info("DEBUG: panels length " +
                // panels.size());
                // plugin.getLogger().info("DEBUG: panel name " + panelName);
                if (slot == -999) {
                    player.closeInventory();
                    return;
                }
                if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {                    
                    player.closeInventory();
                    player.updateInventory();
                    return;
                }
                HashMap<Integer, CPItem> thisPanel = panels.get(panelName);
                if (slot >= 0 && slot < thisPanel.size()) {
                    // plugin.getLogger().info("DEBUG: slot is " + slot);
                    // Do something
                    String command = thisPanel.get(slot).getCommand();
                    String nextSection = ChatColor.translateAlternateColorCodes('&', thisPanel.get(slot).getNextSection());
                    if (!command.isEmpty()) {
                        player.closeInventory(); // Closes the inventory
                        event.setCancelled(true);
                        // plugin.getLogger().info("DEBUG: performing command "
                        // + command);
                        player.performCommand(command);
                        return;
                    }
                    if (!nextSection.isEmpty()) {
                        player.closeInventory(); // Closes the inventory
                        Inventory next = controlPanel.get(nextSection);
                        if (next == null) {
                            // plugin.getLogger().info("DEBUG: next panel is null");
                        }
                        // plugin.getLogger().info("DEBUG: opening next cp "+nextSection);
                        player.openInventory(next);
                        event.setCancelled(true);
                        return;
                    }
                    player.closeInventory(); // Closes the inventory
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * @return the defaultPanelName
     */
    public static String getDefaultPanelName() {
        return defaultPanelName;
    }
}