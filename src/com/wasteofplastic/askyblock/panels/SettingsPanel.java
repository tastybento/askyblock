package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.Locale;
import com.wasteofplastic.askyblock.Settings;

public class SettingsPanel {
    // Island Guard Settings Panel
    private static List<IPItem> ip = new ArrayList<IPItem>();
    private static Inventory newPanel;
    static {
	ip.add(new IPItem(Settings.allowAnvilUse, Material.ANVIL));
	Class<?> clazz;
	try {
	    clazz = Class.forName("org.bukkit.entity.ArmorStand");
	} catch (Exception e) {
	    clazz = null;
	}
	if (clazz != null) {
	    ip.add(new IPItem(Settings.allowArmorStandUse, Material.ARMOR_STAND, Locale.igsArmorStand));
	}
	ip.add(new IPItem(Settings.allowBeaconAccess, Material.BEACON, Locale.igsBeacon));
	ip.add(new IPItem(Settings.allowBedUse, Material.BED, Locale.igsBed));
	ip.add(new IPItem(Settings.allowBreakBlocks, Material.DIRT, Locale.igsBreakBlocks));
	ip.add(new IPItem(Settings.allowBreeding, Material.CARROT_ITEM, Locale.igsBreeding));
	ip.add(new IPItem(Settings.allowBrewing, Material.BREWING_STAND_ITEM, Locale.igsBrewing));
	ip.add(new IPItem(Settings.allowBucketUse, Material.BUCKET, Locale.igsBucket));
	ip.add(new IPItem(Settings.allowChestAccess, Material.CHEST, Locale.igsChest));
	ip.add(new IPItem(Settings.allowChestDamage, Material.CHEST, Locale.igsChestDamage));
	ip.add(new IPItem(Settings.allowCrafting, Material.WORKBENCH, Locale.igsWorkbench));
	ip.add(new IPItem(Settings.allowCropTrample, Material.WHEAT, Locale.igsCropTrampling));
	ip.add(new IPItem(Settings.allowDoorUse, Material.WOOD_DOOR, Locale.igsDoor));
	ip.add(new IPItem(Settings.allowEnchanting, Material.ENCHANTMENT_TABLE, Locale.igsEnchanting));
	ip.add(new IPItem(Settings.allowEnderPearls, Material.ENDER_PEARL,Locale.igsEnderPearl));
	ip.add(new IPItem(Settings.allowFire, Material.FLINT_AND_STEEL, Locale.igsFire));
	ip.add(new IPItem(Settings.allowFurnaceUse, Material.FURNACE, Locale.igsFurnace));
	ip.add(new IPItem(Settings.allowGateUse, Material.FENCE_GATE, Locale.igsGate));
	ip.add(new IPItem(Settings.allowHurtMobs, Material.EGG, Locale.igsHurtAnimals));
	ip.add(new IPItem(Settings.allowHurtMonsters, Material.SKULL_ITEM, Locale.igsHurtMobs));
	ip.add(new IPItem(Settings.allowLeashUse, Material.LEASH, Locale.igsLeash));
	ip.add(new IPItem(Settings.allowLeverButtonUse, Material.LEVER, Locale.igsLever));
	ip.add(new IPItem(Settings.allowMonsterEggs, Material.MONSTER_EGG, Locale.igsSpawnEgg));
	ip.add(new IPItem(Settings.allowMusic, Material.JUKEBOX, Locale.igsJukebox));
	ip.add(new IPItem(Settings.allowPlaceBlocks, Material.DIRT, Locale.igsPlaceBlocks));
	ip.add(new IPItem(Settings.allowPortalUse, Material.OBSIDIAN, Locale.igsPortalUse));
	ip.add(new IPItem(Settings.allowPvP, Material.ARROW, Locale.igsPVP));
	ip.add(new IPItem(Settings.allowRedStone, Material.REDSTONE, Locale.igsRedstone));
	ip.add(new IPItem(Settings.allowShearing, Material.SHEARS, Locale.igsShears));
	ip.add(new IPItem(Settings.allowTeleportWhenFalling, Material.GLASS, Locale.igsTeleport));
	ip.add(new IPItem(Settings.allowTNTDamage, Material.TNT, Locale.igsTNT));
	ip.add(new IPItem(Settings.allowVisitorItemDrop, Material.GOLD_INGOT, Locale.igsVisitorDrop));
	ip.add(new IPItem(Settings.allowVisitorItemPickup, Material.DIAMOND, Locale.igsVisitorPickUp));
	ip.add(new IPItem(Settings.allowVisitorKeepInvOnDeath, Material.IRON_CHESTPLATE, Locale.igsVisitorKeep));
	if (ip.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = ip.size() + 8;
	    size -= (size % 9);
	    newPanel = Bukkit.createInventory(null, size, Locale.igsTitle);
	    // Fill the inventory and return
	    int slot = 0;
	    for (IPItem i : ip) {
		i.setSlot(slot);
		newPanel.addItem(i.getItem());
	    }
	}
    }

    public static Inventory islandGuardPanel() {
	return newPanel;
    }
}
