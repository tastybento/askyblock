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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author tastybento
 *         This class is for a control panel button that has an icon, a command
 *         to run if pressed or a link to
 *         another control panel.
 */
public class CPItem {

    private ItemStack item;
    private String command;
    private String nextSection;

    /**
     * @param item
     * @param material
     * @param name
     * @param command
     * @param nextSection
     */
    public CPItem(Material material, String name, String command, String nextSection) {
	this.command = command;
	this.nextSection = nextSection;
	item = new ItemStack(material);
	ItemMeta meta = item.getItemMeta();
	// Handle multi line names (split by |)
	List<String> desc = new ArrayList<String>(Arrays.asList(name.split("\\|")));
	meta.setDisplayName(desc.get(0));
	if (desc.size() > 1) {
	    desc.remove(0); // Remove the name
	    meta.setLore(desc);
	}
	item.setItemMeta(meta);
    }

    public CPItem(ItemStack itemStack, String name, String command, String nextSection) {
	this.command = command;
	this.nextSection = nextSection;
	this.item = itemStack;
	ItemMeta meta = item.getItemMeta();
	// Handle multi line names (split by |)
	List<String> desc = new ArrayList<String>(Arrays.asList(name.split("\\|")));
	meta.setDisplayName(desc.get(0));
	if (desc.size() > 1) {
	    desc.remove(0); // Remove the name
	    meta.setLore(desc);
	}
	item.setItemMeta(meta);
    }

    // For warps
    public CPItem(ItemStack itemStack, String command) {
	this.command = command;
	this.nextSection = "";
	this.item = itemStack;
    }    

    public void setLore(List<String> lore) {
	ItemMeta meta = item.getItemMeta();
	meta.setLore(lore);
	item.setItemMeta(meta);
    }

    /**
     * @return the command
     */
    public String getCommand() {
	return command;
    }

    /**
     * @return the nextSection
     */
    public String getNextSection() {
	return nextSection;
    }

    /**
     * @param nextSection
     *            the nextSection to set
     */
    public void setNextSection(String nextSection) {
	this.nextSection = nextSection;
    }

    public ItemStack getItem() {
	return item;
    }

}
