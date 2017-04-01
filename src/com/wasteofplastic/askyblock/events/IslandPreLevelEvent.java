/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired after ASkyBlock calculates an island level but before it is communicated
 * to the player.
 * Use getLevel() to see the level calculated and setLevel() to change it.
 * Canceling this event will result in no change in level.
 * See IslandPostLevelEvent to cancel notifications to the player.
 * 
 * @author tastybento
 * 
 */
public class IslandPreLevelEvent extends ASkyBlockEvent implements Cancellable {
    private int level;
    private boolean cancelled;
    private int points;

    /**
     * @param player
     * @param island
     * @param level
     */
    public IslandPreLevelEvent(UUID player, Island island, int level) {
        super(player, island);
        this.level = level;
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
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
     * Set the number of blocks the player requires to reach the next level.
     * If this is set to a negative number, the player will not be informed of
     * how many points they need to reach the next level.
     * @param points
     */
    public void setPointsToNextLevel(int points) {
        this.points = points;
        
    }

    /**
     * @return the number of points
     */
    public int getPointsToNextLevel() {
        return points;
    }

}
