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

package com.wasteofplastic.askyblock.nms;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public interface NMSAbstraction {

    boolean isPotion(ItemStack item);

    /**
     * Gets a monster egg itemstack
     * @param type
     * @param amount
     * @return itemstack
     */
    public ItemStack getSpawnEgg(EntityType type, int amount);
}
