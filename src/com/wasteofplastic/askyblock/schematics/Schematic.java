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
package com.wasteofplastic.askyblock.schematics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.Settings.GameType;

public class Schematic {

    private short[] blocks;
    private byte[] data;
    private short width;
    private short length;
    private short height;
    private Map<BlockVector, Map<String, Tag>> tileEntitiesMap = new HashMap<BlockVector, Map<String, Tag>>();
    private File file;
    private String heading;
    private String name;
    private String perm;
    private String description;
    private int rating;
    private boolean useDefaultChest;
    private Material icon;    
    private Biome biome;
    private boolean usePhysics;

    public Schematic() {
	// Initialize 
	name = "";
	heading = "";
	description = "Default Island";
	perm = "";
	icon = Material.MAP;
	rating = 50;
	useDefaultChest = true;	
	biome = Settings.defaultBiome;
	usePhysics = Settings.usePhysics;
	file = null;
    }

    public Schematic(File file) {
	// Initialize
	name = file.getName();
	heading = "";
	description = "";
	perm = "";
	icon = Material.MAP;
	rating = 50;
	useDefaultChest = true;
	biome = Settings.defaultBiome;
	usePhysics = Settings.usePhysics;
	this.file = file;
	// Try to load the file
	try { 
	    FileInputStream stream = new FileInputStream(file);
	    // InputStream is = new DataInputStream(new
	    // GZIPInputStream(stream));
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

	    width = getChildTag(schematic, "Width", ShortTag.class).getValue();
	    length = getChildTag(schematic, "Length", ShortTag.class).getValue();
	    height = getChildTag(schematic, "Height", ShortTag.class).getValue();

	    String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
	    if (!materials.equals("Alpha")) {
		throw new IllegalArgumentException("Schematic file is not an Alpha schematic");
	    }

	    byte[] blockId = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
	    data = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
	    byte[] addId = new byte[0];
	    blocks = new short[blockId.length]; // Have to later combine IDs
	    // We support 4096 block IDs using the same method as vanilla
	    // Minecraft, where
	    // the highest 4 bits are stored in a separate byte array.
	    if (schematic.containsKey("AddBlocks")) {
		addId = getChildTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
	    }

	    // Combine the AddBlocks data with the first 8-bit block ID
	    for (int index = 0; index < blockId.length; index++) {
		if ((index >> 1) >= addId.length) { // No corresponding
		    // AddBlocks index
		    blocks[index] = (short) (blockId[index] & 0xFF);
		} else {
		    if ((index & 1) == 0) {
			blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blockId[index] & 0xFF));
		    } else {
			blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blockId[index] & 0xFF));
		    }
		}
	    }

	    // Need to pull out tile entities
	    List<Tag> tileEntities = getChildTag(schematic, "TileEntities", ListTag.class).getValue();
	    // Map<BlockVector, Map<String, Tag>> tileEntitiesMap = new
	    // HashMap<BlockVector, Map<String, Tag>>();

	    for (Tag tag : tileEntities) {
		if (!(tag instanceof CompoundTag))
		    continue;
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
	} catch (IOException e) {
	    Bukkit.getLogger().severe("Could not load island schematic! Error in file.");
	    e.printStackTrace();
	}
    }

    /**
     * @return the biome
     */
    public Biome getBiome() {
	return biome;
    }

    /**
     * @return the blocks
     */
    public short[] getBlocks() {
	return blocks;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
	return data;
    }

    /**
     * @return the description
     */
    public String getDescription() {
	return description;
    }

    /**
     * @return the file
     */
    public File getFile() {
	return file;
    }

    /**
     * @return the heading
     */
    public String getHeading() {
	return heading;
    }

    /**
     * @return the height
     */
    public short getHeight() {
	return height;
    }

    /**
     * @return the icon
     */
    public Material getIcon() {
	return icon;
    }

    /**
     * @return the length
     */
    public short getLength() {
	return length;
    }

    /**
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * @return the perm
     */
    public String getPerm() {
	return perm;
    }

    /**
     * @return the rating
     */
    public int getRating() {
	return rating;
    }

    /**
     * @return the tileEntitiesMap
     */
    public Map<BlockVector, Map<String, Tag>> getTileEntitiesMap() {
	return tileEntitiesMap;
    }

    /**
     * @return the width
     */
    public short getWidth() {
	return width;
    }

    /**
     * @return the useDefaultChest
     */
    public boolean isUseDefaultChest() {
	return useDefaultChest;
    }

    /**
     * @return the usePhysics
     */
    public boolean isUsePhysics() {
	return usePhysics;
    }

    /**
     * This method pastes a schematic and returns a location where a cow (or other entity)
     * could be placed. Actually, the location should be that of a grass block.
     * @param world
     * @param loc
     * @param player
     * @return Location of highest grass block
     */
    @SuppressWarnings("deprecation")
    public void pasteSchematic(final Location loc, final Player player) {
	// If this is not a file schematic, paste the default island
	if (this.file == null) {
	    if (Settings.GAMETYPE == GameType.ACIDISLAND) {
		generateIslandBlocks(loc,player);
	    } else {
		loc.getBlock().setType(Material.BEDROCK);
		ASkyBlock.getPlugin().getLogger().severe("Missing schematic - using bedrock block only");
	    }
	    return;
	}
	World world = loc.getWorld();
	Map<BlockVector, Map<String, Tag>> tileEntitiesMap = this.getTileEntitiesMap();
	// Bukkit.getLogger().info("World is " + world.getName() +
	// "and schematic size is " + schematic.getBlocks().length);
	// Bukkit.getLogger().info("DEBUG Location to place island is:" +
	// loc.toString());
	// Find top most bedrock - this is the key stone
	// Find top most chest
	// Find top most grass
	Location bedrock = null;
	Location chest = null;
	Location welcomeSign = null;
	Set<Vector> grassBlocks = new HashSet<Vector>();
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    // Bukkit.getLogger().info("DEBUG " + index +
		    // " changing to ID:"+blocks[index] + " data = " +
		    // blockData[index]);
		    if (blocks[index] == 7) {
			// Last bedrock
			if (bedrock == null || bedrock.getY() < y) {
			    bedrock = new Location(world, x, y, z);
			    //Bukkit.getLogger().info("DEBUG higher bedrock found:" + bedrock.toString());
			}
		    } else if (blocks[index] == 54) {
			// Last chest
			if (chest == null || chest.getY() < y) {
			    chest = new Location(world, x, y, z);
			    // Bukkit.getLogger().info("Island loc:" +
			    // loc.toString());
			    // Bukkit.getLogger().info("Chest relative location is "
			    // + chest.toString());
			}
		    } else if (blocks[index] == 63) {
			// Sign
			if (welcomeSign == null || welcomeSign.getY() < y) {
			    welcomeSign = new Location(world, x, y, z);
			    // Bukkit.getLogger().info("DEBUG higher sign found:"
			    // + welcomeSign.toString());
			}
		    } else if (blocks[index] == 2) {
			// Grass
			grassBlocks.add(new Vector(x,y,z));
		    }
		}
	    }
	}
	if (bedrock == null) {
	    Bukkit.getLogger().severe("Schematic must have at least one bedrock in it!");
	    return;
	}
	if (chest == null) {
	    Bukkit.getLogger().severe("Schematic must have at least one chest in it!");
	    return;
	}
	/*
	 * These are now optional
	 * if (welcomeSign == null) {
	 * Bukkit.getLogger().severe(
	 * "ASkyBlock: Schematic must have at least one sign post in it!");
	 * return null;
	 * }
	 */
	if (grassBlocks.isEmpty()) {
	    Bukkit.getLogger().severe("Schematic must have at least one grass block in it!");
	    return;
	}
	// Center on the last bedrock location
	//Bukkit.getLogger().info("DEBUG bedrock is:" + bedrock.toString());
	// Bukkit.getLogger().info("DEBUG loc is before subtract:" +
	// loc.toString());
	Location blockLoc = new Location(world, loc.getX(), loc.getY(), loc.getZ());
	blockLoc.subtract(bedrock);
	// Bukkit.getLogger().info("DEBUG loc is after subtract:" +
	// loc.toString());
	//Bukkit.getLogger().info("DEBUG blockloc is:" + blockLoc.toString());
	// Bukkit.getLogger().info("DEBUG there are " + tileEntitiesMap.size() +
	// " tile entities in the schematic");
	// Bukkit.getLogger().info("Placing blocks...");
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    Block block = new Location(world, x, y, z).add(blockLoc).getBlock();
		    try {
			// Do not post torches because they fall off every so often
			// May have to include banners too
			if (blocks[index] != Material.TORCH.getId()) {
			    block.setTypeIdAndData(blocks[index], data[index], this.usePhysics);
			}
		    } catch (Exception e) {
			// Do some 1.7.9 helping for the built-in schematic
			if (blocks[index] == 179) {
			    // Red sandstone - use red sand instead
			    block.setTypeIdAndData(12, (byte)1, this.usePhysics); 
			} else {
			    Bukkit.getLogger().info("Could not set (" + x + "," + y + "," + z + ") block ID:" + blocks[index] + " block data = " + data[index]);
			}
		    }
		}
	    }
	}

	// Second pass
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    Block block = new Location(world, x, y, z).add(blockLoc).getBlock();
		    try {
			block.setTypeIdAndData(blocks[index], data[index], this.usePhysics);
		    } catch (Exception e) {
			// Do some 1.7.9 helping for the built-in schematic
			if (blocks[index] == 179) {
			    // Red sandstone - use red sand instead
			    block.setTypeIdAndData(12, (byte)1, this.usePhysics); 
			} else {
			    Bukkit.getLogger().info("Could not set (" + x + "," + y + "," + z + ") block ID:" + blocks[index] + " block data = " + data[index]);
			}
		    }
		    if (tileEntitiesMap.containsKey(new BlockVector(x, y, z))) {
			String ver = Bukkit.getServer().getBukkitVersion();
			int major = Integer.valueOf(ver.substring(0, 1));
			int minor = Integer.valueOf(ver.substring(ver.indexOf(".") + 1, ver.indexOf(".") + 2));
			if (major >= 1 && minor >= 8) {
			    if (block.getType() == Material.STANDING_BANNER || block.getType() == Material.WALL_BANNER) {
				BannerBlock.set(block, tileEntitiesMap.get(new BlockVector(x, y, z)));
			    }
			}
			if ((block.getType() == Material.SIGN_POST) || (block.getType() == Material.WALL_SIGN)) {
			    Sign sign = (Sign) block.getState();
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x, y, z));

			    // for (String key : tileData.keySet()) {
			    // Bukkit.getLogger().info("DEBUG: key = " + key +
			    // " : " + tileData.get(key));
			    // //StringTag st = (StringTag) tileData.get(key);
			    // Bukkit.getLogger().info("DEBUG: key = " + key +
			    // " : " + st.getName() + " " + st.getValue());
			    // }
			    List<String> text = new ArrayList<String>();
			    text.add(((StringTag) tileData.get("Text1")).getValue());
			    text.add(((StringTag) tileData.get("Text2")).getValue());
			    text.add(((StringTag) tileData.get("Text3")).getValue());
			    text.add(((StringTag) tileData.get("Text4")).getValue());
			    // TODO Parse sign text formatting, colors and ULR's using JSON - this does not work right now

			    JSONParser parser = new JSONParser();
			    ContainerFactory containerFactory = new ContainerFactory(){
				public List creatArrayContainer() {
				    return new LinkedList();
				}

				public Map createObjectContainer() {
				    return new LinkedHashMap();
				}

			    };
			    /*
			    for (int line = 0; line < 4; line++) {
				if (!text.get(line).equals("\"\"")) {
				    try{
					Bukkit.getLogger().info("Text: '" + text.get(line) + "'");
					Map json = (Map)parser.parse(text.get(line), containerFactory);
					Iterator iter = json.entrySet().iterator();
					System.out.println("==iterate result==");
					while(iter.hasNext()){
					    Map.Entry entry = (Map.Entry)iter.next();
					    if (entry.getValue().toString().equals("extra")) {
						List content = (List)parser.parse(entry)
					    }
					    System.out.println(entry.getKey() + "=>" + entry.getValue());
					}

					System.out.println("==toJSONString()==");
					System.out.println(JSONValue.toJSONString(json));
				    }
				    catch(ParseException pe){
					System.out.println(pe);
				    }
				}
			     */
			    // This just removes all the JSON formatting and provides the raw text
			    for (int line = 0; line < 4; line++) {
				if (!text.get(line).equals("\"\"") && !text.get(line).isEmpty()) {
				    //String lineText = text.get(line).replace("{\"extra\":[\"", "").replace("\"],\"text\":\"\"}", "");
				    //Bukkit.getLogger().info("DEBUG: sign text = '" + text.get(line) + "'");
				    String lineText = "";
				    if (text.get(line).startsWith("{")) {
					// JSON string
				    try {
					
					Map json = (Map)parser.parse(text.get(line), containerFactory);
					List list = (List) json.get("extra");
					//System.out.println("DEBUG1:" + JSONValue.toJSONString(list));
					Iterator iter = list.iterator();
					while(iter.hasNext()){
					    Object next = iter.next();
					    String format = JSONValue.toJSONString(next);
					    //System.out.println("DEBUG2:" + format);
					    // This doesn't see right, but appears to be the easiest way to identify this string as JSON...
					    if (format.startsWith("{")) {
						// JSON string
						Map jsonFormat = (Map)parser.parse(format, containerFactory);
						Iterator formatIter = jsonFormat.entrySet().iterator();
						while (formatIter.hasNext()) {
						    Map.Entry entry = (Map.Entry)formatIter.next();
						    //System.out.println("DEBUG3:" + entry.getKey() + "=>" + entry.getValue());
						    String key = entry.getKey().toString();
						    String value = entry.getValue().toString();
						    if (key.equalsIgnoreCase("color")) {
							try {
							    lineText += ChatColor.valueOf(value.toUpperCase());
							} catch (Exception noColor) {
							    Bukkit.getLogger().warning("Unknown color " + value +" in sign when pasting schematic, skipping...");
							}
						    } else if (key.equalsIgnoreCase("text")) {
							lineText += value;
						    } else {
							// Formatting - usually the value is always true, but check just in case
							if (key.equalsIgnoreCase("obfuscated") && value.equalsIgnoreCase("true")) {
							    lineText += ChatColor.MAGIC;
							} else if (key.equalsIgnoreCase("underlined") && value.equalsIgnoreCase("true")) {
							    lineText += ChatColor.UNDERLINE;
							} else {
							    // The rest of the formats
							    try {
								lineText += ChatColor.valueOf(key.toUpperCase());
							    } catch (Exception noFormat) {
								// Ignore
								Bukkit.getLogger().warning("Unknown format " + value +" in sign when pasting schematic, skipping...");
							    }
							}
						    }   
						}
					    } else {
						// This is unformatted text. It is included in "". A reset is required to clear
						// any previous formatting
						if (format.length()>1) {
						    lineText += ChatColor.RESET + format.substring(format.indexOf('"')+1,format.lastIndexOf('"'));
						}
					    } 
					}
				    } catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				    } else {
					// This is unformatted text (not JSON). It is included in "".
					if (text.get(line).length() > 1) {
					    lineText = text.get(line).substring(text.get(line).indexOf('"')+1,text.get(line).lastIndexOf('"'));
					} else {
					    // ust in case it isn't - show the raw line
					    lineText = text.get(line);
					}
				    }
				    //Bukkit.getLogger().info("Line " + line + " is " + lineText);

				    // Set the line
				    sign.setLine(line, lineText);
				}
			    }
			    sign.update();
			}
			if (block.getType().equals(Material.CHEST)) {
			    Chest chestBlock = (Chest) block.getState();
			    // Bukkit.getLogger().info("Chest tile entity found");
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x, y, z));
			    try {
				ListTag chestItems = (ListTag) tileData.get("Items");
				if (chestItems != null) {
				    for (Tag item : chestItems.getValue()) {
					// Format for chest items is:
					// id = short value of item id
					// Damage = short value of item damage
					// Count = the number of items
					// Slot = the slot in the chest
					// inventory
					if (item instanceof CompoundTag) {
					    try {
						// Id is a number
						short itemType = (Short) ((CompoundTag) item).getValue().get("id").getValue();
						short itemDamage = (Short) ((CompoundTag) item).getValue().get("Damage").getValue();
						byte itemAmount = (Byte) ((CompoundTag) item).getValue().get("Count").getValue();
						byte itemSlot = (Byte) ((CompoundTag) item).getValue().get("Slot").getValue();
						ItemStack chestItem = new ItemStack(itemType, itemAmount, itemDamage);
						chestBlock.getInventory().setItem(itemSlot, chestItem);
					    } catch (ClassCastException ex) {
						// Id is a material
						String itemType = (String) ((CompoundTag) item).getValue().get("id").getValue();
						try {
						    // Get the material
						    if (itemType.startsWith("minecraft:")) {
							String material = itemType.substring(10).toUpperCase();
							// Special case for non-standard material names
							// REEDS, that is sugar
							// cane
							if (material.equalsIgnoreCase("REEDS")) {
							    material = "SUGAR_CANE";
							}
							if (material.equalsIgnoreCase("MYCELIUM")) {
							    material = "MYCEL";
							}
							if (material.equalsIgnoreCase("COOKED_PORKCHOP")) {
							    material = "GRILLED_PORK";
							}
							Material itemMaterial = Material.valueOf(material);
							short itemDamage = (Short) ((CompoundTag) item).getValue().get("Damage").getValue();
							byte itemAmount = (Byte) ((CompoundTag) item).getValue().get("Count").getValue();
							byte itemSlot = (Byte) ((CompoundTag) item).getValue().get("Slot").getValue();
							ItemStack chestItem = new ItemStack(itemMaterial, itemAmount, itemDamage);
							chestBlock.getInventory().setItem(itemSlot, chestItem);
							// Bukkit.getLogger().info("Adding "
							// +
							// chestItem.toString()
							// + " to chest");
						    }
						} catch (Exception exx) {
						    // Bukkit.getLogger().info(item.toString());
						    // Bukkit.getLogger().info(((CompoundTag)item).getValue().get("id").getName());
						    Bukkit.getLogger().severe(
							    "Could not parse item [" + itemType.substring(10).toUpperCase() + "] in schematic - skipping!");
						    // Bukkit.getLogger().severe(item.toString());
						    // exx.printStackTrace();
						}

					    }

					    // Bukkit.getLogger().info("Set chest inventory slot "
					    // + itemSlot + " to " +
					    // chestItem.toString());
					}
				    }
				}
			    } catch (Exception e) {
				Bukkit.getLogger().severe("Could not parse schematic file item, skipping!");
				// e.printStackTrace();
			    }
			}
		    }
		}
	    }
	}
	// Go through all the grass blocks and try to find a safe one
	// Sort by height
	List<Vector> sorted = new ArrayList<Vector>();
	for (Vector v : grassBlocks) {
	    v.subtract(bedrock.toVector());
	    v.add(loc.toVector());
	    v.add(new Vector(0.5D,1.1D,0.5D)); // Center of block
	    //if (GridManager.isSafeLocation(v.toLocation(world))) {
	    // Add to sorted list
	    boolean inserted = false;
	    for (int i = 0; i < sorted.size(); i++) {
		if (v.getBlockY() > sorted.get(i).getBlockY()) {
		    sorted.add(i, v);
		    inserted = true;
		    break;
		}
	    }
	    if (!inserted) {
		// just add to the end of the list
		sorted.add(v);
	    }
	    //}
	}
	final Location grass = sorted.get(0).toLocation(world);
	//Bukkit.getLogger().info("DEBUG cow location " + grass.toString());
	Block blockToChange = null;
	// world.spawnEntity(grass, EntityType.COW);
	// Place a helpful sign in front of player
	if (welcomeSign != null) {
	    // Bukkit.getLogger().info("DEBUG welcome sign schematic relative is:"
	    // + welcomeSign.toString());
	    welcomeSign.subtract(bedrock);
	    // Bukkit.getLogger().info("DEBUG welcome sign relative to bedrock is:"
	    // + welcomeSign.toString());
	    welcomeSign.add(loc);
	    // Bukkit.getLogger().info("DEBUG welcome sign actual position is:"
	    // + welcomeSign.toString());
	    blockToChange = welcomeSign.getBlock();
	    blockToChange.setType(Material.SIGN_POST);
	    Sign sign = (Sign) blockToChange.getState();
	    sign.setLine(0, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine1.replace("[player]", player.getName()));
	    sign.setLine(1, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine2.replace("[player]", player.getName()));
	    sign.setLine(2, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine3.replace("[player]", player.getName()));
	    sign.setLine(3, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine4.replace("[player]", player.getName()));
	    // BlockFace direction = ((org.bukkit.material.Sign)
	    // sign.getData()).getFacing();
	    ((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
	    sign.update();
	}
	chest.subtract(bedrock);
	chest.add(loc);
	// Place the chest - no need to use the safe spawn function because we
	// know what this island looks like
	blockToChange = chest.getBlock();
	// Bukkit.getLogger().info("Chest block = " + blockToChange);
	// blockToChange.setType(Material.CHEST);
	// Bukkit.getLogger().info("Chest item settings = " +
	// Settings.chestItems[0]);
	// Bukkit.getLogger().info("Chest item settings length = " +
	// Settings.chestItems.length);
	if (useDefaultChest && Settings.chestItems[0] != null) {
	    // Fill the chest
	    if (blockToChange.getType() == Material.CHEST) {
		final Chest islandChest = (Chest) blockToChange.getState();
		final Inventory inventory = islandChest.getInventory();
		inventory.clear();
		inventory.setContents(Settings.chestItems);
	    }
	}
	if (Settings.islandCompanion != null) {
	    Bukkit.getServer().getScheduler().runTaskLater(ASkyBlock.getPlugin(), new Runnable() {
		@Override
		public void run() {
		    spawnCompanion(player, grass);
		}
	    }, 40L);
	}
    }

    /**
     * @param biome the biome to set
     */
    public void setBiome(Biome biome) {
	this.biome = biome;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
	this.description = description;
    }

    /**
     * @param heading the heading to set
     */
    public void setHeading(String heading) {
	this.heading = heading;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(Material icon) {
	this.icon = icon;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
	this.name = name;
    }

    /**
     * @param perm the perm to set
     */
    public void setPerm(String perm) {
	this.perm = perm;
    }

    /**
     * @param rating the rating to set
     */
    public void setRating(int rating) {
	this.rating = rating;
    }

    /**
     * @param useDefaultChest the useDefaultChest to set
     */
    public void setUseDefaultChest(boolean useDefaultChest) {
	this.useDefaultChest = useDefaultChest;
    }

    /**
     * @param usePhysics the usePhysics to set
     */
    public void setUsePhysics(boolean usePhysics) {
	this.usePhysics = usePhysics;
    }


    /**
     * Creates an island block by block
     * 
     * @param x
     * @param z
     * @param player
     * @param world
     */
    @SuppressWarnings("deprecation")
    public static void generateIslandBlocks(final Location islandLoc, final Player player) {
	// AcidIsland
	// Build island layer by layer
	// Start from the base
	// half sandstone; half sand
	int x = islandLoc.getBlockX();
	int z = islandLoc.getBlockZ();
	World world = islandLoc.getWorld();
	int y = 0;
	for (int x_space = x - 4; x_space <= x + 4; x_space++) {
	    for (int z_space = z - 4; z_space <= z + 4; z_space++) {
		final Block b = world.getBlockAt(x_space, y, z_space);
		b.setType(Material.BEDROCK);
	    }
	}
	for (y = 1; y < Settings.island_level + 5; y++) {
	    for (int x_space = x - 4; x_space <= x + 4; x_space++) {
		for (int z_space = z - 4; z_space <= z + 4; z_space++) {
		    final Block b = world.getBlockAt(x_space, y, z_space);
		    if (y < (Settings.island_level / 2)) {
			b.setType(Material.SANDSTONE);
		    } else {
			b.setType(Material.SAND);
			b.setData((byte) 0);
		    }
		}
	    }
	}
	// Then cut off the corners to make it round-ish
	for (y = 0; y < Settings.island_level + 5; y++) {
	    for (int x_space = x - 4; x_space <= x + 4; x_space += 8) {
		for (int z_space = z - 4; z_space <= z + 4; z_space += 8) {
		    final Block b = world.getBlockAt(x_space, y, z_space);
		    b.setType(Material.STATIONARY_WATER);
		}
	    }
	}
	// Add some grass
	for (y = Settings.island_level + 4; y < Settings.island_level + 5; y++) {
	    for (int x_space = x - 2; x_space <= x + 2; x_space++) {
		for (int z_space = z - 2; z_space <= z + 2; z_space++) {
		    final Block blockToChange = world.getBlockAt(x_space, y, z_space);
		    blockToChange.setType(Material.GRASS);
		}
	    }
	}
	// Place bedrock - MUST be there (ensures island are not
	// overwritten
	Block b = world.getBlockAt(x, Settings.island_level, z);
	b.setType(Material.BEDROCK);
	// Then add some more dirt in the classic shape
	y = Settings.island_level + 3;
	for (int x_space = x - 2; x_space <= x + 2; x_space++) {
	    for (int z_space = z - 2; z_space <= z + 2; z_space++) {
		b = world.getBlockAt(x_space, y, z_space);
		b.setType(Material.DIRT);
	    }
	}
	b = world.getBlockAt(x - 3, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x + 3, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z - 3);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z + 3);
	b.setType(Material.DIRT);
	y = Settings.island_level + 2;
	for (int x_space = x - 1; x_space <= x + 1; x_space++) {
	    for (int z_space = z - 1; z_space <= z + 1; z_space++) {
		b = world.getBlockAt(x_space, y, z_space);
		b.setType(Material.DIRT);
	    }
	}
	b = world.getBlockAt(x - 2, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x + 2, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z - 2);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z + 2);
	b.setType(Material.DIRT);
	y = Settings.island_level + 1;
	b = world.getBlockAt(x - 1, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x + 1, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z - 1);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z + 1);
	b.setType(Material.DIRT);

	// Add island items
	y = Settings.island_level;
	// Add tree (natural)
	final Location treeLoc = new Location(world, x, y + 5D, z);
	world.generateTree(treeLoc, TreeType.ACACIA);
	// Place the cow
	final Location cowSpot = new Location(world, x, (Settings.island_level + 5), z - 2);

	// Place a helpful sign in front of player
	Block blockToChange = world.getBlockAt(x, Settings.island_level + 5, z + 3);
	blockToChange.setType(Material.SIGN_POST);
	Sign sign = (Sign) blockToChange.getState();
	sign.setLine(0, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine1.replace("[player]", player.getName()));
	sign.setLine(1, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine2.replace("[player]", player.getName()));
	sign.setLine(2, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine3.replace("[player]", player.getName()));
	sign.setLine(3, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine4.replace("[player]", player.getName()));
	((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
	sign.update();
	// Place the chest - no need to use the safe spawn function
	// because we
	// know what this island looks like
	blockToChange = world.getBlockAt(x, Settings.island_level + 5, z + 1);
	blockToChange.setType(Material.CHEST);
	// Only set if the config has items in it
	if (Settings.chestItems.length > 0) {
	    final Chest chest = (Chest) blockToChange.getState();
	    final Inventory inventory = chest.getInventory();
	    inventory.clear();
	    inventory.setContents(Settings.chestItems);
	    chest.update();
	}
	// Fill the chest and orient it correctly (1.8 faces it north!
	DirectionalContainer dc = (DirectionalContainer) blockToChange.getState().getData();
	dc.setFacingDirection(BlockFace.SOUTH);
	blockToChange.setData(dc.getData(), true);

	if (Settings.islandCompanion != null) {
	    Bukkit.getServer().getScheduler().runTaskLater(ASkyBlock.getPlugin(), new Runnable() {
		@Override
		public void run() {
		    spawnCompanion(player, cowSpot);
		}
	    }, 40L);
	}
    }
    /**
     * Get child tag of a NBT structure.
     * 
     * @param items
     *            The parent tag map
     * @param key
     *            The name of the tag to get
     * @param expected
     *            The expected type of the tag
     * @return child tag casted to the expected type
     * @throws DataException
     *             if the tag does not exist or the tag is not of the
     *             expected type
     */
    private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected) throws IllegalArgumentException {
	if (!items.containsKey(key)) {
	    throw new IllegalArgumentException("Schematic file is missing a \"" + key + "\" tag");
	}
	Tag tag = items.get(key);
	if (!expected.isInstance(tag)) {
	    throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
	}
	return expected.cast(tag);
    }
    /**
     * Spawns a companion for the player at the location given
     * @param player
     * @param cowSpot
     */
    protected static void spawnCompanion(Player player, Location cowSpot) {
	// Older versions of the server require custom names to only apply to Living Entities
	LivingEntity companion = (LivingEntity) player.getWorld().spawnEntity(cowSpot, Settings.islandCompanion);  
	if (!Settings.companionNames.isEmpty()) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt(Settings.companionNames.size());
	    String name = Settings.companionNames.get(randomNum).replace("[player]", player.getDisplayName());
	    //plugin.getLogger().info("DEBUG: name is " + name);
	    companion.setCustomName(name);
	    companion.setCustomNameVisible(true);
	} 
    }
}