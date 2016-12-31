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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;

public class SettingsPanel implements Listener {
    // Island Guard Settings Panel
    private ASkyBlock plugin;
    private Class<?> clazz;
    private static boolean hasChorusFruit;
    private HashMap<UUID,Long> pvpCoolDown = new HashMap<UUID,Long>();

    private static HashMap<Material,SettingsFlag> lookup = new HashMap<Material,SettingsFlag>();
    static {
        hasChorusFruit = (Material.getMaterial("CHORUS_FRUIT") != null);

        lookup.put( Material.ANVIL,SettingsFlag.ANVIL);
        lookup.put( Material.BEACON,SettingsFlag.BEACON);
        lookup.put( Material.BED,SettingsFlag.BED);
        lookup.put( Material.STONE,SettingsFlag.BREAK_BLOCKS);
        lookup.put( Material.CARROT_ITEM,SettingsFlag.BREEDING);
        lookup.put( Material.BREWING_STAND_ITEM,SettingsFlag.BREWING);
        lookup.put( Material.LAVA_BUCKET,SettingsFlag.BUCKET);
        lookup.put( Material.CHEST,SettingsFlag.CHEST);
        lookup.put( Material.WORKBENCH,SettingsFlag.CRAFTING);
        lookup.put( Material.WHEAT,SettingsFlag.CROP_TRAMPLE);
        lookup.put( Material.WOOD_DOOR,SettingsFlag.DOOR);
        lookup.put( Material.ENCHANTMENT_TABLE,SettingsFlag.ENCHANTING);
        lookup.put( Material.ENDER_PEARL,SettingsFlag.ENDERPEARL);
        lookup.put( Material.FURNACE,SettingsFlag.FURNACE);
        lookup.put( Material.FENCE_GATE,SettingsFlag.GATE);
        lookup.put( Material.GOLD_BARDING,SettingsFlag.HORSE_INVENTORY);
        lookup.put( Material.DIAMOND_BARDING,SettingsFlag.HORSE_RIDING);
        lookup.put( Material.EGG,SettingsFlag.HURT_MOBS);
        lookup.put( Material.LEASH,SettingsFlag.LEASH);
        lookup.put( Material.LEVER,SettingsFlag.LEVER_BUTTON);
        lookup.put( Material.JUKEBOX,SettingsFlag.MUSIC);
        lookup.put( Material.DIRT,SettingsFlag.PLACE_BLOCKS);
        lookup.put( Material.OBSIDIAN,SettingsFlag.PORTAL);
        lookup.put( Material.GOLD_PLATE,SettingsFlag.PRESSURE_PLATE);
        lookup.put( Material.ARROW,SettingsFlag.PVP);
        lookup.put( Material.NETHERRACK,SettingsFlag.NETHER_PVP);
        lookup.put( Material.REDSTONE_COMPARATOR,SettingsFlag.REDSTONE);
        lookup.put( Material.SHEARS,SettingsFlag.SHEARING);
        lookup.put( Material.EMERALD,SettingsFlag.VILLAGER_TRADING);
        if (hasChorusFruit) {
            lookup.put( Material.CHORUS_FRUIT,SettingsFlag.CHORUS_FRUIT);
        }
        lookup.put( Material.NOTE_BLOCK,SettingsFlag.ENTER_EXIT_MESSAGES);
        lookup.put(Material.MOB_SPAWNER, SettingsFlag.MONSTER_SPAWN);
        lookup.put(Material.GOLD_INGOT, SettingsFlag.VISITOR_ITEM_DROP);
        lookup.put(Material.DIAMOND, SettingsFlag.VISITOR_ITEM_PICKUP);
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

    /**
     * Presents a GUI for toggling or viewing settings
     * @param player
     * @return
     */
    public Inventory islandGuardPanel(Player player) {
        UUID uuid = player.getUniqueId();
        // Get the island settings for this player's location
        Island island = plugin.getGrid().getProtectedIslandAt(player.getLocation());
        List<IPItem> ip = new ArrayList<IPItem>();
        Inventory newPanel = null;
        if (island == null) {
            ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsGeneralTitle, plugin.myLocale(uuid).igsSettingsGeneralDesc));
            // General settings
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ANVIL), Material.ANVIL, plugin.myLocale(uuid).igsAnvil));
            if (clazz != null) {
                ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ARMOR_STAND), Material.ARMOR_STAND, plugin.myLocale(uuid).igsArmorStand));
            }
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BEACON), Material.BEACON, plugin.myLocale(uuid).igsBeacon));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BED), Material.BED, plugin.myLocale(uuid).igsBed));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BREAK_BLOCKS), Material.STONE, plugin.myLocale(uuid).igsBreakBlocks));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BREEDING), Material.CARROT_ITEM, plugin.myLocale(uuid).igsBreeding));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BREWING), Material.BREWING_STAND_ITEM, plugin.myLocale(uuid).igsBrewing));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BUCKET), Material.LAVA_BUCKET, plugin.myLocale(uuid).igsBucket));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.CHEST), Material.CHEST, plugin.myLocale(uuid).igsChest));
            ip.add(new IPItem(Settings.allowChestDamage, Material.CHEST, plugin.myLocale(uuid).igsChestDamage));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.CRAFTING), Material.WORKBENCH, plugin.myLocale(uuid).igsWorkbench));
            ip.add(new IPItem(Settings.allowCreeperDamage, Material.SKULL_ITEM, 4, plugin.myLocale(uuid).igsCreeperDamage));
            ip.add(new IPItem(Settings.allowCreeperGriefing, Material.SKULL_ITEM, 4, plugin.myLocale(uuid).igsCreeperGriefing));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.CROP_TRAMPLE), Material.WHEAT, plugin.myLocale(uuid).igsCropTrampling));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.DOOR), Material.WOOD_DOOR, plugin.myLocale(uuid).igsDoor));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ENCHANTING), Material.ENCHANTMENT_TABLE, plugin.myLocale(uuid).igsEnchanting));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ENDERPEARL), Material.ENDER_PEARL,plugin.myLocale(uuid).igsEnderPearl));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.FURNACE), Material.FURNACE, plugin.myLocale(uuid).igsFurnace));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.GATE), Material.FENCE_GATE, plugin.myLocale(uuid).igsGate));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.HORSE_INVENTORY), Material.GOLD_BARDING, plugin.myLocale(uuid).igsHorseInvAccess));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.HORSE_RIDING), Material.DIAMOND_BARDING, plugin.myLocale(uuid).igsHorseRiding));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.HURT_MOBS), Material.EGG, plugin.myLocale(uuid).igsHurtAnimals));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.LEASH), Material.LEASH, plugin.myLocale(uuid).igsLeash));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.LEVER_BUTTON), Material.LEVER, plugin.myLocale(uuid).igsLever));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.MUSIC), Material.JUKEBOX, plugin.myLocale(uuid).igsJukebox));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.PLACE_BLOCKS), Material.DIRT, plugin.myLocale(uuid).igsPlaceBlocks));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.PORTAL), Material.OBSIDIAN, plugin.myLocale(uuid).igsPortalUse));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.PRESSURE_PLATE), Material.GOLD_PLATE, plugin.myLocale(uuid).igsPressurePlate));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.PVP), Material.ARROW, plugin.myLocale(uuid).igsPVP));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.NETHER_PVP), Material.NETHERRACK, plugin.myLocale(uuid).igsNetherPVP));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.REDSTONE), Material.REDSTONE_COMPARATOR, plugin.myLocale(uuid).igsRedstone));
            ip.add(new IPItem(!Settings.restrictWither, Material.SKULL_ITEM, 1, plugin.myLocale(uuid).igsWitherDamage));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.SHEARING), Material.SHEARS, plugin.myLocale(uuid).igsShears));
            ip.add(new IPItem(Settings.allowTNTDamage, Material.TNT, plugin.myLocale(uuid).igsTNT));
            ip.add(new IPItem(Settings.allowVisitorKeepInvOnDeath, Material.IRON_CHESTPLATE, plugin.myLocale(uuid).igsVisitorKeep));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.VILLAGER_TRADING), Material.EMERALD, plugin.myLocale(uuid).igsVillagerTrading));
            if (hasChorusFruit) {
                ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.CHORUS_FRUIT), Material.CHORUS_FRUIT, plugin.myLocale(uuid).igsChorusFruit));
            }
            // Place new settings here
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ENTER_EXIT_MESSAGES), Material.NOTE_BLOCK, plugin.myLocale(uuid).igsJoinLeaveIslandMessage));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.MONSTER_SPAWN), Material.MOB_SPAWNER, plugin.myLocale(uuid).igsMobSpawning));
        } else if (island.isSpawn()) {
            ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsSpawnTitle, plugin.myLocale(uuid).igsSettingsSpawnDesc));
            // Spawn settings
            // TODO: convert to use default spawn settings, not default island settings
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.ANVIL), Material.ANVIL, plugin.myLocale(uuid).igsAnvil));
            if (clazz != null) {
                ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ARMOR_STAND), Material.ARMOR_STAND, plugin.myLocale(uuid).igsArmorStand));
            }
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.BEACON), Material.BEACON, plugin.myLocale(uuid).igsBeacon));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BED), Material.BED, plugin.myLocale(uuid).igsBed));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.BREAK_BLOCKS), Material.STONE, plugin.myLocale(uuid).igsBreakBlocks));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BREEDING), Material.CARROT_ITEM, plugin.myLocale(uuid).igsBreeding));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.BREWING), Material.BREWING_STAND_ITEM, plugin.myLocale(uuid).igsBrewing));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.BUCKET), Material.LAVA_BUCKET, plugin.myLocale(uuid).igsBucket));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.CHEST), Material.CHEST, plugin.myLocale(uuid).igsChest));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.CRAFTING), Material.WORKBENCH, plugin.myLocale(uuid).igsWorkbench));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.CROP_TRAMPLE), Material.WHEAT, plugin.myLocale(uuid).igsCropTrampling));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.DOOR), Material.WOOD_DOOR, plugin.myLocale(uuid).igsDoor));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.ENCHANTING), Material.ENCHANTMENT_TABLE, plugin.myLocale(uuid).igsEnchanting));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.ENDERPEARL), Material.ENDER_PEARL,plugin.myLocale(uuid).igsEnderPearl));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.FURNACE), Material.FURNACE, plugin.myLocale(uuid).igsFurnace));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.GATE), Material.FENCE_GATE, plugin.myLocale(uuid).igsGate));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.HORSE_INVENTORY), Material.GOLD_BARDING, plugin.myLocale(uuid).igsHorseInvAccess));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.HORSE_RIDING), Material.DIAMOND_BARDING, plugin.myLocale(uuid).igsHorseRiding));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.HURT_MOBS), Material.EGG, plugin.myLocale(uuid).igsHurtAnimals));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.LEASH), Material.LEASH, plugin.myLocale(uuid).igsLeash));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.LEVER_BUTTON), Material.LEVER, plugin.myLocale(uuid).igsLever));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.MUSIC), Material.JUKEBOX, plugin.myLocale(uuid).igsJukebox));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.PLACE_BLOCKS), Material.DIRT, plugin.myLocale(uuid).igsPlaceBlocks));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.PORTAL), Material.OBSIDIAN, plugin.myLocale(uuid).igsPortalUse));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.PRESSURE_PLATE), Material.GOLD_PLATE, plugin.myLocale(uuid).igsPressurePlate));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.PVP), Material.ARROW, plugin.myLocale(uuid).igsPVP));
            ip.add(new IPItem(Settings.defaultIslandSettings.get(SettingsFlag.NETHER_PVP), Material.NETHERRACK, plugin.myLocale(uuid).igsNetherPVP));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.REDSTONE), Material.REDSTONE_COMPARATOR, plugin.myLocale(uuid).igsRedstone));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.SHEARING), Material.SHEARS, plugin.myLocale(uuid).igsShears));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.VILLAGER_TRADING), Material.EMERALD, plugin.myLocale(uuid).igsVillagerTrading));
            if (hasChorusFruit) {
                ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.CHORUS_FRUIT), Material.CHORUS_FRUIT, plugin.myLocale(uuid).igsChorusFruit));
            }
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.VISITOR_ITEM_DROP), Material.GOLD_INGOT, plugin.myLocale(uuid).igsVisitorDrop));
            ip.add(new IPItem(Settings.spawnSettings.get(SettingsFlag.VISITOR_ITEM_PICKUP), Material.DIAMOND, plugin.myLocale(uuid).igsVisitorPickUp));   
        } else {
            // Standard island
            ip.add(new IPItem(Material.MAP, plugin.myLocale(uuid).igsSettingsIslandTitle, plugin.myLocale(uuid).igsSettingsIslandDesc));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.ANVIL), Material.ANVIL, plugin.myLocale(uuid).igsAnvil));
            if (clazz != null) {
                ip.add(new IPItem(island.getIgsFlag(SettingsFlag.ARMOR_STAND), Material.ARMOR_STAND, plugin.myLocale(uuid).igsArmorStand));
            }
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.BEACON), Material.BEACON, plugin.myLocale(uuid).igsBeacon));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.BED), Material.BED, plugin.myLocale(uuid).igsBed));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.BREAK_BLOCKS), Material.STONE, plugin.myLocale(uuid).igsBreakBlocks));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.BREEDING), Material.CARROT_ITEM, plugin.myLocale(uuid).igsBreeding));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.BREWING), Material.BREWING_STAND_ITEM, plugin.myLocale(uuid).igsBrewing));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.BUCKET), Material.LAVA_BUCKET, plugin.myLocale(uuid).igsBucket));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.CHEST), Material.CHEST, plugin.myLocale(uuid).igsChest));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.CRAFTING), Material.WORKBENCH, plugin.myLocale(uuid).igsWorkbench));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.CROP_TRAMPLE), Material.WHEAT, plugin.myLocale(uuid).igsCropTrampling));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.DOOR), Material.WOOD_DOOR, plugin.myLocale(uuid).igsDoor));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.ENCHANTING), Material.ENCHANTMENT_TABLE, plugin.myLocale(uuid).igsEnchanting));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.ENDERPEARL), Material.ENDER_PEARL,plugin.myLocale(uuid).igsEnderPearl));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.FURNACE), Material.FURNACE, plugin.myLocale(uuid).igsFurnace));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.GATE), Material.FENCE_GATE, plugin.myLocale(uuid).igsGate));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.HORSE_INVENTORY), Material.GOLD_BARDING, plugin.myLocale(uuid).igsHorseInvAccess));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.HORSE_RIDING), Material.DIAMOND_BARDING, plugin.myLocale(uuid).igsHorseRiding));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.HURT_MOBS), Material.EGG, plugin.myLocale(uuid).igsHurtAnimals));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.LEASH), Material.LEASH, plugin.myLocale(uuid).igsLeash));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.LEVER_BUTTON), Material.LEVER, plugin.myLocale(uuid).igsLever));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.MUSIC), Material.JUKEBOX, plugin.myLocale(uuid).igsJukebox));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.PLACE_BLOCKS), Material.DIRT, plugin.myLocale(uuid).igsPlaceBlocks));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.PORTAL), Material.OBSIDIAN, plugin.myLocale(uuid).igsPortalUse));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.PRESSURE_PLATE), Material.GOLD_PLATE, plugin.myLocale(uuid).igsPressurePlate));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.PVP), Material.ARROW, plugin.myLocale(uuid).igsPVP));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.NETHER_PVP), Material.NETHERRACK, plugin.myLocale(uuid).igsNetherPVP));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.REDSTONE), Material.REDSTONE_COMPARATOR, plugin.myLocale(uuid).igsRedstone));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.SHEARING), Material.SHEARS, plugin.myLocale(uuid).igsShears));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.VILLAGER_TRADING), Material.EMERALD, plugin.myLocale(uuid).igsVillagerTrading));
            if (hasChorusFruit) {
                ip.add(new IPItem(island.getIgsFlag(SettingsFlag.CHORUS_FRUIT), Material.CHORUS_FRUIT, plugin.myLocale(uuid).igsChorusFruit));
            }
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.ENTER_EXIT_MESSAGES), Material.NOTE_BLOCK, plugin.myLocale(uuid).igsJoinLeaveIslandMessage));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.MONSTER_SPAWN), Material.MOB_SPAWNER, plugin.myLocale(uuid).igsMobSpawning));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.VISITOR_ITEM_DROP), Material.GOLD_INGOT, plugin.myLocale(uuid).igsVisitorDrop));
            ip.add(new IPItem(island.getIgsFlag(SettingsFlag.VISITOR_ITEM_PICKUP), Material.DIAMOND, plugin.myLocale(uuid).igsVisitorPickUp));
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
        if (inventory.getName() == null) {
            return;
        }
        int slot = event.getRawSlot();
        // Check this is the right panel
        if (!inventory.getName().equals(plugin.myLocale(player.getUniqueId()).igsTitle)) {
            return;
        }
        // Stop removal of items
        event.setCancelled(true);
        if (event.getSlotType() == SlotType.OUTSIDE) {
            player.closeInventory();
            inventory.clear();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            inventory.clear();
            player.updateInventory();
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
        SettingsFlag flag = null;
        if (lookup.containsKey(event.getCurrentItem().getType())) {
            // All other items
            flag = lookup.get(event.getCurrentItem().getType());
        } else if (clazz != null && event.getCurrentItem().getType() == Material.ARMOR_STAND) {
            // Special handling to avoid errors on 1.7.x servers
            flag = SettingsFlag.ARMOR_STAND;
        }
        // If flag is null, do nothing
        if (flag == null) {
            return;
        }
        // Players can only do something if they own the island or are op
        Island island = plugin.getGrid().getIslandAt(player.getLocation());
        if (island != null && (player.isOp() || (island.getOwner() != null && island.getOwner().equals(player.getUniqueId())))) {
            //plugin.getLogger().info("DEBUG: Check perm " + flag.toString());
            // Check perms
            if (player.hasPermission(Settings.PERMPREFIX + "settings." + flag.toString())) {
                //plugin.getLogger().info("DEBUG: Player has perm " + flag.toString());
                if (flag.equals(SettingsFlag.PVP) || flag.equals(SettingsFlag.NETHER_PVP)) {
                    // PVP always results in an inventory closure
                    player.closeInventory();
                    inventory.clear();
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
                                if (flag.equals(SettingsFlag.NETHER_PVP)) {
                                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(p.getUniqueId()).igsNetherPVP + " " + plugin.myLocale(p.getUniqueId()).igsAllowed);
                                } else {
                                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(p.getUniqueId()).igsPVP + " " + plugin.myLocale(p.getUniqueId()).igsAllowed);
                                }
                                
                                if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                    player.getWorld().playSound(player.getLocation(), Sound.valueOf("ARROW_HIT"), 1F, 1F);
                                } else {
                                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
                                }
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
                                if (flag.equals(SettingsFlag.NETHER_PVP)) {
                                    p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).igsNetherPVP + " " + plugin.myLocale(p.getUniqueId()).igsDisallowed);
                                } else {
                                    p.sendMessage(ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).igsPVP + " " + plugin.myLocale(p.getUniqueId()).igsDisallowed);
                                }
                                if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                    p.getWorld().playSound(p.getLocation(), Sound.valueOf("FIREWORK_TWINKLE"), 1F, 1F);
                                } else {
                                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_TWINKLE, 1F, 1F);
                                }
                                
                            }
                        }
                    }
                } else {
                    island.toggleIgs(flag);
                }
            }
            //player.closeInventory();
            inventory.clear();
            player.openInventory(islandGuardPanel(player));
        }
    }
}
