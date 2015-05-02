package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wasteofplastic.askyblock.schematics.Schematic;
import com.wasteofplastic.askyblock.util.Util;

/**
 * Schematics Panel Item
 * @author tastybento
 */
public class SPItem {
    private ItemStack item;
    private List<String> description = new ArrayList<String>();
    private String heading;
    private String name;
    private String perm;
    private int slot;

    /**
     * This constructor is for the default schematic/island
     * @param material
     * @param name
     * @param description
     * @param slot
     */
    public SPItem(Material material, String name, String description, int slot) {
	this.slot = slot;
	this.name = name;
	this.perm = "";
	this.heading = "";
	this.description.clear();
	item = new ItemStack(material);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	this.description.addAll(Util.chop(ChatColor.AQUA, description, 25));
	meta.setLore(this.description);
	item.setItemMeta(meta);
    }

    /**
     * This constructor is for schematics that will do something if chosen
     * @param material
     * @param perm
     * @param name
     * @param description
     * @param slot
     */
    public SPItem(Schematic schematic, int slot) {
	this.slot = slot;
	this.name = schematic.getName();
	this.perm = schematic.getPerm();
	this.heading = schematic.getHeading();
	this.description.clear();
	this.item = new ItemStack(schematic.getIcon());
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	// This neat bit of code makes a list out of the description split by new line character
	List<String> desc = new ArrayList<String>(Arrays.asList(schematic.getDescription().split("\\|")));
	this.description.addAll(desc);
	meta.setLore(this.description);
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

    public String getName() {
	return name;
    }

    /**
     * @return the perm
     */
    public String getPerm() {
	return perm;
    }

    /**
     * @return the heading
     */
    public String getHeading() {
        return heading;
    }


}