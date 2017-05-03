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

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.Island;

/**
 * Fired when an entity (player and items excluded) receives damage from acid
 * @author Poslovitch
 * @since 4.0
 */
public class EntityDamageByAcidEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    
    private Island island;
    private Entity entity;
    private double damage;
    
    public EntityDamageByAcidEvent(Island island, Entity entity, double damage) {
        this.island = island;
        this.entity = entity;
        this.damage = damage;
    }
    
    /**
     * Gets the island where stands the damaged Entity
     * @return the island where stands the damaged Entity
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * Gets the Entity who is receiving Acid
     * @return the damaged Entity
     */
    public Entity getEntity(){
        return entity;
    }
    
    /**
     * Gets the amount of damage that is applied to the Entity
     * @return the amount of damage caused by the acid
     */
    public double getDamage(){
        return damage;
    }
    
    /**
     * Sets the amount of damage that will be applied to the entity
     * @param damage - the amount of damage caused by the acid
     */
    public void setDamage(double damage){
        this.damage = damage;
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
