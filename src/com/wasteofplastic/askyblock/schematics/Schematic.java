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
import java.lang.reflect.InvocationTargetException;
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
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
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
import com.wasteofplastic.askyblock.nms.NMSAbstraction;

public class Schematic {
    private ASkyBlock plugin;
    private short[] blocks;
    private byte[] data;
    private short width;
    private short length;
    private short height;
    private Map<BlockVector, Map<String, Tag>> tileEntitiesMap = new HashMap<BlockVector, Map<String, Tag>>();
    //private HashMap<BlockVector, EntityType> entitiesMap = new HashMap<BlockVector, EntityType>();
    private List<EntityObject> entitiesList = new ArrayList<EntityObject>();
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
    private boolean pasteEntities;
    private boolean visible;
    private int order;
    // These hashmaps enable translation between WorldEdit strings and Bukkit names
    private HashMap<String, Material> WEtoM = new HashMap<String, Material>();
    private HashMap<String, EntityType> WEtoME = new HashMap<String, EntityType>();
    private List<EntityType> islandCompanion;
    private List<String> companionNames;
    private ItemStack[] defaultChestItems;
    // Name of a schematic this one is paired with
    private String partnerName = "";
    // Key blocks
    private Vector bedrock;
    private Vector chest;
    private Vector welcomeSign;
    private Vector topGrass;
    private Vector playerSpawn;
    private Material playerSpawnBlock;
    private NMSAbstraction nms;
    private Set<Material> attachable = new HashSet<Material>();

    public Schematic(ASkyBlock plugin) {
	this.plugin = plugin;
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
	islandCompanion = new ArrayList<EntityType>();
	islandCompanion.add(Settings.islandCompanion);
	companionNames = Settings.companionNames;
	defaultChestItems = Settings.chestItems;
	visible = true;
	order = 0;
	bedrock = null;
	chest = null;
	welcomeSign = null;
	topGrass = null;
	playerSpawn = null;
	playerSpawnBlock = null;
	partnerName = "";
    }

