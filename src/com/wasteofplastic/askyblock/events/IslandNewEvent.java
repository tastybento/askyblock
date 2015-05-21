package com.wasteofplastic.askyblock.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.schematics.Schematic;

/**
 * This event is fired when a player starts a new island
 * 
 * @author tastybento
 * 
 */
public class IslandNewEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Schematic schematic;
    private final Island island;

    /**
     * @param player
     * @param schematic
     * @param island
     */
    public IslandNewEvent(Player player, Schematic schematic, Island island) {
	this.player = player;
	this.schematic = schematic;
	this.island = island;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return the schematicName
     */
    public Schematic getSchematicName() {
        return schematic;
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
