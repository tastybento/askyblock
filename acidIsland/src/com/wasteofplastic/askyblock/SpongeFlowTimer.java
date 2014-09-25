package com.wasteofplastic.askyblock;

import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

/**
 * This is a thread that clears the deleted sponge area database after the water
 * has had a chance to propagate.
 * 
 * @author Devil Boy
 * 
 */
public class SpongeFlowTimer implements Runnable {
    private final ASkyBlock plugin;
    private LinkedList<Block> removedCoords;
    int waittime;

    // public SpongeFlowTimer(ASkyBlock plugin, LinkedList<String>
    // removedCoords) {
    public SpongeFlowTimer(ASkyBlock plugin, LinkedList<Block> removedCoords) {
	this.plugin = plugin;
	this.removedCoords = removedCoords;

	waittime = Settings.spongeRadius * Settings.flowTimeMult;
    }

    @Override
    public void run() {
	if (plugin.debug) {
	    Bukkit.getLogger().info("FlowTimer running!");
	}
	try {
	    Thread.sleep(waittime);
	} catch (InterruptedException e) {
	}
	for (Block currentCoord : removedCoords) {
	    plugin.removeFromSpongeAreas(plugin.getBlockCoords(currentCoord));
	}
	if (plugin.debug) {
	    Bukkit.getLogger().info("Water is out of time!");
	}
	plugin.flowTimers.remove(this);
    }
}
