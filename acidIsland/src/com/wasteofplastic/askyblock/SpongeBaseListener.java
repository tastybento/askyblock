package com.wasteofplastic.askyblock;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;

public class SpongeBaseListener implements Listener {
    private final ASkyBlock plugin;

    public SpongeBaseListener(ASkyBlock aSkyBlock) {
	plugin = aSkyBlock;
    }

    // Deal with the birth of a sponge
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
	if (!event.isCancelled()) {
	    if (plugin.debug) {
		plugin.getLogger().info(event.getPlayer().getName() + " placed a block...");
	    }
	    // Check if the block is a Sponge
	    // if (plugin.isSponge(event.getBlock()) &&
	    // !Settings.excludedWorlds.contains(event.getBlock().getWorld().getName()))
	    // {
	    if (plugin.isSponge(event.getBlock())) {
		if (plugin.debug) {
		    plugin.getLogger().info("and it's a sponge!!!!!");
		}
		plugin.enableSponge(event.getBlock());
	    }

	    // Check if a water block is being placed within sponge's area
	    if (!Settings.canPlaceWater
		    && ((plugin.blockIsAffected(event.getBlock())) && plugin.spongeAreas.containsKey(plugin.getBlockCoords(event.getBlock())))) {
		event.setCancelled(true);
		if (plugin.debug) {
		    plugin.getLogger().info("You cannot put liquid there!! :O");
		}
	    }
	}
    }

    // Check if water is being dumped in the sponge's area
    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
	Block involvedBlock = event.getBlockClicked().getRelative(event.getBlockFace());
	String dumpLocation = plugin.getBlockCoords(involvedBlock);
	Material bucketType = event.getBucket();
	if (plugin.debug) {
	    plugin.getLogger().info(bucketType + " emptied!");
	}
	if (plugin.debug) {
	    plugin.getLogger().info(involvedBlock.getType() + " dumped out!");
	}
	if (!Settings.canPlaceWater
		&& ((bucketType == Material.WATER_BUCKET || (Settings.absorbLava && bucketType == Material.LAVA_BUCKET)) && plugin.spongeAreas
			.containsKey(dumpLocation))) {
	    event.setCancelled(true);
	    if (plugin.debug) {
		plugin.getLogger().info("You can't dump liquid there!! :O (" + dumpLocation + ")");
	    }
	    event.setItemStack(new ItemStack(Material.BUCKET, 1));
	}
    }

    // Remove broken sponges from database
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
	final Block b = event.getBlock();
	if (b.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (b.getType().equals(Material.SPONGE)) {
		Block wasBlock = event.getBlock();
		plugin.getLogger().info("Sponge destroyed!");
		SpongeFlowTimer flowTimer = new SpongeFlowTimer(plugin, plugin.disableSponge(wasBlock));
		plugin.workerThreads.execute(flowTimer);
		plugin.flowTimers.add(flowTimer);
	    } else if (plugin.isIce(event.getBlock())) {
		Block wasBlock = event.getBlock();
		if (plugin.debug) {
		    plugin.getLogger().info("Ice destroyed!");
		}
		// Check if the ice was within a sponge's area.
		if (!Settings.canPlaceWater && plugin.spongeAreas.containsKey(plugin.getBlockCoords(wasBlock))) {
		    wasBlock.setType(Material.AIR);
		    if (plugin.debug) {
			plugin.getLogger().info("Melted ice gone now :D");
		    }
		}
	    }
	}
    }

    // Save the sponge database
    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
	if (plugin.debug) {
	    plugin.getLogger().info("World saved, along with sponges!");
	}
	plugin.saveSpongeData(Settings.threadedSpongeSave);
    }
}
