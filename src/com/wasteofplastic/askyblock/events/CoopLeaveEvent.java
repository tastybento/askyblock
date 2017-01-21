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
 * Fired when a player is leaves an island coop
 * @author tastybento
 *
 */
public class CoopLeaveEvent extends ASkyBlockEvent implements Cancellable {
    private final UUID expeller;
    private boolean cancelled;

    /**
     * Note that not all coop leaving events can be cancelled because they could be due to bigger events than
     * coop, e.g., an island being reset.
     * @param expelledPlayer
     * @param expellingPlayer
     * @param island
     */
    public CoopLeaveEvent(UUID expelledPlayer, UUID expellingPlayer, Island island) {
        super(expellingPlayer, island);
        this.expeller = expellingPlayer;
        //Bukkit.getLogger().info("DEBUG: Coop leave event " + Bukkit.getServer().getOfflinePlayer(expelledPlayer).getName() + " was expelled from " 
        //	+ Bukkit.getServer().getOfflinePlayer(expellingPlayer).getName() + "'s island.");
    }

    /**
     * @return the expelling player
     */
    public UUID getExpeller() {
        return expeller;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /* (non-Javadoc)
     * @see org.bukkit.event.Cancellable#setCancelled(boolean)
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;   
    }

}
