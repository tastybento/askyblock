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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.wasteofplastic.askyblock.util.Util;

/**
 * Stores all the info about an island
 * Managed by GridManager
 *
 * @author tastybento
 *
 */
public class Island {
    ASkyBlock plugin;
    // Coordinates of the island area
    private int minX;
    private int minZ;
    // Coordinates of minimum protected area
    private int minProtectedX;
    private int minProtectedZ;
    // Protection size
    private int protectionRange;
    // Height of island
    private int y;
    // The actual center of the island itself
    private Location center;
    // World the island is in
    private World world;
    // The owner of the island
    private UUID owner;
    // Time parameters
    private long createdDate;
    private long updatedDate;
    // A password associated with the island
    private String password;
    // Votes for how awesome the island is
    private int votes;
    private int islandDistance;
    private boolean locked = false;
    // Set if this island is a spawn island
    private boolean isSpawn = false;
    // Protection against deletion or not
    private boolean purgeProtected;
    // The spawn point
    private Location spawnPoint;
    // Tile entities
    private Multiset<Material> tileEntityCount = HashMultiset.create();
    // Biome
    private Biome biome;

    // Island protection settings
    private static final List<String> islandSettingsKey = new ArrayList<>();
    static {
        islandSettingsKey.clear();
        islandSettingsKey.add("");
    }
    private HashMap<SettingsFlag, Boolean> igs = new HashMap<>();
    private int levelHandicap;
    /**
     * Island Guard Setting flags
     * Covers island, spawn and system settings
     */
    public enum SettingsFlag {
        /**
         * Water is acid above sea level
         */
        ACID_DAMAGE,
        /**
         * Anvil use
         */
        ANVIL,
        /**
         * Armor stand use
         */
        ARMOR_STAND,
        /**
         * Beacon use
         */
        BEACON,
        /**
         * Bed use
         */
        BED,
        /**
         * Can break blocks
         */
        BREAK_BLOCKS,
        /**
         * Can breed animals
         */
        BREEDING,
        /**
         * Can use brewing stand
         */
        BREWING,
        /**
         * Can empty or fill buckets
         */
        BUCKET,
        /**
         * Can collect lava
         */
        COLLECT_LAVA,
        /**
         * Can collect water
         */
        COLLECT_WATER,
        /**
         * Can open chests or hoppers or dispensers
         */
        CHEST,
        /**
         * Can eat and teleport with chorus fruit
         */
        CHORUS_FRUIT,
        /**
         * Can use the work bench
         */
        CRAFTING,
        /**
         * Allow creepers to hurt players (but not damage blocks)
         */
        CREEPER_PAIN,
        /**
         * Can trample crops
         */
        CROP_TRAMPLE,
        /**
         * Can open doors or trapdoors
         */
        DOOR,
        /**
         * Chicken eggs can be thrown
         */
        EGGS,
        /**
         * Can use the enchanting table
         */
        ENCHANTING,
        /**
         * Can throw ender pearls
         */
        ENDER_PEARL,
        /**
         * Can toggle enter/exit names to island
         */
        ENTER_EXIT_MESSAGES,
        /**
         * Fire use/placement in general
         */
        FIRE,
        /**
         * Can extinguish fires by punching them
         */
        FIRE_EXTINGUISH,
        /**
         * Allow fire spread
         */
        FIRE_SPREAD,
        /**
         * Can use furnaces
         */
        FURNACE,
        /**
         * Can use gates
         */
        GATE,
        /**
         * Can open horse or other animal inventories, e.g. llama
         */
        HORSE_INVENTORY,
        /**
         * Can ride an animal
         */
        HORSE_RIDING,
        /**
         * Can hurt friendly mobs, e.g. cows
         */
        HURT_MOBS,
        /**
         * Can hurt monsters
         */
        HURT_MONSTERS,
        /**
         * Can leash or unleash animals
         */
        LEASH,
        /**
         * Can use buttons or levers
         */
        LEVER_BUTTON,
        /**
         * Animals, etc. can spawn
         */
        MILKING,
        /**
         * Can do PVP in the nether
         */
        MOB_SPAWN,
        /**
         * Monsters can spawn
         */
        MONSTER_SPAWN,
        /**
         * Can operate jukeboxes, note boxes etc.
         */
        MUSIC,
        /**
         * Can place blocks
         */
        NETHER_PVP,
        /**
         * Can interact with redstone items, like diodes
         */
        PLACE_BLOCKS,
        /**
         * Can go through portals
         */
        PORTAL,
        /**
         * Will activate pressure plates
         */
        PRESSURE_PLATE,
        /**
         * Can do PVP in the overworld
         */
        PVP,
        /**
         * Cows can be milked
         */
        REDSTONE,
        /**
         * Spawn eggs can be used
         */
        SPAWN_EGGS,
        /**
         * Can shear sheep
         */
        SHEARING,
        /**
         * Can trade with villagers
         */
        VILLAGER_TRADING,
        /**
         * Visitors can drop items
         */
        VISITOR_ITEM_DROP,
        /**
         * Visitors can pick up items
         */
        VISITOR_ITEM_PICKUP
    }


