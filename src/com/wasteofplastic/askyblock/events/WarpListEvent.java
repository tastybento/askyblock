package com.wasteofplastic.askyblock.events;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * This event is fired when request is made for a sorted list of warps or when
 * the API updateWarpPanel method is called.
 * A listener to this event can reorder or rewrite the warp list by using setWarps.
 * This new order will then be used in the warp panel.
 * 
 * @author tastybento
 * 
 */
public class WarpListEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Collection<UUID> warps;

    /**
     * @param plugin
     * @param warps
     */
    public WarpListEvent(ASkyBlock plugin, Collection<UUID> warps) {
	this.warps = warps;
    }


    /**
     *The warp list is a collection of player UUID's and the default order is
     * that players with the most recent login will be first.
     * @return the warps
     */
    public Collection<UUID> getWarps() {
        return warps;
    }

    /**
     * @param warps the warps to set
     */
    public void setWarps(Collection<UUID> warps) {
        this.warps = warps;
    }


    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }
}
