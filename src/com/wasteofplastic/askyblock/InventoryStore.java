package com.wasteofplastic.askyblock;

import org.bukkit.inventory.ItemStack;

/**
 * @author tastybento
 * Where player inventories are stored
 */
public class InventoryStore {
    private ItemStack[] inventory;
    private ItemStack[] armor;

    /**
     * @param inventory
     * @param armor
     */
    protected InventoryStore(ItemStack[] inventory, ItemStack[] armor) {
	this.inventory = inventory;
	this.armor = armor;
    }
    /**
     * @return the inventory
     */
    protected ItemStack[] getInventory() {
        return inventory;
    }
    /**
     * @param inventory the inventory to set
     */
    protected void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }
    /**
     * @return the armor
     */
    protected ItemStack[] getArmor() {
        return armor;
    }
    /**
     * @param armor the armor to set
     */
    protected void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }
}
