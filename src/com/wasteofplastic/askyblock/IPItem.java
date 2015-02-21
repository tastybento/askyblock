package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author tastybento
 * Info panel item
 */
public class IPItem {
    private ItemStack item;
    private List<String> description = new ArrayList<String>();
    private String name;
    private int slot;
    private boolean flagValue;
    private Type type;
    protected enum Type {INFO, TOGGLE};

    // Info icons
    protected IPItem(Material material, String name, String description) {
	this.flagValue = false;
	this.slot = -1;
	this.name = name;
	this.type = Type.INFO;
	this.description.clear();
	item = new ItemStack(material);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	this.description.addAll(Util.chop(ChatColor.AQUA, description, 25));
	meta.setLore(this.description);
	item.setItemMeta(meta);
    }

    protected IPItem(boolean flagValue, Material material) {
	createToggleableItem(flagValue, material, ASkyBlock.prettifyText(material.toString()) + " use");
    }
    
    protected IPItem(boolean flagValue, Material material, String name) {
	createToggleableItem(flagValue, material, name);
    }

    private void createToggleableItem(boolean flagValue, Material material, String name) {
	this.flagValue = flagValue;
	this.slot = -1;
	this.name = name;
	this.type = Type.TOGGLE;
	description.clear();
	item = new ItemStack(material);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	if (flagValue) {
	    description.add(ChatColor.GREEN + "Allowed");
	} else {
	    description.add(ChatColor.RED + "Disallowed");	    
	}
	meta.setLore(description);
	item.setItemMeta(meta);	
    }
    
    protected void setLore(List<String> lore) {
	ItemMeta meta = item.getItemMeta();
	meta.setLore(lore);
	item.setItemMeta(meta);
    }

    protected ItemStack getItem() {
	return item;
    }

    /**
     * @return the slot
     */
    protected int getSlot() {
	return slot;
    }

    protected void setSlot(int slot) {
	this.slot = slot;
    }
    /**
     * @return the flagValue
     */
    protected boolean isFlagValue() {
	return flagValue;
    }

    /**
     * @param flagValue the flagValue to set
     */
    protected void setFlagValue(boolean flagValue) {
	this.flagValue = flagValue;
	description.clear();
	ItemMeta meta = item.getItemMeta();
	if (flagValue) {
	    description.add(ChatColor.GREEN + "Allowed by all");
	} else {
	    description.add(ChatColor.RED + "Disallowed for visitors");	    
	}
	meta.setLore(description);
	item.setItemMeta(meta);
    }

    protected String getName() {
	return name;
    }

    /**
     * @return the type
     */
    protected Type getType() {
	return type;
    }

}