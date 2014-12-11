/*******************************************************************************
 *
w *     ASkyBlock is free software: you can redistribute it and/or modify
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
import org.bukkit.World.Environment;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.PerlinOctaveGenerator;

/**
 * @author ben
 * Creates the world
 */
public class ChunkGeneratorNether extends ChunkGenerator {
    Random rand = new Random();
    PerlinOctaveGenerator gen;

    //@SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
    {
	
	rand.setSeed(world.getSeed());
	gen = new PerlinOctaveGenerator((long) (random.nextLong() * random.nextGaussian()), 8);
	byte[][] result = new byte[world.getMaxHeight() / 16][];
	// This is a nether generator
	if (!world.getEnvironment().equals(Environment.NETHER)) {
	    return result;
	}
	// Make the roof - common across the world
	for (int x = 0; x < 16; x++) {
	    for (int z = 0; z < 16; z++) {	
		// Do the ceiling
		//Bukkit.getLogger().info("debug: " + x + ", " + (world.getMaxHeight()-1) + ", " + z);
		setBlock(result,x,(world.getMaxHeight()-1),z, (byte)Material.BEDROCK.getId());
		// Next three layers are a mix of bedrock and netherrack
		for (int y = 2; y < 5; y++) {
		    double r = gen.noise(x, (world.getMaxHeight() - y), z,0.5,0.5);
		    if (r > 0D) {
			setBlock(result,x,(world.getMaxHeight()-y),z, (byte)Material.BEDROCK.getId());
		    } else {
			setBlock(result,x,(world.getMaxHeight()-y),z, (byte)Material.NETHERRACK.getId());
		    }
		}
		// Next three layers are a mix of netherrack and air
		for (int y = 5; y < 8; y++) {
		    double r = gen.noise(x, world.getMaxHeight() - y, z,0.5,0.5);
		    if (r > 0D) {
			setBlock(result,x,(world.getMaxHeight()-y),z, (byte)Material.NETHERRACK.getId());
		    } else {
			setBlock(result,x,(world.getMaxHeight()-y),z, (byte)Material.AIR.getId());
		    }
		}
		// Layer 8 may be glowstone
		double r = gen.noise(x, world.getMaxHeight() - 8, z,random.nextFloat(),random.nextFloat());
		if (r > 0.5D) {
		    setBlock(result,x,(world.getMaxHeight()-8),z, (byte)Material.GLOWSTONE.getId());
		} else {
		    setBlock(result,x,(world.getMaxHeight()-8),z, (byte)Material.AIR.getId());
		}
	    }
	}
	// Center points of the chunk I'm being given
	int actualX = chunkX * 16;
	int actualZ = chunkZ * 16;
	//Bukkit.getLogger().info("DEBUG: sea_level" + Settings.sea_level);
	int chunkDist = Math.round((float)Settings.islandDistance / 16F);
	// Main island creation. Make if the chunks match the isandDistance
	if (chunkX % chunkDist == 0 && chunkZ % chunkDist == 0) {
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {		    
		    // This should be okay for the nether, because the nether is half the height of over world
		    int island_height = Settings.island_level;
		    for (int y = island_height -13 ; y < island_height; y++) {
			double r = gen.noise(x, y, z,0.5,0.5);
			// Put a fence around the top
			if (y == (island_height-1)) {
			    if (x == 0 || z == 0 || x == 15 || z == 15) {
				setBlock(result,x,y,z, (byte)Material.NETHER_FENCE.getId());
			    }
			    // Put brick for the fence to sit on
			} else if (y == (island_height-2)) {
			    if (x == 0 || z == 0 || x == 15 || z == 15) {
				setBlock(result,x,y,z, (byte)Material.NETHER_BRICK.getId());
			    } else {
				// Place random fire on the top level
				//Bukkit.getLogger().info("nether noise = " + r);
				if (r > 0.5D) {
				    setBlock(result,x,y,z, (byte)Material.FIRE.getId());
				}
			    }
			} else {
			    // Make a wall around the island with a few windows (okay holes)
			    if (x == 0 || z == 0 || x == 15 || z == 15) {
				if(r > -0.5D)
				{
				    setBlock(result,x,y,z, (byte)Material.NETHER_BRICK.getId());
				}
			    } else {
				// Fill the inside of the island randomly with blocks
				if (r > 0D) {
				    setBlock(result,x,y,z, (byte)Material.QUARTZ_ORE.getId());
				} else if(r > -0.4D) {
				    setBlock(result,x,y,z, (byte)Material.NETHERRACK.getId());
				    // Put lava in the holes if it is below the lava level
				} else if (y < island_height -10) {
				    setBlock(result,x,y,z, (byte) Material.LAVA.getId()); 
				} 
			    }
			} 
		    }
		}

	    }
	    // Monster island towers
	} else if (Math.abs(chunkX % chunkDist) == 2 && Math.abs(chunkZ % chunkDist) == 2) {
	    // Fill the area with lava first
	    /*
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {
		    for (int y = 0; y < 50; y++) {
			setBlock(result,x,y,z, (byte)Material.STATIONARY_LAVA.getId());
		    }
		}
	    }*/
	    // Make the towers
	    for (int x = 5; x < 11; x++) {
		for (int z = 5; z < 11; z++) {
		    int island_height = Settings.island_level+25;
		    for (int y = Settings.island_level-20; y < island_height; y++) {
			double r = gen.noise(x, y, z,0.5,0.5);
			// Fence around the top
			if (y == (island_height-1)) {
			    if (x == 5 || z == 5 || x == 10 || z == 10) {
				setBlock(result,x,y,z, (byte)Material.NETHER_FENCE.getId());
			    } else {
				// Place random spawners. The type is set in the block populator
				if (r > 0.1D) {
				    setBlock(result,x,y,z, (byte)Material.MOB_SPAWNER.getId());
				}
			    }
			    // Fence sits on nether brick
			} else if (y == (island_height-2)) {	    
			    setBlock(result,x,y,z, (byte)Material.NETHER_BRICK.getId());
			} else {
			    if (x == 5 || z == 5 || x == 10 || z == 10) {
				// Wall around the island with occasional holes
				if(r > -0.5D)
				{
				    setBlock(result,x,y,z, (byte)Material.NETHER_BRICK.getId());
				} else {
				    setBlock(result,x,y,z, (byte)Material.LAVA.getId());
				}
			    } else {
				// Fill with random blocks
				if (r > 0.8D){
				    setBlock(result,x,y,z, (byte) Material.LAVA.getId()); 
				} else if (r > 0.45D) {
				    setBlock(result,x,y,z, (byte)Material.QUARTZ_ORE.getId());
				} else if (r > 0.20D) {
				    setBlock(result,x,y,z, (byte)Material.SOUL_SAND.getId());
				} else if(r > -0.5D) {
				    setBlock(result,x,y,z, (byte)Material.NETHERRACK.getId());
				} 
			    }
			} 
		    }
		}

	    }
	} else {
	    // Everywhere else (not tower, not island)
	    /*
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {
		    makeRoof(x,z);
		}
	    }*/
	}
	return result;

    }

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
	//return Arrays.asList(new BlockPopulator[0]);
	return Arrays.<BlockPopulator>asList(new NetherPopulator());
    }
}