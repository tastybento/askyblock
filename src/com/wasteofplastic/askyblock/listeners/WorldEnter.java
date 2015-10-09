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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
	//plugin.getLogger().info("DEBUG " + event.getEventName());
	if (!event.getPlayer().getWorld().equals(ASkyBlock.getIslandWorld()) &&
		!event.getPlayer().getWorld().equals(ASkyBlock.getNetherWorld())) {
	    return;
	}
	//plugin.getLogger().info("DEBUG correct world");
	Location islandLoc = plugin.getPlayers().getIslandLocation(event.getPlayer().getUniqueId());
	if (islandLoc == null) {
	    //plugin.getLogger().info("DEBUG  no island");
	    // They have no island
	    if (Settings.makeIslandIfNone || Settings.immediateTeleport) {
		event.getPlayer().performCommand(Settings.ISLANDCOMMAND);
	    }
	    //plugin.getLogger().info("DEBUG Make island");
	} else {
	    // They have an island and are going to their own world
	    if (Settings.immediateTeleport && islandLoc.getWorld().equals(event.getPlayer().getWorld())) {
		//plugin.getLogger().info("DEBUG teleport");
		event.getPlayer().performCommand(Settings.ISLANDCOMMAND + " go");
	    }
	}
    }
}
