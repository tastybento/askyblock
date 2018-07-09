package com.wasteofplastic.askyblock.events;

import com.wasteofplastic.askyblock.Island;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * This event is fired when a player resets an island, before {@link IslandDeleteEvent} and {@link IslandResetEvent}
 *
 * @author Poslovitch
 * @since 02.03.2018 - Commande Heliosard
 */
public class IslandPreDeleteEvent  extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID playerUUID;
    private final Island island;

    /**
     * @param playerUUID
     * @param island
     */
    public IslandPreDeleteEvent(UUID playerUUID, Island island) {
        this.playerUUID = playerUUID;
        this.island = island;
    }

    /**
     * @return the player's UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}