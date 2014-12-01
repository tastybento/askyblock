package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CoopPlay {

    private static CoopPlay instance = new CoopPlay(ASkyBlock.getPlugin());
    private HashMap<UUID, Set<Location>> coopPlayers = new HashMap<UUID, Set<Location>>();
    private ASkyBlock plugin;
    /**
     * @param instance
     */
    public CoopPlay(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * Temporarily adds a player to an island.
     * @param requester
     * @param newPlayer
     */
    public void addCoopPlayer(Player requester, Player newPlayer) {
	Location island = null;
	if (plugin.getPlayers().inTeam(requester.getUniqueId())) {
	    island = plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId());
	} else {
	    island = plugin.getPlayers().getIslandLocation(requester.getUniqueId());
	}
	if (coopPlayers.containsKey(newPlayer.getUniqueId())) {
	    // Add this island to the set
	    coopPlayers.get(newPlayer.getUniqueId()).add(island);
	} else {
	    Set<Location> loc = new HashSet<Location>();
	    loc.add(island);
	    coopPlayers.put(newPlayer.getUniqueId(),loc);
	}
    }

    /**
     * Removes a coop player
     * @param requester
     * @param targetPlayer
     * @return true if the player was a coop player, and false if not
     */
    public boolean removeCoopPlayer(Player requester, Player targetPlayer) {
	boolean removed = false;
	if (coopPlayers.containsKey(targetPlayer.getUniqueId())) {
	    // Remove any and all islands related to requester
	    if (plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId()) != null) {
		removed = coopPlayers.get(targetPlayer.getUniqueId()).remove(plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId()));	
	    }
	    if (plugin.getPlayers().getIslandLocation(requester.getUniqueId()) != null) {
		removed = coopPlayers.get(targetPlayer.getUniqueId()).remove(plugin.getPlayers().getIslandLocation(requester.getUniqueId()));
	    }
	} 
	return removed;
    }
    /**
     * Returns the list of islands that this player is coop on or empty if none
     * @param player
     * @return
     */
    public Set<Location> getCoopIslands(Player player) {
	if (coopPlayers.containsKey(player.getUniqueId())) {
	    return coopPlayers.get(player.getUniqueId()); 
	}
	return new HashSet<Location>();
    }

    /**
     * Removes the player from the coop list if they are on it
     * @param player
     */
    public boolean clearCoopPlayer(Player player) {
	// Return false if there was no player to remove, otherwise true
	return (coopPlayers.remove(player.getUniqueId()) == null) ? false : true;
    }
    
    /**
     * Removes all coop players from an island - used when doing an island reset
     * @param player
     */
    public void clearAllIslandCoops(UUID player) {
	// Remove any and all islands related to requester
	Location teamIsland = plugin.getPlayers().getTeamIslandLocation(player);
	Location island = plugin.getPlayers().getIslandLocation(player);
	Iterator<Entry<UUID, Set<Location>>> it = coopPlayers.entrySet().iterator();
	while (it.hasNext()) {
	    Entry<UUID, Set<Location>> en = it.next();
	    Iterator<Location> l = en.getValue().iterator();
	    while (l.hasNext()) {
		Location loc = l.next();
		if ((island != null && loc.equals(island)) || (teamIsland != null && loc.equals(teamIsland))) {
		    l.remove();
		}
	    }	    
	}
    }
    
    /**
     * Removes all coop players from an island - used when doing an island reset
     * @param player
     */
    public void clearAllIslandCoops(Location island) {
	// Remove any and all islands related to requester
	Iterator<Entry<UUID, Set<Location>>> it = coopPlayers.entrySet().iterator();
	while (it.hasNext()) {
	    Entry<UUID, Set<Location>> en = it.next();
	    Iterator<Location> l = en.getValue().iterator();
	    while (l.hasNext()) {
		Location loc = l.next();
		if ((island != null && loc.equals(island))) {
		    l.remove();
		}
	    }	    
	}
    }
    /**
     * @return the instance
     */
    public static CoopPlay getInstance() {
	return instance;
    } 
}
