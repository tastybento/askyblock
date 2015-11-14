package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player leaves an island team
 * @author tastybento
 *
 */
public class IslandLeaveEvent extends ASkyBlockEvent {

    /**
     * @param player
     * @param island
     */
    public IslandLeaveEvent(UUID player, Island island) {
	super(player, island);
    }

}
