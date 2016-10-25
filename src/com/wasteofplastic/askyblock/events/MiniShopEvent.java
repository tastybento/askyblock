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

import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.panels.MiniShopItem;


/**
 * Fired when a player buys or sells in the mini shop
 * @author tastybento
 *
 */
public class MiniShopEvent extends ASkyBlockEvent {
    public enum TransactionType {BUY, SELL}
    private final MiniShopItem item;
    private final TransactionType type;

    /**
     * Called to create the event
     * @param player
     * @param item
     * @param type
     */
    public MiniShopEvent(final UUID player, final MiniShopItem item, final TransactionType type) {
        super(player);
        this.item = item;
        this.type = type;
    }

    /**
     * @return Description of the item
     */
    public String getDescription() {
        return item.getDescription();
    }

    /**
     * @return The item in itemstack form
     */
    public ItemStack getItem() {
        return item.getItemClean();
    }

    /**
     * @return The sell price or buy price
     */
    public double getPrice() {
        if (type == TransactionType.BUY) {
            return item.getPrice();
        } else {
            return item.getSellPrice();
        }
    }

    /**
     * @return The number of the item
     */
    public int getQuantity() {
        return item.getQuantity();
    }

    /**
     * @return The transaction type BUY or SELL
     */
    public TransactionType getTransactionType() {
        return type;
    }


}
