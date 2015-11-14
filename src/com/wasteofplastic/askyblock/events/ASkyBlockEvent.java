/**
 * 
 */
package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.Island;

/**
 * 
 * @author tastybento
 *
 */
public abstract class ASkyBlockEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final Island island;

    /**
     * @param plugin
     */
    public ASkyBlockEvent(UUID player, Island island) {
	this.player = player;
	this.island = island;
    }

    /**
     * Gets the player involved in this event
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * The island involved in the event
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Convenience function to obtain the island's protection size
     * @return the protectionSize
     */
    public int getProtectionSize() {
        return island.getProtectionSize();
    }

    /**
     * Convenience function to obtain the island's locked status
     * @return the isLocked
     */
    public boolean isLocked() {
        return island.isLocked();
    }

    /**
     * Convenience function to obtain the island's distance
     * @return the islandDistance
     */
    public int getIslandDistance() {
        return island.getIslandDistance();
    }
    
    /**
     * @return the teamLeader
     */
    public UUID getTeamLeader() {
        return island.getOwner();
    }

    /**
     * Convenience function to obtain the island's owner
     * @return UUID of owner
     */
    public UUID getIslandOwner() {
	return island.getOwner();
    }
    
    /**
     * Convenience function to obtain the island's center location
     * @return the island location
     */
    public Location getIslandLocation() {
        return island.getCenter();
    }
    
    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }
    
}
