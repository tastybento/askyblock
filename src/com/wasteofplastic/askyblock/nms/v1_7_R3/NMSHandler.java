package com.wasteofplastic.askyblock.nms.v1_7_R3;

import java.lang.reflect.Field;

import net.minecraft.server.v1_7_R3.ChunkSection;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;

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


}
