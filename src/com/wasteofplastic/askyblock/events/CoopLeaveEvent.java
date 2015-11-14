package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import com.wasteofplastic.askyblock.Island;


/**
 * Fired when a player is leaves an island coop
 * @author tastybento
 *
 */
public class CoopLeaveEvent extends ASkyBlockEvent {
    private final UUID expeller;

    /**
     * @param expelledPlayer
     * @param expellingPlayer
     * @param island
     */
    public CoopLeaveEvent(UUID expelledPlayer, UUID expellingPlayer, Island island) {
	super(expellingPlayer, island);
	this.expeller = expellingPlayer;
	//Bukkit.getLogger().info("DEBUG: Coop leave event " + Bukkit.getServer().getOfflinePlayer(expelledPlayer).getName() + " was expelled from " 
	//	+ Bukkit.getServer().getOfflinePlayer(expellingPlayer).getName() + "'s island.");
    }

    /**
     * @return the expelling player
     */
    public UUID getExpeller() {
        return expeller;
    }
    

}
