package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

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
	    ip.add(new IPItem(Settings.allowArmorStandUse, Material.ARMOR_STAND));
	}
	ip.add(new IPItem(Settings.allowBeaconAccess, Material.BEACON));
	ip.add(new IPItem(Settings.allowBedUse, Material.BED));
	ip.add(new IPItem(Settings.allowBreakBlocks, Material.DIRT, "Break blocks"));
	ip.add(new IPItem(Settings.allowBreeding, Material.CARROT_ITEM, "Breeding"));
	ip.add(new IPItem(Settings.allowBrewing, Material.BREWING_STAND_ITEM, "Potion Brewing"));
	ip.add(new IPItem(Settings.allowBucketUse, Material.BUCKET));
	ip.add(new IPItem(Settings.allowChestAccess, Material.CHEST));
	ip.add(new IPItem(Settings.allowChestDamage, Material.CHEST, "Chest damage by TNT"));
	ip.add(new IPItem(Settings.allowCrafting, Material.WORKBENCH));
	ip.add(new IPItem(Settings.allowCropTrample, Material.WHEAT, "Crop trampling"));
	ip.add(new IPItem(Settings.allowDoorUse, Material.WOOD_DOOR, "Door use"));
	ip.add(new IPItem(Settings.allowEnchanting, Material.ENCHANTMENT_TABLE));
	ip.add(new IPItem(Settings.allowEnderPearls, Material.ENDER_PEARL));
	ip.add(new IPItem(Settings.allowFire, Material.FLINT_AND_STEEL, "Fire"));
	ip.add(new IPItem(Settings.allowFurnaceUse, Material.FURNACE));
	ip.add(new IPItem(Settings.allowGateUse, Material.FENCE_GATE));
	ip.add(new IPItem(Settings.allowHurtMobs, Material.EGG, "Hurting animals"));
	ip.add(new IPItem(Settings.allowHurtMonsters, Material.SKULL_ITEM, "Hurting monsters"));
	ip.add(new IPItem(Settings.allowLeashUse, Material.LEASH));
	ip.add(new IPItem(Settings.allowLeverButtonUse, Material.LEVER, "Leaver or Button Use"));
	ip.add(new IPItem(Settings.allowMonsterEggs, Material.MONSTER_EGG));
	ip.add(new IPItem(Settings.allowMusic, Material.JUKEBOX));
	ip.add(new IPItem(Settings.allowPlaceBlocks, Material.DIRT, "Place blocks"));
	ip.add(new IPItem(Settings.allowPortalUse, Material.OBSIDIAN, "Portal use"));
	ip.add(new IPItem(Settings.allowPvP, Material.ARROW, "PVP"));
	ip.add(new IPItem(Settings.allowRedStone, Material.REDSTONE));
	ip.add(new IPItem(Settings.allowShearing, Material.SHEARS));
	ip.add(new IPItem(Settings.allowTeleportWhenFalling, Material.GLASS, "Teleport when falling"));
	ip.add(new IPItem(Settings.allowTNTDamage, Material.TNT, "TNT Damage"));
	ip.add(new IPItem(Settings.allowVisitorItemDrop, Material.GOLD_INGOT, "Visitor item dropping"));
	ip.add(new IPItem(Settings.allowVisitorItemPickup, Material.DIAMOND, "Visitor item pick-up"));
	ip.add(new IPItem(Settings.allowVisitorKeepInvOnDeath, Material.IRON_CHESTPLATE, "Visitor keep item on death"));
	if (ip.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = ip.size() +8;
	    size -= (size % 9);
	    newPanel = Bukkit.createInventory(null, size, "Island Guard Settings");
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
