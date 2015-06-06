package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.util.Util;

/**
 * @author tastybento
 *         Info panel item
 */
public class IPItem {
    private ItemStack item;
    private List<String> description = new ArrayList<String>();
    private String name;
    private int slot;
    private boolean flagValue;
    private Type type;

    public enum Type {
	INFO, TOGGLE
    };

    // Info icons
    public IPItem(Material material, String name, String description) {
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

    public IPItem(boolean flagValue, Material material) {
	createToggleableItem(flagValue, material, 0, Util.prettifyText(material.toString()) + " use");
    }
    
    public IPItem(boolean flagValue, Material material, int durability) {
	createToggleableItem(flagValue, material, durability, Util.prettifyText(material.toString()) + " use");
    }

    public IPItem(boolean flagValue, Material material, String name) {
	createToggleableItem(flagValue, material, 0, name);
    }
    
    public IPItem(boolean flagValue, Material material, int durability, String name) {
	createToggleableItem(flagValue, material, durability, name);
    }

    private void createToggleableItem(boolean flagValue, Material material, int durability, String name) {
	this.flagValue = flagValue;
	this.slot = -1;
	this.name = name;
	this.type = Type.TOGGLE;
	description.clear();
	item = new ItemStack(material);
	item.setDurability((short)durability);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	if (flagValue) {
	    description.add(ChatColor.GREEN + ASkyBlock.getPlugin().myLocale().igsAllowed);
	} else {
	    description.add(ChatColor.RED + ASkyBlock.getPlugin().myLocale().igsDisallowed);
	}
	meta.setLore(description);
	item.setItemMeta(meta);
    }

    public void setLore(List<String> lore) {
	ItemMeta meta = item.getItemMeta();
	meta.setLore(lore);
	item.setItemMeta(meta);
    }

    public ItemStack getItem() {
	return item;
    }

    /**
     * @return the slot
     */
    public int getSlot() {
	return slot;
    }

    public void setSlot(int slot) {
	this.slot = slot;
    }

    /**
     * @return the flagValue
     */
    public boolean isFlagValue() {
	return flagValue;
    }

    /**
     * @param flagValue
     *            the flagValue to set
     */
    public void setFlagValue(boolean flagValue) {
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

    public String getName() {
	return name;
    }

    /**
     * @return the type
     */
    public Type getType() {
	return type;
    }

}