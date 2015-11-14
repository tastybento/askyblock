package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import com.wasteofplastic.askyblock.Island;

/**
 * This event is fired when a player joins an island team as a coop member
 * 
 * @author tastybento
 * 
 */
public class CoopJoinEvent extends ASkyBlockEvent {
    
    private final UUID inviter;

    /**
     * @param player
     * @param teamLeader
     */
    public CoopJoinEvent(UUID player, Island island, UUID inviter) {
	super(player, island);
	this.inviter = inviter;
	//Bukkit.getLogger().info("DEBUG: Coop join event " + Bukkit.getServer().getOfflinePlayer(player).getName() + " joined " 
	//+ Bukkit.getServer().getOfflinePlayer(inviter).getName() + "'s island.");
    }

    /**
     * The UUID of the player who invited the player to join the island
     * @return the inviter
     */
    public UUID getInviter() {
        return inviter;
    }

}
