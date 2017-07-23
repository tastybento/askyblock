package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired after ASkyBlock calculates an island level and when it sends notification to the player.
 * Use getLevel() to see the level calculated and getPointsToNextLevel() to see how much points are needed to reach next level.
 * Canceling this event will result in no notifications to the player.
 * 
 * @author Poslovitch, tastybento
 */
public class IslandPostLevelEvent extends ASkyBlockEvent implements Cancellable {
    private long level;
    private boolean cancelled;
    private long points;

    /**
     * @param player
     * @param island
     * @param l
     */
    public IslandPostLevelEvent(UUID player, Island island, long l, long m) {
        super(player, island);
        this.level = l;
        this.points = m;
    }

    /**
     * @deprecated
     * level is now stored as a long, so the int value may not be accurate
     * @return the level
     */
    public int getLevel() {
        return (int)level;
    }
    
    /**
     * @return the level
     */
    public long getLongLevel() {
        return level;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * @deprecated
     * level is now stored as a long, so the int value may not be accurate
     * @return the number of points
     */
    public int getPointsToNextLevel() {
        return (int)points;
    }
    
    /**
     * @return the number of points
     */
    public long getLongPointsToNextLevel() {
        return points;
    }

}
