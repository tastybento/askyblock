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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.Island;

/**
 * 
 * @author tastybento
 *
 */
public abstract class ASkyBlockEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final Island island;


    /**
     * @param player
     * @param island
     */
    public ASkyBlockEvent(UUID player, Island island) {
        this.player = player;
        this.island = island;
    }

    public ASkyBlockEvent(UUID player) {
        this.player = player;
        this.island = null;
    }

    /**
     * Gets the player involved in this event
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * The island involved in the event
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Convenience function to obtain the island's protection size
     * @return the protectionSize
     */
    public int getProtectionSize() {
        return island.getProtectionSize();
    }

    /**
     * Convenience function to obtain the island's locked status
     * @return the isLocked
     */
    public boolean isLocked() {
        return island.isLocked();
    }

    /**
     * Convenience function to obtain the island's distance
     * @return the islandDistance
     */
    public int getIslandDistance() {
        return island.getIslandDistance();
    }

    /**
     * @return the teamLeader
     */
    public UUID getTeamLeader() {
        return island.getOwner();
    }

    /**
     * Convenience function to obtain the island's owner
     * @return UUID of owner
     */
    public UUID getIslandOwner() {
        return island.getOwner();
    }

    /**
     * Convenience function to obtain the island's center location
     * @return the island location
     */
    public Location getIslandLocation() {
        return island.getCenter();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
