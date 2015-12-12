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

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when a player completes a challenge level
 * 
 * @author tastybento
 * 
 */
public class ChallengeLevelCompleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int oldLevel;
    private final int newLevel;

    /**
     * @param player
     * @param oldLevel
     * @param newLevel
     */
    public ChallengeLevelCompleteEvent(Player player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return the oldLevel
     */
    public int getOldLevel() {
        return oldLevel;
    }

    /**
     * @return the newLevel
     */
    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
