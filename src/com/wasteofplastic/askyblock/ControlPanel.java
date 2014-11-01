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
package com.wasteofplastic.askyblock;

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
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author ben
 * Provides a handy control panel and minishop
 */
public class ControlPanel implements Listener {

    private static YamlConfiguration miniShopFile;
    private static HashMap<Integer, MiniShopItem> store = new HashMap<Integer,MiniShopItem>();
    private static YamlConfiguration cpFile;
    private ASkyBlock plugin;
    private static boolean allowSelling;
    private static String defaultPanelName;


    /**
     * @param plugin
     */
    public ControlPanel(ASkyBlock plugin) {
	this.plugin = plugin;
	loadShop();
	loadControlPanel();
    }


    /**
     * Map of panel contents by name
     */
    private static HashMap<String, HashMap<Integer,CPItem>> panels = new HashMap<String, HashMap<Integer,CPItem>>();
    //public static final Inventory challenges = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Challenges");

    /**
     * Map of CP inventories by name
     */
    public static HashMap<String,Inventory> controlPanel = new HashMap<String,Inventory>();

    public static final Inventory miniShop = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&',Locale.islandMiniShopTitle));
    // The first parameter, is the inventory owner. I make it null to let everyone use it.
    //The second parameter, is the slots in a inventory. Must be a multiple of 9. Can be up to 54.
    //The third parameter, is the inventory name. This will accept chat colors.


