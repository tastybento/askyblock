package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import com.wasteofplastic.askyblock.Island;


/**
 * Fired when the owner of an island changes
 * 
 * @author Poslovitch
 * @version 1.0
 */
public class IslandChangeOwnerEvent extends ASkyBlockEvent {
    private final UUID oldOwner, newOwner;

    /**
     * @param island
     * @param oldOwner
     * @param newOwner
     */
    public IslandChangeOwnerEvent(Island island, UUID oldOwner, UUID newOwner) {
        super(oldOwner, island);
        this.oldOwner = oldOwner;
        this.newOwner = newOwner;
    }

    /**
     * @return the old owner
     */
    public UUID getOldOwner() {
        return oldOwner;
    }

    /**
     * @return the new owner
     */
    public UUID getNewOwner() {
        return newOwner;
    }
}
