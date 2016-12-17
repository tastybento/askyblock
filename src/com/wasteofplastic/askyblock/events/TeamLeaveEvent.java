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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when a player leaves an existing Team
 * 
 * @author Exloki
 * 
 */
public class TeamLeaveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final UUID player;
    private final UUID oldTeamLeader;
    private boolean cancelled;

    public TeamLeaveEvent(UUID player, UUID oldTeamLeader) {
        super();
        this.player = player;
        this.oldTeamLeader = oldTeamLeader;
    }

    /**
     * The UUID of the player changing Team
     * @return the player UUID
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * The UUID of the player's previous Team's Leader
     * @return the team leader
     */
    public UUID getOldTeamLeader() {
        return oldTeamLeader;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
        
    }
}
