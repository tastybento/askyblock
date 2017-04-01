package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired after ASkyBlock calculates an island level and when it sends notification to the player.
 * Use getLevel() to see the level calculated and getPointsToNextLevel() to see how much points are needed to reach next level.
 * Canceling this event will result in no notifications to the player.
 * 
 * @author Poslovitch
 */
public class IslandPostLevelEvent extends ASkyBlockEvent implements Cancellable {
    private int level;
    private boolean cancelled;
    private int points;

    /**
     * @param player
     * @param island
     * @param level
     */
    public IslandPostLevelEvent(UUID player, Island island, int level, int points) {
        super(player, island);
        this.level = level;
        this.points = points;
    }

    /**
     * @return the level
     */
    public int getLevel() {
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
     * @return the number of points
     */
    public int getPointsToNextLevel() {
        return points;
    }

}
