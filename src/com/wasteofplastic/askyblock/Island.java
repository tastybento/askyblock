package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

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
    // Stats variables
    private HashMap<EntityType, Integer> entities = new HashMap<EntityType, Integer>();
    // Protection against deletion or not
    private boolean purgeProtected;
    // The spawn point 
    private Location spawnPoint;

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
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Add a new island using the island center method
     * 
     * @param minX
     * @param minZ
     */
    public Island(ASkyBlock plugin, int x, int z) {
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
    }

    /**
     * Checks if a location is within this island's protected area
     * 
     * @param loc
     * @return
     */
    public boolean onIsland(Location target) {
	if (world != null) {
	    // If the new nether is being used, islands exist in the nether too
	    if (target.getWorld().equals(world) || (Settings.createNether && Settings.newNether && target.getWorld().equals(ASkyBlock.getNetherWorld()))) {
		if (target.getX() >= center.getBlockX() - protectionRange / 2 && target.getX() < center.getBlockX() + protectionRange / 2
			&& target.getZ() >= center.getBlockZ() - protectionRange / 2 && target.getZ() < center.getBlockZ() + protectionRange / 2) {
		    return true;
		}
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
     * @param z
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
     * @param protectionRange
     *            the protectionRange to set
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

    public String serialize() {
	// x:height:z:protection range:island distance:owner UUID
	String ownerString = "null";
	if (owner != null) {
	    ownerString = owner.toString();
	} else {
	    //Bukkit.getLogger().info("DEBUG: null owner when serializing for " + center);
	}
	if (isSpawn) {
	    // Bukkit.getLogger().info("DEBUG: island is spawn");
	    ownerString = "spawn";
	    if (spawnPoint != null) {
		return center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ() + ":" + protectionRange + ":" 
			+ islandDistance + ":" + ownerString + ":" + locked + ":" + purgeProtected + ":SP:" + Util.getStringLocation(spawnPoint);
	    }
	}
	return center.getBlockX() + ":" + center.getBlockY() + ":" + center.getBlockZ() + ":" + protectionRange + ":" 
	+ islandDistance + ":" + ownerString + ":" + locked + ":" + purgeProtected;
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
	result.addAll(CoopPlay.getInstance().getCoopPlayers(center));
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
		    if (entity instanceof Villager) {
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
	int result = 0;	
	for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionSize() - 1)/16; x++) {
	    for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionSize() - 1)/16; z++) {
		for (BlockState holder : world.getChunkAt(x, z).getTileEntities()) {
		    if (holder instanceof Hopper) {
			result++;
		    }
		}
	    }  
	}
	return result;

    }

    public void setSpawnPoint(Location location) {
	spawnPoint = location;

    }

    public Location getSpawnPoint() {
	return spawnPoint;
    }
}
