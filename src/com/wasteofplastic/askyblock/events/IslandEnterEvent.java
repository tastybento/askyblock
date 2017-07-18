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

import org.bukkit.Location;

import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player enters an island's area
 * @author tastybento
 *
 */
public class IslandEnterEvent extends ASkyBlockEvent {
    private final Location location;

    /**
     * Called to create the event
     * @param player
     * @param island - island the player is entering
     * @param location - Location of where the player entered the island or tried to enter
     */
    public IslandEnterEvent(UUID player, Island island, Location location) {
        super(player,island);
        this.location = location;
        //Bukkit.getLogger().info("DEBUG: IslandEnterEvent called");
    }

    /**
     * Location of where the player entered the island or tried to enter
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

}
