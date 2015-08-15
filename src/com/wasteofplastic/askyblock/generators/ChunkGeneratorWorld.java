/*******************************************************************************
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

package com.wasteofplastic.askyblock.generators;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.PerlinOctaveGenerator;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

/**
 * @author tastybento
 *         Creates the world
 */
public class ChunkGeneratorWorld extends ChunkGenerator {
    Random rand = new Random();
    PerlinOctaveGenerator gen;
    ASkyBlock plugin = ASkyBlock.getPlugin();

    @SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
	// Bukkit.getLogger().info("DEBUG: world environment = " +
	// world.getEnvironment().toString());
	if (world.getEnvironment().equals(World.Environment.NETHER)) {
	    return generateNetherBlockSections(world, random, chunkX, chunkZ, biomeGrid);
	}
	byte[][] result = new byte[world.getMaxHeight() / 16][];
	if (Settings.sea_level == 0) {
	    return result;
	} else {
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {
		    for (int y = 0; y < Settings.sea_level; y++) {
			setBlock(result, x, y, z, (byte) Material.STATIONARY_WATER.getId()); // Stationary
			// Water
			// Allows stuff to fall through into oblivion, thus
			// keeping lag to a minimum
		    }
		}
	    }
	    return result;
	}
    }

    /*
     * @Override
     * public byte[] generate(final World world, final Random rand, final int
     * chunkx, final int chunkz) {
     * final byte[] result = new byte[(world.getMaxHeight() / 16)*4096];
     * // This generator creates water world with no base
     * for (int x = 0; x < 16; x++) {
     * for (int z = 0; z < 16; z++) {
     * for (int y = 0; y < 50; y++) {
     * result[(x * 16 + z) * 128 + y] = 9; // Stationary Water
     * // Allows stuff to fall through into oblivion, thus keeping lag to a
     * minimum
     * }
     * }
     * }
     * return result;
     * }
     */

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
	/*
	if (world.getEnvironment().equals(World.Environment.NETHER)) {
	    return Arrays.<BlockPopulator> asList(new NetherPopulator());
	}*/
	return Arrays.asList(new BlockPopulator[0]);
    }

    /*
     * Nether Section
     */
    @SuppressWarnings("deprecation")
    private byte[][] generateNetherBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
	// Bukkit.getLogger().info("DEBUG: world environment(nether) = " +
	// world.getEnvironment().toString());
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
		// Bukkit.getLogger().info("debug: " + x + ", " +
		// (world.getMaxHeight()-1) + ", " + z);
		int maxHeight = world.getMaxHeight();
		setBlock(result, x, (maxHeight - 1), z, (byte) Material.BEDROCK.getId());
		// Next three layers are a mix of bedrock and netherrack
		for (int y = 2; y < 5; y++) {
		    double r = gen.noise(x, (maxHeight - y), z, 0.5, 0.5);
		    if (r > 0D) {
			setBlock(result, x, (maxHeight - y), z, (byte) Material.BEDROCK.getId());
		    } else {
			setBlock(result, x, (maxHeight - y), z, (byte) Material.NETHERRACK.getId());
		    }
		}
		// Next three layers are a mix of netherrack and air
		for (int y = 5; y < 8; y++) {
		    double r = gen.noise(x, maxHeight - y, z, 0.5, 0.5);
		    if (r > 0D) {
			setBlock(result, x, (maxHeight - y), z, (byte) Material.NETHERRACK.getId());
		    } else {
			setBlock(result, x, (maxHeight - y), z, (byte) Material.AIR.getId());
		    }
		}
		// Layer 8 may be glowstone
		double r = gen.noise(x, maxHeight - 8, z, random.nextFloat(), random.nextFloat());
		if (r > 0.5D) {
		    // Have blobs of glowstone
		    switch (random.nextInt(4)) {
		    case 1:
			// Single block
			setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
			if (x < 14 && z < 14) {
			    setBlock(result, x + 1, (maxHeight - 8), z + 1, (byte) Material.GLOWSTONE.getId());
			    setBlock(result, x + 2, (maxHeight - 8), z + 2, (byte) Material.GLOWSTONE.getId());
			    setBlock(result, x + 1, (maxHeight - 8), z + 2, (byte) Material.GLOWSTONE.getId());
			    setBlock(result, x + 1, (maxHeight - 8), z + 2, (byte) Material.GLOWSTONE.getId());
			}
			break;
		    case 2:
			// Stalatite
			for (int i = 0; i < random.nextInt(10); i++) {
			    setBlock(result, x, (maxHeight - 8 - i), z, (byte) Material.GLOWSTONE.getId());
			}
		    case 3:
			setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
			if (x > 3 && z > 3) {
			    for (int xx = 0; xx < 3; xx++) {
				for (int zz = 0; zz < 3; zz++) {
				    setBlock(result, x - xx, (maxHeight - 8 - random.nextInt(2)), z - xx, (byte) Material.GLOWSTONE.getId());
				}
			    }
			}
			break;
		    default:
			setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
		    }
		    setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
		} else {
		    setBlock(result, x, (maxHeight - 8), z, (byte) Material.AIR.getId());
		}
	    }
	}
	/*
	// Center points of the chunk I'm being given
	// int actualX = chunkX * 16;
	// int actualZ = chunkZ * 16;
	// Bukkit.getLogger().info("DEBUG: sea_level" + Settings.sea_level);
	int chunkDist = Math.round((float) Settings.islandDistance / 16F);
	// Main island creation. Make if the chunks match the isandDistance
	if (chunkX % chunkDist == 0 && chunkZ % chunkDist == 0) {
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {
		    // This should be okay for the nether, because the nether is
		    // half the height of over world
		    int island_height = Settings.island_level;
		    for (int y = island_height - 13; y < island_height; y++) {
			double r = gen.noise(x, y, z, 0.5, 0.5);
			// Put a fence around the top
			if (y == (island_height - 1)) {
			    if (x == 0 || z == 0 || x == 15 || z == 15) {
				setBlock(result, x, y, z, (byte) Material.NETHER_FENCE.getId());
			    }
			    // Put brick for the fence to sit on
			} else if (y == (island_height - 2)) {
			    if (x == 0 || z == 0 || x == 15 || z == 15) {
				setBlock(result, x, y, z, (byte) Material.NETHER_BRICK.getId());
			    } else if (x == 7 && z == 7) {
				// Place the chest
				// Bukkit.getLogger().info("DEBUG: placing dirt block");
				setBlock(result, x, y, z, (byte) Material.OBSIDIAN.getId());
			    } else if (x == 12 && z == 12) {
				// Player the nether-tree dirt block
				// Bukkit.getLogger().info("DEBUG: placing dirt block");
				setBlock(result, x, y, z, (byte) Material.DIRT.getId());
			    } else {
				// Place random fire on the top level
				// Bukkit.getLogger().info("nether noise = " +
				// r);
				if (r > 0.5D) {
				    setBlock(result, x, y, z, (byte) Material.FIRE.getId());
				}
			    }
			} else {
			    // Make a wall around the island with a few windows
			    // (okay holes)
			    if (x == 0 || z == 0 || x == 15 || z == 15) {
				if (r > -0.5D) {
				    setBlock(result, x, y, z, (byte) Material.NETHER_BRICK.getId());
				}
			    } else {
				// Fill the inside of the island randomly with
				// blocks
				if (r > 0.25D) {
				    setBlock(result, x, y, z, (byte) Material.AIR.getId());
				} else if (r > 0.15D) {
				    setBlock(result, x, y, z, (byte) Material.STONE.getId());
				} else if (r > 0D) {
				    setBlock(result, x, y, z, (byte) Material.SOUL_SAND.getId());
				} else if (r > -0.4D) {
				    setBlock(result, x, y, z, (byte) Material.NETHERRACK.getId());
				    // Put lava in the holes if it is below the
				    // lava level
				} else if (y < island_height - 10) {
				    setBlock(result, x, y, z, (byte) Material.LAVA.getId());
				}
			    }
			}
		    }
		}

	    }
	    // Monster island towers
	    // (a % b + b) % b is required to make Java % always give positive remainders
	} else if (((chunkX % chunkDist) + chunkDist) % chunkDist == 2 && ((chunkZ % chunkDist) + chunkDist) % chunkDist == 2) {
	    //Bukkit.getLogger().info("chunk x  = " + chunkX + " chunk z = " + chunkZ);
	    //Bukkit.getLogger().info("Math.abs(chunkX) % chunkDist = " + (Math.abs(chunkX) % chunkDist));
	    //Bukkit.getLogger().info("Math.abs(chunkZ) % chunkDist = " + (Math.abs(chunkZ) % chunkDist));
	    // Fill the area with lava first
	    /*
	 * for (int x = 0; x < 16; x++) {
	 * for (int z = 0; z < 16; z++) {
	 * for (int y = 0; y < 50; y++) {
	 * setBlock(result,x,y,z, (byte)Material.STATIONARY_LAVA.getId());
	 * }
	 * }
	 * }
	 */
	// Make the towers
	/*
	    for (int x = 0; x < 11; x++) {
		for (int z = 0; z < 11; z++) {
		    int island_height = Settings.island_level + 25;
		    for (int y = Settings.island_level - 20; y < island_height; y++) {
			double r = gen.noise(x, y, z, 0.5, 0.5);
			// Fence around the top
			if (y == (island_height - 1)) {
			    if (x == 0 || z == 0 || x == 10 || z == 10) {
				setBlock(result, x, y, z, (byte) Material.NETHER_FENCE.getId());
			    } else {
				// Place random spawners. The type is set in the
				// block populator
				if (random.nextInt(10) > 8) {
				    // TODO: Check around so there are no chests
				    // next to each other
				    setBlock(result, x, y, z, (byte) Material.OBSIDIAN.getId());
				} else if (random.nextInt(10) > 8) {
				    setBlock(result, x, y, z, (byte) Material.MOB_SPAWNER.getId());
				}
			    }
			    // Fence sits on nether brick
			} else if (y == (island_height - 2)) {
			    setBlock(result, x, y, z, (byte) Material.NETHER_BRICK.getId());
			} else {
			    if (x == 0 || z == 0 || x == 10 || z == 10) {
				// Wall around the island with occasional holes
				if (r > -0.5D) {
				    setBlock(result, x, y, z, (byte) Material.NETHER_BRICK.getId());
				} else {
				    if (random.nextInt(2) == 1) {
					setBlock(result, x, y, z, (byte) Material.LAVA.getId());
				    } else {
					setBlock(result, x, y, z, (byte) Material.AIR.getId());
				    }
				}

			    } else {
				// Fill with random blocks
				if (r > 0.8D) {
				    setBlock(result, x, y, z, (byte) Material.LAVA.getId());
				} else if (r > 0.5D) {
				    setBlock(result, x, y, z, (byte) Material.AIR.getId());
				} else if (r > 0.4D) {
				    setBlock(result, x, y, z, (byte) Material.STONE.getId());
				} else if (r > 0.3D) {
				    setBlock(result, x, y, z, (byte) Material.GRAVEL.getId());
				} else if (r > 0.2D) {
				    setBlock(result, x, y, z, (byte) Material.SOUL_SAND.getId());
				} else if (r > -0.5D) {
				    setBlock(result, x, y, z, (byte) Material.NETHERRACK.getId());
				}
			    }
			    // Fray the bottom
			    if (y < Settings.island_level - 15) {
				if (r > 0D) {
				    setBlock(result, x, y, z, (byte) Material.AIR.getId());
				}
			    }
			}
		    }
		}

	    }
	} else {
	    // Everywhere else (not tower, not island)
	    /*
	 * for (int x = 0; x < 16; x++) {
	 * for (int z = 0; z < 16; z++) {
	 * makeRoof(x,z);
	 * }
	 * }
	 */
	//}
	return result;

    }
}