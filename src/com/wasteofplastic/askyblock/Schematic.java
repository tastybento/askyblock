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
package com.wasteofplastic.askyblock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;


public class Schematic {

    private byte[] blocks;
    private byte[] data;
    private short width;
    private short length;
    private short height;
    private Map<BlockVector, Map<String, Tag>> tileEntitiesMap;

    protected Schematic(byte[] blocks, byte[] data, short width, short length, short height, Map<BlockVector, Map<String, Tag>> tileEntitiesMap)
    {
	this.blocks = blocks;
	this.data = data;
	this.width = width;
	this.length = length;
	this.height = height;
	this.tileEntitiesMap = tileEntitiesMap;
    }

    /**
     * @return the blocks
     */
    protected byte[] getBlocks()
    {
	return blocks;
    }

    /**
     * @return the data
     */
    protected byte[] getData()
    {
	return data;
    }

    /**
     * @return the width
     */
    protected short getWidth()
    {
	return width;
    }

    /**
     * @return the length
     */
    protected short getLength()
    {
	return length;
    }

    /**
     * @return the height
     */
    protected short getHeight()
    {
	return height;
    }

    /**
     * @return the tileEntitiesMap
     */
    protected Map<BlockVector, Map<String, Tag>> getTileEntitiesMap() {
	return tileEntitiesMap;
    }

