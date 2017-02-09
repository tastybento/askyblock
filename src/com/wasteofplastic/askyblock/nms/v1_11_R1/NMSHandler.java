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

package com.wasteofplastic.askyblock.nms.v1_11_R1;

import java.util.HashMap;

import net.minecraft.server.v1_11_R1.NBTTagCompound;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    private static HashMap<EntityType, String> bToMConversion;

    static {
        bToMConversion = new HashMap<EntityType, String> ();
        bToMConversion.put(EntityType.MUSHROOM_COW, "mooshroom");
        bToMConversion.put(EntityType.PIG_ZOMBIE, "zombie_pigman");
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#isPotion(org.bukkit.inventory.ItemStack)
     */
    @Override
    public boolean isPotion(ItemStack item) {
        //Bukkit.getLogger().info("DEBUG:item = " + item);
        if (item.getType().equals(Material.POTION)) {
            net.minecraft.server.v1_11_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
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
    public ItemStack getSpawnEgg(EntityType type, int amount) {
        //Bukkit.getLogger().info("DEBUG: setting spawn egg " + type.toString());
        ItemStack item = new ItemStack(Material.MONSTER_EGG, amount);
        net.minecraft.server.v1_11_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tagCompound = stack.getTag();
        if(tagCompound == null){
            tagCompound = new NBTTagCompound();
        }
        //Bukkit.getLogger().info("DEBUG: tag = " + tagCompound);
        NBTTagCompound id = new NBTTagCompound();
        if (!bToMConversion.containsKey(type)) {
            id.setString("id", "minecraft:" + type.toString().toLowerCase());
        } else {
            id.setString("id", "minecraft:" + bToMConversion.get(type));
        }
        tagCompound.set("EntityTag", id);
        stack.setTag(tagCompound);
        //Bukkit.getLogger().info("DEBUG: after tag = " + tagCompound);
        return CraftItemStack.asBukkitCopy(stack);
    }
}