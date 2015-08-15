package com.wasteofplastic.askyblock.nms.fallback;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jnbt.Tag;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

/**
 * @author ben
 *
 */
public class NMSHandler implements NMSAbstraction {

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#setBlockSuperFast(org.bukkit.block.Block, org.bukkit.Material)
     */
    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
	b.setTypeId(blockId);
	b.setData(data);
    }

    @Override
    public ItemStack setBook(Tag item) {
	Bukkit.getLogger().warning("Written books in schematics not supported with this version of server");
	return new ItemStack(Material.WRITTEN_BOOK);
    }
}