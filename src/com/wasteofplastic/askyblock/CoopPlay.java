package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.events.CoopJoinEvent;
import com.wasteofplastic.askyblock.events.CoopLeaveEvent;

/**
 * Handles coop play interactions
 * 
 * @author tastybento
 * 
 */
public class CoopPlay {
    private static CoopPlay instance = new CoopPlay(ASkyBlock.getPlugin());
    // Stores all the coop islands, the coop player, the location and the
    // inviter
    private HashMap<UUID, HashMap<Location, UUID>> coopPlayers = new HashMap<UUID, HashMap<Location, UUID>>();
    // Defines whether a player is on a coop island or not
    // private HashMap<UUID, Location> onCoopIsland = new HashMap<UUID,
    // Location>();
    private ASkyBlock plugin;

    /**
     * @param instance
     */
    private CoopPlay(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * Adds a player to an island as a coop player.
     * 
     * @param requester
     * @param newPlayer
     */
    public void addCoopPlayer(Player requester, Player newPlayer) {
	// plugin.getLogger().info("DEBUG: adding coop player");
	// Find out which island this coop player is being requested to join
	Location island = null;
	if (plugin.getPlayers().inTeam(requester.getUniqueId())) {
	    island = plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId());
	    // Tell the team owner
	    UUID leaderUUID = plugin.getPlayers().getTeamLeader(requester.getUniqueId());
	    // Tell all the team members
	    for (UUID member : plugin.getPlayers().getMembers(leaderUUID)) {
		// plugin.getLogger().info("DEBUG: " + member.toString());
		if (!member.equals(requester.getUniqueId())) {
		    Player player = plugin.getServer().getPlayer(member);
		    if (player != null) {
			player.sendMessage(ChatColor.GOLD
				+ plugin.myLocale(player.getUniqueId()).coopInvited.replace("[name]", requester.getDisplayName()).replace("[player]", newPlayer.getDisplayName()));
			player.sendMessage(ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).coopUseExpel);
		    } else {
			if (member.equals(leaderUUID)) {
			    // offline - tell leader
			    plugin.getMessages().setMessage(leaderUUID,
				    plugin.myLocale(leaderUUID).coopInvited.replace("[name]", requester.getDisplayName()).replace("[player]", newPlayer.getDisplayName()));
			}
		    }
		}
	    }
	} else {
	    island = plugin.getPlayers().getIslandLocation(requester.getUniqueId());
	}
	// Add the coop to the list. If the location already exists then the new
	// requester will replace the old
	if (coopPlayers.containsKey(newPlayer.getUniqueId())) {
	    // This is an existing player in the list
	    // Add this island to the set
	    coopPlayers.get(newPlayer.getUniqueId()).put(island, requester.getUniqueId());
	} else {
	    // First time. Create the hashmap
	    HashMap<Location, UUID> loc = new HashMap<Location, UUID>();
	    loc.put(island, requester.getUniqueId());
	    coopPlayers.put(newPlayer.getUniqueId(), loc);
	}
	// Fire event
	Island coopIsland = plugin.getGrid().getIslandAt(island);
	final CoopJoinEvent event = new CoopJoinEvent(newPlayer.getUniqueId(), coopIsland, requester.getUniqueId());
	plugin.getServer().getPluginManager().callEvent(event);
    }

    /**
     * Removes a coop player
     * 
     * @param requester
     * @param targetPlayer
     * @return true if the player was a coop player, and false if not
     */
    public boolean removeCoopPlayer(Player requester, Player targetPlayer) {
	boolean removed = false;
	// Only bother if the player is in the list
	if (coopPlayers.containsKey(targetPlayer.getUniqueId())) {
	    // Remove any and all islands related to requester
	    /*
	    if (plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId()) != null) {
		removed = coopPlayers.get(targetPlayer.getUniqueId()).remove(plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId())) != null ? true
			: false;
	    }*/
	    if (plugin.getPlayers().getIslandLocation(requester.getUniqueId()) != null) {
		removed = coopPlayers.get(targetPlayer.getUniqueId()).remove(plugin.getPlayers().getIslandLocation(requester.getUniqueId())) != null ? true
			: false;
		// Fire event
		Island coopIsland = plugin.getGrid().getIsland(requester.getUniqueId());
		final CoopLeaveEvent event = new CoopLeaveEvent(targetPlayer.getUniqueId(), requester.getUniqueId(), coopIsland);
		plugin.getServer().getPluginManager().callEvent(event);
	    }
	}
	return removed;
    }

    /**
     * Returns the list of islands that this player is coop on or empty if none
     * 
     * @param player
     * @return
     */
    public Set<Location> getCoopIslands(Player player) {
	if (coopPlayers.containsKey(player.getUniqueId())) {
	    return coopPlayers.get(player.getUniqueId()).keySet();
	}
	return new HashSet<Location>();
    }

    /**
     * Gets a list of all the players that are currently coop on this island
     * 
     * @param island
     * @return List of UUID's of players that have coop rights to the island
     */
    public List<UUID> getCoopPlayers(Location island) {
	List<UUID> result = new ArrayList<UUID>();
	for (UUID player : coopPlayers.keySet()) {
	    if (coopPlayers.get(player).containsKey(island)) {
		result.add(player);
	    }
	}
	return result;
    }

    /**
     * Removes all coop players from an island - used when doing an island reset
     * 
     * @param player
     */
    public void clearAllIslandCoops(UUID player) {
	// Remove any and all islands related to requester
	Island island = plugin.getGrid().getIsland(player);
	for (HashMap<Location, UUID> coopPlayer : coopPlayers.values()) {
	    for (UUID inviter : coopPlayer.values()) {
		// Fire event
		final CoopLeaveEvent event = new CoopLeaveEvent(player, inviter, island);
		plugin.getServer().getPluginManager().callEvent(event);
	    }
	    coopPlayer.remove(island.getCenter());
	}
    }

    /**
     * Deletes all coops from player.
     * Used when player logs out.
     * 
     * @param player
     */
    public void clearMyCoops(Player player) {
	//plugin.getLogger().info("DEBUG: clear my coops - clearing coops memberships of " + player.getName());
	Island coopIsland = plugin.getGrid().getIsland(player.getUniqueId());
	if (coopPlayers.get(player.getUniqueId()) != null) {
	    //plugin.getLogger().info("DEBUG: " + player.getName() + " is a member of a coop");
	    for (UUID inviter : coopPlayers.get(player.getUniqueId()).values()) {
		// Fire event
		//plugin.getLogger().info("DEBUG: removing invite from " + plugin.getServer().getPlayer(inviter).getName());
		final CoopLeaveEvent event = new CoopLeaveEvent(player.getUniqueId(), inviter, coopIsland);
		plugin.getServer().getPluginManager().callEvent(event);
	    }
	    coopPlayers.remove(player.getUniqueId());
	}
    }

    /**
     * Goes through all the known coops and removes any that were invited by
     * clearer. Returns any inventory
     * Can be used when clearer logs out or when they are kicked or leave a team
     * 
     * @param clearer
     * @param target
     */
    public void clearMyInvitedCoops(Player clearer) {
	//plugin.getLogger().info("DEBUG: clear my invited coops - clearing coops that were invited by " + clearer.getName());
	Island coopIsland = plugin.getGrid().getIsland(clearer.getUniqueId());
	for (UUID players : coopPlayers.keySet()) {
	    Iterator<Entry<Location, UUID>> en = coopPlayers.get(players).entrySet().iterator();
	    while (en.hasNext()) {
		Entry<Location, UUID> entry = en.next();
		// Check if this invite was sent by clearer
		if (entry.getValue().equals(clearer.getUniqueId())) {
		    // Yes, so get the invitee (target)
		    Player target = plugin.getServer().getPlayer(players);
		    if (target != null) {
			target.sendMessage(ChatColor.RED + "You are no longer a coop player with " + clearer.getDisplayName() + ".");
		    }
		    // Fire event
		    final CoopLeaveEvent event = new CoopLeaveEvent(players, clearer.getUniqueId(), coopIsland);
		    plugin.getServer().getPluginManager().callEvent(event);
		    // Mark them as no longer on a coop island
		    // setOnCoopIsland(players, null);
		    // Remove this entry
		    en.remove();
		}
	    }
	}
    }

    /**
     * Removes all coop players from an island - used when doing an island reset
     * 
     * @param player
     */
    public void clearAllIslandCoops(Location island) {
	if (island == null) {
	    return;
	}
	Island coopIsland = plugin.getGrid().getIslandAt(island);
	// Remove any and all islands related to requester
	for (HashMap<Location, UUID> coopPlayer : coopPlayers.values()) {
	    // Fire event
	    final CoopLeaveEvent event = new CoopLeaveEvent(coopPlayer.get(island), coopIsland.getOwner(), coopIsland);
	    plugin.getServer().getPluginManager().callEvent(event);
	    coopPlayer.remove(island);
	}
    }

    /**
     * @return the instance
     */
    public static CoopPlay getInstance() {
	return instance;
    }
}