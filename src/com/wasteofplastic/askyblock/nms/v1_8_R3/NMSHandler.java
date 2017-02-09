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

package com.wasteofplastic.askyblock.nms.v1_8_R3;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;
//import net.minecraft.server.v1_8_R3.EnumSkyBlock;

public class NMSHandler implements NMSAbstraction {

    @Override
    public boolean isPotion(ItemStack item) {
        if (item.getType().equals(Material.POTION) && item.getDurability() != 0) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#getSpawnEgg(org.bukkit.entity.EntityType, int)
     */
    @Override
    public ItemStack getSpawnEgg(EntityType type, int amount) {
        SpawnEgg egg = new SpawnEgg(type);
        return egg.toItemStack(amount);
    }
}
