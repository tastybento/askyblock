package com.wasteofplastic.askyblock;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.BlockPopulator;

/**
 * @author tastybento
 * Populates the Nether with appropriate blocks
 *
 */
public class NetherPopulator extends BlockPopulator {

    @Override
    public void populate(World world, Random random, Chunk source) {
	// Rough check - convert spawners to Nether spawners
	for (int x = 0; x<16; x++) {
	    for (int y = 0; y < world.getMaxHeight(); y++) {
		for (int z = 0; z<16; z++) {
		    Block b = source.getBlock(x, y, z);
		    if (b.getType().equals(Material.MOB_SPAWNER)){
			CreatureSpawner cs = (CreatureSpawner) b.getState();
			switch(random.nextInt(3)) {
			case 0:
			    cs.setSpawnedType(EntityType.BLAZE);
			    break;
			case 1:
			    cs.setSpawnedType(EntityType.SKELETON);
			    break;
			case 2:
			    cs.setSpawnedType(EntityType.MAGMA_CUBE);
			    break;
			default:
			    cs.setSpawnedType(EntityType.BLAZE);	
			}
		    }
		}
	    }
	}
    }

}
