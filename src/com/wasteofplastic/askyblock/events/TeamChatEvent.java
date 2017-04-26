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

import java.util.UUID;

import org.bukkit.event.Cancellable;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired when a player talks in TeamChat
 * 
 * @author Poslovitch
 * @since 4.0
 */
public class TeamChatEvent extends ASkyBlockEvent implements Cancellable{

	private String message;
	private boolean cancelled;
	
	/**
	 * @param player
	 * @param island
	 * @param message
	 */
	public TeamChatEvent(UUID player, Island island, String message){
		super(player, island);
		this.message = message;
	}
	
	/**
	 * Gets the message that the player is attempting to send.
	 * @return the message
	 */
	public String getMessage(){
		return this.message;
	}
	
	/**
	 * Sets the message that the player will send.
	 * @param the message to send
	 */
	public void setMessage(String message){
		this.message = message;
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
