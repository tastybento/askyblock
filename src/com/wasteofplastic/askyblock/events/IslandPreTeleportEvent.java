package com.wasteofplastic.askyblock.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when a player use for example: /is, /is warp...
 *
 * @author Tauchet
 *
 */

public class IslandPreTeleportEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final Type type;

    private final Location location;

    public IslandPreTeleportEvent(Player player, Type type, Location location) {
        this.player = player;
        this.type = type;
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    public Type getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public enum Type {

        HOME,
        WARP,
        SPAWN

    }

}
