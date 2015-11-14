package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.Location;

import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player enters an island's area
 * @author tastybento
 *
 */
public class IslandEnterEvent extends ASkyBlockEvent {
    private final Location location;

    /**
     * Called to create the event
     * @param plugin
     * @param player
     * @param island
     * @param location
     */
    public IslandEnterEvent(UUID player, Island island, Location location) {
	super(player,island);
	this.location = location;
    }
    
    /**
     * Location of where the player entered the island or tried to enter
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

}
