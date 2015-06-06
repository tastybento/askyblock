package com.wasteofplastic.askyblock.nms.fallback;

import org.bukkit.block.Block;

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

}