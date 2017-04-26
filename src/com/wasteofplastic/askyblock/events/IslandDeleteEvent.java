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

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when an island is deleted.
 *
 * @author tastybento
 * @since 3.0.2.1
 */
public class IslandDeleteEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final UUID playerUUID;
	private final Location location;

	/**
	 * @param playerUUID
	 * @param oldLocation
	 */
	public IslandDeleteEvent(UUID playerUUID, Location oldLocation) {
		this.playerUUID = playerUUID;
		this.location = oldLocation;
	}

	/**
	 * @return the player's UUID
	 */
	public UUID getPlayerUUID() {
		return playerUUID;
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