    /**
     * This loads the minishop from the minishop.yml file
     */
    public static void loadShop() {
	//The first parameter is the Material, then the durability (if wanted), slot, descriptions
	// Minishop
	store.clear();
	miniShopFile = ASkyBlock.loadYamlFile("minishop.yml");
	allowSelling = miniShopFile.getBoolean("config.allowselling", false);
	ConfigurationSection items = miniShopFile.getConfigurationSection("items");
	ASkyBlock plugin = ASkyBlock.getPlugin();
	//plugin.getLogger().info("DEBUG: loading the shop. items = " + items.toString());
	if (items != null) {
	    // Run through items
	    int slot = 0;
	    for (String item : items.getKeys(false)) {
		try {
		    String m = items.getString(item + ".material");
		    //plugin.getLogger().info("Material = " + m);
		    Material material = Material.matchMaterial(m);
		    int quantity = items.getInt(item + ".quantity", 0);
		    String extra = items.getString(item + ".extra", "");
		    double price = items.getDouble(item + ".price",-1D);
		    double sellPrice = items.getDouble(item + ".sellprice",-1D);
		    if (!allowSelling) {
			sellPrice = -1;
		    }
		    String description = items.getString(item + ".description");
		    MiniShopItem shopItem = new MiniShopItem(material,extra,slot,description,quantity,price,sellPrice);
		    store.put(slot, shopItem);
		    miniShop.setItem(slot, shopItem.getItem());
		    slot++;
		    if (slot > 8) {
			break;
		    }
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
	cpFile = ASkyBlock.loadYamlFile("controlpanel.yml");
	ConfigurationSection controlPanels = cpFile.getRoot();
	if (controlPanels == null) {
	    plugin.getLogger().severe("Controlpanel.yml is corrupted! Delete so it can be regenerated or fix!");
	    return;
	}	
	// Go through the yml file and create inventories and panel maps
	for (String panel : controlPanels.getKeys(false)) {
	    //plugin.getLogger().info("DEBUG: Panel " + panel);
	    ConfigurationSection panelConf = cpFile.getConfigurationSection(panel);
	    // New panel map
	    HashMap<Integer,CPItem> cp = new HashMap<Integer,CPItem>();
	    String panelName = ChatColor.translateAlternateColorCodes('&',panelConf.getString("panelname", "Commands"));
	    if (panel.equalsIgnoreCase("default")) {
		defaultPanelName = panelName;
	    }
	    //plugin.getLogger().info("DEBUG: Panel section " + panelName);
	    // New inventory
	    Inventory newPanel = Bukkit.createInventory(null, 9, panelName);
	    if (newPanel == null) {
		//plugin.getLogger().info("DEBUG: new panel is null!");
	    }
	    // Add inventory to map of inventories
	    controlPanel.put(newPanel.getName(),newPanel);
	    //plugin.getLogger().info("DEBUG: putting panel " + newPanel.getName());
	    ConfigurationSection buttons = cpFile.getConfigurationSection(panel + ".buttons");
	    if (buttons != null) {
		// Run through buttons
		int slot = 0;
		for (String item : buttons.getKeys(false)) {
		    try {
			String m = buttons.getString(item + ".material","AIR");
			//plugin.getLogger().info("Material = " + m);
			Material material = Material.matchMaterial(m);
			String description = buttons.getString(item + ".description","");
			String command = buttons.getString(item + ".command","");
			String nextSection = buttons.getString(item + ".nextsection","");
			CPItem cpItem = new CPItem(material,description,command,nextSection);
			cp.put(slot, cpItem);
			newPanel.setItem(slot, cpItem.getItem());
			slot++;
			if (slot > 8) {
			    break;
			}
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


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	// Check world
	if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	ItemStack clicked = event.getCurrentItem(); // The item that was clicked
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	//ASkyBlock plugin = ASkyBlock.getPlugin();
	int slot = event.getRawSlot();
	// Check control panels
	for (String panelName : controlPanel.keySet()) {
	    if (inventory.getName().equals(panelName)) {
		//plugin.getLogger().info("DEBUG: panels length " + panels.size());
		//plugin.getLogger().info("DEBUG: panel name " + panelName);
		if (slot == -999) {
		    player.closeInventory();
		    event.setCancelled(true);
		    return;
		}
		HashMap<Integer, CPItem> thisPanel = panels.get(panelName);
		if (slot >= 0 && slot < thisPanel.size()) {
		    //plugin.getLogger().info("DEBUG: slot is " + slot);
		    // Do something
		    String command = thisPanel.get(slot).getCommand();
		    String nextSection = ChatColor.translateAlternateColorCodes('&',thisPanel.get(slot).getNextSection());
		    if (!command.isEmpty()) {
			player.closeInventory(); // Closes the inventory
			event.setCancelled(true);
			//plugin.getLogger().info("DEBUG: performing command " + command);
			player.performCommand(command);
			return;
		    }
		    if (!nextSection.isEmpty()) {
			player.closeInventory(); // Closes the inventory
			Inventory next = controlPanel.get(nextSection);
			if (next == null) {
			    //plugin.getLogger().info("DEBUG: next panel is null");
			}
			//plugin.getLogger().info("DEBUG: opening next cp "+nextSection);
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
	if (inventory.getName().equals(Locale.challengesguiTitle)) {
	    event.setCancelled(true);
	    if (slot == -999) {
		player.closeInventory();
		return;
	    }

	    // Get the list of items in this inventory
	    //plugin.getLogger().info("You clicked on slot " + slot);
	    List<CPItem> challenges = plugin.getChallenges().getCP(player);
	    if (slot >=0 && slot < challenges.size()) {
		CPItem item = challenges.get(slot);
		// Check that it is the top items that are bing clicked on
		if (clicked.equals(item.getItem())) {
		    //plugin.getLogger().info("You clicked on a challenge item");
		    //plugin.getLogger().info("performing  /" + item.getCommand());
		    if (item.getCommand() != null) {
			player.performCommand(item.getCommand());
			player.closeInventory();
			player.openInventory(plugin.getChallenges().challengePanel(player));
		    }
		}
	    }
	}
	/*
	 * Minishop section
	 */	
	if (inventory.getName().equals(miniShop.getName())) { // The inventory is our custom Inventory
	    String message = "";
	    //plugin.getLogger().info("You clicked on slot " + slot);
	    event.setCancelled(true); // Don't let them pick it up
	    if (slot == -999) {
		player.closeInventory();
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
			    if (!VaultHelper.econ.has(player, player.getWorld().getName(), item.getPrice())) {
				//message = "You cannot afford that item!";
				message = (Locale.minishopYouCannotAfford).replace("[description]", item.getDescription());
			    } else {
				EconomyResponse r = VaultHelper.econ.withdrawPlayer(player, player.getWorld().getName(), item.getPrice());
				if (r.transactionSuccess()) {
				    //message = "You bought " + item.getQuantity() + " " + item.getDescription() + " for " + VaultHelper.econ.format(item.getPrice());			
				    message = Locale.minishopYouBought.replace("[number]", Integer.toString(item.getQuantity()));
				    message = message.replace("[description]", item.getDescription());
				    message = message.replace("[price]", VaultHelper.econ.format(item.getPrice()));
				    player.getInventory().addItem(item.getItemClean());
				} else {
				    //message = "There was a problem puchasing that item: " + r.errorMessage;
				    message = (Locale.minishopBuyProblem).replace("[description]", item.getDescription());
				}
			    }
			}
		    } else if (event.getClick().equals(ClickType.RIGHT) && allowSelling && item.getSellPrice()>0D) {
			// Check if they have the item
			if (player.getInventory().containsAtLeast(item.getItemClean(),item.getQuantity())) {
			    player.getInventory().removeItem(item.getItemClean());
			    VaultHelper.econ.depositPlayer(player, item.getSellPrice());
			    //message = "You sold " + item.getQuantity() + " " + item.getDescription() + " for " + VaultHelper.econ.format(item.getSellPrice());
			    message = Locale.minishopYouSold.replace("[number]", Integer.toString(item.getQuantity()));
			    message = message.replace("[description]", item.getDescription());
			    message = message.replace("[price]", VaultHelper.econ.format(item.getSellPrice()));
			} else {
			    //message = "You do not have enough of that item to sell it.";
			    message = (Locale.minishopSellProblem).replace("[description]", item.getDescription());;
			}
		    }
		    //player.closeInventory(); // Closes the inventory
		    if (!message.isEmpty()) {
			player.sendMessage(message);	
		    }
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