package com.wasteofplastic.askyblock.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

public class WorldLoader implements Listener {
    private ASkyBlock plugin;
    private boolean worldLoaded = false;

    /**
     * Class to force world loading before plugins.
     * @param aSkyBlock
     */
    public WorldLoader(ASkyBlock aSkyBlock) {
	this.plugin = aSkyBlock;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(final ChunkLoadEvent event) {
	if (worldLoaded) {
	    return;
	}
	//plugin.getLogger().info("DEBUG: " + event.getWorld().getName());
	if (event.getWorld().getName().equals(Settings.worldName) || event.getWorld().getName().equals(Settings.worldName + "_nether")) {
	    return;
	}
	// Load the world
	worldLoaded = true;
	ASkyBlock.getIslandWorld();
    }
}
