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

package com.wasteofplastic.askyblock.events.acid;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.Island;

/**
 * Fired when an ItemStack (water bottle or bucket) is filled with acid
 * @author Poslovitch
 * @since 4.0
 */
public class ItemFillWithAcidEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    
    private Island island;
    private Player player;
    private ItemStack item;
    
    public ItemFillWithAcidEvent(Island island, Player player, ItemStack item) {
        this.island = island;
        this.player = player;
        this.item = item;
    }
    
    /**
     * Gets the island where the event happened
     * @return the island where the event happened
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * Gets the player who triggered the event
     * @return the player who triggered the event
     */
    public Player getPlayer(){
        return player;
    }
    
    /**
     * Gets the item that will be acid-ified
     * @return the item that will be acid-ified
     */
    public ItemStack getItem(){
        return item;
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
