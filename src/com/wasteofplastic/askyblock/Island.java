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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
public class Island implements Cloneable {
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
    // Stats variables
    private HashMap<EntityType, Integer> entities = new HashMap<EntityType, Integer>();
    // Protection against deletion or not
    private boolean purgeProtected;
    // The spawn point 
    private Location spawnPoint;
    // Tile entities
    private Multiset<Material> tileEntityCount = HashMultiset.create();
    // Biome
    Biome biome;
    // Island protection settings
    private HashMap<Flags, Boolean> igs = new HashMap<Flags, Boolean>();
    private int levelHandicap;
    /**
     * Island Guard Setting flags
     *
     */
    public enum Flags {
        allowAnvilUse,
        allowArmorStandUse,
        allowBeaconAccess,
        allowBedUse,
        allowBreakBlocks,
        allowBreeding,
        allowBrewing,
        allowBucketUse,
        allowChestAccess,
        allowCrafting,
        allowCropTrample,
        allowDoorUse,
        allowEnchanting,
        allowEnderPearls,
        allowFurnaceUse,
        allowGateUse,
        allowHorseInvAccess,
        allowHorseRiding,
        allowHurtMobs,
        allowLeashUse,
        allowLeverButtonUse,
        allowMusic,
        allowPlaceBlocks,
        allowPortalUse,
        allowPressurePlate,
        allowPvP,
        allowNetherPvP,
        allowRedStone,
        allowShearing,
        allowVillagerTrading,
        allowChorusFruit,
        enableJoinAndLeaveIslandMessages,
        allowMobSpawning, 
        allowVisitorItemDrop,
        allowVisitorItemPickup
    }


