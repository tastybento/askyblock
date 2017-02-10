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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.registry.WorldData;
import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.org.jnbt.Tag;

public class Schematic {
    private ASkyBlock plugin;
    //private short[] blocks;
    //private byte[] data;
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
    private boolean visible;
    private int order;
    // These hashmaps enable translation between WorldEdit strings and Bukkit names
    //private HashMap<String, EntityType> WEtoME = new HashMap<String, EntityType>();
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
    private int durability;
    private int levelHandicap;
    private double cost;
    private Vector minPoint;
    private boolean pasteAir;
    private EditSession editSession;
    private ClipboardHolder clipboardHolder;
    private WorldEditPlugin we;

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
        biome = null;
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
        //playerSpawnBlock = null;
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
        biome = null;
        usePhysics = Settings.usePhysics;
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
        //playerSpawnBlock = null;
        partnerName = "";
        this.file = file;
        we = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
    }

    /**
     * Loads the schematic into memory
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public void loadSchematic() throws IOException {
        plugin.getLogger().info("DEBUG: loading schematic");
        // This section uses WorldEdit to load the schematic into memory.
        // This is done so that the schematic can be pasted when required.
        if (we != null) {
            Closer closer = Closer.create();
            FileInputStream fileIn = new FileInputStream(file);
            FileInputStream fileInputStream = closer.register(fileIn);
            BufferedInputStream bufIn = new BufferedInputStream(fileInputStream);
            BufferedInputStream bufferedInputStream = closer.register(bufIn);
            ClipboardReader clipboardReader = ClipboardFormat.SCHEMATIC.getReader(bufferedInputStream);
            com.sk89q.worldedit.world.World world = LocalWorldAdapter.adapt(new BukkitWorld(ASkyBlock.getIslandWorld()));
            WorldData worldData = world.getWorldData();
            clipboardHolder = new ClipboardHolder(clipboardReader.read(worldData), worldData);
            clipboardHolder.getClipboard().setOrigin(clipboardHolder.getClipboard().getMinimumPoint());
            bufferedInputStream.close();
            fileIn.close();
            bufIn.close();
            closer.close();


            // Check for key blocks
            // Find top most bedrock - this is the key stone
            // Find top most chest
            // Find top most grass
            minPoint = new Vector(clipboardHolder.getClipboard().getMinimumPoint().getBlockX(),
                    clipboardHolder.getClipboard().getMinimumPoint().getBlockY(),
                    clipboardHolder.getClipboard().getMinimumPoint().getBlockZ());
            List<Vector> grassBlocks = new ArrayList<Vector>();
            for (int y = clipboardHolder.getClipboard().getMinimumPoint().getBlockY(); y <= clipboardHolder.getClipboard().getMaximumPoint().getBlockY(); y ++) {
                for (int x = clipboardHolder.getClipboard().getMinimumPoint().getBlockX(); x <= clipboardHolder.getClipboard().getMaximumPoint().getBlockX(); x++) {
                    for (int z = clipboardHolder.getClipboard().getMinimumPoint().getBlockZ(); z <= clipboardHolder.getClipboard().getMaximumPoint().getBlockZ(); z++) {
                        BaseBlock block = clipboardHolder.getClipboard().getLazyBlock(new com.sk89q.worldedit.Vector(x,y,z));
                        if (block.getId() == Material.BEDROCK.getId()) {
                            // Last bedrock
                            if (bedrock == null || bedrock.getY() < y) {
                                bedrock = new Vector(x,y,z);
                                bedrock.subtract(minPoint);
                                //Bukkit.getLogger().info("DEBUG higher bedrock found:" + bedrock.toString());
                            }
                        } else if (block.getId() == Material.CHEST.getId()) {
                            // Last chest
                            if (chest == null || chest.getY() < y) {
                                chest = new Vector(x, y, z);
                                chest.subtract(minPoint);
                                //Bukkit.getLogger().info("DEBUG: Chest relative location is " + chest.toString());
                            }
                        } else if (block.getId() == Material.SIGN_POST.getId()) {
                            //Bukkit.getLogger().info("DEBUG: sign:" + welcomeSign);
                            // Sign
                            if (welcomeSign == null || welcomeSign.getY() < y) {
                                welcomeSign = new Vector(x, y, z);
                                welcomeSign.subtract(minPoint);
                                //Bukkit.getLogger().info("DEBUG: higher sign found:" + welcomeSign.toString());
                            }
                        } else if (block.getId() == Material.GRASS.getId()) {
                            // Grass
                            grassBlocks.add((new Vector(x, y, z)).subtract(minPoint));
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
        } else {
            throw new IOException("WorldEdit not loaded!");
        }
        plugin.getLogger().info("DEBUG: loaded schematic");
    }

    /**
     * Creates an island block by block
     * @param islandLoc
     * @param player
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
        // Teleport player
        plugin.getGrid().homeTeleport(player);
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
     * @return the biome. If no biome is defined, this will return null.
     */
    public Biome getBiome() {
        return biome;
    }

    /**
     * @return the cost
     */
    public double getCost() {
        return cost;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the durability of the icon
     */
    public int getDurability() {
        return durability;
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
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the partnerName
     */
    public String getPartnerName() {
        return partnerName;
    }

    /**
     * @return the perm
     */
    public String getPerm() {
        return perm;
    }

    /**
     * @return the playerSpawn Location given a paste location
     */
    public Location getPlayerSpawn(Location pasteLocation) {
        return pasteLocation.clone().add(playerSpawn);
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
     * @return if Biome is HELL, this is true
     */
    public boolean isInNether() {
        if (biome == Biome.HELL) {
            return true;
        }
        return false;
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
     * Whether the schematic is visible or not
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /*
     * This function pastes using World Edit - problem is that because it reads a file, it's slow.
    @SuppressWarnings("deprecation")
     */
    public void pasteSchematic(final Location loc, final Player player, boolean teleport) {
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
        plugin.getLogger().info("DEBUG: Pasting schematic air paste = " + pasteAir);
        long nano = System.currentTimeMillis();
        com.sk89q.worldedit.world.World world = LocalWorldAdapter.adapt(new BukkitWorld(ASkyBlock.getIslandWorld()));
        //Bukkit.getLogger().info("DEBUG: topgrass = " + topGrass);
        if (biome != null && biome.equals(Biome.HELL) && Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
            world = LocalWorldAdapter.adapt(new BukkitWorld(ASkyBlock.getNetherWorld()));
        }
        // Obtain the size of the schematic and create an edit session with that number of blocks reserved.
        Region region = clipboardHolder.getClipboard().getRegion();
        editSession = we.getWorldEdit().getEditSessionFactory().getEditSession(world, region.getWidth() * region.getHeight() * region.getLength());
        // This enables some kind of queuing in WE. 
        editSession.enableQueue();
        // This sets the origin of the schematic to the minimum point.
        clipboardHolder.getClipboard().setOrigin(clipboardHolder.getClipboard().getMinimumPoint());
        editSession.setFastMode(!usePhysics);
        // Paste schematic using WorldEdit
        try {
            Operations.completeLegacy(clipboardHolder.createPaste(editSession, editSession.getWorld().getWorldData())
                    .to(BukkitUtil.toVector(loc).subtract(BukkitUtil.toVector(bedrock)))
                    .ignoreAirBlocks(pasteAir)
                    .build());
            editSession.flushQueue();
        }
        catch (MaxChangedBlocksException e)
        {
            plugin.getLogger().severe("Too many blocks changed.");
            e.printStackTrace();
            return;
        }
        plugin.getLogger().info("DEBUG: " + (System.currentTimeMillis()-nano));
        // Place a helpful sign in front of player 
        //plugin.getLogger().info("DEBUG: welcome sign = " + welcomeSign);
        if (welcomeSign != null) {
            Block blockToChange = loc.clone().add(welcomeSign).subtract(bedrock).getBlock();
            BlockState signState = blockToChange.getState();
            if (signState instanceof Sign) {
                Sign sign = (Sign) signState;
                if (sign.getLine(0).isEmpty() || sign.getLine(0).equals("\"\"")) {
                    sign.setLine(0, plugin.myLocale(player.getUniqueId()).signLine1.replace("[player]", player.getName()));
                }
                if (sign.getLine(1).isEmpty() || sign.getLine(1).equals("\"\"")) {
                    sign.setLine(1, plugin.myLocale(player.getUniqueId()).signLine2.replace("[player]", player.getName()));
                }
                if (sign.getLine(2).isEmpty() || sign.getLine(2).equals("\"\"")) {
                    sign.setLine(2, plugin.myLocale(player.getUniqueId()).signLine3.replace("[player]", player.getName()));
                }
                if (sign.getLine(3).isEmpty() || sign.getLine(3).equals("\"\"")) {
                    sign.setLine(3, plugin.myLocale(player.getUniqueId()).signLine4.replace("[player]", player.getName()));
                }
                sign.update();
            }
        }
        if (chest != null) {
            // Place the chest
            Block blockToChange = loc.clone().add(chest).subtract(bedrock).getBlock();
            if (useDefaultChest) {
                // Fill the chest
                if (blockToChange.getType() == Material.CHEST) {
                    final Chest islandChest = (Chest) blockToChange.getState();
                    DoubleChest doubleChest = null;
                    InventoryHolder iH = islandChest.getInventory().getHolder();
                    if (iH instanceof DoubleChest) {
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

        if (teleport) {
            if (player.getWorld().equals(loc.getWorld())) {
                player.teleport(loc.getWorld().getSpawnLocation());
            }
            if (playerSpawn == null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

                    @Override
                    public void run() {
                        plugin.getGrid().homeTeleport(player);

                    }}, 10L);
            } else {
                // We have a specific location to place the player
                Block blockToChange = loc.clone().add(playerSpawn).getBlock();
                blockToChange.setType(Material.AIR);
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

                    @Override
                    public void run() {
                        player.teleport(getPlayerSpawn(loc));
                    }
                }, 10L);
            }
        }

        if (!islandCompanion.isEmpty()) {
            // Find the grass spot
            final Location grass;
            if (topGrass != null) {
                grass = loc.clone().add(topGrass).subtract(bedrock).add(new Vector(0.5D,1.1D,0.5D));
                Bukkit.getServer().getScheduler().runTaskLater(ASkyBlock.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        spawnCompanion(player, grass);
                    }
                }, 40L);
            }
        }
        plugin.getLogger().info("DEBUG: Second time " + (System.currentTimeMillis()-nano));
    }

    /**
     * @param biome the biome to set
     */
    public void setBiome(Biome biome) {
        this.biome = biome;
    }


    /**
     * @param companionNames the companionNames to set
     */
    public void setCompanionNames(List<String> companionNames) {
        this.companionNames = companionNames;
    }

    /**
     * Set the cost
     * @param cost
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * @param defaultChestItems the defaultChestItems to set
     */
    public void setDefaultChestItems(ItemStack[] defaultChestItems) {
        this.defaultChestItems = defaultChestItems;
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

    public void setIcon(Material icon, int damage) {
        this.icon = icon;
        this.durability = damage;    
    }

    /**
     * @param islandCompanion the islandCompanion to set
     */
    public void setIslandCompanion(List<EntityType> islandCompanion) {
        this.islandCompanion = islandCompanion;
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * @param partnerName the partnerName to set
     */
    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    /**
     * Set where air should be pasted or not
     * @param pasteAir
     */
    public void setPasteAir(boolean pasteAir) {
        this.pasteAir = pasteAir;
    }

    /**
     * @param perm the perm to set
     */
    public void setPerm(String perm) {
        this.perm = perm;
    }

    /**
     * @param playerSpawnBlock the playerSpawnBlock to set
     * @return true if block is found otherwise false
     */
    @SuppressWarnings("deprecation")
    public boolean setPlayerSpawnBlock(Material playerSpawnBlock) {
        if (bedrock == null) {
            return false;
        }
        playerSpawn = null;
        // Run through the schematic and try and find the spawnBlock
        for (int y = clipboardHolder.getClipboard().getMinimumPoint().getBlockY(); y <= clipboardHolder.getClipboard().getMaximumPoint().getBlockY(); y ++) {
            for (int x = clipboardHolder.getClipboard().getMinimumPoint().getBlockX(); x <= clipboardHolder.getClipboard().getMaximumPoint().getBlockX(); x++) {
                for (int z = clipboardHolder.getClipboard().getMinimumPoint().getBlockZ(); z <= clipboardHolder.getClipboard().getMaximumPoint().getBlockZ(); z++) {
                    BaseBlock block = clipboardHolder.getClipboard().getLazyBlock(new com.sk89q.worldedit.Vector(x,y,z));
                    if (block.getId() == playerSpawnBlock.getId()) {
                        playerSpawn = new Vector(x,y,z).subtract(bedrock).subtract(minPoint).add(new Vector(0.5D,0D,0.5D));
                        return true;
                    }
                }
            }
        }
        return false;
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
     * Sets if the schematic can be seen in the schematics GUI or not by the player
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
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

}