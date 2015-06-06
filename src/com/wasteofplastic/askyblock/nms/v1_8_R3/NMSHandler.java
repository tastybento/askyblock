package com.wasteofplastic.askyblock.nms.v1_8_R3;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
	net.minecraft.server.v1_8_R3.World w = ((CraftWorld) b.getWorld()).getHandle();
        net.minecraft.server.v1_8_R3.Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        int combined = blockId + (data << 12);
        IBlockData ibd = net.minecraft.server.v1_8_R3.Block.getByCombinedId(combined);
        chunk.a(bp, ibd);
        if (applyPhysics) {
            net.minecraft.server.v1_8_R3.Block block = chunk.getType(bp);
            w.update(bp, block);
        }      
    }

}