    /**
     * New island by loading islands.yml
     * @param plugin
     * @param serial
     */
    public Island(ASkyBlock plugin, String serial) {
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
                if (split[6].equalsIgnoreCase("true")) {
                    this.locked = true;
                } else {
                    this.locked = false;
                }
                // Bukkit.getLogger().info("DEBUG: " + locked);
            } else {
                this.locked = false;
            }
            // Check if deletable
            if (split.length > 7) {
                if (split[7].equalsIgnoreCase("true")) {
                    this.purgeProtected = true;
                } else {
                    this.purgeProtected = false;
                }
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
            if (!isSpawn) {
                //plugin.getLogger().info("DEBUG: NOT SPAWN owner is " + owner + " location " + center);
                // Set defaults
                setDefaults();
                // Load settings
                if (split.length > 8) {
                    // Parse the 8th string into island guard protection settings
                    int index = 0;
                    // Run through the enum and set
                    for (Flags f : Flags.values()) {
                        if (split[8].length() == index) {
                            break;
                        }
                        this.igs.put(f, split[8].charAt(index++) == '1' ? true : false);
                    }
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets the island protection settings to their default as set in config.yml
     */
    public void setDefaults() {
        for (Flags flag: Flags.values()) {
            this.igs.put(flag, Settings.defaultIslandSettings.get(flag));
        }
    }

    /**
     * Add a new island using the island center method
     * @param plugin
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
        this.minProtectedX = x - Settings.island_protectionRange / 2;
        this.minProtectedZ = z - Settings.island_protectionRange / 2;
        this.y = Settings.island_level;
        this.islandDistance = Settings.islandDistance;
        this.protectionRange = Settings.island_protectionRange;
        this.world = ASkyBlock.getIslandWorld();
        this.center = new Location(world, x, y, z);
        this.createdDate = new Date().getTime();
        this.updatedDate = createdDate;
        this.password = "";
        this.votes = 0;
        this.owner = owner;
        // Island Guard Settings
        setDefaults();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            // This should never happen
            throw new InternalError(e.toString());
        }
    }

    /**
     * Checks if a location is within this island's protected area
     * 
     * @param target
     * @return true if it is, false if not
     */
    public boolean onIsland(Location target) {
        if (world != null) {
            // If the new nether is being used, islands exist in the nether too
            //plugin.getLogger().info("DEBUG: target x = " + target.getBlockX() + " target z = " + target.getBlockZ());
            //plugin.getLogger().info("DEBUG: min prot x = " + minProtectedX + " min z = " + minProtectedZ);
            //plugin.getLogger().info("DEBUG: max x = " + (minProtectedX + protectionRange) + " max z = " + (minProtectedZ + protectionRange));

            if (target.getWorld().equals(world) || (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null && target.getWorld().equals(ASkyBlock.getNetherWorld()))) {
                if (target.getBlockX() >= minProtectedX && target.getBlockX() < (minProtectedX + protectionRange)
                        && target.getBlockZ() >= minProtectedZ && target.getBlockZ() < (minProtectedZ + protectionRange)) {
                    return true;
                }
                /*
                if (target.getX() >= center.getBlockX() - protectionRange / 2 && target.getX() < center.getBlockX() + protectionRange / 2
                        && target.getZ() >= center.getBlockZ() - protectionRange / 2 && target.getZ() < center.getBlockZ() + protectionRange / 2) {

                    return true;
                }
                 */
            }
        }
        return false;
    }

    /**
     * Checks if location is anywhere in the island space (island distance)
     * 
     * @param target
     * @return true if in the area
     */
    public boolean inIslandSpace(Location target) {
        if (target.getWorld().equals(ASkyBlock.getIslandWorld()) || target.getWorld().equals(ASkyBlock.getNetherWorld())) {
            if (target.getX() >= center.getBlockX() - islandDistance / 2 && target.getX() < center.getBlockX() + islandDistance / 2
                    && target.getZ() >= center.getBlockZ() - islandDistance / 2 && target.getZ() < center.getBlockZ() + islandDistance / 2) {
                return true;
            }
        }
        return false;
    }

    public boolean inIslandSpace(int x, int z) {
        if (x >= center.getBlockX() - islandDistance / 2 && x < center.getBlockX() + islandDistance / 2 && z >= center.getBlockZ() - islandDistance / 2
                && z < center.getBlockZ() + islandDistance / 2) {
            return true;
        }
        return false;
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
        String result = "";
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
        // Personal island protection settings - serialize enum into 1's and 0's representing the boolean values
        try {
            for (Flags f: Flags.values()) {
                if (this.igs.containsKey(f)) {
                    result += this.igs.get(f) ? "1" : "0";
                } else {
                    result += "0";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "";
        }
        return center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ() + ":" + protectionRange + ":" 
        + islandDistance + ":" + ownerString + ":" + locked + ":" + purgeProtected + ":" + result + ":" + getBiome().toString() + ":" + levelHandicap;
    }

    /**
     * Get the Island Guard flag status
     * @param flag
     * @return true or false, or false if flag is not in the list
     */
    public boolean getIgsFlag(Flags flag) {
        //plugin.getLogger().info("DEBUG: asking for " + flag + " = " + igs.get(flag));
        if (this.igs.containsKey(flag)) {
            return igs.get(flag);
        }
        return false;
    }

    /**
     * Set the Island Guard flag
     * @param flag
     * @param value
     */
    public void setIgsFlag(Flags flag, boolean value) {
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

    public void addEntity(EntityType type) {
        if (this.entities.containsKey(type)) {
            int sum = this.entities.get(type);
            this.entities.put(type, (sum + 1));
        } else {
            this.entities.put(type, 1);
        }
    }

    public int getEntity(EntityType type) {
        if (this.entities.containsKey(type)) {
            return this.entities.get(type);
        }
        return 0;
    }

    /**
     * @return the entities
     */
    public HashMap<EntityType, Integer> getEntities() {
        return entities;
    }

    public void clearStats() {
        this.entities.clear();
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
     * @param material
     * @return count of how many tile entities of type mat are on the island at last count. Counts are done when a player places
     * a tile entity.
     */
    public int getTileEntityCount(Material material) {
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
     * @param flag
     */
    public void toggleIgs(Flags flag) {
        if (igs.containsKey(flag)) {
            igs.put(flag, igs.get(flag) ? false : true);
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
}
