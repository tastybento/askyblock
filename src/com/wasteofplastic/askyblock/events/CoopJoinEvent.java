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
 * This event is fired when a player joins an island team as a coop member
 * 
 * @author tastybento
 * 
 */
public class CoopJoinEvent extends ASkyBlockEvent implements Cancellable {

    private final UUID inviter;
    private boolean cancelled;

    /**
     * @param player
     * @param island
     * @param inviter
     */
    public CoopJoinEvent(UUID player, Island island, UUID inviter) {
        super(player, island);
        this.inviter = inviter;
        //Bukkit.getLogger().info("DEBUG: Coop join event " + Bukkit.getServer().getOfflinePlayer(player).getName() + " joined " 
        //+ Bukkit.getServer().getOfflinePlayer(inviter).getName() + "'s island.");
    }

    /**
     * The UUID of the player who invited the player to join the island
     * @return the inviter
     */
    public UUID getInviter() {
        return inviter;
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
