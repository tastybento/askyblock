package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

public class SettingsPanel {
    // Island Guard Settings Panel
    private static List<IPItem> ip = new ArrayList<IPItem>();
    private static Inventory newPanel;
    static {
	ip.add(new IPItem(Settings.allowAnvilUse, Material.ANVIL, ASkyBlock.getPlugin().myLocale().igsAnvil));
	Class<?> clazz;
	try {
	    clazz = Class.forName("org.bukkit.entity.ArmorStand");
	} catch (Exception e) {
	    clazz = null;
	}
	if (clazz != null) {
	    ip.add(new IPItem(Settings.allowArmorStandUse, Material.ARMOR_STAND, ASkyBlock.getPlugin().myLocale().igsArmorStand));
	}
	ip.add(new IPItem(Settings.allowBeaconAccess, Material.BEACON, ASkyBlock.getPlugin().myLocale().igsBeacon));
	ip.add(new IPItem(Settings.allowBedUse, Material.BED, ASkyBlock.getPlugin().myLocale().igsBed));
	ip.add(new IPItem(Settings.allowBreakBlocks, Material.DIRT, ASkyBlock.getPlugin().myLocale().igsBreakBlocks));
	ip.add(new IPItem(Settings.allowBreeding, Material.CARROT_ITEM, ASkyBlock.getPlugin().myLocale().igsBreeding));
	ip.add(new IPItem(Settings.allowBrewing, Material.BREWING_STAND_ITEM, ASkyBlock.getPlugin().myLocale().igsBrewing));
	ip.add(new IPItem(Settings.allowBucketUse, Material.LAVA_BUCKET, ASkyBlock.getPlugin().myLocale().igsBucket));
	ip.add(new IPItem(Settings.allowChestAccess, Material.CHEST, ASkyBlock.getPlugin().myLocale().igsChest));
	ip.add(new IPItem(Settings.allowChestDamage, Material.CHEST, ASkyBlock.getPlugin().myLocale().igsChestDamage));
	ip.add(new IPItem(Settings.allowCrafting, Material.WORKBENCH, ASkyBlock.getPlugin().myLocale().igsWorkbench));
	ip.add(new IPItem(Settings.allowCropTrample, Material.WHEAT, ASkyBlock.getPlugin().myLocale().igsCropTrampling));
	ip.add(new IPItem(Settings.allowDoorUse, Material.WOOD_DOOR, ASkyBlock.getPlugin().myLocale().igsDoor));
	ip.add(new IPItem(Settings.allowEnchanting, Material.ENCHANTMENT_TABLE, ASkyBlock.getPlugin().myLocale().igsEnchanting));
	ip.add(new IPItem(Settings.allowEnderPearls, Material.ENDER_PEARL,ASkyBlock.getPlugin().myLocale().igsEnderPearl));
	ip.add(new IPItem(Settings.allowFire, Material.FLINT_AND_STEEL, ASkyBlock.getPlugin().myLocale().igsFire));
	ip.add(new IPItem(Settings.allowFurnaceUse, Material.FURNACE, ASkyBlock.getPlugin().myLocale().igsFurnace));
	ip.add(new IPItem(Settings.allowGateUse, Material.FENCE_GATE, ASkyBlock.getPlugin().myLocale().igsGate));
	ip.add(new IPItem(Settings.allowHurtMobs, Material.EGG, ASkyBlock.getPlugin().myLocale().igsHurtAnimals));
	ip.add(new IPItem(Settings.allowHurtMonsters, Material.SKULL_ITEM, ASkyBlock.getPlugin().myLocale().igsHurtMobs));
	ip.add(new IPItem(Settings.allowCreeperDamage, Material.SKULL_ITEM, 4, ASkyBlock.getPlugin().myLocale().igsCreeperDamage));
	ip.add(new IPItem(Settings.allowCreeperGriefing, Material.SKULL_ITEM, 4, ASkyBlock.getPlugin().myLocale().igsCreeperGriefing));
	ip.add(new IPItem(!Settings.restrictWither, Material.SKULL_ITEM, 1, ASkyBlock.getPlugin().myLocale().igsWitherDamage));
	ip.add(new IPItem(Settings.allowLeashUse, Material.LEASH, ASkyBlock.getPlugin().myLocale().igsLeash));
	ip.add(new IPItem(Settings.allowLeverButtonUse, Material.LEVER, ASkyBlock.getPlugin().myLocale().igsLever));
	ip.add(new IPItem(Settings.allowMonsterEggs, Material.MONSTER_EGG, ASkyBlock.getPlugin().myLocale().igsSpawnEgg));
	ip.add(new IPItem(Settings.allowMusic, Material.JUKEBOX, ASkyBlock.getPlugin().myLocale().igsJukebox));
	ip.add(new IPItem(Settings.allowPlaceBlocks, Material.DIRT, ASkyBlock.getPlugin().myLocale().igsPlaceBlocks));
	ip.add(new IPItem(Settings.allowPortalUse, Material.OBSIDIAN, ASkyBlock.getPlugin().myLocale().igsPortalUse));
	ip.add(new IPItem(Settings.allowPvP, Material.ARROW, ASkyBlock.getPlugin().myLocale().igsPVP));
	ip.add(new IPItem(Settings.allowNetherPvP, Material.NETHERRACK, ASkyBlock.getPlugin().myLocale().igsNetherPVP));
	ip.add(new IPItem(Settings.allowRedStone, Material.REDSTONE_COMPARATOR, ASkyBlock.getPlugin().myLocale().igsRedstone));
	ip.add(new IPItem(Settings.allowShearing, Material.SHEARS, ASkyBlock.getPlugin().myLocale().igsShears));
	ip.add(new IPItem(Settings.allowTeleportWhenFalling, Material.GLASS, ASkyBlock.getPlugin().myLocale().igsTeleport));
	ip.add(new IPItem(Settings.allowTNTDamage, Material.TNT, ASkyBlock.getPlugin().myLocale().igsTNT));
	ip.add(new IPItem(Settings.allowVisitorItemDrop, Material.GOLD_INGOT, ASkyBlock.getPlugin().myLocale().igsVisitorDrop));
	ip.add(new IPItem(Settings.allowVisitorItemPickup, Material.DIAMOND, ASkyBlock.getPlugin().myLocale().igsVisitorPickUp));
	ip.add(new IPItem(Settings.allowVisitorKeepInvOnDeath, Material.IRON_CHESTPLATE, ASkyBlock.getPlugin().myLocale().igsVisitorKeep));
	if (ip.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = ip.size() + 8;
	    size -= (size % 9);
	    newPanel = Bukkit.createInventory(null, size, ASkyBlock.getPlugin().myLocale().igsTitle);
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