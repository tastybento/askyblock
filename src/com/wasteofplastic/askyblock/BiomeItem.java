package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BiomeItem {
    private ItemStack item;
    private int slot;
    private double price;
    private String description;
    private String name;
    private boolean confirm;
    private Biome biome;
    /**
     * @param slot
     * @param cost
     * @param description
     * @param name
     */
    protected BiomeItem(Material material, int slot, double cost, String description, String name, boolean confirm, Biome biome) {
	this.slot = slot;
	this.price = cost;
	this.description = description;
	this.name = name;
	this.confirm = confirm;
	this.biome = biome;
	// Make the item(s)
	item = new ItemStack(material);
	// Set the description and price	    
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	List<String> Lore = new ArrayList<String>();
	Lore = Challenges.chop(ChatColor.YELLOW, description, 20);
	// Create price
	if (Settings.useEconomy && cost > 0D) {
	    Lore.add(VaultHelper.econ.format(cost));
	}
	meta.setLore(Lore);
	item.setItemMeta(meta);
    }
    /**
     * @return the item
     */
    protected ItemStack getItem() {
        return item;
    }
    /**
     * @return the slot
     */
    protected int getSlot() {
        return slot;
    }
    /**
     * @return the confirm
     */
    protected boolean isConfirm() {
        return confirm;
    }
    /**
     * @return the biome
     */
    protected Biome getBiome() {
        return biome;
    }
    /**
     * @return the name
     */
    protected String getName() {
        return name;
    }
    /**
     * @return the price
     */
    protected double getPrice() {
        return price;
    }
    /**
     * @param biome the biome to set
     */
    protected void setBiome(Biome biome) {
        this.biome = biome;
    }


}
