package com.wasteofplastic.askyblock.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

public class WorldEnter implements Listener {
    private ASkyBlock plugin;

    public WorldEnter(ASkyBlock aSkyBlock) {
	this.plugin = aSkyBlock;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
	//plugin.getLogger().info("DEBUG " + event.getEventName());
	if (!event.getPlayer().getWorld().equals(ASkyBlock.getIslandWorld())) {
	    return;
	}
	//plugin.getLogger().info("DEBUG correct world");
	Location islandLoc = plugin.getPlayers().getIslandLocation(event.getPlayer().getUniqueId());
	if (islandLoc == null) {
	    //plugin.getLogger().info("DEBUG  no island");
	    // They have no island
	    if (!Settings.makeIslandIfNone) {
		// If player should go to spawn, just return
		//plugin.getLogger().info("DEBUG go to spawn");
		return;
	    }
	    //plugin.getLogger().info("DEBUG Make island");
	} else {
	    // They have an island
	    if (!Settings.immediateTeleport) {
		//plugin.getLogger().info("DEBUG no teleport");
		// No need to teleport
		return;
	    }  
	    //plugin.getLogger().info("DEBUG immediate teleport");
	}
	// Make new island or teleport there - it's the same command
	event.getPlayer().performCommand(Settings.ISLANDCOMMAND);
	/*
	// Set velocity to zero just in case they sped through a portal!
	plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

	    @Override
	    public void run() {
		event.getPlayer().setVelocity(new Vector());
	    }});
	
	return;*/
    }
}
