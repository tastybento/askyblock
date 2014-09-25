package com.wasteofplastic.askyblock;

import java.util.LinkedList;

import org.bukkit.block.Block;

public class SpongeMultiSpongeThread implements Runnable {
    final ASkyBlock plugin;
    LinkedList<Block> enables;
    LinkedList<Block> disables;

    public SpongeMultiSpongeThread(LinkedList<Block> enableSponges, LinkedList<Block> disableSponges, final ASkyBlock pluginI) {
	plugin = pluginI;
	enables = enableSponges;
	disables = disableSponges;
    }

    public void run() {
	if (!enables.isEmpty()) {
	    for (Block blk : enables) {
		plugin.enableSponge(blk);
	    }
	}
	if (!disables.isEmpty()) {
	    for (Block blk : disables) {
		plugin.disableSponge(blk);
	    }
	}
    }
}