    public Schematic(ASkyBlock plugin, File file) throws IOException {
	this.plugin = plugin;
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
	islandCompanion = new ArrayList<EntityType>();
	islandCompanion.add(Settings.islandCompanion);
	companionNames = Settings.companionNames;
	defaultChestItems = Settings.chestItems;
	pasteEntities = false;
	visible = true;
	order = 0;
	bedrock = null;
	chest = null;
	welcomeSign = null;
	topGrass = null;
	playerSpawn = null;
	playerSpawnBlock = null;
	partnerName = "";

	attachable.add(Material.STONE_BUTTON);
	attachable.add(Material.WOOD_BUTTON);
	attachable.add(Material.COCOA);
	attachable.add(Material.LADDER);
	attachable.add(Material.LEVER);
	attachable.add(Material.PISTON_EXTENSION);
	attachable.add(Material.REDSTONE_TORCH_OFF);
	attachable.add(Material.REDSTONE_TORCH_ON);
	attachable.add(Material.WALL_SIGN);
	attachable.add(Material.TORCH);
	attachable.add(Material.TRAP_DOOR);
	attachable.add(Material.TRIPWIRE_HOOK);
	try {
	    nms = checkVersion();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	// Establish the World Edit to Material look up
	// V1.8 items
	if (!Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
	    attachable.add(Material.IRON_TRAPDOOR);
	    attachable.add(Material.WALL_BANNER);

	    WEtoM.put("ACACIA_DOOR",Material.ACACIA_DOOR_ITEM);
	    WEtoM.put("BIRCH_DOOR",Material.BIRCH_DOOR_ITEM);
	    WEtoM.put("BIRCH_STAIRS",Material.BIRCH_WOOD_STAIRS);
	    WEtoM.put("DARK_OAK_DOOR",Material.DARK_OAK_DOOR_ITEM);
	    WEtoM.put("JUNGLE_DOOR",Material.JUNGLE_DOOR_ITEM);
	    WEtoM.put("SLIME",Material.SLIME_BLOCK);
	    WEtoM.put("SPRUCE_DOOR",Material.SPRUCE_DOOR_ITEM);
	}
	WEtoM.put("BREWING_STAND",Material.BREWING_STAND_ITEM);
	WEtoM.put("CARROT_ON_A_STICK",Material.CARROT_STICK);
	WEtoM.put("CARROT",Material.CARROT_ITEM);
	WEtoM.put("CAULDRON", Material.CAULDRON_ITEM);
	WEtoM.put("COBBLESTONE_WALL",Material.COBBLE_WALL);
	WEtoM.put("COMPARATOR",Material.REDSTONE_COMPARATOR);
	WEtoM.put("COOKED_PORKCHOP", Material.GRILLED_PORK);	
	WEtoM.put("DIAMOND_HORSE_ARMOR",Material.DIAMOND_BARDING);
	WEtoM.put("DIAMOND_SHOVEL",Material.DIAMOND_SPADE);
	WEtoM.put("DYE",Material.INK_SACK);
	WEtoM.put("END_PORTAL_FRAME",Material.ENDER_PORTAL_FRAME);
	WEtoM.put("END_STONE",Material.ENDER_STONE);
	WEtoM.put("EXPERIENCE_BOTTLE",Material.EXP_BOTTLE);
	WEtoM.put("FILLED_MAP",Material.MAP);
	WEtoM.put("FIRE_CHARGE",Material.FIREBALL);
	WEtoM.put("FIREWORKS",Material.FIREWORK);
	WEtoM.put("FLOWER_POT", Material.FLOWER_POT_ITEM);
	WEtoM.put("GLASS_PANE",Material.THIN_GLASS);
	WEtoM.put("GOLDEN_CHESTPLATE",Material.GOLD_CHESTPLATE);
	WEtoM.put("GOLDEN_HORSE_ARMOR",Material.GOLD_BARDING);
	WEtoM.put("GOLDEN_LEGGINGS",Material.GOLD_LEGGINGS);
	WEtoM.put("GOLDEN_PICKAXE",Material.GOLD_PICKAXE);
	WEtoM.put("GOLDEN_RAIL",Material.POWERED_RAIL);
	WEtoM.put("GOLDEN_SHOVEL",Material.GOLD_SPADE);
	WEtoM.put("GOLDEN_SWORD", Material.GOLD_SWORD);
	WEtoM.put("GOLDEN_HELMET", Material.GOLD_HELMET);
	WEtoM.put("GOLDEN_HOE", Material.GOLD_HOE);
	WEtoM.put("GOLDEN_AXE", Material.GOLD_AXE);
	WEtoM.put("GOLDEN_BOOTS", Material.GOLD_BOOTS);
	WEtoM.put("HARDENED_CLAY",Material.HARD_CLAY);
	WEtoM.put("HEAVY_WEIGHTED_PRESSURE_PLATE",Material.GOLD_PLATE);
	WEtoM.put("IRON_BARS",Material.IRON_FENCE);
	WEtoM.put("IRON_HORSE_ARMOR",Material.IRON_BARDING);
	WEtoM.put("IRON_SHOVEL",Material.IRON_SPADE);
	WEtoM.put("LEAD",Material.LEASH);
	WEtoM.put("LEAVES2",Material.LEAVES_2);
	WEtoM.put("LIGHT_WEIGHTED_PRESSURE_PLATE",Material.IRON_PLATE);
	WEtoM.put("LOG2",Material.LOG_2);
	WEtoM.put("MAP",Material.EMPTY_MAP);
	WEtoM.put("MYCELIUM", Material.MYCEL);
	WEtoM.put("NETHER_BRICK_FENCE",Material.NETHER_FENCE);
	WEtoM.put("NETHER_WART",Material.NETHER_STALK);
	WEtoM.put("NETHERBRICK",Material.NETHER_BRICK_ITEM);
	WEtoM.put("OAK_STAIRS",Material.WOOD_STAIRS);
	WEtoM.put("POTATO", Material.POTATO_ITEM);
	WEtoM.put("RAIL",Material.RAILS);
	WEtoM.put("RECORD_11",Material.RECORD_11);
	WEtoM.put("RECORD_13",Material.GOLD_RECORD);
	WEtoM.put("RECORD_BLOCKS",Material.RECORD_3);
	WEtoM.put("RECORD_CAT",Material.GREEN_RECORD);
	WEtoM.put("RECORD_CHIRP",Material.RECORD_4);
	WEtoM.put("RECORD_FAR",Material.RECORD_5);
	WEtoM.put("RECORD_MALL",Material.RECORD_6);
	WEtoM.put("RECORD_MELLOHI",Material.RECORD_7);
	WEtoM.put("RECORD_STAL",Material.RECORD_8);
	WEtoM.put("RECORD_STRAD",Material.RECORD_9);
	WEtoM.put("RECORD_WAIT",Material.RECORD_12);
	WEtoM.put("RECORD_WARD",Material.RECORD_10);
	WEtoM.put("RED_FLOWER",Material.RED_ROSE);
	WEtoM.put("REEDS",Material.SUGAR_CANE);
	WEtoM.put("REPEATER",Material.DIODE);
	WEtoM.put("SKULL", Material.SKULL_ITEM);
	WEtoM.put("SPAWN_EGG",Material.MONSTER_EGG);
	WEtoM.put("STONE_BRICK_STAIRS",Material.BRICK_STAIRS);
	WEtoM.put("STONE_BRICK_STAIRS",Material.SMOOTH_STAIRS);
	WEtoM.put("STONE_SHOVEL",Material.STONE_SPADE);
	WEtoM.put("STONE_SLAB",Material.STEP);
	WEtoM.put("STONE_STAIRS",Material.COBBLESTONE_STAIRS);
	WEtoM.put("TNT_MINECART",Material.EXPLOSIVE_MINECART);
	WEtoM.put("WATERLILY",Material.WATER_LILY);
	WEtoM.put("WOODEN_AXE",Material.WOOD_AXE);
	WEtoM.put("WOODEN_BUTTON",Material.WOOD_BUTTON);
	WEtoM.put("WOODEN_DOOR",Material.WOOD_DOOR);
	WEtoM.put("WOODEN_HOE",Material.WOOD_HOE);
	WEtoM.put("WOODEN_PICKAXE",Material.WOOD_PICKAXE);
	WEtoM.put("WOODEN_PRESSURE_PLATE",Material.WOOD_PLATE);
	WEtoM.put("WOODEN_SHOVEL",Material.WOOD_SPADE);
	WEtoM.put("WOODEN_SLAB",Material.WOOD_STEP);
	WEtoM.put("WOODEN_SWORD",Material.WOOD_SWORD);
	// Entities
	WEtoME.put("LAVASLIME", EntityType.MAGMA_CUBE);
	WEtoME.put("ENTITYHORSE", EntityType.HORSE);
	WEtoME.put("OZELOT", EntityType.OCELOT);
	WEtoME.put("MUSHROOMCOW", EntityType.MUSHROOM_COW);
	WEtoME.put("PIGZOMBIE", EntityType.PIG_ZOMBIE);

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

	    Vector origin = null;
	    try {
		int originX = getChildTag(schematic, "WEOriginX", IntTag.class).getValue();
		int originY = getChildTag(schematic, "WEOriginY", IntTag.class).getValue();
		int originZ = getChildTag(schematic, "WEOriginZ", IntTag.class).getValue();
		Vector min = new Vector(originX, originY, originZ);

		//int offsetX = getChildTag(schematic, "WEOffsetX", IntTag.class).getValue();
		//int offsetY = getChildTag(schematic, "WEOffsetY", IntTag.class).getValue();
		//int offsetZ = getChildTag(schematic, "WEOffsetZ", IntTag.class).getValue();
		//Vector offset = new Vector(offsetX, offsetY, offsetZ);

		//origin = min.subtract(offset);
		origin = min.clone();
	    } catch (Exception ignored) {}
	    //Bukkit.getLogger().info("Origin = " + origin);


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
	    // Entities
	    List<Tag> entities = getChildTag(schematic, "Entities", ListTag.class).getValue();
	    for (Tag tag : entities) {
		if (!(tag instanceof CompoundTag))
		    continue;
		CompoundTag t = (CompoundTag) tag;
		//Bukkit.getLogger().info("**************************************");
		EntityObject ent = new EntityObject();
		for (Map.Entry<String, Tag> entry : t.getValue().entrySet()) {
		    //Bukkit.getLogger().info("DEBUG " + entry.getKey() + ">>>>" + entry.getValue());
		    //Bukkit.getLogger().info("++++++++++++++++++++++++++++++++++++++++++++++++++");
		    if (entry.getKey().equals("id")) {
			String id = ((StringTag)entry.getValue()).getValue().toUpperCase();
			//Bukkit.getLogger().info("ID is " + id);
			if (WEtoME.containsKey(id)) {
			    ent.setType(WEtoME.get(id));
			} else {
			    try {
				ent.setType(EntityType.valueOf(id));
			    } catch (Exception ex) {
				if (!id.equalsIgnoreCase("ITEM")) {
				    Bukkit.getLogger().warning("MobType " + id + " unknown, skipping");
				}
			    }
			}
		    }
		    if (entry.getKey().equals("Pos")) {
			//Bukkit.getLogger().info("DEBUG Pos fond");
			if (entry.getValue() instanceof ListTag) {
			    //Bukkit.getLogger().info("DEBUG coord found");
			    List<Tag> pos = new ArrayList<Tag>();
			    pos = ((ListTag) entry.getValue()).getValue();
			    //Bukkit.getLogger().info("DEBUG pos: " + pos);
			    double x = (double)pos.get(0).getValue() - origin.getX();
			    double y = (double)pos.get(1).getValue() - origin.getY();
			    double z = (double)pos.get(2).getValue() - origin.getZ();
			    ent.setLocation(new BlockVector(x,y,z));
			}
		    } else if (entry.getKey().equals("Motion")) {
			//Bukkit.getLogger().info("DEBUG Pos fond");
			if (entry.getValue() instanceof ListTag) {
			    //Bukkit.getLogger().info("DEBUG coord found");
			    List<Tag> pos = new ArrayList<Tag>();
			    pos = ((ListTag) entry.getValue()).getValue();
			    //Bukkit.getLogger().info("DEBUG pos: " + pos);
			    ent.setMotion(new Vector((double)pos.get(0).getValue(), (double)pos.get(1).getValue()
				    ,(double)pos.get(2).getValue()));
			}
		    } else if (entry.getKey().equals("Rotation")) {
			//Bukkit.getLogger().info("DEBUG Pos fond");
			if (entry.getValue() instanceof ListTag) {
			    //Bukkit.getLogger().info("DEBUG coord found");
			    List<Tag> pos = new ArrayList<Tag>();
			    pos = ((ListTag) entry.getValue()).getValue();
			    //Bukkit.getLogger().info("DEBUG pos: " + pos);
			    ent.setPitch((float)pos.get(0).getValue());
			    ent.setYaw((float)pos.get(1).getValue());
			}
		    } else if (entry.getKey().equals("Color")) {
			if (entry.getValue() instanceof ByteTag) {
			    ent.setColor(((ByteTag) entry.getValue()).getValue());
			}
		    } else if (entry.getKey().equals("Sheared")) {
			if (entry.getValue() instanceof ByteTag) {
			    if (((ByteTag) entry.getValue()).getValue() != (byte)0) {
				ent.setSheared(true);
			    } else {
				ent.setSheared(false);
			    }
			}
		    } else if (entry.getKey().equals("RabbitType")) {
			if (entry.getValue() instanceof IntTag) {
			    ent.setRabbitType(((IntTag)entry.getValue()).getValue());
			}
		    } else if (entry.getKey().equals("Profession")) {
			if (entry.getValue() instanceof IntTag) {
			    ent.setProfession(((IntTag)entry.getValue()).getValue());
			}
		    } else if (entry.getKey().equals("CarryingChest")) {
			if (entry.getValue() instanceof ByteTag) {
			    ent.setCarryingChest(((ByteTag) entry.getValue()).getValue());
			}
		    } else if (entry.getKey().equals("OwnerUUID")) {
			ent.setOwned(true);
		    } else if (entry.getKey().equals("CollarColor")) {
			if (entry.getValue() instanceof ByteTag) {
			    ent.setCollarColor(((ByteTag) entry.getValue()).getValue());
			}
		    }
		}
		if (ent.getType() != null) {
		    //Bukkit.getLogger().info("DEBUG: adding " + ent.getType().toString() + " at " + ent.getLocation().toString());
		    //entitiesMap.put(new BlockVector(x,y,z), mobType);
		    entitiesList.add(ent);
		}
	    }
	    //Bukkit.getLogger().info("DEBUG: size of entities = " + entities.size());
	    // Tile entities
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
	    //e.printStackTrace();
	    throw new IOException();
	}

	// Check for key blocks
	// Find top most bedrock - this is the key stone
	// Find top most chest
	// Find top most grass
	List<Vector> grassBlocks = new ArrayList<Vector>();
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
			    bedrock = new Vector(x, y, z);
			    //Bukkit.getLogger().info("DEBUG higher bedrock found:" + bedrock.toString());
			}
		    } else if (blocks[index] == 54) {
			// Last chest
			if (chest == null || chest.getY() < y) {
			    chest = new Vector(x, y, z);
			    // Bukkit.getLogger().info("Island loc:" +
			    // loc.toString());
			    // Bukkit.getLogger().info("Chest relative location is "
			    // + chest.toString());
			}
		    } else if (blocks[index] == 63) {
			// Sign
			if (welcomeSign == null || welcomeSign.getY() < y) {
			    welcomeSign = new Vector(x, y, z);
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
	    throw new IOException();
	}
	// Find other key blocks
	if (!grassBlocks.isEmpty()) {
	    // Sort by height
	    List<Vector> sorted = new ArrayList<Vector>();
	    for (Vector v : grassBlocks) {
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
	    }
	    topGrass = sorted.get(0);
	} else {
	    topGrass = null;
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
	//Bukkit.getLogger().info("World is " + world.getName() + "and schematic size is " + width + " x " + height + " x " + length);

	// Bukkit.getLogger().info("DEBUG Location to place island is:" +
	// loc.toString());

	// Center on the last bedrock location
	//Bukkit.getLogger().info("DEBUG bedrock is:" + bedrock.toString());
	// Bukkit.getLogger().info("DEBUG loc is before subtract:" +
	// loc.toString());
	Location blockLoc = new Location(world, loc.getX(), loc.getY(), loc.getZ());
	blockLoc.subtract(bedrock);
	//Bukkit.getLogger().info("DEBUG loc is after subtract:" +
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
		    block.setBiome(biome);
		    try {
			// Do not post torches because they fall off every so often
			// May have to include banners too
			//if (blocks[index] != Material.TORCH.getId()) {
			Material blockMat = Material.getMaterial(blocks[index]);
			if (blockMat != null && !attachable.contains(blockMat)) {    
			    //block.setTypeIdAndData(blocks[index], data[index], this.usePhysics);
			    nms.setBlockSuperFast(block, blocks[index], data[index], this.usePhysics);
			} else if (blocks[index] == 179) {
			    // Red sandstone - use red sand instead
			    //block.setTypeIdAndData(12, (byte)1, this.usePhysics);
			    nms.setBlockSuperFast(block, 12, (byte)1, this.usePhysics);
			}
		    } catch (Exception e) {
			e.printStackTrace();
			plugin.getLogger().info("Could not set (" + x + "," + y + "," + z + ") block ID:" + blocks[index] + " block data = " + data[index]);
		    }
		}
	    }
	}

	// Second pass - just paste attachables and deal with chests etc.
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    Block block = new Location(world, x, y, z).add(blockLoc).getBlock();
		    try {
			Material blockMat = Material.getMaterial(blocks[index]);
			if (blockMat != null && attachable.contains(blockMat)) {   
			    nms.setBlockSuperFast(block, blocks[index], data[index], this.usePhysics);
			}
			//block.setTypeIdAndData(blocks[index], data[index], this.usePhysics);
		    } catch (Exception e) {
			plugin.getLogger().info("Could not set (" + x + "," + y + "," + z + ") block ID:" + blocks[index] + " block data = " + data[index]);
		    }
		    // Tile Entities
		    if (tileEntitiesMap.containsKey(new BlockVector(x, y, z))) {
			String ver = Bukkit.getServer().getBukkitVersion();
			int major = Integer.valueOf(ver.substring(0, 1));
			int minor = Integer.valueOf(ver.substring(ver.indexOf(".") + 1, ver.indexOf(".") + 2));
			if (major >= 1 && minor >= 8) {
			    if (block.getType() == Material.STANDING_BANNER || block.getType() == Material.WALL_BANNER) {
				BannerBlock.set(block, tileEntitiesMap.get(new BlockVector(x, y, z)));
			    }
			}
			// Monster spawner blocks
			if (block.getType() == Material.MOB_SPAWNER) {
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x, y, z));
			    CreatureSpawner cs = (CreatureSpawner)block.getState();
			    String creatureType = ((StringTag) tileData.get("EntityId")).getValue().toUpperCase();
			    EntityType et = null;
			    if (WEtoME.containsKey(creatureType)) {
				et = WEtoME.get(creatureType);
			    } else {
				et = EntityType.valueOf(creatureType);
			    }
			    cs.setSpawnedType(et);

			    //Bukkit.getLogger().info("DEBUG: " + tileData);
			    /*
			    for (String key : tileData.keySet()) {
				Bukkit.getLogger().info("DEBUG: key = " + key + " : " + tileData.get(key));
				StringTag st = (StringTag) tileData.get(key);
				Bukkit.getLogger().info("DEBUG: key = " + key + " : " + st.getName() + " " + st.getValue());
			    }
			     */
			}
			// Signs
			if ((block.getType() == Material.SIGN_POST) || (block.getType() == Material.WALL_SIGN)) {
			    Sign sign = (Sign) block.getState();
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x, y, z));
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
					    try {
						lineText = text.get(line).substring(text.get(line).indexOf('"')+1,text.get(line).lastIndexOf('"'));
					    } catch (Exception e) {
						//There may not be those "'s, so just use the raw line
						lineText = text.get(line);
					    }
					} else {
					    // just in case it isn't - show the raw line
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
			    // Check if there is another chest around (double chest)
			    Chest chestBlock = (Chest) block.getState();
			    DoubleChest doubleChest = null;
			    InventoryHolder iH = chestBlock.getInventory().getHolder();
			    if (iH instanceof DoubleChest) {
				//Bukkit.getLogger().info("DEBUG: double chest");
				doubleChest = (DoubleChest) iH;
			    }
			    //Bukkit.getLogger().info("Chest tile entity found at " + x + " " + y + " " + z);
			    Map<String, Tag> tileData = tileEntitiesMap.get(new BlockVector(x, y, z));
			    try {
				ListTag chestItems = (ListTag) tileData.get("Items");
				if (chestItems != null) {
				    //int number = 0;
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
							Material itemMaterial;

							//Bukkit.getLogger().info("DEBUG: " + material);

							if (WEtoM.containsKey(material)) {
							    //Bukkit.getLogger().info("DEBUG: Found in hashmap");
							    itemMaterial = WEtoM.get(material);
							} else {
							    //Bukkit.getLogger().info("DEBUG: Not in hashmap");
							    itemMaterial = Material.valueOf(material);
							}
							short itemDamage = (Short) ((CompoundTag) item).getValue().get("Damage").getValue();
							byte itemAmount = (Byte) ((CompoundTag) item).getValue().get("Count").getValue();
							byte itemSlot = (Byte) ((CompoundTag) item).getValue().get("Slot").getValue();
							ItemStack chestItem = new ItemStack(itemMaterial, itemAmount, itemDamage);
							if (doubleChest != null) {
							    // This does not respect the original order so gaps won't be there.
							    doubleChest.getInventory().addItem(chestItem);
							} else {
							    chestBlock.getInventory().setItem(itemSlot, chestItem);
							}
							//number++;
						    }
						} catch (Exception exx) {
						    // Bukkit.getLogger().info(item.toString());
						    // Bukkit.getLogger().info(((CompoundTag)item).getValue().get("id").getName());
						    Bukkit.getLogger().severe(
							    "Could not parse item [" + itemType.substring(10).toUpperCase() + "] in schematic - skipping!");
						    // Bukkit.getLogger().severe(item.toString());
						    //exx.printStackTrace();
						}

					    }

					    // Bukkit.getLogger().info("Set chest inventory slot "
					    // + itemSlot + " to " +
					    // chestItem.toString());
					}
				    }
				    //Bukkit.getLogger().info("Added " + number + " items to chest");
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
	// PASTE ENTS
	//Bukkit.getLogger().info("Block loc = " + blockLoc);
	if (pasteEntities) {
	    for (EntityObject ent : entitiesList) {
		Location entitySpot = ent.getLocation().toLocation(blockLoc.getWorld()).add(blockLoc.toVector());
		entitySpot.setPitch(ent.getPitch());
		entitySpot.setYaw(ent.getYaw());
		//Bukkit.getLogger().info("Spawning " + ent.getType().toString() + " at " + entitySpot);
		Entity spawned = blockLoc.getWorld().spawnEntity(entitySpot, ent.getType());
		spawned.setVelocity(ent.getMotion());
		if (ent.getType() == EntityType.SHEEP) {
		    Sheep sheep = (Sheep)spawned;
		    if (ent.isSheared()) {   
			sheep.setSheared(true);
		    }
		    DyeColor[] set = DyeColor.values();
		    sheep.setColor(set[ent.getColor()]);
		    sheep.setAge(ent.getAge());
		} else if (ent.getType() == EntityType.HORSE) {
		    Horse horse = (Horse)spawned;
		    Horse.Color[] set = Horse.Color.values();
		    horse.setColor(set[ent.getColor()]);
		    horse.setAge(ent.getAge());
		    horse.setCarryingChest(ent.isCarryingChest());
		} else if (ent.getType() == EntityType.VILLAGER) {
		    Villager villager = (Villager)spawned;
		    villager.setAge(ent.getAge());
		    Profession[] proffs = Profession.values();
		    villager.setProfession(proffs[ent.getProfession()]);
		} else if (ent.getType() == EntityType.RABBIT) {
		    Rabbit rabbit = (Rabbit)spawned;
		    Rabbit.Type[] set = Rabbit.Type.values();
		    rabbit.setRabbitType(set[ent.getRabbitType()]);
		    rabbit.setAge(ent.getAge());
		} else if (ent.getType() == EntityType.OCELOT) {
		    Ocelot cat = (Ocelot)spawned;
		    if (ent.isOwned()) {
			cat.setTamed(true);
			cat.setOwner(player);
		    }
		    Ocelot.Type[] set = Ocelot.Type.values();
		    cat.setCatType(set[ent.getCatType()]);
		    cat.setAge(ent.getAge());
		    cat.setSitting(ent.isSitting());
		} else if (ent.getType() == EntityType.WOLF) {
		    Wolf wolf = (Wolf)spawned;
		    if (ent.isOwned()) {
			wolf.setTamed(true);
			wolf.setOwner(player);
		    }
		    wolf.setAge(ent.getAge());
		    wolf.setSitting(ent.isSitting());
		    DyeColor[] color = DyeColor.values();
		    wolf.setCollarColor(color[ent.getCollarColor()]);
		}
	    }
	}
	// TODO do this on load because it can be pre-processed
	// Go through all the grass blocks and try to find a safe one
	final Location grass;
	if (topGrass != null) {
	    Location gr = topGrass.clone().toLocation(loc.getWorld()).subtract(bedrock);
	    gr.add(loc.toVector());
	    gr.add(new Vector(0.5D,1.1D,0.5D)); // Center of block and a bit up so the animal drops a bit
	    grass = gr;
	} else {
	    grass = null;
	}	

	//Bukkit.getLogger().info("DEBUG cow location " + grass);
	Block blockToChange = null;
	// world.spawnEntity(grass, EntityType.COW);
	// Place a helpful sign in front of player
	if (welcomeSign != null) {
	    // Bukkit.getLogger().info("DEBUG welcome sign schematic relative is:"
	    // + welcomeSign.toString());
	    Vector ws = welcomeSign.clone().subtract(bedrock);
	    // Bukkit.getLogger().info("DEBUG welcome sign relative to bedrock is:"
	    // + welcomeSign.toString());
	    ws.add(loc.toVector());
	    // Bukkit.getLogger().info("DEBUG welcome sign actual position is:"
	    // + welcomeSign.toString());
	    blockToChange = ws.toLocation(world).getBlock();
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
	if (chest != null) {
	    Vector ch = chest.clone().subtract(bedrock);
	    ch.add(loc.toVector());
	    // Place the chest - no need to use the safe spawn function because we
	    // know what this island looks like
	    blockToChange = ch.toLocation(world).getBlock();
	    // Bukkit.getLogger().info("Chest block = " + blockToChange);
	    // blockToChange.setType(Material.CHEST);
	    // Bukkit.getLogger().info("Chest item settings = " +
	    // Settings.chestItems[0]);
	    // Bukkit.getLogger().info("Chest item settings length = " +
	    // Settings.chestItems.length);
	    if (useDefaultChest) {
		// Fill the chest
		if (blockToChange.getType() == Material.CHEST) {
		    final Chest islandChest = (Chest) blockToChange.getState();
		    DoubleChest doubleChest = null;
		    InventoryHolder iH = islandChest.getInventory().getHolder();
		    if (iH instanceof DoubleChest) {
			//Bukkit.getLogger().info("DEBUG: double chest");
			doubleChest = (DoubleChest) iH;
		    }
		    if (doubleChest != null) {
			Inventory inventory = doubleChest.getInventory();
			inventory.clear();
			inventory.setContents(defaultChestItems);
		    } else {
			Inventory inventory = islandChest.getInventory();
			inventory.clear();
			inventory.setContents(defaultChestItems);
		    }
		}
	    }
	}
	if (!islandCompanion.isEmpty() && grass != null) {
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
    public void generateIslandBlocks(final Location islandLoc, final Player player) {
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
		b.setBiome(biome);
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
	final Location location = new Location(world, x, (Settings.island_level + 5), z - 2);

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
	if (!islandCompanion.isEmpty()) {
	    Bukkit.getServer().getScheduler().runTaskLater(ASkyBlock.getPlugin(), new Runnable() {
		@Override
		public void run() {
		    spawnCompanion(player, location);
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
     * Spawns a random companion for the player with a random name at the location given
     * @param player
     * @param location
     */
    protected void spawnCompanion(Player player, Location location) {
	// Older versions of the server require custom names to only apply to Living Entities
	//Bukkit.getLogger().info("DEBUG: spawning compantion at " + location);
	if (!islandCompanion.isEmpty() && location != null) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt(islandCompanion.size());
	    EntityType type = islandCompanion.get(randomNum);
	    if (type != null) {
		LivingEntity companion = (LivingEntity) location.getWorld().spawnEntity(location, type);
		if (!companionNames.isEmpty()) {
		    randomNum = rand.nextInt(companionNames.size());
		    String name = companionNames.get(randomNum).replace("[player]", player.getDisplayName());
		    //plugin.getLogger().info("DEBUG: name is " + name);
		    companion.setCustomName(name);
		    companion.setCustomNameVisible(true);
		} 
	    }
	}
    }

    /**
     * @param islandCompanion the islandCompanion to set
     */
    public void setIslandCompanion(List<EntityType> islandCompanion) {
	this.islandCompanion = islandCompanion;
    }

    /**
     * @param companionNames the companionNames to set
     */
    public void setCompanionNames(List<String> companionNames) {
	this.companionNames = companionNames;
    }

    /**
     * @param defaultChestItems the defaultChestItems to set
     */
    public void setDefaultChestItems(ItemStack[] defaultChestItems) {
	this.defaultChestItems = defaultChestItems;
    }

    /**
     * @return if Biome is HELL, this is true
     */
    public boolean isInNether() {
	if (biome == Biome.HELL) {
	    return true;
	}
	return false;
    }

    /**
     * @return the partnerName
     */
    public String getPartnerName() {
	return partnerName;
    }

    /**
     * @param partnerName the partnerName to set
     */
    public void setPartnerName(String partnerName) {
	this.partnerName = partnerName;
    }

    /**
     * @return the pasteEntities
     */
    public boolean isPasteEntities() {
	return pasteEntities;
    }

    /**
     * @param pasteEntities the pasteEntities to set
     */
    public void setPasteEntities(boolean pasteEntities) {
	this.pasteEntities = pasteEntities;
    }

    /**
     * Whether the schematic is visible or not
     * @return the visible
     */
    public boolean isVisible() {
	return visible;
    }

    /**
     * Sets if the schematic can be seen in the schematics GUI or not by the player
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
	this.visible = visible;
    }

    /**
     * @return the order
     */
    public int getOrder() {
	return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
	this.order = order;
    }


    /**
     * @return true if player spawn exists in this schematic
     */
    public boolean isPlayerSpawn() {
	if (playerSpawn == null) {
	    return false;
	}
	return true;
    }

    /**
     * @return the playerSpawn Location given a paste location
     */
    public Location getPlayerSpawn(Location pasteLocation) {
	return pasteLocation.clone().add(playerSpawn);
    }

    /**
     * @param playerSpawnBlock the playerSpawnBlock to set
     * @return true if block is found otherwise false
     */
    public boolean setPlayerSpawnBlock(Material playerSpawnBlock) {
	// Run through the schematic and try and find the spawnBlock
	playerSpawn = null;
	int playerSpawnIndex = 0;
	int blockCount = 0;
	for (int x = 0; x < width; ++x) {
	    for (int y = 0; y < height; ++y) {
		for (int z = 0; z < length; ++z) {
		    int index = y * width * length + z * width + x;
		    if (playerSpawnBlock != null && blocks[index] == playerSpawnBlock.getId()) {
			// Where the player should spawn
			playerSpawn = new Vector(x, y, z);
			playerSpawnIndex = index;
			blockCount++;
		    }
		}
	    }
	}
	if (blockCount > 1) {
	    // Block has to be unique
	    return false;
	}
	// Remove the player spawn block if it exists
	if (playerSpawn != null && bedrock != null) {
	    // Set to air
	    blocks[playerSpawnIndex] = 0;
	    // Correct spawn location block for use later. Just need to add the pasting location
	    playerSpawn.subtract(bedrock).add(new Vector(0.5D,0D,0.5D));
	    return true;
	}
	return false;
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return NMSAbstraction class
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private NMSAbstraction checkVersion() throws ClassNotFoundException, IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
    NoSuchMethodException {
	String serverPackageName = plugin.getServer().getClass().getPackage().getName();
	String pluginPackageName = plugin.getClass().getPackage().getName();
	String version = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
	Class<?> clazz;
	try {
	    //plugin.getLogger().info("DEBUG: Trying " + pluginPackageName + ".nms." + version + ".NMSHandler");
	    clazz = Class.forName(pluginPackageName + ".nms." + version + ".NMSHandler");
	} catch (Exception e) {
	    plugin.getLogger().info("No NMS Handler found, falling back to Bukkit API.");
	    clazz = Class.forName(pluginPackageName + ".nms.fallback.NMSHandler");
	}
	//plugin.getLogger().info("DEBUG: " + serverPackageName);
	//plugin.getLogger().info("DEBUG: " + pluginPackageName);
	// Check if we have a NMSAbstraction implementing class at that location.
	if (NMSAbstraction.class.isAssignableFrom(clazz)) {
	    return (NMSAbstraction) clazz.getConstructor().newInstance();
	} else {
	    throw new IllegalStateException("Class " + clazz.getName() + " does not implement NMSAbstraction");
	}
    }

}