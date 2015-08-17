package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.schematics.Schematic;

public class SchematicsPanel implements Listener {
    private ASkyBlock plugin;
    private HashMap<UUID, List<SPItem>> schematicItems = new HashMap<UUID, List<SPItem>>();

    /**
     * @param plugin
     */
    public SchematicsPanel(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * Returns a customized panel of available Schematics for the player
     * 
     * @param player
     * @return custom Inventory object
     */
    public Inventory getPanel(Player player) {
	// Go through the available schematics for this player
	int slot = 0;
	List<SPItem> items = new ArrayList<SPItem>();
	List<Schematic> availableSchems = plugin.getIslandCmd().getSchematics(player, false);
	// Add an info icon
	//items.add(new SPItem(Material.MAP,"Choose your island", "Pick from the selection...",slot++));
	// Generate additional available schematics
	for (Schematic schematic : availableSchems) {
	    if (schematic.isVisible()) {
		items.add(new SPItem(schematic, slot++));
	    }
	}
	//plugin.getLogger().info("DEBUG: there are " + items.size() + " in the panel");
	// Now create the inventory panel
	if (items.size() > 0) {
	    // Save the items for later retrieval when the player clicks on them
	    schematicItems.put(player.getUniqueId(), items);
	    // Make sure size is a multiple of 9
	    int size = items.size() + 8;
	    size -= (size % 9);
	    Inventory newPanel = Bukkit.createInventory(null, size, plugin.myLocale(player.getUniqueId()).schematicsTitle);
	    // Fill the inventory and return
	    for (SPItem i : items) {
		newPanel.setItem(i.getSlot(), i.getItem());
	    }
	    return newPanel;
	} else {
	    player.sendMessage(ChatColor.RED + plugin.myLocale().errorCommandNotReady);
	}
	return null;
    }

    /**
     * Handles when the schematics panel is actually clicked
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that
	// clicked the item
	Inventory inventory = event.getInventory(); // The inventory that was
	// clicked in
	int slot = event.getRawSlot();
	// Check this is the right panel
	if (!inventory.getName().equals(plugin.myLocale(player.getUniqueId()).schematicsTitle)) {
	    return;
	}
	if (slot == -999) {
	    player.closeInventory();
	    event.setCancelled(true);
	    return;
	}
	// Get the list of items for this player
	List<SPItem> thisPanel = schematicItems.get(player.getUniqueId());
	if (thisPanel == null) {
	    player.closeInventory();
	    event.setCancelled(true);
	    return;
	}
	if (slot >= 0 && slot < thisPanel.size()) {
	    event.setCancelled(true);
	    // plugin.getLogger().info("DEBUG: slot is " + slot);
	    player.closeInventory(); // Closes the inventory
	    // Get the item clicked
	    SPItem item = thisPanel.get(slot);
	    // Do something
	    player.performCommand(Settings.ISLANDCOMMAND + " make " + item.getHeading());
	}
	return;
    }
}