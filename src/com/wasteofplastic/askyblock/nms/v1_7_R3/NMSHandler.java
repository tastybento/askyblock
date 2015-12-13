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

package com.wasteofplastic.askyblock.nms.v1_7_R3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_7_R3.ChunkSection;
import net.minecraft.server.v1_7_R3.NBTTagCompound;
import net.minecraft.server.v1_7_R3.NBTTagList;
import net.minecraft.server.v1_7_R3.NBTTagString;
import net.minecraft.server.v1_7_R3.TileEntityFlowerPot;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    @SuppressWarnings("deprecation")
    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
        net.minecraft.server.v1_7_R3.World w = ((CraftWorld) b.getWorld()).getHandle();
        net.minecraft.server.v1_7_R3.Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        try {
            Field f = chunk.getClass().getDeclaredField("sections");
            f.setAccessible(true);
            ChunkSection[] sections = (ChunkSection[]) f.get(chunk);
            ChunkSection chunksection = sections[b.getY() >> 4];

            if (chunksection == null) {
                chunksection = sections[b.getY() >> 4] = new ChunkSection(b.getY() >> 4 << 4, !chunk.world.worldProvider.f);
            }
            net.minecraft.server.v1_7_R3.Block mb = net.minecraft.server.v1_7_R3.Block.e(blockId);
            chunksection.setTypeId(b.getX() & 15, b.getY() & 15, b.getZ() & 15, mb);
            chunksection.setData(b.getX() & 15, b.getY() & 15, b.getZ() & 15, data);
            if (applyPhysics) {
                w.update(b.getX(), b.getY(), b.getZ(), mb);
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Error");
            b.setTypeIdAndData(blockId, data, applyPhysics);
        }


    }

    @Override
    public ItemStack setBook(Tag item) {
        ItemStack chestItem = new ItemStack(Material.WRITTEN_BOOK);
        //Bukkit.getLogger().info("item data");
        //Bukkit.getLogger().info(item.toString());

        Map<String,Tag> contents = (Map<String,Tag>) ((CompoundTag) item).getValue().get("tag").getValue();
        //BookMeta bookMeta = (BookMeta) chestItem.getItemMeta();
        String author = ((StringTag)contents.get("author")).getValue();
        //Bukkit.getLogger().info("Author: " + author);
        //bookMeta.setAuthor(author);
        String title = ((StringTag)contents.get("title")).getValue();
        //Bukkit.getLogger().info("Title: " + title);
        //bookMeta.setTitle(title);

        Map<String,Tag> display = (Map<String, Tag>) (contents.get("display")).getValue();
        List<Tag> loreTag = ((ListTag)display.get("Lore")).getValue();
        List<String> lore = new ArrayList<String>();
        for (Tag s: loreTag) {
            lore.add(((StringTag)s).getValue());
        }
        //Bukkit.getLogger().info("Lore: " + lore);
        net.minecraft.server.v1_7_R3.ItemStack stack = CraftItemStack.asNMSCopy(chestItem); 
        // Pages
        NBTTagCompound tag = new NBTTagCompound(); //Create the NMS Stack's NBT (item data)
        tag.setString("title", title); //Set the book's title
        tag.setString("author", author);
        NBTTagList pages = new NBTTagList();
        List<Tag> pagesTag = ((ListTag)contents.get("pages")).getValue();
        for (Tag s: pagesTag) {
            pages.add(new NBTTagString(((StringTag)s).getValue()));
        }
        tag.set("pages", pages); //Add the pages to the tag
        stack.setTag(tag); //Apply the tag to the item
        chestItem = CraftItemStack.asCraftMirror(stack); 
        ItemMeta bookMeta = (ItemMeta) chestItem.getItemMeta();
        bookMeta.setLore(lore);
        chestItem.setItemMeta(bookMeta);
        return chestItem;
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#setBlock(org.bukkit.block.Block, org.bukkit.inventory.ItemStack)
     * Credis: Mister_Frans (THANK YOU VERY MUCH !)
     */
    @Override
    public void setFlowerPotBlock(Block block, ItemStack itemStack) {
        Location loc = block.getLocation();
        CraftWorld cw = (CraftWorld)block.getWorld();
        TileEntityFlowerPot te = (TileEntityFlowerPot)cw.getHandle().getTileEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Bukkit.getLogger().info("Debug: flowerpot materialdata = " + itemStack.toString());
        net.minecraft.server.v1_7_R3.ItemStack cis = CraftItemStack.asNMSCopy(itemStack);
        te.a(cis.getItem(), cis.getData());
        te.update();
        cw.getHandle().notify(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Chunk ch = loc.getChunk();
        cw.refreshChunk(ch.getX(), ch.getZ());
    }
}
