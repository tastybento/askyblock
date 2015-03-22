package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Pair;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class Biomes implements Listener {
    private static ASkyBlock plugin = ASkyBlock.getPlugin();
    private static HashMap<UUID, List<BiomeItem>> biomeItems = new HashMap<UUID, List<BiomeItem>>();

    /**
     * Returns a customized panel of available Biomes for the player
     * 
     * @param player
     * @return custom Inventory object
     */
    public static Inventory getBiomePanel(Player player) {
	// Go through the available biomes and check permission

	int slot = 0;
	List<BiomeItem> items = new ArrayList<BiomeItem>();
	for (String biomeName : plugin.getConfig().getConfigurationSection("biomes").getKeys(false)) {
	    // Check the biome is actually real
	    try {
		Biome biome = Biome.valueOf(biomeName);
		// Check permission
		String permission = plugin.getConfig().getString("biomes." + biomeName + ".permission", "");
		if (permission.isEmpty() || VaultHelper.permission.has(player, permission)) {
		    // Build inventory item
		    // Get icon
		    String icon = plugin.getConfig().getString("biomes." + biomeName + ".icon", "SAPLING");
		    Material material = null;
		    try {
			material = Material.valueOf(icon);
		    } catch (Exception e) {
			plugin.getLogger().warning("Error parsing biome icon value " + icon + ". Using default SAPLING.");
			material = Material.SAPLING;
		    }
		    // Get cost
		    double cost = plugin.getConfig().getDouble("biomes." + biomeName + ".cost", Settings.biomeCost);
		    // Get friendly name
		    String name = plugin.getConfig().getString("biomes." + biomeName + ".friendlyname", Util.prettifyText(biomeName));
		    // Get description
		    String description = plugin.getConfig().getString("biomes." + biomeName + ".description", "");
		    // Get confirmation or not
		    boolean confirm = plugin.getConfig().getBoolean("biomes." + biomeName + ".confirm", false);
		    // Add item to list
		    // plugin.getLogger().info("DEBUG: " + description + name +
		    // confirm);
		    BiomeItem item = new BiomeItem(material, slot++, cost, description, name, confirm, biome);
		    // Add to item list
		    items.add(item);
		}
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not recognize " + biomeName + " as valid Biome! Skipping...");
	    }
	}
	// Now create the inventory panel
	if (items.size() > 0) {
	    // Save the items for later retrieval when the player clicks on them
	    biomeItems.put(player.getUniqueId(), items);
	    // Make sure size is a multiple of 9
	    int size = items.size() + 8;
	    size -= (size % 9);
	    Inventory newPanel = Bukkit.createInventory(null, size, plugin.myLocale().biomePanelTitle);
	    // Fill the inventory and return
	    for (BiomeItem i : items) {
		newPanel.addItem(i.getItem());
	    }
	    return newPanel;
	} else {
	    player.sendMessage(ChatColor.RED + plugin.myLocale().errorCommandNotReady);
	    plugin.getLogger().warning("There are no biomes in config.yml so /island biomes will not work!");
	}
	return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that
							// clicked the item
	UUID playerUUID = player.getUniqueId();
	Inventory inventory = event.getInventory(); // The inventory that was
						    // clicked in
	int slot = event.getRawSlot();
	// Check this is the right panel
	if (!inventory.getName().equals(plugin.myLocale().biomePanelTitle)) {
	    return;
	}
	if (slot == -999) {
	    player.closeInventory();
	    event.setCancelled(true);
	    return;
	}
	// Get the list of items for this player
	List<BiomeItem> thisPanel = biomeItems.get(player.getUniqueId());
	if (thisPanel == null) {
	    player.closeInventory();
	    event.setCancelled(true);
	    return;
	}
	if (slot >= 0 && slot < thisPanel.size()) {
	    event.setCancelled(true);
	    // plugin.getLogger().info("DEBUG: slot is " + slot);
	    // Do something
	    Biome biome = thisPanel.get(slot).getBiome();
	    if (biome != null) {
		event.setCancelled(true);
		if (Settings.useEconomy) {
		    // Check cost
		    double cost = thisPanel.get(slot).getPrice();
		    if (cost > 0D) {
			if (!VaultHelper.econ.has(player, cost)) {
			    player.sendMessage(ChatColor.RED + plugin.myLocale().minishopYouCannotAfford.replace("[description]", VaultHelper.econ.format(cost)));
			    return;
			} else {
			    VaultHelper.econ.withdrawPlayer(player, Settings.worldName, cost);
			    player.sendMessage(ChatColor.GREEN + plugin.myLocale().biomeYouBought.replace("[cost]", VaultHelper.econ.format(cost)));
			}
		    }
		}
	    }
	    player.closeInventory(); // Closes the inventory
	    // Actually set the biome
	    if (plugin.getPlayers().inTeam(playerUUID) && plugin.getPlayers().getTeamIslandLocation(playerUUID) != null) {
		setIslandBiome(plugin.getPlayers().getTeamIslandLocation(playerUUID), biome);
	    } else {
		setIslandBiome(plugin.getPlayers().getIslandLocation(player.getUniqueId()), biome);
	    }
	    player.sendMessage(ChatColor.GREEN + plugin.myLocale().biomeSet.replace("[biome]", thisPanel.get(slot).getName()));
	}
	return;
    }

    /**
     * Sets all blocks in an island to a specified biome type
     * 
     * @param islandLoc
     * @param biomeType
     */
    public boolean setIslandBiome(Location islandLoc, Biome biomeType) {
	final int islandX = islandLoc.getBlockX();
	final int islandZ = islandLoc.getBlockZ();
	final World world = islandLoc.getWorld();
	final int range = (int) Math.round((double) Settings.islandDistance / 2);
	List<Pair> chunks = new ArrayList<Pair>();
	try {
	    // Biomes only work in 2D, so there's no need to set every block in
	    // the island area
	    // However, we need to collect the chunks and push them out again
	    // getLogger().info("DEBUG: Protection range is = " +
	    // Settings.island_protectionRange);
	    for (int x = -range; x <= range; x++) {
		for (int z = -range; z <= range; z++) {
		    Location l = new Location(world, (islandX + x), 0, (islandZ + z));
		    final Pair chunkCoords = new Pair(l.getChunk().getX(), l.getChunk().getZ());
		    if (!chunks.contains(chunkCoords)) {
			chunks.add(chunkCoords);
		    }
		    // getLogger().info("DEBUG: Chunk saving  " +
		    // l.getChunk().getX() + "," + l.getChunk().getZ());
		    /*
		     * // Weird stuff going on here. Sometimes the location does
		     * not get created.
		     * if (l.getBlockX() != (islandX +x)) {
		     * getLogger().info("DEBUG: Setting " + (islandX + x) + ","
		     * + (islandZ + z));
		     * getLogger().info("DEBUG: disparity in x");
		     * }
		     * if (l.getBlockZ() != (islandZ +z)) {
		     * getLogger().info("DEBUG: Setting " + (islandX + x) + ","
		     * + (islandZ + z));
		     * getLogger().info("DEBUG: disparity in z");
		     * }
		     */
		    l.getBlock().setBiome(biomeType);
		}
	    }
	} catch (Exception noBiome) {
	    return false;
	}
	// Now do some adjustments based on the Biome
	switch (biomeType) {
	case MESA:
	case MESA_BRYCE:
	case DESERT:
	case JUNGLE:
	case SAVANNA:
	case SAVANNA_MOUNTAINS:
	case SAVANNA_PLATEAU:
	case SAVANNA_PLATEAU_MOUNTAINS:
	case SWAMPLAND:
	    // No ice or snow allowed
	    for (int y = islandLoc.getWorld().getMaxHeight(); y >= Settings.sea_level; y--) {
		for (int x = islandX - range; x <= islandX + range; x++) {
		    for (int z = islandZ - range; z <= islandZ + range; z++) {
			switch (world.getBlockAt(x, y, z).getType()) {
			case ICE:
			case SNOW:
			    world.getBlockAt(x, y, z).setType(Material.AIR);
			    break;
			default:
			}
		    }
		}
	    }
	    break;
	case HELL:
	    // No water or ice allowed
	    for (int y = islandLoc.getWorld().getMaxHeight(); y >= Settings.sea_level; y--) {
		for (int x = islandX - range; x <= islandX + range; x++) {
		    for (int z = islandZ - range; z <= islandZ + range; z++) {
			switch (world.getBlockAt(x, y, z).getType()) {
			case ICE:
			case WATER:
			case STATIONARY_WATER:
			case SNOW:
			    world.getBlockAt(x, y, z).setType(Material.AIR);
			    break;
			default:
			}
		    }
		}
	    }
	    break;
	case BEACH:
	    break;
	case BIRCH_FOREST:
	    break;
	case BIRCH_FOREST_HILLS:
	    break;
	case BIRCH_FOREST_HILLS_MOUNTAINS:
	    break;
	case BIRCH_FOREST_MOUNTAINS:
	    break;
	case COLD_BEACH:
	    break;
	case COLD_TAIGA:
	    break;
	case COLD_TAIGA_HILLS:
	    break;
	case COLD_TAIGA_MOUNTAINS:
	    break;
	case DEEP_OCEAN:
	    break;
	case DESERT_HILLS:
	    break;
	case DESERT_MOUNTAINS:
	    break;
	case EXTREME_HILLS:
	    break;
	case EXTREME_HILLS_MOUNTAINS:
	    break;
	case EXTREME_HILLS_PLUS:
	    break;
	case EXTREME_HILLS_PLUS_MOUNTAINS:
	    break;
	case FLOWER_FOREST:
	    break;
	case FOREST:
	    break;
	case FOREST_HILLS:
	    break;
	case FROZEN_OCEAN:
	    break;
	case FROZEN_RIVER:
	    break;
	case ICE_MOUNTAINS:
	    break;
	case ICE_PLAINS:
	    break;
	case ICE_PLAINS_SPIKES:
	    break;
	case JUNGLE_EDGE:
	    break;
	case JUNGLE_EDGE_MOUNTAINS:
	    break;
	case JUNGLE_HILLS:
	    break;
	case JUNGLE_MOUNTAINS:
	    break;
	case MEGA_SPRUCE_TAIGA:
	    break;
	case MEGA_SPRUCE_TAIGA_HILLS:
	    break;
	case MEGA_TAIGA:
	    break;
	case MEGA_TAIGA_HILLS:
	    break;
	case MUSHROOM_ISLAND:
	    break;
	case MUSHROOM_SHORE:
	    break;
	case OCEAN:
	    break;
	case PLAINS:
	    break;
	case RIVER:
	    break;
	case ROOFED_FOREST:
	    break;
	case ROOFED_FOREST_MOUNTAINS:
	    break;
	case SKY:
	    break;
	case SMALL_MOUNTAINS:
	    break;
	case STONE_BEACH:
	    break;
	case SUNFLOWER_PLAINS:
	    break;
	case SWAMPLAND_MOUNTAINS:
	    break;
	case TAIGA:
	    break;
	case TAIGA_HILLS:
	    break;
	case TAIGA_MOUNTAINS:
	    break;
	case MESA_PLATEAU:
	    break;
	case MESA_PLATEAU_FOREST:
	    break;
	case MESA_PLATEAU_FOREST_MOUNTAINS:
	    break;
	case MESA_PLATEAU_MOUNTAINS:
	    break;
	default:
	    break;
	}
	// Update chunks - does not do anything any more
	/*
	for (Pair p : chunks) {
	    islandLoc.getWorld().refreshChunk(p.getLeft(), p.getRight());
	    // plugin.getLogger().info("DEBUG: refreshing " + p.getLeft() + ","
	    // + p.getRight());
	}
	*/
	return true;
    }
}