package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * This event is fired when a Warp is removed (when a warp sign is broken)
 * A Listener to this event can use it only to get informations. e.g: broadcast something
 * 
 * @author Poslovitch
 *
 */
public class WarpRemoveEvent extends Event{
	private static final HandlerList handlers = new HandlerList();
	
	private final Location warpLoc;
	private final UUID remover;
	
	/**
	 * @param plugin - ASkyBlock plugin object
	 * @param warpLoc
	 * @param remover
	 */
	public WarpRemoveEvent(ASkyBlock plugin, Location warpLoc, UUID remover){
		this.warpLoc = warpLoc;
		this.remover = remover;
	}
	
	/**
	 * Get the location of the removed Warp
	 * @return removed warp's location
	 */
	public Location getWarpLocation(){return this.warpLoc;}
	
	/**
	 * Get who has removed the warp
	 * @return the warp's remover
	 */
	public UUID getRemover(){return this.remover;}
	
	@Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}