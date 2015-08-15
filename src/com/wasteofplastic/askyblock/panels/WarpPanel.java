package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

public class WarpPanel implements Listener {
    private ASkyBlock plugin;
    private List<Inventory> warpPanel;

    /**
     * @param plugin
     */
    public WarpPanel(ASkyBlock plugin) {
	this.plugin = plugin;
	warpPanel = new ArrayList<Inventory>();
	updatePanel();
    }

    /**
     * This needs to be called if a warp is added or deleted
     */
    public void updatePanel() {
	warpPanel.clear();
	int panelSize = 45; // Must be a multiple of 9
	// Create the warp panels
	Collection<UUID> warps = plugin.getWarpSignsListener().listSortedWarps();
	//plugin.getLogger().info("DEBUG: warps size = " + warps.size());
	int panelNumber = warps.size() / (panelSize-2);
	int remainder = (warps.size() % (panelSize-2)) + 8 + 2;
	remainder -= (remainder % 9);
	//plugin.getLogger().info("DEBUG: panel number = " + panelNumber + " remainder = " + remainder);
	int i = 0;
	// TODO: Make panel title a string
	for (i = 0; i < panelNumber; i++) {
	    //plugin.getLogger().info("DEBUG: created panel " + (i+1));
	    warpPanel.add(Bukkit.createInventory(null, panelSize, plugin.myLocale().warpsTitle + " #" + (i+1)));
	}
	// Make the last panel
	//plugin.getLogger().info("DEBUG: created panel " + (i+1));
	warpPanel.add(Bukkit.createInventory(null, remainder, plugin.myLocale().warpsTitle + " #" + (i+1)));
	panelNumber = 0;
	int slot = 0;
	int count = 0;
	// Add this buttons to each panel
	for (UUID playerUUID : warps) {
	    count++;
	    // Make a head if the player is known
	    String playerName = plugin.getServer().getOfflinePlayer(playerUUID).getName();
	    if (playerName != null) {
		ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
		meta.setOwner(playerName);
		meta.setDisplayName(playerName);
		//get the sign info
		Location signLocation = plugin.getWarpSignsListener().getWarp(playerUUID);
		//plugin.getLogger().info("DEBUG: " + playerName + ": block type = " + signLocation.getBlock().getType());
		if (signLocation.getBlock().getType().equals(Material.SIGN_POST) || signLocation.getBlock().getType().equals(Material.WALL_SIGN)) {
		    Sign sign = (Sign)signLocation.getBlock().getState();
		    List<String> lines = Arrays.asList(sign.getLines());
		    meta.setLore(lines);
		    //plugin.getLogger().info("DEBUG: " + playerName + ": lines = " + lines);
		} 
		/* else // TEST CODE
		    if (signLocation.getBlock().getType().equals(Material.AIR)) {
			signLocation.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
			signLocation.getBlock().setType(Material.SIGN_POST);
			Sign sign = (Sign)signLocation.getBlock().getState();
			for (int line = 0; line < 4; line++) {
			    sign.setLine(line, "Line #" + line);
			}
			sign.update();
		    }*/
		playerSkull.setItemMeta(meta);
		// Add item to the panel
		//plugin.getLogger().info("DEBUG: adding item to panel number = " + panelNumber + " slot = " + slot);
		CPItem newButton = new CPItem(playerSkull, Settings.ISLANDCOMMAND + " warp " + playerName);
		warpPanel.get(panelNumber).setItem(slot++, newButton.getItem());
	    } else {
		// Just make a blank space
		//warpPanel.get(panelNumber).setItem(slot, new ItemStack(Material.AIR));
		// TEST CODE
		ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
		meta.setDisplayName("#" + count);
		playerSkull.setItemMeta(meta);
		warpPanel.get(panelNumber).setItem(slot++,playerSkull);
	    }
	    // Check if the panel is full
	    if (slot == panelSize-2) {
		// Add navigation buttons
		if (panelNumber > 0) {
		    warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.SIGN,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
		}
		warpPanel.get(panelNumber).setItem(slot, new CPItem(Material.SIGN,plugin.myLocale().warpsNext,"warps " + (panelNumber+1),"").getItem());
		// Move onto the next panel
		panelNumber++;
		slot = 0;
	    } 
	}
	if (remainder != 0 && panelNumber > 0) {
	    warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.SIGN,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
	}
    }

    public Inventory getWarpPanel(int panelNumber) {
	//makePanel();
	if (panelNumber < 0) {
	    panelNumber = 0;
	} else if (panelNumber > warpPanel.size()-1) {
	    panelNumber = warpPanel.size()-1;
	}
	return warpPanel.get(panelNumber);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	String title = inventory.getTitle();
	if (!inventory.getTitle().startsWith(plugin.myLocale().warpsTitle + " #")) {
	    return;
	}
	// The player that clicked the item
	Player player = (Player) event.getWhoClicked();
	event.setCancelled(true);
	if (event.getSlotType().equals(SlotType.OUTSIDE)) {
	    player.closeInventory();
	    return;
	}
	ItemStack clicked = event.getCurrentItem(); // The item that was clicked
	//plugin.getLogger().info("DEBUG: inventory size = " + inventory.getSize());
	//plugin.getLogger().info("DEBUG: clicked = " + clicked);
	//plugin.getLogger().info("DEBUG: rawslot = " + event.getRawSlot());
	if (event.getRawSlot() >= event.getInventory().getSize() || clicked.getType() == Material.AIR) {
	    return;
	}
	int panelNumber = 0;
	try {
	    panelNumber = Integer.valueOf(title.substring(title.indexOf('#')+ 1));
	} catch (Exception e) {
	    panelNumber = 0;
	}
	String command = clicked.getItemMeta().getDisplayName();
	//plugin.getLogger().info("DEBUG: command = " + command);
	if (command != null) {
	    if (command.equalsIgnoreCase(plugin.myLocale().warpsNext)) {
		player.closeInventory();
		player.performCommand(Settings.ISLANDCOMMAND + " warps " + (panelNumber+1));
	    } else if (command.equalsIgnoreCase(plugin.myLocale().warpsPrevious)) {
		player.closeInventory();
		player.performCommand(Settings.ISLANDCOMMAND + " warps " + (panelNumber-1));
	    } else {
		player.closeInventory();
		player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpswarpToPlayersSign.replace("<player>", command));
		player.performCommand(Settings.ISLANDCOMMAND + " warp " + command);
	    }
	}
    }
}
