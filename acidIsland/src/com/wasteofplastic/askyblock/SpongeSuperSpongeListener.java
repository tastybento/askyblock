package com.wasteofplastic.askyblock;

import java.util.LinkedList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class SpongeSuperSpongeListener implements Listener {
    private final ASkyBlock plugin;

    /**
     * Handles additional sponge listners
     * 
     * @param pluginI
     */
    public SpongeSuperSpongeListener(final ASkyBlock pluginI) {
	plugin = pluginI;
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
	if (plugin.isNewIsland())
	    return;
	if (plugin.debug) {
	    plugin.getLogger().info("Liquid incoming at: " + event.getToBlock().getX() + ", " + event.getToBlock().getY() + ", " + event.getToBlock().getZ());
	}
	if (plugin.spongeAreas.containsKey(plugin.getBlockCoords(event.getToBlock()))) {
	    if (plugin.debug) {
		plugin.getLogger().info("Recede from sponge!");
	    }
	    if (plugin.blockIsAffected(event.getBlock())) {
		event.setCancelled(true);
	    }
	} else if (plugin.spongeAreas.containsKey(plugin.getDeletedBlockCoords(event.getToBlock())) && plugin.blockIsAffected(event.getBlock())) {
	    Block receivingBlock = event.getToBlock();
	    if (plugin.isAir(receivingBlock)) {
		receivingBlock.setType(event.getBlock().getType());
	    }
	}
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
	if (plugin.debug) {
	    plugin.getLogger().info("Fire incoming at: " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ());
	}
	if (Settings.absorbFire && event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (plugin.spongeAreas.containsKey(plugin.getBlockCoords(event.getBlock()))) {
		if (plugin.debug) {
		    plugin.getLogger().info("Fire shall not pass!");
		}
		event.setCancelled(true);
	    } else if ((plugin.isNextToSpongeArea(event.getBlock()) && Settings.attackFire)
		    && Settings.worldName.equals(event.getBlock().getWorld().getName())) {
		if (plugin.debug) {
		    plugin.getLogger().info("Extinguish fire with sponge!");
		}
		event.getBlock().setType(Material.AIR);
		event.setCancelled(true);
	    }
	}
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
	if (plugin.debug) {
	    plugin.getLogger().info("Block Burning at: " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ());
	}
	if (Settings.absorbFire && event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (plugin.spongeAreas.containsKey(plugin.getBlockCoords(event.getBlock()))
		    && Settings.worldName.equals(event.getBlock().getWorld().getName())) {
		if (plugin.debug) {
		    plugin.getLogger().info("Sponge never lets a block burn!");
		}
		event.setCancelled(true);
		plugin.killSurroundingFire(event.getBlock());
	    }
	}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockFade(BlockFadeEvent event) {
	if (event.getBlock().getType() == Material.ICE && event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (plugin.debug) {
		plugin.getLogger().info("Ice melting at: " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ());
	    }
	    if (!Settings.canPlaceWater && plugin.spongeAreas.containsKey(plugin.getBlockCoords(event.getBlock()))) {
		if (plugin.debug) {
		    plugin.getLogger().info("Sneaky ice, you thought you could let water in!");
		}
		event.setCancelled(true);
		event.getBlock().setType(Material.AIR);
	    }
	}
    }

    /**
     * Handles pistons extending
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
	if (plugin.flag) {
	    event.setCancelled(true);
	    plugin.getLogger().info("Blocked");
	    return;
	}
	if (plugin.debug) {
	    plugin.getLogger().info("Piston extending");
	}
	if (plugin.hasSponges(event.getBlocks()) && event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (plugin.debug) {
		plugin.getLogger().info("Piston has sponges");
	    }

	    if (Settings.pistonMove) {
		if (plugin.debug) {
		    plugin.getLogger().info("Piston move is true");
		}

		LinkedList<Block> movedSponges = plugin.getSponges(event.getBlocks());
		// LinkedList<Block> toEnable = new LinkedList<Block>();
		// LinkedList<Block> toDisable = new LinkedList<Block>();
		if (movedSponges.size() == 1) { // No need to check for
						// sequential sponges
		    if (plugin.debug) {
			plugin.getLogger().info("Piston move sponges = 1");
		    }

		    final Block spawnge = movedSponges.getLast();
		    plugin.disableSponge(spawnge);
		    plugin.enableSponge(spawnge.getRelative(event.getDirection()));
		    // toDisable.add(spawnge);
		    // toEnable.add(spawnge.getRelative(event.getDirection()));
		} else {
		    if (plugin.debug) {
			plugin.getLogger().info("Piston has more than 1 sponges");
		    }

		    for (final Block spawnge : movedSponges) {
			// Disable old spot?
			if (!plugin.isSponge(spawnge.getRelative(event.getDirection().getOppositeFace()))) {
			    plugin.disableSponge((spawnge));
			    // toDisable.add(spawnge);
			}
			// Enable new spot?
			if (!plugin.isSponge(spawnge.getRelative(event.getDirection()))) {
			    plugin.enableSponge(spawnge.getRelative(event.getDirection()));
			    // toEnable.add(spawnge.getRelative(event.getDirection()));
			}
		    }
		}
		// plugin.getServer().getScheduler().runTask(plugin, new
		// SRMultiSpongeThread(toEnable, toDisable, plugin));
		// plugin.workerThreads.execute(new
		// SRMultiSpongeThread(toEnable, toDisable, plugin));
	    } else {
		event.setCancelled(true);
	    }
	}
    }

    @EventHandler
    public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
	if (plugin.flag) {
	    event.setCancelled(true);
	    return;
	}
	if (event.isSticky() && plugin.isSponge(event.getRetractLocation().getBlock())
		&& event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (plugin.debug) {
		plugin.getLogger().info("Sticky Piston retracting");
	    }
	    if (Settings.pistonMove) {
		if (plugin.debug) {
		    plugin.getLogger().info("Sticky Piston - move = true");
		}
		// LinkedList<Block> toEnable = new LinkedList<Block>();
		// LinkedList<Block> toDisable = new LinkedList<Block>();
		// plugin.getServer().getScheduler().runTask(plugin, new
		// SpongeMultiSpongeThread(toEnable, toDisable, plugin));
		plugin.disableSponge(event.getRetractLocation().getBlock());
		plugin.enableSponge(event.getBlock().getRelative(event.getDirection()));
		// toDisable.add(event.getRetractLocation().getBlock());
		// toEnable.add(event.getBlock().getRelative(event.getDirection()));
		// plugin.getServer().getScheduler().runTask(plugin, new
		// SRMultiSpongeThread(toEnable, toDisable, plugin));
		// plugin.workerThreads.execute(new
		// SRMultiSpongeThread(toEnable, toDisable, plugin));
	    } else {
		event.setCancelled(true);
	    }
	}
    }
}
