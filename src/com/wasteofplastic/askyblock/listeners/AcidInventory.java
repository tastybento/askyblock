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

package com.wasteofplastic.askyblock.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockIterator;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

/**
 * @author tastybento
 *         This listener will check to see if a player has a water bucket and if
 *         so change it to acid bucket
 *         It also checks for interactions with water bottles
 */
public class AcidInventory implements Listener {
    private final ASkyBlock plugin;
    private List<String> lore = new ArrayList<String>();

    public AcidInventory(ASkyBlock aSkyBlock) {
	plugin = aSkyBlock;
    }

    /**
     * This covers items in a chest, etc. inventory, then change the name then
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent e) {
	// plugin.getLogger().info("Inventory open event called");
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    Inventory inventory = e.getInventory();
	    if (Settings.acidDamage == 0D) {
		return;
	    }
	    // If this is the minishop - forget it
	    if (inventory.getName() != null && inventory.getName().equalsIgnoreCase(plugin.myLocale(e.getPlayer().getUniqueId()).islandMiniShopTitle)) {
		return;
	    }
	    if (inventory.contains(Material.WATER_BUCKET)) {
		// plugin.getLogger().info("Inventory contains water bucket");
		ItemStack[] inv = inventory.getContents();
		for (ItemStack item : inv) {
		    if (item != null) {
			// plugin.getLogger().info(item.toString());
			if (item.getType() == Material.WATER_BUCKET) {
			    // plugin.getLogger().info("Found it!");
			    ItemMeta meta = item.getItemMeta();
			    meta.setDisplayName(plugin.myLocale(e.getPlayer().getUniqueId()).acidBucket);
			    lore = Arrays.asList(plugin.myLocale(e.getPlayer().getUniqueId()).acidLore.split("\n"));
			    meta.setLore(lore);
			    item.setItemMeta(meta);
			}
		    }
		}
	    } else if (inventory.contains(Material.POTION)) {
		// plugin.getLogger().info("Inventory contains water bottle");
		ItemStack[] inv = inventory.getContents();
		for (ItemStack item : inv) {
		    if (item != null) {
			// plugin.getLogger().info(item.toString());
			if (item.getType() == Material.POTION && item.getDurability() == 0) {
			    // plugin.getLogger().info("Found it!");
			    ItemMeta meta = item.getItemMeta();
			    meta.setDisplayName(plugin.myLocale(e.getPlayer().getUniqueId()).acidBottle);
			    meta.setLore(lore);
			    item.setItemMeta(meta);
			}
		    }
		}
	    }
	}
    }

    /**
     * If the player filled up the bucket themselves
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent e) {
	// plugin.getLogger().info("Player filled the bucket");
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // plugin.getLogger().info("Correct world");
	    if (Settings.acidDamage > 0D) {
		ItemStack item = e.getItemStack();
		if (item.getType().equals(Material.WATER_BUCKET)) {
		    ItemMeta meta = item.getItemMeta();
		    meta.setDisplayName(plugin.myLocale(e.getPlayer().getUniqueId()).acidBucket);
		    meta.setLore(lore);
		    item.setItemMeta(meta);
		}
	    }
	}
    }

    /**
     * Checks to see if a player is drinking acid
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onWaterBottleDrink(final PlayerItemConsumeEvent e) {
	if (Settings.acidDamage == 0D)
	    return;
	// plugin.getLogger().info(e.getEventName() + " called for " +
	// e.getItem().getType().toString());
	if (e.getItem().getType().equals(Material.POTION) && e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (e.getItem().getDurability() == 0) {
		plugin.getLogger().info(e.getPlayer().getName() + " " + plugin.myLocale().drankAcidAndDied);
		if (!Settings.muteDeathMessages) {
		    for (Player p : plugin.getServer().getOnlinePlayers()) {
			p.sendMessage(e.getPlayer().getDisplayName() + " " + plugin.myLocale(p.getUniqueId()).drankAcid);
		    }
		}
		final ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
		e.getPlayer().setItemInHand(item);
		e.getPlayer().setHealth(0D);
		e.setCancelled(true);
	    }
	}
    }

    /**
     * This event makes sure that any acid bottles become potions without the
     * warning
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBrewComplete(final BrewEvent e) {
	if (e.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // plugin.getLogger().info("DEBUG: Brew Event called");
	    BrewerInventory inv = e.getContents();
	    int i = 0;
	    for (ItemStack item : inv.getContents()) {
		if (item != null) {
		    // Remove lore
		    ItemMeta meta = item.getItemMeta();
		    // plugin.getLogger().info("DEBUG: " +
		    // meta.getDisplayName());
		    meta.setDisplayName(null);
		    meta.setLore(null);
		    item.setItemMeta(null);
		    inv.setItem(i, item);
		}
		i++;
	    }
	}
    }

    /**
     * Event that covers filling a bottle
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onWaterBottleFill(final PlayerInteractEvent e) {
	Player player = e.getPlayer();
	if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName))
	    return;
	if (Settings.acidDamage == 0D)
	    return;
	if (!player.getItemInHand().getType().equals(Material.GLASS_BOTTLE)) {
	    return;
	}
	// plugin.getLogger().info(e.getEventName() + " called");
	// Look at what the player was looking at
	BlockIterator iter = new BlockIterator(player, 10);
	Block lastBlock = iter.next();
	while (iter.hasNext()) {
	    lastBlock = iter.next();
	    if (lastBlock.getType() == Material.AIR)
		continue;
	    break;
	}
	// plugin.getLogger().info(lastBlock.getType().toString());
	if (lastBlock.getType().equals(Material.WATER) || lastBlock.getType().equals(Material.STATIONARY_WATER)
		|| lastBlock.getType().equals(Material.CAULDRON)) {
	    // They *may* have filled a bottle with water
	    // Check inventory for POTIONS in a tick
	    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
		@Override
		public void run() {
		    // plugin.getLogger().info("Checking inventory");
		    PlayerInventory inv = e.getPlayer().getInventory();
		    if (inv.contains(Material.POTION)) {
			// plugin.getLogger().info("POTION in inventory");
			// They have a POTION of some kind in inventory
			int i = 0;
			for (ItemStack item : inv.getContents()) {
			    if (item != null) {
				// plugin.getLogger().info(i + ":" +
				// item.getType().toString());
				if (item.getType().equals(Material.POTION) && item.getDurability() == 0) {
				    // plugin.getLogger().info("Water bottle found!");
				    ItemMeta meta = item.getItemMeta();
				    meta.setDisplayName(plugin.myLocale(e.getPlayer().getUniqueId()).acidBottle);
				    // ArrayList<String> lore = new
				    // ArrayList<String>(Arrays.asList("Poison",
				    // "Beware!", "Do not drink!"));
				    meta.setLore(lore);
				    item.setItemMeta(meta);
				    inv.setItem(i, item);
				}
			    }
			    i++;
			}
		    }
		}
	    });
	}

    }
}