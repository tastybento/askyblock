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
package com.wasteofplastic.askyblock.v1_7_R3;

import java.lang.reflect.Field;

import net.minecraft.server.v1_7_R3.ChunkSection;

import org.bukkit.Chunk;
import org.bukkit.Material;
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
    @SuppressWarnings("deprecation")
    @Override
    public void setBlockSuperFast(Block b, Material mat) {
	int typeId = mat.getId();
	Chunk c = b.getChunk();
	try {

	    net.minecraft.server.v1_7_R3.Chunk chunk = ((org.bukkit.craftbukkit.v1_7_R3.CraftChunk) c).getHandle();

	    Field f = chunk.getClass().getDeclaredField("sections");
	    f.setAccessible(true);
	    ChunkSection[] sections = (ChunkSection[]) f.get(chunk);
	    ChunkSection chunksection = sections[b.getY() >> 4];

	    if (chunksection == null) {
		if (typeId == 0)
		    return;
		chunksection = sections[b.getY() >> 4] = new ChunkSection(b.getY() >> 4 << 4, !chunk.world.worldProvider.f);
	    }

	    net.minecraft.server.v1_7_R3.Block mb = net.minecraft.server.v1_7_R3.Block.e(typeId);
	    chunksection.setTypeId(b.getX() & 15, b.getY() & 15, b.getZ() & 15, mb);
	    //chunksection.setData(b.getX() & 15, b.getY() & 15, b.getZ() & 15, data);

	} catch (Exception e) {
	    b.setType(mat);
	    //e.printStackTrace();
	}

    }
}
