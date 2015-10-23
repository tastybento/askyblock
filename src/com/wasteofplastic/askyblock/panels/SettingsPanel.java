package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.Flags;
import com.wasteofplastic.askyblock.Settings;

public class SettingsPanel implements Listener {
    // Island Guard Settings Panel
    private ASkyBlock plugin;
    private Class<?> clazz;
    private HashMap<UUID,Long> pvpCoolDown = new HashMap<UUID,Long>();

    private static HashMap<Material,Flags> lookup = new HashMap<Material,Flags>();
    static {
	lookup.put( Material.ANVIL,Flags.allowAnvilUse);
	lookup.put( Material.BEACON,Flags.allowBeaconAccess);
	lookup.put( Material.BED,Flags.allowBedUse);
	lookup.put( Material.STONE,Flags.allowBreakBlocks);
	lookup.put( Material.CARROT_ITEM,Flags.allowBreeding);
	lookup.put( Material.BREWING_STAND_ITEM,Flags.allowBrewing);
	lookup.put( Material.LAVA_BUCKET,Flags.allowBucketUse);
	lookup.put( Material.CHEST,Flags.allowChestAccess);
	lookup.put( Material.WORKBENCH,Flags.allowCrafting);
	lookup.put( Material.WHEAT,Flags.allowCropTrample);
	lookup.put( Material.WOOD_DOOR,Flags.allowDoorUse);
	lookup.put( Material.ENCHANTMENT_TABLE,Flags.allowEnchanting);
	lookup.put( Material.ENDER_PEARL,Flags.allowEnderPearls);
	lookup.put( Material.FURNACE,Flags.allowFurnaceUse);
	lookup.put( Material.FENCE_GATE,Flags.allowGateUse);
	lookup.put( Material.GOLD_BARDING,Flags.allowHorseInvAccess);
	lookup.put( Material.DIAMOND_BARDING,Flags.allowHorseRiding);
	lookup.put( Material.EGG,Flags.allowHurtMobs);
	lookup.put( Material.LEASH,Flags.allowLeashUse);
	lookup.put( Material.LEVER,Flags.allowLeverButtonUse);
	lookup.put( Material.JUKEBOX,Flags.allowMusic);
	lookup.put( Material.DIRT,Flags.allowPlaceBlocks);
	lookup.put( Material.OBSIDIAN,Flags.allowPortalUse);
	lookup.put( Material.GOLD_PLATE,Flags.allowPressurePlate);
	lookup.put( Material.ARROW,Flags.allowPvP);
	lookup.put( Material.NETHERRACK,Flags.allowNetherPvP);
	lookup.put( Material.REDSTONE_COMPARATOR,Flags.allowRedStone);
	lookup.put( Material.SHEARS,Flags.allowShearing);
    }

    public SettingsPanel(ASkyBlock plugin) {
	this.plugin = plugin;
	try {
	    clazz = Class.forName("org.bukkit.entity.ArmorStand");
	} catch (Exception e) {
	    clazz = null;
	}
	//plugin.getLogger().info("DEBUG: Settings Panel loaded");
    }

