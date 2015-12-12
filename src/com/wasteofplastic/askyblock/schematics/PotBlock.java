package com.wasteofplastic.askyblock.schematics;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.FlowerPot;
import org.bukkit.material.MaterialData;

import org.jnbt.IntTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.TileEntityFlowerPot;

/**
 * This class describes banners and is used in schematic importing
 * 
 * @author SpyL1nk
 * 
 */
public class PotBlock {
    private Material potItem;
    private int potItemData;
    
    private static HashMap<String, Material> potItemList;
    
    static {
    	potItemList = new HashMap<String, Material>();
    	potItemList.put("", Material.AIR);
    	potItemList.put("minecraft:red_flower", Material.RED_ROSE);
    	potItemList.put("minecraft:yellow_flower", Material.YELLOW_FLOWER);
    	potItemList.put("minecraft:sapling", Material.SAPLING);
    	potItemList.put("minecraft:red_mushroom", Material.RED_MUSHROOM);
    	potItemList.put("minecraft:brown_mushroom", Material.BROWN_MUSHROOM);
    	potItemList.put("minecraft:cactus", Material.CACTUS);
    	potItemList.put("minecraft:deadbush", Material.LONG_GRASS);
    	potItemList.put("minecraft:tallgrass", Material.LONG_GRASS);
    }
    
    // Credis: Mister_Frans (THANK YOU VERY MUCH !)
	public boolean set(Block block) {
		if(potItem != Material.AIR){
			Location loc = block.getLocation();
			CraftWorld cw = (CraftWorld)block.getWorld();
			BlockPosition bp = new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
			
			TileEntityFlowerPot te = (TileEntityFlowerPot)cw.getHandle().getTileEntity(bp);
			//Bukkit.getLogger().info("Debug: flowerpot materialdata = " + (new ItemStack(potItem, 1,(short) potItemData).toString()));
			net.minecraft.server.v1_8_R3.ItemStack cis = CraftItemStack.asNMSCopy(new ItemStack(potItem, 1,(short) potItemData));
			te.a(cis.getItem(), cis.getData());
			te.update();
			cw.getHandle().notify(bp);
			Chunk ch = loc.getChunk();
			cw.refreshChunk(ch.getX(), ch.getZ());	
		}
		return true;
    }

    public boolean prep(Map<String, Tag> tileData) {
		try {
			if(tileData.containsKey("Item")){
				String itemName = ((StringTag) tileData.get("Item")).getValue();
				// We can't set everything in a flowerpot, check that
				if(potItemList.containsKey(itemName)){
					potItem = potItemList.get(itemName);
					
					if(tileData.containsKey("Data")){
						int dataTag = ((IntTag) tileData.get("Data")).getValue();
						// We should check data for each type of potItem 
						if(potItem == Material.RED_ROSE){
							if(dataTag >= 0 && dataTag <= 8){
								potItemData = dataTag;
							} else {
								// Prevent hacks
								potItemData = 0;
							}
						} else if(potItem == Material.YELLOW_FLOWER ||
									potItem == Material.RED_MUSHROOM ||
									potItem == Material.BROWN_MUSHROOM ||
									potItem == Material.CACTUS){
							// Set to 0 anyway
							potItemData = 0;
						} else if(potItem == Material.SAPLING){
							if(dataTag >= 0 && dataTag <= 4){
								potItemData = dataTag;
							} else {
								// Prevent hacks
								potItemData = 0;
							}
						} else if(potItem == Material.LONG_GRASS){
							// Only 0 or 2
							if(dataTag == 0 || dataTag == 2){
								potItemData = dataTag;
							} else {
								potItemData = 0;
							}
						} else {
							// ERROR ?
							potItemData = 0;
						}
					}
					else {
						potItemData = 0;
					}
				}
				else {
					// Prevent hacks, set to null (empty flower pot)
					potItem = potItemList.get("");
					potItemData = 0;
				}
			}
			else {
				// Prevent hacks, set to null (empty flower pot)
				potItem = potItemList.get("");
				potItemData = 0;
			}
			//Bukkit.getLogger().info("Debug: flowerpot item = " + potItem.toString());
			//Bukkit.getLogger().info("Debug: flowerpot item data = " + potItemData);
	    	//Bukkit.getLogger().info("Debug: flowerpot materialdata = " + new MaterialData(potItem,(byte) potItemData).toString());
		} catch (Exception e) {
		    e.printStackTrace();
	}
	return true;
    }
}