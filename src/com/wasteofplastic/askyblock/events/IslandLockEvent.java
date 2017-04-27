/*******************************************************************************
 * This file is part of ASkyBlock.
 * <p>
 * ASkyBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * ASkyBlock is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.askyblock.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired when an island is going to be locked.
 * <p>
 * Cancelling this event will result in keeping the island unlocked.
 * @author Poslovitch
 * @since 4.0
 */
public class IslandLockEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private final Island island;
	private boolean cancelled;
	
	/**
	 * @param island
	 */
	public IslandLockEvent(Island island){
		this.island = island;
	}
	
	/**
	 * @return the locked island
	 */
	public Island getIsland(){
		return this.island;
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
        this.cancelled = cancel;
    }
}
