package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player enters an island's area
 * @author tastybento
 *
 */
public class IslandEnterEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final Island island;
    private final Location location;

    /**
     * Called to create the event
     * @param plugin
     * @param player
     * @param island
     * @param location
     */
    public IslandEnterEvent(ASkyBlock plugin, UUID player, Island island, Location location) {
	this.player = player;
	this.island = island;
	this.location = location;
	//plugin.getLogger().info("DEBUG: Island Enter Event");
    }
    
    /**
     * Returns the island object. This contains lots of info on the island itself.
     * @return island being entered
     */
    public Island getIsland() {
	return island;
    }
    
    /**
     * Location of the event
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * UUID of the player who entered
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * UUID of the island's owner or leader
     * @return the owner
     */
    public UUID getIslandOwner() {
        return island.getOwner();
    }

    /**
     * @return the island center location
     */
    public Location getIslandLocation() {
        return island.getCenter();
    }
    
    /**
     * @return the protectionSize
     */
    public int getProtectionSize() {
        return island.getProtectionSize();
    }

    /**
     * @return the isLocked
     */
    public boolean isLocked() {
        return island.isLocked();
    }

    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }
}