    @SuppressWarnings("deprecation")
    protected static Location pasteSchematic(final World world, final Location loc, final Schematic schematic, final Player player)
    {
	byte[] blocks = schematic.getBlocks();
	byte[] blockData = schematic.getData();

	short length = schematic.getLength();
	short width = schematic.getWidth();
	short height = schematic.getHeight();
	Map<BlockVector, Map<String, Tag>> tileEntitiesMap = schematic.getTileEntitiesMap();
	//Bukkit.getLogger().info("World is " + world.getName() + "and schematic size is " + schematic.getBlocks().length);
	//Bukkit.getLogger().info("DEBUG Location to place island is:" + loc.toString());
	// Find top most bedrock - this is the key stone
	// Find top most chest
	// Find top most grass
	Location bedrock = null;
	Location chest = null;
	Location welcomeSign = null;
	Location grass = null;
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    //Bukkit.getLogger().info("DEBUG " + index + " changing to ID:"+blocks[index] + " data = " + blockData[index]);
		    if (blocks[index] == 7) {
			// Last bedrock
			if (bedrock == null || bedrock.getY() < y) {
			    bedrock = new Location(world,x,y,z);
			    //Bukkit.getLogger().info("DEBUG higher bedrock found:" + bedrock.toString());
			}
		    } else if (blocks[index] == 54) {
			// Last chest
			if (chest == null || chest.getY() < y) {
			    chest = new Location(world, x,y,z);
			    //Bukkit.getLogger().info("Island loc:" + loc.toString());
			    //Bukkit.getLogger().info("Chest relative location is " + chest.toString());
			}
		    } else if (blocks[index] == 63) {
			// Sign
			if (welcomeSign == null || welcomeSign.getY()< y) {
			    welcomeSign = new Location(world,x,y,z);
			    //Bukkit.getLogger().info("DEBUG higher sign found:" + welcomeSign.toString());
			}
		    } else if (blocks[index] == 2) {
			// Grass
			if (grass == null || grass.getY()< y) {
			    grass = new Location(world,x,y,z);
			    //Bukkit.getLogger().info("DEBUG higher grass found:" + grass.toString());
			}
			// Grass
			if (grass.getY() == y && grass.getX() < x) {
			    grass = new Location(world,x,y,z);
			    //Bukkit.getLogger().info("DEBUG more x found:" + grass.toString());
			}
			// Grass
			if (grass.getY() == y || grass.getZ()< z) {
			    grass = new Location(world,x,y,z);
			    //Bukkit.getLogger().info("DEBUG more Z found:" + grass.toString());
			}

		    }
		}
	    }
	}
	if (bedrock == null) {
	    Bukkit.getLogger().severe("Schematic must have at least one bedrock in it!");
	    return null;
	}
	if (chest == null) {
	    Bukkit.getLogger().severe("Schematic must have at least one chest in it!");
	    return null;
	}
	/*
	 * These are now optional
	if (welcomeSign == null) {
	    Bukkit.getLogger().severe("ASkyBlock: Schematic must have at least one sign post in it!");
	    return null;
	}*/
	if (grass == null) {
	    Bukkit.getLogger().severe("Schematic must have at least one grass block in it!");
	    return null;
	}
	// Center on the last bedrock location
	//Bukkit.getLogger().info("DEBUG loc is before subtract:" + loc.toString());
	Location blockLoc = new Location(world, loc.getX(), loc.getY(), loc.getZ());
	blockLoc.subtract(bedrock);
	//Bukkit.getLogger().info("DEBUG loc is after subtract:" + loc.toString());
	//Bukkit.getLogger().info("DEBUG blockloc is:" + blockLoc.toString());
	//Bukkit.getLogger().info("DEBUG there are " + tileEntitiesMap.size() + " tile entities in the schematic");
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    Block block = new Location(world, x, y, z).add(blockLoc).getBlock();
		    //Bukkit.getLogger().info("DEBUG" + x + "," + y + "," + z + " block to change is " + block.toString());
		    //Bukkit.getLogger().info("DEBUG " + index + " changing to ID:"+blocks[index] + " data = " + blockData[index])
		    try {
			int type = blocks[index];
			if (type < 0) {
			    type +=256;
			}
			block.setTypeId(type);
			block.setData(blockData[index]);
			// Using this command sometimes doesn't set the data correctly...
			//block.setTypeIdAndData(type, blockData[index], true);
			/*
			if (block.getType() == Material.SIGN_POST) {
			    org.bukkit.material.Sign s = (org.bukkit.material.Sign) block.getState().getData();
			    Bukkit.getLogger().info(s.getFacing().toString());
			    
			    Bukkit.getLogger().info("SIGN - data type = " + type + ":" + blockData[index]);
			    Bukkit.getLogger().info("actual block data = " + block.getData());
			}*/
		    } catch (Exception e) {
			Bukkit.getLogger().info("Could not set ("+ x + "," + y + "," + z +") block ID:"+blocks[index] + " block data = "+ blockData[index] );
		    }
		    if (tileEntitiesMap.containsKey(new BlockVector(x,y,z))) {
			//Block block = new Location(world, x, y, z).add(blockLoc).getBlock();
			// TODO: Add support for other tile entities, like signs...
			if (block.getType().equals(Material.CHEST)) {
			    Chest chestBlock = (Chest)block.getState();
			    //Bukkit.getLogger().info("Chest tile entity found");
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x,y,z));
			    try {
				ListTag chestItems = (ListTag) tileData.get("Items");
				if (chestItems != null) {
				    for (Tag item : chestItems.getValue()) {
					// Format for chest items is:
					// id = short value of item id
					// Damage = short value of item damage
					// Count = the number of items
					// Slot = the slot in the chest inventory
					if (item instanceof CompoundTag) {
					    try {
						// Id is a number
						short itemType = (Short) ((CompoundTag)item).getValue().get("id").getValue();
						short itemDamage = (Short) ((CompoundTag)item).getValue().get("Damage").getValue();
						byte itemAmount = (Byte) ((CompoundTag)item).getValue().get("Count").getValue();
						byte itemSlot = (Byte) ((CompoundTag)item).getValue().get("Slot").getValue();
						ItemStack chestItem = new ItemStack(itemType, itemAmount, itemDamage);
						chestBlock.getInventory().setItem(itemSlot, chestItem);
					    } catch (ClassCastException ex) {
						// Id is a material
						String itemType = (String) ((CompoundTag)item).getValue().get("id").getValue();
						try {
						    // Get the material
						    if (itemType.startsWith("minecraft:")) {
							String material = itemType.substring(10).toUpperCase();
							Material itemMaterial = Material.valueOf(material);
							short itemDamage = (Short) ((CompoundTag)item).getValue().get("Damage").getValue();
							byte itemAmount = (Byte) ((CompoundTag)item).getValue().get("Count").getValue();
							byte itemSlot = (Byte) ((CompoundTag)item).getValue().get("Slot").getValue();
							ItemStack chestItem = new ItemStack(itemMaterial, itemAmount, itemDamage);
							chestBlock.getInventory().setItem(itemSlot, chestItem);
							//Bukkit.getLogger().info("Adding " + chestItem.toString() + " to chest");
						    }
						} catch (Exception exx){
						    //Bukkit.getLogger().info(item.toString());
						    //Bukkit.getLogger().info(((CompoundTag)item).getValue().get("id").getName());
						    Bukkit.getLogger().severe("Could not parse item ["+ itemType.substring(10).toUpperCase() + "] in schematic - skipping!");
						    //Bukkit.getLogger().severe(item.toString());
						    //exx.printStackTrace();
						}

					    }

					    //Bukkit.getLogger().info("Set chest inventory slot " + itemSlot + " to " + chestItem.toString());
					}
				    }
				}
			    } catch (Exception e) {
				Bukkit.getLogger().severe("Could not parse schematic file item, skipping!");
				//e.printStackTrace();
			    }
			}
		    }
		}
	    }
	}
	// Tile entities
	/*
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    if (tileEntitiesMap.containsKey(new BlockVector(x,y,z))) {
			Block block = new Location(world, x, y, z).add(blockLoc).getBlock();
			if (block.getType().equals(Material.CHEST)) {
			    Chest chestBlock = (Chest)block.getState();
			    //Bukkit.getLogger().info("Chest tile entity found");
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x,y,z));
			    try {
				ListTag chestItems = (ListTag) tileData.get("Items");
				if (chestItems != null) {
				    for (Tag item : chestItems.getValue()) {
					// Format for chest items is:
					// id = short value of item id
					// Damage = short value of item damage
					// Count = the number of items
					// Slot = the slot in the chest inventory
					if (item instanceof CompoundTag) {
					    short itemType = (Short) ((CompoundTag)item).getValue().get("id").getValue();
					    short itemDamage = (Short) ((CompoundTag)item).getValue().get("Damage").getValue();
					    byte itemAmount = (Byte) ((CompoundTag)item).getValue().get("Count").getValue();
					    byte itemSlot = (Byte) ((CompoundTag)item).getValue().get("Slot").getValue();
					    ItemStack chestItem = new ItemStack(itemType, itemAmount, itemDamage);
					    chestBlock.getInventory().setItem(itemSlot, chestItem);
					    //Bukkit.getLogger().info("Set chest inventory slot " + itemSlot + " to " + chestItem.toString());
					}
				    }
				}
			    } catch (Exception e) {
				e.printStackTrace();
			    }
			}
		    }
		}
	    }
	}*/

	//Bukkit.getLogger().info("DEBUG loc is after island build:" + loc.toString());
	// Add island items
	//int y = Settings.sea_level;
	// Add tree (natural)
	//final Location treeLoc = new Location(world,x,y + 5, z);
	//world.generateTree(treeLoc, TreeType.ACACIA);
	// Place the cow
	//Location cowSpot = new Location(world, x, Settings.sea_level + 5, z - 2);
	grass.subtract(bedrock);
	grass.add(loc);
	while (!ASkyBlock.isSafeLocation(grass) && grass.getY() < 250) {
	    grass.setY(grass.getY() + 1.1D);
	}
	Block blockToChange = null;
	//Bukkit.getLogger().info("DEBUG cow location " + grass.toString());
	//world.spawnEntity(grass, EntityType.COW);
	// Place a helpful sign in front of player
	if (welcomeSign != null) {
	    //Bukkit.getLogger().info("DEBUG welcome sign schematic relative is:" + welcomeSign.toString());
	    welcomeSign.subtract(bedrock);
	    //Bukkit.getLogger().info("DEBUG welcome sign relative to bedrock is:" + welcomeSign.toString());	
	    welcomeSign.add(loc);
	    //Bukkit.getLogger().info("DEBUG welcome sign actual position is:" + welcomeSign.toString());	
	    blockToChange = welcomeSign.getBlock();
	    blockToChange.setType(Material.SIGN_POST);
	    Sign sign = (Sign) blockToChange.getState();
	    sign.setLine(0, ChatColor.translateAlternateColorCodes('&', Locale.signLine1.replace("[player]", player.getName())));
	    sign.setLine(1, ChatColor.translateAlternateColorCodes('&', Locale.signLine2.replace("[player]", player.getName())));
	    sign.setLine(2, ChatColor.translateAlternateColorCodes('&', Locale.signLine3.replace("[player]", player.getName())));
	    sign.setLine(3, ChatColor.translateAlternateColorCodes('&', Locale.signLine4.replace("[player]", player.getName())));
	    //BlockFace direction = ((org.bukkit.material.Sign) sign.getData()).getFacing();
	    ((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
	    sign.update();
	}
	chest.subtract(bedrock);
	chest.add(loc);
	// Place the chest - no need to use the safe spawn function because we
	// know what this island looks like
	blockToChange = chest.getBlock();
	//blockToChange.setType(Material.CHEST);
	//Bukkit.getLogger().info("Chest item settings = " + Settings.chestItems[0]);
	//Bukkit.getLogger().info("Chest item settings length = " + Settings.chestItems.length);
	if (Settings.chestItems[0] != null) {
	    // Fill the chest
	    final Chest islandChest = (Chest) blockToChange.getState();
	    final Inventory inventory = islandChest.getInventory();
	    inventory.clear();
	    inventory.setContents(Settings.chestItems);
	}
	return grass;
    }

    protected static Schematic loadSchematic(File file) throws IOException
    {
	FileInputStream stream = new FileInputStream(file);
	//InputStream is = new DataInputStream(new GZIPInputStream(stream));
	NBTInputStream nbtStream = new NBTInputStream(stream);

	CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
	nbtStream.close();
	if (!schematicTag.getName().equals("Schematic")) {
	    throw new IllegalArgumentException("Tag \"Schematic\" does not exist or is not first");
	}

	Map<String, Tag> schematic = schematicTag.getValue();
	if (!schematic.containsKey("Blocks")) {
	    throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
	}

	short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
	short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
	short height = getChildTag(schematic, "Height", ShortTag.class).getValue();

	String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
	if (!materials.equals("Alpha")) {
	    throw new IllegalArgumentException("Schematic file is not an Alpha schematic");
	}

	byte[] blocks = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
	byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
	byte[] addId = new byte[0];
	short[] blockss = new short[blocks.length]; // Have to later combine IDs
	// We support 4096 block IDs using the same method as vanilla Minecraft, where
	// the highest 4 bits are stored in a separate byte array.
	if (schematic.containsKey("AddBlocks")) {
	    addId = getChildTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
	}

	// Combine the AddBlocks data with the first 8-bit block ID
	for (int index = 0; index < blocks.length; index++) {
	    if ((index >> 1) >= addId.length) { // No corresponding AddBlocks index
		blockss[index] = (short) (blocks[index] & 0xFF);
	    } else {
		if ((index & 1) == 0) {
		    blockss[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blocks[index] & 0xFF));
		} else {
		    blockss[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blocks[index] & 0xFF));
		}
	    }
	}

	// Need to pull out tile entities
	List<Tag> tileEntities = getChildTag(schematic, "TileEntities", ListTag.class).getValue();
	Map<BlockVector, Map<String, Tag>> tileEntitiesMap = new HashMap<BlockVector, Map<String, Tag>>();

	for (Tag tag : tileEntities) {
	    if (!(tag instanceof CompoundTag)) continue;
	    CompoundTag t = (CompoundTag) tag;

	    int x = 0;
	    int y = 0;
	    int z = 0;

	    Map<String, Tag> values = new HashMap<String, Tag>();

	    for (Map.Entry<String, Tag> entry : t.getValue().entrySet()) {
		if (entry.getKey().equals("x")) {
		    if (entry.getValue() instanceof IntTag) {
			x = ((IntTag) entry.getValue()).getValue();
		    }
		} else if (entry.getKey().equals("y")) {
		    if (entry.getValue() instanceof IntTag) {
			y = ((IntTag) entry.getValue()).getValue();
		    }
		} else if (entry.getKey().equals("z")) {
		    if (entry.getValue() instanceof IntTag) {
			z = ((IntTag) entry.getValue()).getValue();
		    }
		}

		values.put(entry.getKey(), entry.getValue());
	    }

	    BlockVector vec = new BlockVector(x, y, z);
	    tileEntitiesMap.put(vec, values);
	}

	return new Schematic(blocks, blockData, width, length, height, tileEntitiesMap);
    }

    /**
     * Get child tag of a NBT structure.
     *
     * @param items The parent tag map
     * @param key The name of the tag to get
     * @param expected The expected type of the tag
     * @return child tag casted to the expected type
     * @throws DataException if the tag does not exist or the tag is not of the
     * expected type
     */
    private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected) throws IllegalArgumentException
    {
	if (!items.containsKey(key)) {
	    throw new IllegalArgumentException("Schematic file is missing a \"" + key + "\" tag");
	}
	Tag tag = items.get(key);
	if (!expected.isInstance(tag)) {
	    throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
	}
	return expected.cast(tag);
    }
}