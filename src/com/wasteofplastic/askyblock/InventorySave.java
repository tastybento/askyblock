package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventorySave {
    private static InventorySave instance = new InventorySave();
    private HashMap<UUID,ItemStack[]> inventory;
    private HashMap<UUID,ItemStack[]> armor;
    /**
     * 
     */
    public InventorySave() {
	inventory = new HashMap<UUID, ItemStack[]>();
	armor = new HashMap<UUID, ItemStack[]>();
    }
    
    public void savePlayerInventory(Player player) {
	// Save the player's armor and things
	inventory.put(player.getUniqueId(), player.getInventory().getContents());
	armor.put(player.getUniqueId(), player.getInventory().getArmorContents());
    }
    
    public void loadPlayerInventory(Player player) {
	if (inventory.containsKey(player.getUniqueId())) {
	    player.getInventory().setContents(inventory.get(player.getUniqueId()));
	}
	if (armor.containsKey(player.getUniqueId())) {
	    player.getInventory().setArmorContents(armor.get(player.getUniqueId()));
	}
	inventory.remove(player.getUniqueId());
	armor.remove(player.getUniqueId());
    }
    
    public static InventorySave getInstance() {
	return instance;
    }
    
}
