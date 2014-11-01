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

package com.wasteofplastic.askyblock;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

/**
 * @author ben
 * Creates the world
 */
public class AcidChunkGenerator extends ChunkGenerator {
    //@SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
    {
	byte[][] result = new byte[world.getMaxHeight() / 16][];
	//Bukkit.getLogger().info("DEBUG: sea_level" + Settings.sea_level);
	if (Settings.sea_level == 0) {
	    return result;
	} else {
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {
		    for (int y = 0; y < Settings.sea_level; y++) {
			setBlock(result,x,y,z, (byte) Material.STATIONARY_WATER.getId()); // Stationary Water
			// Allows stuff to fall through into oblivion, thus keeping lag to a minimum
		    }
		}
	    }
	    return result;
	}
    }
    /*
    @Override
    public byte[] generate(final World world, final Random rand, final int chunkx, final int chunkz) {
	final byte[] result = new byte[(world.getMaxHeight() / 16)*4096];
	// This generator creates water world with no base
	for (int x = 0; x < 16; x++) {
	    for (int z = 0; z < 16; z++) {
		for (int y = 0; y < 50; y++) {
		    result[(x * 16 + z) * 128 + y] = 9; // Stationary Water
		    // Allows stuff to fall through into oblivion, thus keeping lag to a minimum
		}
	    }
	}
	return result;
    }*/

    void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
	// is this chunk part already initialized?
	if (result[y >> 4] == null) {
	    // Initialize the chunk part
	    result[y >> 4] = new byte[4096];
	}
	// set the block (look above, how this is done)
	result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }

    // This needs to be set to return true to override minecraft's default
    // behavior
    @Override
    public boolean canSpawn(World world, int x, int z) {
	return true;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(final World world) {
	return Arrays.asList(new BlockPopulator[0]);
    }
}