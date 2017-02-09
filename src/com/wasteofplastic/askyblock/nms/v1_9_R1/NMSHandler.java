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

package com.wasteofplastic.askyblock.nms.v1_9_R1;

import net.minecraft.server.v1_9_R1.NBTTagCompound;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#isPotion(org.bukkit.inventory.ItemStack)
     */
    @Override
    public boolean isPotion(ItemStack item) {
        //Bukkit.getLogger().info("DEBUG:item = " + item);
        if (item.getType().equals(Material.POTION)) {
            net.minecraft.server.v1_9_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tag = stack.getTag();
            //Bukkit.getLogger().info("DEBUG: tag is " + tag);
            //Bukkit.getLogger().info("DEBUG: display is " + tag.getString("display"));
            /*
            for (String list : tag.c()) {
                Bukkit.getLogger().info("DEBUG: list = " + list);
            }*/
            if (tag != null && (!tag.getString("Potion").equalsIgnoreCase("minecraft:water") || tag.getString("Potion").isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get spawn egg
     * @param type
     * @param amount
     * @return
     */
    @SuppressWarnings("deprecation")
    public ItemStack getSpawnEgg(EntityType type, int amount) {
        ItemStack item = new ItemStack(Material.MONSTER_EGG, amount);
        net.minecraft.server.v1_9_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tagCompound = stack.getTag();
        if(tagCompound == null){
            tagCompound = new NBTTagCompound();
        }
        NBTTagCompound id = new NBTTagCompound();
        id.setString("id", type.getName());
        tagCompound.set("EntityTag", id);
        stack.setTag(tagCompound);
        return CraftItemStack.asBukkitCopy(stack);
    }
}