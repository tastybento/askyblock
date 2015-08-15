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
	    // First time. Create the hashamp
	    HashMap<Location, UUID> loc = new HashMap<Location, UUID>();
	    loc.put(island, requester.getUniqueId());
	    coopPlayers.put(newPlayer.getUniqueId(), loc);
	}
	// plugin.getLogger().info("DEBUG: Storing coop. coop size is " +
	// coopPlayers.size() + " players");
	// plugin.getLogger().info("DEBUG: number of locations for this player "
	// + coopPlayers.get(newPlayer.getUniqueId()).size());
	// plugin.getLogger().info("DEBUG: " + island.toString()+"  "+
	// coopPlayers.get(newPlayer.getUniqueId()).get(island));
	// Check if the player is on the island now
	/*
	 * if (plugin.isOnIsland(requester, newPlayer)) {
	 * plugin.getLogger().info("DEBUG: new player is on requester's island");
	 * newPlayer.sendMessage(ChatColor.GREEN + "Switching inventories.");
	 * // Switch inventories
	 * if (plugin.getPlayers().inTeam(newPlayer.getUniqueId())) {
	 * InventorySave.getInstance().switchPlayerInventory(newPlayer,
	 * plugin.getPlayers().getTeamIslandLocation(newPlayer.getUniqueId()),
	 * island);
	 * } else {
	 * InventorySave.getInstance().switchPlayerInventory(newPlayer,
	 * plugin.getPlayers().getIslandLocation(newPlayer.getUniqueId()),
	 * island);
	 * }
	 * }
	 */
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
	    if (plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId()) != null) {
		removed = coopPlayers.get(targetPlayer.getUniqueId()).remove(plugin.getPlayers().getTeamIslandLocation(requester.getUniqueId())) != null ? true
			: false;
	    }
	    if (plugin.getPlayers().getIslandLocation(requester.getUniqueId()) != null) {
		removed = coopPlayers.get(targetPlayer.getUniqueId()).remove(plugin.getPlayers().getIslandLocation(requester.getUniqueId())) != null ? true
			: false;
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
     * Removes the player from the coop list if they are on it
     * 
     * @param player
     */
    /*
     * public boolean clearCoopPlayer(Player player) {
     * // Return any coop inventory items that this player has
     * if (onCoopIsland.containsKey(player.getUniqueId())) {
     * returnInventory(player,onCoopIsland.get(player.getUniqueId()));
     * }
     * onCoopIsland.remove(player.getUniqueId());
     * // Return false if there was no player to remove, otherwise true
     * return (coopPlayers.remove(player.getUniqueId()) == null) ? false : true;
     * }
     */
    /**
     * Removes all coop players from an island - used when doing an island reset
     * 
     * @param player
     */
    public void clearAllIslandCoops(UUID player) {
	// Remove any and all islands related to requester
	Location teamIsland = plugin.getPlayers().getTeamIslandLocation(player);
	Location island = plugin.getPlayers().getIslandLocation(player);
	for (HashMap<Location, UUID> coopPlayer : coopPlayers.values()) {
	    if (island != null) {
		coopPlayer.remove(island);
	    }
	    if (teamIsland != null) {
		coopPlayer.remove(teamIsland);
	    }
	}
	// Clear any players who were on this island
	/*
	 * Iterator<Entry<UUID, Location>> coopPlayer =
	 * onCoopIsland.entrySet().iterator();
	 * while (coopPlayer.hasNext()) {
	 * Entry<UUID,Location> entry = coopPlayer.next();
	 * if ((island!= null && entry.getValue().equals(island)) || (teamIsland
	 * != null && entry.getValue().equals(teamIsland))) {
	 * coopPlayer.remove();
	 * }
	 * }
	 */
    }

    /**
     * Deletes all coops from player.
     * Used when player logs out.
     * 
     * @param player
     */
    public void clearMyCoops(Player player) {
	coopPlayers.remove(player.getUniqueId());

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
	/*
	 * Location expellersIsland = null;
	 * if (plugin.getPlayers().inTeam(clearer.getUniqueId())) {
	 * expellersIsland =
	 * plugin.getPlayers().getTeamIslandLocation(clearer.getUniqueId());
	 * } else {
	 * expellersIsland =
	 * plugin.getPlayers().getIslandLocation(clearer.getUniqueId());
	 * }
	 */
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
			/*
			 * // Return this island inventory to the owner
			 * returnInventory(target, expellersIsland);
			 * // Find out if they are on the island currently
			 * Location coopIsland = onCoopIsland.get(players);
			 * if (coopIsland != null &&
			 * coopIsland.equals(entry.getKey())) {
			 * // They were on the island so return their home
			 * inventory
			 * if (plugin.getPlayers().inTeam(target.getUniqueId()))
			 * {
			 * InventorySave.getInstance().loadPlayerInventory(target
			 * , plugin.getPlayers().getTeamIslandLocation(target.
			 * getUniqueId()));
			 * } else {
			 * InventorySave.getInstance().loadPlayerInventory(target
			 * ,
			 * plugin.getPlayers().getIslandLocation(target.getUniqueId
			 * ()));
			 * }
			 * }
			 */
		    }
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
	// Remove any and all islands related to requester
	for (HashMap<Location, UUID> coopPlayer : coopPlayers.values()) {
	    if (island != null) {
		coopPlayer.remove(island);
	    }
	}
    }

    /**
     * @return the instance
     */
    public static CoopPlay getInstance() {
	return instance;
    }

    /**
     * @return the onCoopIsland
     */
    /*
     * public Location getOnCoopIsland(UUID playerUUID) {
     * if (onCoopIsland.containsKey(playerUUID)) {
     * return onCoopIsland.get(playerUUID);
     * }
     * return null;
     * }
     */
    /**
     * @param to
     *            the onCoopIsland to set
     */
    /*
     * public void setOnCoopIsland(UUID playerUUID, Location to) {
     * this.onCoopIsland.put(playerUUID,to);
     * }
     */
    /**
     * @param player
     *            - the player being ejected
     * @param from
     *            - the location from where they are being rejected
     */
    /*
     * public void returnInventory(Player player, Location from) {
     * ItemStack[] armor = player.getInventory().getArmorContents();
     * ItemStack[] contents = player.getInventory().getContents();
     * // Load the old inventory
     * if (plugin.getPlayers().inTeam(player.getUniqueId())) {
     * InventorySave.getInstance().loadPlayerInventory(player,
     * plugin.getPlayers().getTeamIslandLocation(player.getUniqueId()));
     * } else {
     * InventorySave.getInstance().loadPlayerInventory(player,
     * plugin.getPlayers().getIslandLocation(player.getUniqueId()));
     * }
     * //plugin.getLogger().info("DEBUG: returning items to inviter. coop size is "
     * + coopPlayers.size() + " players");
     * //plugin.getLogger().info("DEBUG: number of locations for this player " +
     * coopPlayers.get(player.getUniqueId()).size());
     * //plugin.getLogger().info("DEBUG: from = " + from.toString() + " "+
     * coopPlayers.get(player.getUniqueId()).get(from));
     * if (coopPlayers.get(player.getUniqueId()) == null) {
     * plugin.getLogger().warning("No such coop player " + player.getName());
     * return;
     * }
     * // Give the player's items to the inviter if they are online
     * Player onlineMember =
     * plugin.getServer().getPlayer(coopPlayers.get(player.
     * getUniqueId()).get(from));
     * if (onlineMember != null) {
     * // Try to give the stuff to the online player
     * for (ItemStack i: contents) {
     * if (i != null) {
     * //plugin.getLogger().info("DEBUG: giving " + i.getType());
     * if (!i.getType().equals(Material.AIR)) {
     * HashMap<Integer,ItemStack> leftOver =
     * onlineMember.getInventory().addItem(i);
     * if (leftOver.size()> 0) {
     * for (ItemStack j: leftOver.values()) {
     * //plugin.getLogger().info("DEBUG: throwing " + j.getType());
     * onlineMember.getWorld().dropItem(onlineMember.getLocation(), j);
     * }
     * }
     * }
     * }
     * }
     * for (ItemStack i: armor) {
     * if (i != null) {
     * //plugin.getLogger().info("DEBUG: giving " + i.getType());
     * if (!i.getType().equals(Material.AIR)) {
     * HashMap<Integer,ItemStack> leftOver =
     * onlineMember.getInventory().addItem(i);
     * if (leftOver.size()> 0) {
     * for (ItemStack j: leftOver.values()) {
     * //plugin.getLogger().info("DEBUG: throwing " + j.getType());
     * onlineMember.getWorld().dropItem(onlineMember.getLocation(), j);
     * }
     * }
     * }
     * }
     * }
     * onlineMember.sendMessage(ChatColor.GREEN +
     * "Returned items from coop player.");
     * }
     * // That should do it! If there are not online then the items are lost.
     * }
     */
    /**
     * Returns all the coop Inventories that a player has - used when they log
     * out or are kicked from a team
     * 
     * @param player
     */
    /*
     * public void returnAllInventories(Player player) {
     * // If the player is not coop then just return
     * if (!coopPlayers.containsKey(player.getUniqueId())) {
     * return;
     * }
     * // The player is currently on a coop island
     * if (onCoopIsland.get(player.getUniqueId()) != null) {
     * plugin.getLogger().info(
     * "DEBUG : currently on coop island - switching back to home");
     * // They are currently on a coop island so switch out for their home
     * inventory
     * InventorySave.getInstance().savePlayerInventory(player,
     * onCoopIsland.get(player.getUniqueId()));
     * }
     * // Go through all the coop islands this player is a part of
     * for (Location coopIslands:
     * coopPlayers.get(player.getUniqueId()).keySet()) {
     * // Get the inviter if they are still online
     * Player onlineMember =
     * plugin.getServer().getPlayer(coopPlayers.get(player.
     * getUniqueId()).get(coopIslands));
     * if (onlineMember != null) {
     * // They are online, so return the junk
     * for (ItemStack i:
     * InventorySave.getInstance().getArmor(player.getUniqueId(),coopIslands)) {
     * if (i != null) {
     * plugin.getLogger().info("DEBUG: giving " + i.getType());
     * if (!i.getType().equals(Material.AIR)) {
     * HashMap<Integer,ItemStack> leftOver =
     * onlineMember.getInventory().addItem(i);
     * if (leftOver.size()> 0) {
     * for (ItemStack j: leftOver.values()) {
     * plugin.getLogger().info("DEBUG: throwing " + j.getType());
     * onlineMember.getWorld().dropItem(onlineMember.getLocation(), j);
     * }
     * }
     * }
     * }
     * }
     * for (ItemStack i:
     * InventorySave.getInstance().getInventory(player.getUniqueId
     * (),coopIslands)) {
     * if (i != null) {
     * plugin.getLogger().info("DEBUG: giving " + i.getType());
     * if (!i.getType().equals(Material.AIR)) {
     * HashMap<Integer,ItemStack> leftOver =
     * onlineMember.getInventory().addItem(i);
     * if (leftOver.size()> 0) {
     * for (ItemStack j: leftOver.values()) {
     * plugin.getLogger().info("DEBUG: throwing " + j.getType());
     * onlineMember.getWorld().dropItem(onlineMember.getLocation(), j);
     * }
     * }
     * }
     * }
     * }
     * onlineMember.sendMessage(ChatColor.GREEN +
     * "Returned items from coop player.");
     * }
     * }
     * // Now reset their inventory to their home inventory
     * if (plugin.getPlayers().inTeam(player.getUniqueId())) {
     * InventorySave.getInstance().loadPlayerInventory(player,
     * plugin.getPlayers().getTeamIslandLocation(player.getUniqueId()));
     * } else {
     * InventorySave.getInstance().loadPlayerInventory(player,
     * plugin.getPlayers().getIslandLocation(player.getUniqueId()));
     * }
     * // Now clear the player's inventories from this store
     * InventorySave.getInstance().removePlayer(player.getUniqueId());
     * onCoopIsland.remove(player.getUniqueId());
     * coopPlayers.remove(player.getUniqueId());
     * // That should do it! If there are not online then the items are lost.
     * }
     * public void saveAndClearInventory(Player player) {
     * // Save the player's inventory based on their island location and clear
     * it
     * UUID playerUUID = player.getUniqueId();
     * if (plugin.getPlayers().inTeam(playerUUID)) {
     * InventorySave.getInstance().savePlayerInventory(player,
     * plugin.getPlayers().getTeamIslandLocation(playerUUID));
     * } else {
     * InventorySave.getInstance().savePlayerInventory(player,
     * plugin.getPlayers().getIslandLocation(playerUUID));
     * }
     * player.getInventory().clear();
     * player.getInventory().setBoots(null);
     * player.getInventory().setChestplate(null);
     * player.getInventory().setHelmet(null);
     * player.getInventory().setLeggings(null);
     * }
     */
}