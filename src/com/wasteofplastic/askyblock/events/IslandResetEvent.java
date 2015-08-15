package com.wasteofplastic.askyblock.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when a player resets an island
 * 
 * @author tastybento
 * 
 */
public class IslandResetEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Location location;

    /**
     * @param player
     * @param oldLocation
     */
    public IslandResetEvent(Player player, Location oldLocation) {
	this.player = player;
	this.location = oldLocation;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }


    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }
}
