package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventorySave {
    private static InventorySave instance = new InventorySave(ASkyBlock.getPlugin());
    private HashMap<UUID,HashMap<Location,InventoryStore>> inventories; 
    private ASkyBlock plugin;
    /**
     * Saves the inventory of a player
     */
    public InventorySave(ASkyBlock plugin) {
	this.plugin = plugin;
	inventories = new HashMap<UUID,HashMap<Location,InventoryStore>>();
    }

    public void savePlayerInventory(Player player, Location loc) {
	//plugin.getLogger().info("DEBUG: Saving inventory: loc = " + loc);
	// Save the player's armor and things
	InventoryStore store = new InventoryStore(player.getInventory().getContents(),player.getInventory().getArmorContents());
	HashMap<Location,InventoryStore> inv = new HashMap<Location,InventoryStore>();
	// Find out if we know about this player or not yet
	if (inventories.containsKey(player.getUniqueId())) {
	    // Yes we do, so just add this to the current hashmap
	    inv = inventories.get(player.getUniqueId());
	}
	// Add or replace
	inv.put(loc, store);
	inventories.put(player.getUniqueId(), inv);
    }

    /**
     * Loads the player's inventory for this location
     * @param player
     * @param loc
     */
    public void loadPlayerInventory(Player player, Location loc) {
	//plugin.getLogger().info("DEBUG: Loading inventory");
	// Get the info for this player
	if (inventories.containsKey(player.getUniqueId())) {
	    //plugin.getLogger().info("DEBUG: player is known");
	    HashMap<Location,InventoryStore> inv = inventories.get(player.getUniqueId());
	    //plugin.getLogger().info("DEBUG: size of inv = " + inv.size());
	    //plugin.getLogger().info("DEBUG: loc = " + loc);
	    // Check if the location exists
	    if (inv.containsKey(loc)) {
		//plugin.getLogger().info("DEBUG: Location is known");
		player.getInventory().setContents(inv.get(loc).getInventory());
		player.getInventory().setArmorContents(inv.get(loc).getArmor());
		inv.remove(loc);
		return;
	    } 
	}
	//plugin.getLogger().info("DEBUG: name or location not known, clearing inventory - nothing to load");
	// Else Clear - NO DO NOT CLEAR - this will clear all inventories if player dies and override server
	// setting keepInventory:true
	/*
	player.getInventory().clear();
	player.getInventory().setBoots(null);
	player.getInventory().setChestplate(null);
	player.getInventory().setHelmet(null);
	player.getInventory().setLeggings(null);
	*/
    }

    /**
     * @return the inventories
     */
    public HashMap<UUID, HashMap<Location, InventoryStore>> getInventories() {
        return inventories;
    }

    /**
     * Switches the player's inventory out for another
     * @param player
     * @param to
     */
    /*
    public void switchPlayerInventory(Player player, Location from, Location to) {
	//plugin.getLogger().info("DEBUG: from " + from + " to " + to);
	savePlayerInventory(player, from);
	loadPlayerInventory(player, to);
    } 
*/
    public static InventorySave getInstance() {
	return instance;
    }
    // For death keep inv

    public void savePlayerInventory(Player player) {
	this.savePlayerInventory(player, player.getLocation());
    }

    public void loadPlayerInventory(Player player) {
	this.loadPlayerInventory(player, player.getLocation());
    }

    public void removePlayer(UUID uniqueId) {
	this.inventories.remove(uniqueId);
    }

    public ItemStack[] getArmor(UUID uniqueId, Location coopIslands) {
	return this.inventories.get(uniqueId).get(coopIslands).getArmor();
    }
    
    public ItemStack[] getInventory(UUID uniqueId, Location coopIslands) {
	return this.inventories.get(uniqueId).get(coopIslands).getInventory();
    }
}
