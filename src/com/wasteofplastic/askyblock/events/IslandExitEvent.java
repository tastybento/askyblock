package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.Location;

import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player enters an island's area
 * @author tastybento
 *
 */
public class IslandExitEvent extends ASkyBlockEvent {
    private final Location location;

    /**
     * @param player
     * @param island
     * @param location
     */
    public IslandExitEvent(UUID player, Island island, Location location) {
	super(player,island);
	this.location = location;
    }
    
    /**
     * Location of where the player exited the island
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

}