    public Inventory islandGuardPanel(Player player) {
	UUID uuid = player.getUniqueId();
	// Get the island settings for this player's location
	Island island = plugin.getGrid().getProtectedIslandAt(player.getLocation());
	List<IPItem> ip = new ArrayList<IPItem>();
	Inventory newPanel = null;
	if (island == null) {
	    ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsGeneralTitle, plugin.myLocale(uuid).igsSettingsGeneralDesc));
	    // General settings
	    ip.add(new IPItem(Settings.allowAnvilUse, Material.ANVIL, plugin.myLocale(uuid).igsAnvil));
	    if (clazz != null) {
		ip.add(new IPItem(Settings.allowArmorStandUse, Material.ARMOR_STAND, plugin.myLocale(uuid).igsArmorStand));
	    }
	    ip.add(new IPItem(Settings.allowBeaconAccess, Material.BEACON, plugin.myLocale(uuid).igsBeacon));
	    ip.add(new IPItem(Settings.allowBedUse, Material.BED, plugin.myLocale(uuid).igsBed));
	    ip.add(new IPItem(Settings.allowBreakBlocks, Material.STONE, plugin.myLocale(uuid).igsBreakBlocks));
	    ip.add(new IPItem(Settings.allowBreeding, Material.CARROT_ITEM, plugin.myLocale(uuid).igsBreeding));
	    ip.add(new IPItem(Settings.allowBrewing, Material.BREWING_STAND_ITEM, plugin.myLocale(uuid).igsBrewing));
	    ip.add(new IPItem(Settings.allowBucketUse, Material.LAVA_BUCKET, plugin.myLocale(uuid).igsBucket));
	    ip.add(new IPItem(Settings.allowChestAccess, Material.CHEST, plugin.myLocale(uuid).igsChest));
	    ip.add(new IPItem(Settings.allowChestDamage, Material.CHEST, plugin.myLocale(uuid).igsChestDamage));
	    ip.add(new IPItem(Settings.allowCrafting, Material.WORKBENCH, plugin.myLocale(uuid).igsWorkbench));
	    ip.add(new IPItem(Settings.allowCreeperDamage, Material.SKULL_ITEM, 4, plugin.myLocale(uuid).igsCreeperDamage));
	    ip.add(new IPItem(Settings.allowCreeperGriefing, Material.SKULL_ITEM, 4, plugin.myLocale(uuid).igsCreeperGriefing));
	    ip.add(new IPItem(Settings.allowCropTrample, Material.WHEAT, plugin.myLocale(uuid).igsCropTrampling));
	    ip.add(new IPItem(Settings.allowDoorUse, Material.WOOD_DOOR, plugin.myLocale(uuid).igsDoor));
	    ip.add(new IPItem(Settings.allowEnchanting, Material.ENCHANTMENT_TABLE, plugin.myLocale(uuid).igsEnchanting));
	    ip.add(new IPItem(Settings.allowEnderPearls, Material.ENDER_PEARL,plugin.myLocale(uuid).igsEnderPearl));
	    ip.add(new IPItem(Settings.allowFurnaceUse, Material.FURNACE, plugin.myLocale(uuid).igsFurnace));
	    ip.add(new IPItem(Settings.allowGateUse, Material.FENCE_GATE, plugin.myLocale(uuid).igsGate));
	    ip.add(new IPItem(Settings.allowHorseInvAccess, Material.GOLD_BARDING, plugin.myLocale(uuid).igsHorseInvAccess));
	    ip.add(new IPItem(Settings.allowHorseRiding, Material.DIAMOND_BARDING, plugin.myLocale(uuid).igsHorseRiding));
	    ip.add(new IPItem(Settings.allowHurtMobs, Material.EGG, plugin.myLocale(uuid).igsHurtAnimals));
	    ip.add(new IPItem(Settings.allowLeashUse, Material.LEASH, plugin.myLocale(uuid).igsLeash));
	    ip.add(new IPItem(Settings.allowLeverButtonUse, Material.LEVER, plugin.myLocale(uuid).igsLever));
	    ip.add(new IPItem(Settings.allowMusic, Material.JUKEBOX, plugin.myLocale(uuid).igsJukebox));
	    ip.add(new IPItem(Settings.allowPlaceBlocks, Material.DIRT, plugin.myLocale(uuid).igsPlaceBlocks));
	    ip.add(new IPItem(Settings.allowPortalUse, Material.OBSIDIAN, plugin.myLocale(uuid).igsPortalUse));
	    ip.add(new IPItem(Settings.allowPressurePlate, Material.GOLD_PLATE, plugin.myLocale(uuid).igsPressurePlate));
	    ip.add(new IPItem(Settings.allowPvP, Material.ARROW, plugin.myLocale(uuid).igsPVP));
	    ip.add(new IPItem(Settings.allowNetherPvP, Material.NETHERRACK, plugin.myLocale(uuid).igsNetherPVP));
	    ip.add(new IPItem(Settings.allowRedStone, Material.REDSTONE_COMPARATOR, plugin.myLocale(uuid).igsRedstone));
	    ip.add(new IPItem(!Settings.restrictWither, Material.SKULL_ITEM, 1, plugin.myLocale(uuid).igsWitherDamage));
	    ip.add(new IPItem(Settings.allowShearing, Material.SHEARS, plugin.myLocale(uuid).igsShears));
	    ip.add(new IPItem(Settings.allowTNTDamage, Material.TNT, plugin.myLocale(uuid).igsTNT));
	    ip.add(new IPItem(Settings.allowVisitorItemDrop, Material.GOLD_INGOT, plugin.myLocale(uuid).igsVisitorDrop));
	    ip.add(new IPItem(Settings.allowVisitorItemPickup, Material.DIAMOND, plugin.myLocale(uuid).igsVisitorPickUp));
	    ip.add(new IPItem(Settings.allowVisitorKeepInvOnDeath, Material.IRON_CHESTPLATE, plugin.myLocale(uuid).igsVisitorKeep));

	} else if (island.isSpawn()) {
	    ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsSpawnTitle, plugin.myLocale(uuid).igsSettingsSpawnDesc));
	    // Spawn settings
	    ip.add(new IPItem(Settings.allowSpawnAnvilUse, Material.ANVIL, plugin.myLocale(uuid).igsAnvil));
	    if (clazz != null) {
		ip.add(new IPItem(Settings.allowArmorStandUse, Material.ARMOR_STAND, plugin.myLocale(uuid).igsArmorStand));
	    }
	    ip.add(new IPItem(Settings.allowSpawnBeaconAccess, Material.BEACON, plugin.myLocale(uuid).igsBeacon));
	    ip.add(new IPItem(Settings.allowBedUse, Material.BED, plugin.myLocale(uuid).igsBed));
	    ip.add(new IPItem(Settings.allowSpawnBreakBlocks, Material.STONE, plugin.myLocale(uuid).igsBreakBlocks));
	    ip.add(new IPItem(Settings.allowBreeding, Material.CARROT_ITEM, plugin.myLocale(uuid).igsBreeding));
	    ip.add(new IPItem(Settings.allowSpawnBrewing, Material.BREWING_STAND_ITEM, plugin.myLocale(uuid).igsBrewing));
	    ip.add(new IPItem(Settings.allowBucketUse, Material.LAVA_BUCKET, plugin.myLocale(uuid).igsBucket));
	    ip.add(new IPItem(Settings.allowSpawnChestAccess, Material.CHEST, plugin.myLocale(uuid).igsChest));
	    ip.add(new IPItem(Settings.allowSpawnCrafting, Material.WORKBENCH, plugin.myLocale(uuid).igsWorkbench));
	    ip.add(new IPItem(Settings.allowCropTrample, Material.WHEAT, plugin.myLocale(uuid).igsCropTrampling));
	    ip.add(new IPItem(Settings.allowSpawnDoorUse, Material.WOOD_DOOR, plugin.myLocale(uuid).igsDoor));
	    ip.add(new IPItem(Settings.allowSpawnEnchanting, Material.ENCHANTMENT_TABLE, plugin.myLocale(uuid).igsEnchanting));
	    ip.add(new IPItem(Settings.allowEnderPearls, Material.ENDER_PEARL,plugin.myLocale(uuid).igsEnderPearl));
	    ip.add(new IPItem(Settings.allowSpawnFurnaceUse, Material.FURNACE, plugin.myLocale(uuid).igsFurnace));
	    ip.add(new IPItem(Settings.allowSpawnGateUse, Material.FENCE_GATE, plugin.myLocale(uuid).igsGate));
	    ip.add(new IPItem(Settings.allowSpawnHorseInvAccess, Material.GOLD_BARDING, plugin.myLocale(uuid).igsHorseInvAccess));
	    ip.add(new IPItem(Settings.allowSpawnHorseRiding, Material.DIAMOND_BARDING, plugin.myLocale(uuid).igsHorseRiding));
	    ip.add(new IPItem(Settings.allowHurtMobs, Material.EGG, plugin.myLocale(uuid).igsHurtAnimals));
	    ip.add(new IPItem(Settings.allowLeashUse, Material.LEASH, plugin.myLocale(uuid).igsLeash));
	    ip.add(new IPItem(Settings.allowSpawnLeverButtonUse, Material.LEVER, plugin.myLocale(uuid).igsLever));
	    ip.add(new IPItem(Settings.allowSpawnMusic, Material.JUKEBOX, plugin.myLocale(uuid).igsJukebox));
	    ip.add(new IPItem(Settings.allowSpawnPlaceBlocks, Material.DIRT, plugin.myLocale(uuid).igsPlaceBlocks));
	    ip.add(new IPItem(Settings.allowPortalUse, Material.OBSIDIAN, plugin.myLocale(uuid).igsPortalUse));
	    ip.add(new IPItem(Settings.allowSpawnPressurePlate, Material.GOLD_PLATE, plugin.myLocale(uuid).igsPressurePlate));
	    ip.add(new IPItem(Settings.allowPvP, Material.ARROW, plugin.myLocale(uuid).igsPVP));
	    ip.add(new IPItem(Settings.allowNetherPvP, Material.NETHERRACK, plugin.myLocale(uuid).igsNetherPVP));
	    ip.add(new IPItem(Settings.allowSpawnRedStone, Material.REDSTONE_COMPARATOR, plugin.myLocale(uuid).igsRedstone));
	    ip.add(new IPItem(Settings.allowShearing, Material.SHEARS, plugin.myLocale(uuid).igsShears));
	} else {
	    // Standard island
	    ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsIslandTitle, plugin.myLocale(uuid).igsSettingsIslandDesc));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowAnvilUse), Material.ANVIL, plugin.myLocale(uuid).igsAnvil));
	    if (clazz != null) {
		ip.add(new IPItem(island.getIgsFlag(Flags.allowArmorStandUse), Material.ARMOR_STAND, plugin.myLocale(uuid).igsArmorStand));
	    }
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowBeaconAccess), Material.BEACON, plugin.myLocale(uuid).igsBeacon));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowBedUse), Material.BED, plugin.myLocale(uuid).igsBed));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowBreakBlocks), Material.STONE, plugin.myLocale(uuid).igsBreakBlocks));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowBreeding), Material.CARROT_ITEM, plugin.myLocale(uuid).igsBreeding));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowBrewing), Material.BREWING_STAND_ITEM, plugin.myLocale(uuid).igsBrewing));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowBucketUse), Material.LAVA_BUCKET, plugin.myLocale(uuid).igsBucket));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowChestAccess), Material.CHEST, plugin.myLocale(uuid).igsChest));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowCrafting), Material.WORKBENCH, plugin.myLocale(uuid).igsWorkbench));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowCropTrample), Material.WHEAT, plugin.myLocale(uuid).igsCropTrampling));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowDoorUse), Material.WOOD_DOOR, plugin.myLocale(uuid).igsDoor));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowEnchanting), Material.ENCHANTMENT_TABLE, plugin.myLocale(uuid).igsEnchanting));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowEnderPearls), Material.ENDER_PEARL,plugin.myLocale(uuid).igsEnderPearl));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowFurnaceUse), Material.FURNACE, plugin.myLocale(uuid).igsFurnace));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowGateUse), Material.FENCE_GATE, plugin.myLocale(uuid).igsGate));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowHorseInvAccess), Material.GOLD_BARDING, plugin.myLocale(uuid).igsHorseInvAccess));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowHorseRiding), Material.DIAMOND_BARDING, plugin.myLocale(uuid).igsHorseRiding));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowHurtMobs), Material.EGG, plugin.myLocale(uuid).igsHurtAnimals));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowLeashUse), Material.LEASH, plugin.myLocale(uuid).igsLeash));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowLeverButtonUse), Material.LEVER, plugin.myLocale(uuid).igsLever));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowMusic), Material.JUKEBOX, plugin.myLocale(uuid).igsJukebox));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowPlaceBlocks), Material.DIRT, plugin.myLocale(uuid).igsPlaceBlocks));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowPortalUse), Material.OBSIDIAN, plugin.myLocale(uuid).igsPortalUse));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowPressurePlate), Material.GOLD_PLATE, plugin.myLocale(uuid).igsPressurePlate));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowPvP), Material.ARROW, plugin.myLocale(uuid).igsPVP));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowNetherPvP), Material.NETHERRACK, plugin.myLocale(uuid).igsNetherPVP));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowRedStone), Material.REDSTONE_COMPARATOR, plugin.myLocale(uuid).igsRedstone));
	    ip.add(new IPItem(island.getIgsFlag(Flags.allowShearing), Material.SHEARS, plugin.myLocale(uuid).igsShears));
	}
	if (ip.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = ip.size() + 8;
	    size -= (size % 9);
	    String title = plugin.myLocale(uuid).igsTitle;
	    if (title.length() > 32) {
		title = title.substring(0, 31);
	    }
	    newPanel = Bukkit.createInventory(null, size, title);
	    // Fill the inventory and return
	    int slot = 0;
	    for (IPItem i : ip) {
		i.setSlot(slot);
		newPanel.addItem(i.getItem());
	    }
	}	    
	return newPanel;
    }

    /**
     * Handle clicks to the Settings panel
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	int slot = event.getRawSlot();
	// Check this is the right panel
	if (!inventory.getName().equals(plugin.myLocale(player.getUniqueId()).igsTitle)) {
	    return;
	}
	// Stop removal of items
	event.setCancelled(true);
	if (event.getSlotType() == SlotType.OUTSIDE) {
	    player.closeInventory();
	    return;
	}
	// Check world
	if (!player.getLocation().getWorld().equals(ASkyBlock.getIslandWorld()) && !player.getLocation().getWorld().equals(ASkyBlock.getNetherWorld())) {
	    return;
	}
	// 1.7.x server
	if (clazz == null && slot > lookup.size()) {
	    return;
	}
	// 1.8.x server
	if (slot > (lookup.size() +1)) {
	    return;
	}
	// Get the flag
	Flags flag = null;
	if (lookup.containsKey(event.getCurrentItem().getType())) {
	    // All other items
	    flag = lookup.get(event.getCurrentItem().getType());
	} else if (clazz != null && event.getCurrentItem().getType() == Material.ARMOR_STAND) {
	    // Special handling to avoid errors on 1.7.x servers
	    flag = Flags.allowArmorStandUse;
	}
	// If flag is null, do nothing
	if (flag == null) {
	    return;
	}
	// Players can only do something if they own the island or are op
	Island island = plugin.getGrid().getIslandAt(player.getLocation());
	if (island != null && (player.isOp() || (island.getOwner() != null && island.getOwner().equals(player.getUniqueId())))) {
	    //plugin.getLogger().info("DEBUG: Check perms");
	    // Check perms
	    if (player.hasPermission(Settings.PERMPREFIX + "settings." + flag.toString())) {
		//plugin.getLogger().info("DEBUG: Player has perm");
		if (flag.equals(Flags.allowPvP) || flag.equals(Flags.allowNetherPvP)) {
		    // PVP always results in an inventory closure
		    player.closeInventory();
		    // Check if the player is allowed to toggle
		    // PVP activation
		    if (!island.getIgsFlag(flag)) {
			// Attempt to activate PVP
			//plugin.getLogger().info("DEBUG: attempt to activate PVP");
			if (pvpCoolDown.containsKey(player.getUniqueId())) {
			    //plugin.getLogger().info("DEBUG: player is in the cooldown list");
			    long setTime = pvpCoolDown.get(player.getUniqueId());
			    //plugin.getLogger().info("DEBUG: set time is " + setTime);
			    long secondsLeft = Settings.pvpRestartCooldown - (System.currentTimeMillis() - setTime) / 1000;
			    //plugin.getLogger().info("DEBUG: seconds left = " + secondsLeft);
			    if (secondsLeft > 0) {
				player.sendMessage(ChatColor.RED + "You must wait " + secondsLeft + " seconds until you can do that again!");
				return;
			    }
			    // Tidy up
			    pvpCoolDown.remove(player.getUniqueId());
			}
			// Warn players on the island
			for (Player p : plugin.getServer().getOnlinePlayers()) {
			    if (island.onIsland(p.getLocation())) {
				if (flag.equals(Flags.allowNetherPvP)) {
				    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(p.getUniqueId()).igsNetherPVP + " " + plugin.myLocale(p.getUniqueId()).igsAllowed);
				} else {
				    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(p.getUniqueId()).igsPVP + " " + plugin.myLocale(p.getUniqueId()).igsAllowed);
				}
				p.getWorld().playSound(p.getLocation(), Sound.ARROW_HIT, 1F, 1F);
			    }
			}
			// Toggle the flag
			island.toggleIgs(flag); 
			// Update warp signs
			final List<UUID> members = island.getMembers();
			// Run one tick later because text gets updated at the end of tick
			plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

			    @Override
			    public void run() {
				for (UUID playerUUID : members) {
				    plugin.getWarpPanel().updateWarp(playerUUID);
				}

			    }});

			return;
		    } else {
			// PVP deactivation
			// Store this deactivation time
			pvpCoolDown.put(player.getUniqueId(), System.currentTimeMillis());
			// Immediately toggle the setting
			island.toggleIgs(flag);
			// Update warp signs
			final List<UUID> members = island.getMembers();
			// Run one tick later because text gets updated at the end of tick
			plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

			    @Override
			    public void run() {
				for (UUID playerUUID : members) {
				    plugin.getWarpPanel().updateWarp(playerUUID);
				}

			    }});
			// Warn players of change
			for (Player p : plugin.getServer().getOnlinePlayers()) {
			    if (island.onIsland(p.getLocation())) {
				// Deactivate PVP
				if (flag.equals(Flags.allowNetherPvP)) {
				    p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).igsNetherPVP + " " + plugin.myLocale(p.getUniqueId()).igsDisallowed);
				} else {
				    p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).igsPVP + " " + plugin.myLocale(p.getUniqueId()).igsDisallowed);
				}
				p.getWorld().playSound(p.getLocation(), Sound.FIREWORK_TWINKLE, 1F, 1F);
			    }
			}
		    }
		} else {
		    island.toggleIgs(flag);
		}
	    }
	    //player.closeInventory();
	    player.openInventory(islandGuardPanel(player));
	}
    }
}