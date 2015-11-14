package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired when a player joins an island team
 * 
 * @author tastybento
 * 
 */
public class IslandJoinEvent extends ASkyBlockEvent {


    /**
     * @param player
     * @param island
     */
    public IslandJoinEvent(UUID player, Island island) {
	super(player,island);
    }

}