    /**
     * New island by loading islands.yml
     * @param plugin - ASkyBlock plugin object
     * @param serial
     * @param settingsKey
     */
    public Island(ASkyBlock plugin, String serial, List<String> settingsKey) {
        this.plugin = plugin;
        // Bukkit.getLogger().info("DEBUG: adding serialized island to grid ");
        // Deserialize
        // Format:
        // x:height:z:protection range:island distance:owner UUID: locked: protected
        String[] split = serial.split(":");
        try {
            protectionRange = Integer.parseInt(split[3]);
            islandDistance = Integer.parseInt(split[4]);
            int x = Integer.parseInt(split[0]);
            int z = Integer.parseInt(split[2]);
            minX = x - islandDistance / 2;
            y = Integer.parseInt(split[1]);
            minZ = z - islandDistance / 2;
            minProtectedX = x - protectionRange / 2;
            minProtectedZ = z - protectionRange / 2;
            this.world = ASkyBlock.getIslandWorld();
            this.center = new Location(world, x, y, z);
            this.createdDate = new Date().getTime();
            this.updatedDate = createdDate;
            this.password = "";
            this.votes = 0;
            if (split.length > 6) {
                // Bukkit.getLogger().info("DEBUG: " + split[6]);
                // Get locked status
                this.locked = split[6].equalsIgnoreCase("true");
                // Bukkit.getLogger().info("DEBUG: " + locked);
            } else {
                this.locked = false;
            }
            // Check if deletable
            if (split.length > 7) {
                this.purgeProtected = split[7].equalsIgnoreCase("true");
            } else {
                this.purgeProtected = false;
            }
            if (!split[5].equals("null")) {
                if (split[5].equals("spawn")) {
                    isSpawn = true;
                    // Try to get the spawn point
                    if (split.length > 8) {
                        //plugin.getLogger().info("DEBUG: " + serial.substring(serial.indexOf(":SP:") + 4));
                        spawnPoint = Util.getLocationString(serial.substring(serial.indexOf(":SP:") + 4));
                    }
                } else {
                    owner = UUID.fromString(split[5]);
                }
            }
            // Check if protection options there
            if (split.length > 8) {
                setSettings(split[8], settingsKey);
            } else {
                setSettings(null, settingsKey);
            }

            // Get the biome
            if (split.length > 9) {
                try {
                    biome = Biome.valueOf(split[9]);

                } catch (IllegalArgumentException ee) {
                    // Unknown biome
                }
            }
            // Get island level handicap
            if (split.length > 10) {
                try {
                    this.levelHandicap = Integer.valueOf(split[10]);
                } catch (Exception e) {
                    this.levelHandicap = 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets the protection settings to their default as set in config.yml for this island
     */
    public void setIgsDefaults() {
        for (SettingsFlag flag: SettingsFlag.values()) {
            if (!Settings.defaultIslandSettings.containsKey(flag)) {
                // Default default
                if (flag.equals(SettingsFlag.MOB_SPAWN) || flag.equals(SettingsFlag.MONSTER_SPAWN)) {
                    this.igs.put(flag, true);
                } else {
                    this.igs.put(flag, false);
                }
            } else {
                if (Settings.defaultIslandSettings.get(flag) == null) {
                    //plugin.getLogger().info("DEBUG: null flag " + flag);
                    if (flag.equals(SettingsFlag.MOB_SPAWN) || flag.equals(SettingsFlag.MONSTER_SPAWN)) {
                        this.igs.put(flag, true);
                    } else {
                        this.igs.put(flag, false);
                    }
                } else {
                    this.igs.put(flag, Settings.defaultIslandSettings.get(flag));
                }
            }
        }
    }

    /**
     * Reset spawn protection settings to their default as set in config.yml for this island
     */
    public void setSpawnDefaults() {
        for (SettingsFlag flag: SettingsFlag.values()) {
            if (!Settings.defaultSpawnSettings.containsKey(flag)) {
                // Default default
                if (flag.equals(SettingsFlag.MOB_SPAWN) || flag.equals(SettingsFlag.MONSTER_SPAWN)) {
                    this.igs.put(flag, true);
                } else {
                    this.igs.put(flag, false);
                }
            } else {
                if (Settings.defaultSpawnSettings.get(flag) == null) {
                    if (flag.equals(SettingsFlag.MOB_SPAWN) || flag.equals(SettingsFlag.MONSTER_SPAWN)) {
                        this.igs.put(flag, true);
                    } else {
                        this.igs.put(flag, false);
                    }
                } else {
                    this.igs.put(flag, Settings.defaultSpawnSettings.get(flag));
                }
            }
        }
    }

    /**
     * Add a new island using the island center method
     * @param plugin - ASkyBlock plugin object
     * @param x
     * @param z
     */
    public Island(ASkyBlock plugin, int x, int z) {
        this(plugin, x, z, null);
    }

    public Island(ASkyBlock plugin, int x, int z, UUID owner) {
        this.plugin = plugin;
        // Calculate min minX and z
        this.minX = x - Settings.islandDistance / 2;
        this.minZ = z - Settings.islandDistance / 2;
        this.minProtectedX = x - Settings.islandProtectionRange / 2;
        this.minProtectedZ = z - Settings.islandProtectionRange / 2;
        this.y = Settings.islandHeight;
        this.islandDistance = Settings.islandDistance;
        this.protectionRange = Settings.islandProtectionRange;
        this.world = ASkyBlock.getIslandWorld();
        this.center = new Location(world, x, y, z);
        this.createdDate = new Date().getTime();
        this.updatedDate = createdDate;
        this.password = "";
        this.votes = 0;
        this.owner = owner;
        // Island Guard Settings
        setIgsDefaults();
    }

    /**
     * Copy constructor
     * @param island - island to copy
     */
    public Island(Island island) {
        this.plugin = island.plugin;
        this.biome = island.biome == null ? null : Biome.valueOf(island.biome.name());
        this.center = island.center != null ? island.center.clone() : null;
        this.createdDate = Long.valueOf(island.createdDate);
        island.igs.forEach((k,v) -> this.igs.put(k, v));
        this.islandDistance = Integer.valueOf(island.islandDistance);
        this.isSpawn = Boolean.valueOf(island.isSpawn);
        this.locked = Boolean.valueOf(island.locked);
        this.levelHandicap = Integer.valueOf(island.levelHandicap);
        this.minProtectedX = Integer.valueOf(island.minProtectedX);
        this.minProtectedZ = Integer.valueOf(island.minProtectedZ);
        this.minX = Integer.valueOf(island.minX);
        this.minZ = Integer.valueOf(island.minZ);
        this.owner = island.owner == null ? null : UUID.fromString(island.owner.toString());
        this.password = island.password;
        this.protectionRange = Integer.valueOf(island.protectionRange);
        this.purgeProtected = Boolean.valueOf(island.purgeProtected);
        this.spawnPoint = island.spawnPoint == null ? null : island.spawnPoint.clone();
        this.tileEntityCount.addAll(island.tileEntityCount);
        this.updatedDate = Long.valueOf(island.updatedDate);
        this.votes = Integer.valueOf(island.votes);
        this.world = island.world == null ? null : Bukkit.getWorld(island.world.getUID());
        this.y = Integer.valueOf(island.y);
    }

    /**
     * Checks if a location is within this island's protected area
     *
     * @param target location to query
     * @return true if it is, false if not
     */
    public boolean onIsland(Location target) {
        if (world != null) {
            // If the new nether is being used, islands exist in the nether too
            if (target.getWorld().equals(world) || (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null && target.getWorld().equals(ASkyBlock.getNetherWorld()))) {
                return target.getBlockX() >= minProtectedX && target.getBlockX() < (minProtectedX
                        + protectionRange)
                        && target.getBlockZ() >= minProtectedZ && target.getBlockZ() < (minProtectedZ
                                + protectionRange);
            }
        }
        return false;
    }

    /**
     * Checks if location is anywhere in the island space (island distance)
     *
     * @param target location to query
     * @return true if in the area
     */
    public boolean inIslandSpace(Location target) {
        if (target.getWorld().equals(ASkyBlock.getIslandWorld()) || target.getWorld().equals(ASkyBlock.getNetherWorld())) {
            return target.getX() >= center.getBlockX() - islandDistance / 2
                    && target.getX() < center.getBlockX() + islandDistance / 2
                    && target.getZ() >= center.getBlockZ() - islandDistance / 2
                    && target.getZ() < center.getBlockZ() + islandDistance / 2;
        }
        return false;
    }

    public boolean inIslandSpace(int x, int z) {
        return x >= center.getBlockX() - islandDistance / 2
                && x < center.getBlockX() + islandDistance / 2
                && z >= center.getBlockZ() - islandDistance / 2
                && z < center.getBlockZ() + islandDistance / 2;
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return minX;
    }

    /**
     * @param minX
     *            the minX to set
     */
    public void setMinX(int minX) {
        this.minX = minX;
    }

    /**
     * @return the z
     */
    public int getMinZ() {
        return minZ;
    }

    /**
     * @param minZ
     *            the z to set
     */
    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    /**
     * @return the minprotectedX
     */
    public int getMinProtectedX() {
        return minProtectedX;
    }

    /**
     * @return the minProtectedZ
     */
    public int getMinProtectedZ() {
        return minProtectedZ;
    }

    /**
     * @return the protectionRange
     */
    public int getProtectionSize() {
        return protectionRange;
    }

    /**
     * @param protectionSize
     *            the protectionSize to set
     */
    public void setProtectionSize(int protectionSize) {
        this.protectionRange = protectionSize;
        this.minProtectedX = center.getBlockX() - protectionSize / 2;
        this.minProtectedZ = center.getBlockZ() - protectionSize / 2;

    }

    /**
     * @return the islandDistance
     */
    public int getIslandDistance() {
        return islandDistance;
    }

    /**
     * @param islandDistance
     *            the islandDistance to set
     */
    public void setIslandDistance(int islandDistance) {
        this.islandDistance = islandDistance;
    }

    /**
     * @return the center
     */
    public Location getCenter() {
        return center;
    }

    /**
     * @param center
     *            the center to set
     */
    public void setCenter(Location center) {
        this.center = center;
    }

    /**
     * @return the owner
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(UUID owner) {
        this.owner = owner;
        //if (owner == null) {
        //    Bukkit.getLogger().info("DEBUG: island owner set to null for " + center);
        //}
    }

    /**
     * @return the createdDate
     */
    public long getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the updatedDate
     */
    public long getUpdatedDate() {
        return updatedDate;
    }

    /**
     * @param updatedDate
     *            the updatedDate to set
     */
    public void setUpdatedDate(long updatedDate) {
        this.updatedDate = updatedDate;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     *            the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the votes
     */
    public int getVotes() {
        return votes;
    }

    /**
     * @param votes
     *            the votes to set
     */
    public void setVotes(int votes) {
        this.votes = votes;
    }

    /**
     * @return the locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked
     *            the locked to set
     */
    public void setLocked(boolean locked) {
        // Bukkit.getLogger().info("DEBUG: island is now " + locked);
        this.locked = locked;
    }

    /**
     * Serializes the island for island.yml storage
     * @return string that represents the island settings
     */
    public String save() {
        // x:height:z:protection range:island distance:owner UUID
        String ownerString = "null";
        if (isSpawn) {
            // Bukkit.getLogger().info("DEBUG: island is spawn");
            ownerString = "spawn";
            if (spawnPoint != null) {
                return center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ() + ":" + protectionRange + ":"
                        + islandDistance + ":" + ownerString + ":" + locked + ":" + purgeProtected + ":SP:" + Util.getStringLocation(spawnPoint);
            }
            return center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ() + ":" + protectionRange + ":"
            + islandDistance + ":" + ownerString + ":" + locked + ":" + purgeProtected;
        }
        // Not spawn
        if (owner != null) {
            ownerString = owner.toString();
        }

        return center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ() + ":" + protectionRange + ":"
        + islandDistance + ":" + ownerString + ":" + locked + ":" + purgeProtected + ":" + getSettings() + ":" + getBiome().toString() + ":" + levelHandicap;
    }

    /**
     * @return Serialized set of settings
     */
    public String getSettings() {
        String result = "";
        // Personal island protection settings - serialize enum into 1's and 0's representing the boolean values
        //plugin.getLogger().info("DEBUG: igs = " + igs.toString());
        try {
            for (SettingsFlag f: SettingsFlag.values()) {
                //plugin.getLogger().info("DEBUG: flag f = " + f);
                if (this.igs.containsKey(f)) {
                    //plugin.getLogger().info("DEBUG: contains key");
                    result += this.igs.get(f) ? "1" : "0";
                } else {
                    //plugin.getLogger().info("DEBUG: does not contain key");
                    result += "0";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "";
        }
        return result;
    }

    /**
     * Get the Island Guard flag status
     * @param flag - settings flag to check
     * @return true or false, or false if flag is not in the list
     */
    public boolean getIgsFlag(SettingsFlag flag) {
        //plugin.getLogger().info("DEBUG: asking for " + flag + " = " + igs.get(flag));
        if (this.igs.containsKey(flag)) {
            return igs.get(flag);
        }
        return false;
    }

    /**
     * Set the Island Guard flag
     * @param flag - settings flag to check
     * @param value - value to set true or false
     */
    public void setIgsFlag(SettingsFlag flag, boolean value) {
        this.igs.put(flag, value);
    }

    /**
     * Provides a list of all the players who are allowed on this island
     * including coop members
     *
     * @return a list of UUIDs that have legitimate access to the island
     */
    public List<UUID> getMembers() {
        List<UUID> result = new ArrayList<UUID>();
        // Add any coop members for this island
        result.addAll(CoopPlay.getInstance().getCoopPlayers(center.toVector().toLocation(ASkyBlock.getIslandWorld())));
        if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null) {
            result.addAll(CoopPlay.getInstance().getCoopPlayers(center.toVector().toLocation(ASkyBlock.getNetherWorld())));
        }
        if (owner == null) {
            return result;
        }
        result.add(owner);
        // Add any team members
        result.addAll(plugin.getPlayers().getMembers(owner));
        return result;
    }

    /**
     * @return the isSpawn
     */
    public boolean isSpawn() {
        return isSpawn;
    }

    /**
     * @param isSpawn
     *            the isSpawn to set
     */
    public void setSpawn(boolean isSpawn) {
        this.isSpawn = isSpawn;
    }

    /**
     * @return the islandDeletable
     */
    public boolean isPurgeProtected() {
        return purgeProtected;
    }

    /**
     * @param purgeProtected the islandDeletable to set
     */
    public void setPurgeProtected(boolean purgeProtected) {
        this.purgeProtected = purgeProtected;
    }

    /**
     * @return Provides count of villagers within the protected island boundaries
     */
    public int getPopulation() {
        int result = 0;
        for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionSize() - 1)/16; x++) {
            for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionSize() - 1)/16; z++) {
                for (Entity entity : world.getChunkAt(x, z).getEntities()) {
                    if (entity instanceof Villager && onIsland(entity.getLocation())) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return number of hoppers on the island
     */
    public int getHopperCount() {
        tileEntityCount.clear();
        int result = 0;
        for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionSize() - 1)/16; x++) {
            for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionSize() - 1)/16; z++) {
                for (BlockState holder : world.getChunkAt(x, z).getTileEntities()) {
                    if (holder instanceof Hopper && onIsland(holder.getLocation())) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @param material Bukkit material to check
     * @param world - world to check
     * @return count of how many tile entities of type mat are on the island at last count. Counts are done when a player places
     * a tile entity.
     */
    public int getTileEntityCount(Material material, World world) {
        int result = 0;
        for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionSize() - 1)/16; x++) {
            for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionSize() - 1)/16; z++) {
                for (BlockState holder : world.getChunkAt(x, z).getTileEntities()) {
                    //plugin.getLogger().info("DEBUG: tile entity: " + holder.getType());
                    if (onIsland(holder.getLocation())) {
                        if (holder.getType() == material) {
                            result++;
                        } else if (material.equals(Material.REDSTONE_COMPARATOR_OFF)) {
                            if (holder.getType().equals(Material.REDSTONE_COMPARATOR_ON)) {
                                result++;
                            }
                        } else if (material.equals(Material.FURNACE)) {
                            if (holder.getType().equals(Material.BURNING_FURNACE)) {
                                result++;
                            }
                        } else if (material.toString().endsWith("BANNER")) {
                            if (holder.getType().toString().endsWith("BANNER")) {
                                result++;
                            }
                        } else if (material.equals(Material.WALL_SIGN) || material.equals(Material.SIGN_POST)) {
                            if (holder.getType().equals(Material.WALL_SIGN) || holder.getType().equals(Material.SIGN_POST)) {
                                result++;
                            }
                        }
                    }
                }
                for (Entity holder : world.getChunkAt(x, z).getEntities()) {
                    //plugin.getLogger().info("DEBUG: entity: " + holder.getType());
                    if (holder.getType().toString().equals(material.toString()) && onIsland(holder.getLocation())) {
                        result++;
                    }
                }
            }
        }
        // Version 1.7.x counts differently to 1.8 (ugh)
        // In 1.7, the entity is present before it is cancelled and so gets counted.
        // Remove 1 from count if it is 1.7.x
        if (!plugin.isOnePointEight()) {
            result--;
        }
        return result;
    }

    public void setSpawnPoint(Location location) {
        spawnPoint = location;

    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * Toggles the Island Guard Flag
     * @param flag  - settings flag to toggle
     */
    public void toggleIgs(SettingsFlag flag) {
        if (igs.containsKey(flag)) {
            igs.put(flag, !igs.get(flag));
        }

    }

    /**
     * @return the biome
     */
    public Biome getBiome() {
        if (biome == null) {
            biome = center.getBlock().getBiome();
        }
        return biome;
    }

    /**
     * @param biome the biome to set
     */
    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
    }

    /**
     * Sets the settings for the island.
     * @param settings - string of 0 and 1's that define the settings for the island
     * @param settingsKey - a list showing the order and what flags each digit refers to
     */
    public void setSettings(String settings, List<String> settingsKey) {

        // Start with defaults
        if (isSpawn) {
            setSpawnDefaults();
        } else {
            setIgsDefaults();
        }
        if(settings == null || settings.isEmpty())
            return;
        if (settingsKey.size() != settings.length()) {
            plugin.getLogger().severe("Island settings does not match settings key in islands.yml. Using defaults.");
            return;
        }
        for (int i = 0; i < settingsKey.size(); i++) {
            try {
                if (settings.charAt(i) == '0') {
                    this.setIgsFlag(SettingsFlag.valueOf(settingsKey.get(i)), false);
                } else {
                    this.setIgsFlag(SettingsFlag.valueOf(settingsKey.get(i)), true);
                }
            } catch (Exception e) {
                // do nothing - bad value, probably a downgrade
            }
        }

    }

}
