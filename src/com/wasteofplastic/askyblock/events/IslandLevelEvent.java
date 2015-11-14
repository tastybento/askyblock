package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired when an island level is calculated
 * 
 * @author tastybento
 * 
 */
public class IslandLevelEvent extends ASkyBlockEvent {
    private int level;

    /**
     * @param player
     * @param island
     * @param level
     */
    public IslandLevelEvent(UUID player, Island island, int level) {
	super(player, island);
	this.level = level;
    }
   
    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

}
