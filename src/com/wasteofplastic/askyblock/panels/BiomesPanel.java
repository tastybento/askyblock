package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class BiomesPanel implements Listener {
    private ASkyBlock plugin;
    private HashMap<UUID, List<BiomeItem>> biomeItems = new HashMap<UUID, List<BiomeItem>>();


    /**
     * @param plugin
     */
    public BiomesPanel(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * Returns a customized panel of available Biomes for the player
     * 
     * @param player
     * @return custom Inventory object
     */
    public Inventory getBiomePanel(Player player) {
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
			if (StringUtils.isNumeric(icon)) {
			    material = Material.getMaterial(Integer.parseInt(icon));
			} else {
			    material = Material.valueOf(icon.toUpperCase());
			}
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
			if (!VaultHelper.econ.has(player, Settings.worldName, cost)) {
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
    public boolean setIslandBiome(final Location islandLoc, final Biome biomeType) {
	final Island island = plugin.getGrid().getIslandAt(islandLoc);
	if (island != null) {
	    island.getCenter().getBlock().setBiome(biomeType);
	    // Get a snapshot of the island
	    final World world = island.getCenter().getWorld();
	    // If the biome is dry, then we need to remove the water, ice, snow, etc.
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
	    case HELL:
		// Get the chunks
		//plugin.getLogger().info("DEBUG: get the chunks");
		List<ChunkSnapshot> chunkSnapshot = new ArrayList<ChunkSnapshot>();
		for (int x = island.getMinProtectedX() /16; x <= (island.getMinProtectedX() + island.getProtectionSize() - 1)/16; x++) {
		    for (int z = island.getMinProtectedZ() /16; z <= (island.getMinProtectedZ() + island.getProtectionSize() - 1)/16; z++) {
			chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
		    }  
		}
		//plugin.getLogger().info("DEBUG: size of chunk ss = " + chunkSnapshot.size());
		final List<ChunkSnapshot> finalChunk = chunkSnapshot;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

		    @SuppressWarnings("deprecation")
		    @Override
		    public void run() {
			//System.out.println("DEBUG: running async task");
			HashMap<Vector,Integer> blocksToRemove = new HashMap<Vector, Integer>();
			// Go through island space and find the offending columns
			for (ChunkSnapshot chunk: finalChunk) {
			    for (int x = 0; x< 16; x++) {
				for (int z = 0; z < 16; z++) {
				    // Check if it is snow, ice or water
				    for (int yy = world.getMaxHeight()-1; yy >= Settings.sea_level; yy--) {
					int type = chunk.getBlockTypeId(x, yy, z);
					if (type == Material.ICE.getId() || type == Material.SNOW.getId() || type == Material.SNOW_BLOCK.getId()
						|| type == Material.WATER.getId() || type == Material.STATIONARY_WATER.getId()) {
					    //System.out.println("DEBUG: offending block found " + Material.getMaterial(type) + " @ " + (chunk.getX()*16 + x) + " " + yy + " " + (chunk.getZ()*16 + z));
					    blocksToRemove.put(new Vector(chunk.getX()*16 + x,yy,chunk.getZ()*16 + z), type);
					} else if (type != Material.AIR.getId()){
					    // Hit a non-offending block so break and store this column of vectors
					    break;
					}
				    }
				}
			    }
			}
			// Now get rid of the blocks
			if (!blocksToRemove.isEmpty()) {
			    final HashMap<Vector, Integer> blocks = blocksToRemove;
			    // Kick of a sync task
			    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

				@Override
				public void run() {
				    //plugin.getLogger().info("DEBUG: Running sync task");
				    for (Entry<Vector, Integer> entry: blocks.entrySet()) {
					if (entry.getValue() == Material.WATER.getId() || entry.getValue() == Material.STATIONARY_WATER.getId()) {
					    if (biomeType.equals(Biome.HELL)) {
						// Remove water from Hell	
						entry.getKey().toLocation(world).getBlock().setType(Material.AIR);
					    }
					} else {
					    entry.getKey().toLocation(world).getBlock().setType(Material.AIR);
					}
				    }
				}});
			}
		    }});
	    default:
	    }
	    return true;
	} else {
	    return false; 
	}
    }

    /**
     * Ensures that any block when loaded will match the biome of the center column of the island
     * if it exists. Does not apply to spawn.
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChunkLoad(ChunkLoadEvent e) {
	// Only affects overworld
	if (!e.getWorld().equals(ASkyBlock.getIslandWorld())) {
	    return;
	}
	Island island = plugin.getGrid().getIslandAt(e.getChunk().getX()*16, e.getChunk().getZ()*16);
	if (island != null && !island.isSpawn()) {
	    Biome biome = island.getCenter().getBlock().getBiome();
	    for (int x = 0; x< 16; x++) {
		for (int z = 0; z< 16; z++) {
		    // Set biome
		    e.getChunk().getBlock(x, 0, z).setBiome(biome);
		}
	    }
	}
    }

}