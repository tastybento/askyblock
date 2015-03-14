package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/**
 * Interface to island.
 * 
 * @author tastybento
 *
 */
public interface IslandInterface {

    /**
     * Checks if a location is within this island's protected area
     * 
     * @param loc
     * @return
     */
    public boolean onIsland(Location target); 

    /**
     * Checks if location is anywhere in the island space (island distance)
     * 
     * @param target
     * @return true if in the area
     */
    public boolean inIslandSpace(Location target); 
    
    public boolean inIslandSpace(int x, int z);

    /**
     * @return the minX
     */
    public int getMinX();

    /**
     * @param minX
     *            the minX to set
     */
    public void setMinX(int minX) ;

    /**
     * @return the z
     */
    public int getMinZ();
    /**
     * @param z
     *            the z to set
     */
    public void setMinZ(int minZ);
    
    /**
     * @return the minprotectedX
     */
    public int getMinProtectedX();

    /**
     * @return the minProtectedZ
     */
    public int getMinProtectedZ();

    /**
     * @return the protectionRange
     */
    public int getProtectionSize();

    /**
     * @param protectionRange
     *            the protectionRange to set
     */
    public void setProtectionSize(int protectionSize);
    
    /**
     * @return the islandDistance
     */
    public int getIslandDistance();

    /**
     * @param islandDistance
     *            the islandDistance to set
     */
    public void setIslandDistance(int islandDistance);
    
    /**
     * @return the center
     */
    public Location getCenter();

    /**
     * @param center
     *            the center to set
     */
    public void setCenter(Location center);

    /**
     * @return the owner
     */
    public UUID getOwner();

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(UUID owner);

    /**
     * @return the createdDate
     */
    public long getCreatedDate();

    /**
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(long createdDate);

    /**
     * @return the updatedDate
     */
    public long getUpdatedDate();

    /**
     * @param updatedDate
     *            the updatedDate to set
     */
    public void setUpdatedDate(long updatedDate);

    /**
     * @return the password
     */
    public String getPassword();

    /**
     * @param password
     *            the password to set
     */
    public void setPassword(String password);
    /**
     * @return the votes
     */
    public int getVotes();

    /**
     * @param votes
     *            the votes to set
     */
    public void setVotes(int votes);

    /**
     * @return the locked
     */
    public boolean isLocked();

    /**
     * @param locked
     *            the locked to set
     */
    public void setLocked(boolean locked);

    public String serialize();

    /**
     * Provides a list of all the players who are allowed on this island
     * including coop members
     * 
     * @return a list of UUIDs that have legitimate access to the island
     */
    public List<UUID> getMembers();

    /**
     * @return the isSpawn
     */
    public boolean isSpawn();
    
    /**
     * @param isSpawn
     *            the isSpawn to set
     */
    public void setSpawn(boolean isSpawn);

    public void addEntity(EntityType type);

    public int getEntity(EntityType type);

    /**
     * @return the entities
     */
    public HashMap<EntityType, Integer> getEntities();

    public void clearStats();
}
