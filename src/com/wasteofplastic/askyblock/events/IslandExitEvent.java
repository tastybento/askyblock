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
 * Fired when a player exits an island's protected area
 * @author tastybento
 *
 */
public class IslandExitEvent extends ASkyBlockEvent {
    private final Location location;

    /**
     * @param player
     * @param island that the player is leaving
     * @param location - Location of where the player exited the island's protected area
     */
    public IslandExitEvent(UUID player, Island island, Location location) {
        super(player,island);
        this.location = location;
        //Bukkit.getLogger().info("DEBUG: IslandExitEvent called");
    }

    /**
     * Location of where the player exited the island's protected area
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

}
