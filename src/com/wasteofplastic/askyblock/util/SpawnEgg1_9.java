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
package com.wasteofplastic.askyblock.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

/**
 * Represents a spawn egg that can be used to spawn mobs. Only for V1.9 servers
 * 
 * @author tastybento
 */
public class SpawnEgg1_9 {
    private EntityType type;
    private NMSAbstraction nms;

    public SpawnEgg1_9(EntityType type) {
        this.type = type;
        nms = null;
        try {
            nms = Util.checkVersion();
        } catch (Exception ex) {
            Bukkit.getLogger().severe("Could not find NMS code to support");
        }
    }

    /**
     * Get an ItemStack of one spawn egg 
     * @return ItemStack
     */
    public ItemStack toItemStack() {
        return toItemStack(1);
    }

    
    /**
     * Get an itemstack of spawn eggs
     * @param amount
     * @return ItemStack of spawn eggs
     */
    public ItemStack toItemStack(int amount) {        
        return nms.getSpawnEgg(type, amount);
    }

}
