package com.wasteofplastic.askyblock.nms;

import org.bukkit.block.Block;

public interface NMSAbstraction {

    /**
     * Update the low-level chunk information for the given block to the new block ID and data.  This
     * change will not be propagated to clients until the chunk is refreshed to them.
     * @param b - block to be changed
     * @param mat - material to change it into
     */
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics);
}
