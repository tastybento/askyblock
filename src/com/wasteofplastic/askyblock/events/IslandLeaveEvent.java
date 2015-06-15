package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player leaves an island team
 * @author tastybento
 *
 */
public class IslandLeaveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final UUID teamLeader;
    private final Island island;

    /**
     * @param player
     * @param teamLeader
     */
    public IslandLeaveEvent(ASkyBlock plugin, UUID player, UUID teamLeader) {
	this.player = player;
	this.teamLeader = teamLeader;
	this.island = plugin.getGrid().getIsland(teamLeader);
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * @return the teamLeader
     */
    public UUID getTeamLeader() {
        return teamLeader;
    }

    /**
     * @return the island
     */
    public Location getIslandLocation() {
        return island.getCenter();
    }
    

    /**
     * @return the protectionSize
     */
    public int getProtectionSize() {
        return island.getProtectionSize();
    }

    /**
     * @return the isLocked
     */
    public boolean isLocked() {
        return island.isLocked();
    }

    /**
     * @return the islandDistance
     */
    public int getIslandDistance() {
        return island.getIslandDistance();
    }

    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }
}
