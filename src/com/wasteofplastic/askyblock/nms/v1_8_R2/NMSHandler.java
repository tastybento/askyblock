package com.wasteofplastic.askyblock.nms.v1_8_R2;

import net.minecraft.server.v1_8_R2.BlockPosition;
import net.minecraft.server.v1_8_R2.IBlockData;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
	net.minecraft.server.v1_8_R2.World w = ((CraftWorld) b.getWorld()).getHandle();
        net.minecraft.server.v1_8_R2.Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        int combined = blockId + (data << 12);
        IBlockData ibd = net.minecraft.server.v1_8_R2.Block.getByCombinedId(combined);
        chunk.a(bp, ibd);
        if (applyPhysics) {
            net.minecraft.server.v1_8_R2.Block block = chunk.getType(bp);
            w.update(bp, block);
        }      
    }

}
