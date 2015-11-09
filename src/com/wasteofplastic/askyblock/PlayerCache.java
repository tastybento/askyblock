/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.wasteofplastic.askyblock;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;

/**
 * Provides a memory cache of online player information
 * This is the one-stop-shop of player info
 * If the player is not cached, then a request is made to Players to obtain it
 * 
 * @author tastybento
 */
public class PlayerCache {
    private HashMap<UUID, Players> playerCache = new HashMap<UUID, Players>();
    private final ASkyBlock plugin;

    public PlayerCache(ASkyBlock plugin) {
	this.plugin = plugin;
	// final Collection<? extends Player> serverPlayers =
	// Bukkit.getServer().getOnlinePlayers();
	for (Player p : getOnlinePlayers()) {
	    if (p.isOnline()) {
		final Players playerInf = new Players(plugin, p.getUniqueId());
		// Make sure parties are working correctly
		if (playerInf.inTeam() && playerInf.getTeamIslandLocation() == null) {
		    final Players leaderInf = new Players(plugin, playerInf.getTeamLeader());
		    playerInf.setTeamIslandLocation(leaderInf.getIslandLocation());
		    playerInf.save();
		}
		// Add this player to the online cache
		playerCache.put(p.getUniqueId(), playerInf);
	    }
	}
    }

    public static List<Player> getOnlinePlayers() {
	List<Player> list = Lists.newArrayList();
	for (World world : Bukkit.getWorlds()) {
	    list.addAll(world.getPlayers());
	}
	return Collections.unmodifiableList(list);
    }

    /*
     * Cache control methods
     */

    public void addPlayer(final UUID playerUUID) {
	// plugin.getLogger().info("DEBUG: added player");
	if (!playerCache.containsKey(playerUUID)) {
	    final Players player = new Players(plugin, playerUUID);
	    playerCache.put(playerUUID, player);
	}
    }

    /**
     * Stores the player's info to a file and removes the player from the list
     * of currently online players
     * 
     * @param player
     *            - name of player
     */
    public void removeOnlinePlayer(final UUID player) {
	if (playerCache.containsKey(player)) {
	    playerCache.get(player).save();
	    playerCache.remove(player);
	    // plugin.getLogger().info("Removing player from cache: " + player);
	}
    }

    /**
     * Removes all players on the server now from cache and saves their info
     */
    public void removeAllPlayers() {
	for (UUID pl : playerCache.keySet()) {
	    playerCache.get(pl).save();
	}
	playerCache.clear();
    }

    /*
     * Player info query methods
     */
    /**
     * Returns location of player's island from cache if available
     * 
     * @param playerUUID
     * @return Location of player's island
     */
    /*
     * public Location getPlayerIsland(final UUID playerUUID) {
     * if (playerCache.containsKey(playerUUID)) {
     * return playerCache.get(playerUUID).getIslandLocation();
     * }
     * final Players player = new Players(plugin, playerUUID);
     * return player.getIslandLocation();
     * }
     */
    /**
     * Checks if the player is known or not by looking through the filesystem
     * 
     * @param uniqueID
     * @return true if player is know, otherwise false
     */
    public boolean isAKnownPlayer(final UUID uniqueID) {
	if (uniqueID == null) {
	    return false;
	}
	if (playerCache.containsKey(uniqueID)) {
	    return true;
	} else {
	    // Get the file system
	    final File folder = plugin.getPlayersFolder();
	    final File[] files = folder.listFiles();
	    // Go through the native YAML files
	    for (final File f : files) {
		// Need to remove the .yml suffix
		if (f.getName().endsWith(".yml")) {
		    if (UUID.fromString(f.getName().substring(0, f.getName().length() - 4)).equals(uniqueID)) {
			return true;
		    }
		}
	    }
	}
	// Not found, sorry.
	return false;
    }

    /**
     * Returns the player object for the named player
     * 
     * @param playerUUID
     *            - String name of player
     * @return - player object
     */
    public Players get(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID);
    }

    /**
     * Checks if player has island from cache if available
     * 
     * @param playerUUID
     *            - string name of player
     * @return true if player has island
     */
    public boolean hasIsland(final UUID playerUUID) {
	addPlayer(playerUUID);
	// plugin.getLogger().info("DEBUG: hasIsland = " + playerUUID.toString()
	// + " = " + playerCache.get(playerUUID).hasIsland());
	return playerCache.get(playerUUID).hasIsland();
    }

    /**
     * Checks if player is in a Team from cache if available
     * 
     * @param playerUUID
     * @return
     */
    public boolean inTeam(final UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).inTeam();
    }

    /**
     * Removes any island associated with this player and generally cleans up
     * the player
     * 
     * @param playerUUID
     */
    public void zeroPlayerData(UUID playerUUID) {
	addPlayer(playerUUID);
	// Remove and clean up any team players (if the asadmin delete command
	// was called this is needed)
	if (playerCache.get(playerUUID).inTeam()) {
	    UUID leader = playerCache.get(playerUUID).getTeamLeader();
	    // If they are the leader, dissolve the team
	    if (leader != null) {
		if (leader.equals(playerUUID)) {
		    for (UUID member : playerCache.get(leader).getMembers()) {
			addPlayer(member);
			playerCache.get(member).setLeaveTeam();
		    }
		} else {
		    // Just remove them from the team
		    addPlayer(leader);
		    playerCache.get(leader).removeMember(playerUUID);
		    playerCache.get(leader).save();
		}
	    }
	}
	playerCache.get(playerUUID).setLeaveTeam();
	playerCache.get(playerUUID).setHasIsland(false);
	playerCache.get(playerUUID).clearHomeLocations();
	playerCache.get(playerUUID).setIslandLocation(null);
	playerCache.get(playerUUID).setIslandLevel(0);
	playerCache.get(playerUUID).save(); // Needed?
	TopTen.topTenRemoveEntry(playerUUID);
    }

    /**
     * Sets the home location for the player
     * @param playerUUID
     * @param location
     * @param number - 1 is default. Can be any number.
     */
    public void setHomeLocation(UUID playerUUID, Location location, int number) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHomeLocation(location,number);
    }

    /**
     * Set the default home location for player
     * @param playerUUID
     * @param location
     */
    public void setHomeLocation(UUID playerUUID, Location location) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHomeLocation(location,1);
    }

    /**
     * Clears any home locations for player
     * @param playerUUID
     */
    public void clearHomeLocations(UUID playerUUID) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).clearHomeLocations();
    }

    /**
     * Returns the home location, or null if none
     * 
     * @param playerUUID
     * @param number 
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID, int number) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getHomeLocation(number);
    }

    /**
     * Gets the default home location for player
     * @param playerUUID
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getHomeLocation(1);
    }

    /**
     * Provides all home locations for player
     * @param playerUUID
     * @return List of home locations
     */
    public HashMap<Integer, Location> getHomeLocations(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getHomeLocations();
    }

    /**
     * Returns the player's island location.
     * Returns an island location OR a team island location
     * 
     * @param playerUUID
     * @return
     */
    public Location getIslandLocation(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getIslandLocation();
    }

    public void setHasIsland(UUID playerUUID, boolean b) {
	// plugin.getLogger().info("DEBUG: setHasIsland " +
	// playerUUID.toString() + " " + b);
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHasIsland(b);
    }

    public void setIslandLocation(UUID playerUUID, Location islandLocation) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setIslandLocation(islandLocation);
    }

    public Integer getIslandLevel(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getIslandLevel();
    }

    public void setIslandLevel(UUID playerUUID, int islandLevel) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setIslandLevel(islandLevel);
	plugin.getChatListener().setPlayerLevel(playerUUID, islandLevel);
    }

    public void setTeamIslandLocation(UUID playerUUID, Location islandLocation) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setTeamIslandLocation(islandLocation);
    }

    /**
     * Checks if a challenge has been completed or not
     * 
     * @param playerUUID
     * @param challenge
     * @return
     */
    public boolean checkChallenge(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).checkChallenge(challenge);
    }

    /**
     * Checks how often a challenge has been completed
     * 
     * @param playerUUID
     * @param challenge
     * @return
     */
    public int checkChallengeTimes(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).checkChallengeTimes(challenge);
    }

    /**
     * Provides the status of all challenges for this player
     * 
     * @param playerUUID
     * @return
     */
    public HashMap<String, Boolean> getChallengeStatus(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getChallengeStatus();
    }

    /**
     * How many times a challenge has been completed
     * 
     * @param playerUUID
     * @return map of completion times
     */
    public HashMap<String, Integer> getChallengeTimes(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getChallengeCompleteTimes();
    }

    public void resetChallenge(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).resetChallenge(challenge);
    }

    /**
     * Resets all the player's challenges. If the boolean is true, then everything will be reset, if false
     * challenges that have the "resetallowed: false" flag in challenges.yml will not be reset
     * @param playerUUID
     * @param resetAll
     */
    public void resetAllChallenges(UUID playerUUID, boolean resetAll) {
	addPlayer(playerUUID);
	if (resetAll) {
	    playerCache.get(playerUUID).resetAllChallenges();
	} else {
	    // Look through challenges and check them
	    for (String challenge: plugin.getChallenges().getAllChallenges()) {
		// Check for the flag
		if (plugin.getChallenges().resetable(challenge)) {
		    playerCache.get(playerUUID).resetChallenge(challenge);
		}
	    }
	}
    }

    public void setJoinTeam(UUID playerUUID, UUID teamLeader, Location islandLocation) {
	addPlayer(playerUUID);
	addPlayer(teamLeader);
	playerCache.get(playerUUID).setJoinTeam(teamLeader, islandLocation);
    }

    /**
     * Called when a player leaves a team Resets inTeam, teamLeader,
     * islandLevel, teamIslandLocation, islandLocation and members array
     */
    public void setLeaveTeam(UUID playerUUID) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setLeaveTeam();
    }

    /**
     * Returns a list of team member UUID's. If the player is not the leader,
     * then the leader's list is used
     * 
     * @param playerUUID
     * @return
     */
    public List<UUID> getMembers(UUID playerUUID) {
	addPlayer(playerUUID);
	UUID leader = getTeamLeader(playerUUID);
	if (leader != null && !leader.equals(playerUUID)) {
	    addPlayer(leader);
	    return playerCache.get(leader).getMembers();
	}
	// I am not the leader, so return the leader's list
	return playerCache.get(playerUUID).getMembers();
    }

    public void addTeamMember(UUID teamLeader, UUID playerUUID) {
	addPlayer(teamLeader);
	addPlayer(playerUUID);
	playerCache.get(teamLeader).addTeamMember(playerUUID);
    }

    public void removeMember(UUID teamLeader, UUID playerUUID) {
	addPlayer(teamLeader);
	addPlayer(playerUUID);
	playerCache.get(teamLeader).removeMember(playerUUID);
	// Remove from team chat too
	plugin.getChatListener().unSetPlayer(playerUUID);
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @param playerUUID
     * @return
     */
    public UUID getTeamLeader(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getTeamLeader();
    }

    /**
     * Saves the player's info to the file system
     * 
     * @param playerUUID
     */
    public void save(UUID playerUUID) {
	playerCache.get(playerUUID).save();
	// Save the name + UUID in the database if it ready
	if (plugin.getTinyDB() != null && plugin.getTinyDB().isDbReady()) {
	    plugin.getTinyDB().savePlayerName(playerCache.get(playerUUID).getPlayerName(), playerUUID);
	}
    }

    public void completeChallenge(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).completeChallenge(challenge);
    }

    public boolean challengeExists(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).challengeExists(challenge);
    }

    /**
     * Attempts to return a UUID for a given player's name
     * 
     * @param string
     * @return
     */
    public UUID getUUID(String string) {
	for (UUID id : playerCache.keySet()) {
	    String name = playerCache.get(id).getPlayerName();
	    // plugin.getLogger().info("DEBUG: Testing name " + name);
	    if (name != null && name.equalsIgnoreCase(string)) {
		return id;
	    }
	}
	// Look in the database if it ready
	if (plugin.getTinyDB() != null && plugin.getTinyDB().isDbReady()) {
	    return plugin.getTinyDB().getPlayerUUID(string);
	}
	return null;
    }

    /**
     * Sets the player's name and updates the name>UUID database is up to date
     * @param uniqueId
     * @param name
     */
    public void setPlayerName(UUID uniqueId, String name) {
	addPlayer(uniqueId);
	playerCache.get(uniqueId).setPlayerN(name);
	// Save the name in the name database. Note that the old name will still work until someone takes it
	// This feature enables admins to locate 'fugitive' players even if they change their name
	if (plugin.getTinyDB() != null && plugin.getTinyDB().isDbReady()) {
	    plugin.getTinyDB().savePlayerName(name, uniqueId);
	}
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     * 
     * @param playerUUID
     * @return String - playerName
     */
    public String getName(UUID playerUUID) {
	if (playerUUID == null) {
	    return "";
	}
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getPlayerName();
    }

    public Location getTeamIslandLocation(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getTeamIslandLocation();
    }

    /**
     * Reverse lookup - returns the owner of an island from the location
     * 
     * @param loc
     * @return
     */
    public UUID getPlayerFromIslandLocation(Location loc) {
	if (loc == null)
	    return null;
	// Look in the grid
	Island island = plugin.getGrid().getIslandAt(loc);
	if (island != null) {
	    return island.getOwner();
	}
	// Check the cache
	/*
	 * for (UUID uuid: playerCache.keySet()) {
	 * // Check for block equiv
	 * Location check = playerCache.get(uuid).getIslandLocation();
	 * if (check != null) {
	 * if (check.getBlockX() == loc.getBlockX()
	 * && check.getBlockZ() == loc.getBlockZ()) {
	 * return uuid;
	 * }
	 * }
	 * }
	 */
	/*
	 * // Look in the file system
	 * for (final File f : plugin.getPlayersFolder().listFiles()) {
	 * // Need to remove the .yml suffix
	 * String fileName = f.getName();
	 * if (fileName.endsWith(".yml")) {
	 * try {
	 * UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() -
	 * 4));
	 * addPlayer(uuid);
	 * Location check = playerCache.get(uuid).getIslandLocation();
	 * if (check != null) {
	 * //plugin.getLogger().info("DEBUG: checking " + check.toString());
	 * if (check.getBlockX() == loc.getBlockX()
	 * && check.getBlockZ() == loc.getBlockZ()) {
	 * // Add to the grid
	 * if (island == null) {
	 * plugin.getGrid().addIsland(loc.getBlockX(), loc.getBlockZ(), uuid);
	 * } else if (island != null) {
	 * island.setOwner(uuid);
	 * }
	 * return uuid;
	 * }
	 * }
	 * } catch (Exception e) {
	 * }
	 * }
	 * }
	 */

	return null;
    }

    /**
     * Gets how many island resets the player has left
     * 
     * @param playerUUID
     * @return
     */
    public int getResetsLeft(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getResetsLeft();
    }

    /**
     * Sets how many resets the player has left
     * 
     * @param playerUUID
     * @param resets
     */
    public void setResetsLeft(UUID playerUUID, int resets) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setResetsLeft(resets);
    }

    /**
     * Returns how long the player must wait before they can be invited to an
     * island with the location
     * 
     * @param playerUUID
     * @param location
     * @return time to wait in minutes/hours
     */
    public long getInviteCoolDownTime(UUID playerUUID, Location location) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getInviteCoolDownTime(location);
    }

    /**
     * Starts the timer for the player for this location before which they can
     * be invited
     * Called when they are kicked from an island or leave.
     * 
     * @param playerUUID
     * @param location
     */
    public void startInviteCoolDownTimer(UUID playerUUID, Location location) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).startInviteCoolDownTimer(location);
    }

    /**
     * Returns the locale for this player. If missing, will return nothing
     * @param playerUUID
     * @return name of the locale this player uses
     */
    public String getLocale(UUID playerUUID) {
	addPlayer(playerUUID);
	if (playerUUID == null) {
	    return "";
	}
	return playerCache.get(playerUUID).getLocale();
    }

    /**
     * Sets the locale this player wants to use
     * @param playerUUID
     * @param localeName
     */
    public void setLocale(UUID playerUUID, String localeName) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setLocale(localeName);
    }

    /**
     * The rating of the initial starter island out of 100. Default is 50
     * @param playerUUID
     * @return rating
     */
    public int getStartIslandRating(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getStartIslandRating();
    }

    /**
     * Record the island rating that the player started with
     * @param playerUUID
     * @param rating
     */
    public void setStartIslandRating(UUID playerUUID, int rating) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setStartIslandRating(rating);
    }

    /**
     * Clear the starter island rating from the player's record
     * @param playerUUID
     */
    public void clearStartIslandRating(UUID playerUUID) {
	setStartIslandRating(playerUUID, 0);
    }

    /**
     * Ban target from a player's island
     * @param playerUUID
     * @param targetUUID
     */
    public void ban(UUID playerUUID, UUID targetUUID) {
	addPlayer(playerUUID);
	addPlayer(targetUUID);
	if (playerCache.get(playerUUID).hasIsland()) {
	    // Player has island
	    playerCache.get(playerUUID).addToBanList(targetUUID);
	} else if (playerCache.get(playerUUID).inTeam()) {
	    // Try to get the leader's 
	    UUID leader = playerCache.get(playerUUID).getTeamLeader();
	    if (leader != null) {
		addPlayer(leader);
		playerCache.get(leader).addToBanList(targetUUID);
		playerCache.get(leader).save();
	    }
	}
    }

    /**
     * Unban target from player's island
     * @param playerUUID
     * @param targetUUID
     */
    public void unBan(UUID playerUUID, UUID targetUUID) {
	addPlayer(playerUUID);
	addPlayer(targetUUID);
	if (playerCache.get(playerUUID).hasIsland()) {
	    // Player has island
	    playerCache.get(playerUUID).unBan(targetUUID);
	} else if (playerCache.get(playerUUID).inTeam()) {
	    // Try to get the leader's 
	    UUID leader = playerCache.get(playerUUID).getTeamLeader();
	    if (leader != null) {
		addPlayer(leader);
		playerCache.get(leader).unBan(targetUUID);
		playerCache.get(leader).save();
	    }
	}
    }

    /**
     * @param playerUUID
     * @param targetUUID
     * @return true if target is banned from player's island
     */
    public boolean isBanned(UUID playerUUID, UUID targetUUID) {
	if (playerUUID == null || targetUUID == null) {
	    // If the island is unowned, then playerUUID could be null
	    return false;
	}
	addPlayer(playerUUID);
	addPlayer(targetUUID);
	if (playerCache.get(playerUUID).hasIsland()) {
	    // Player has island
	    return playerCache.get(playerUUID).isBanned(targetUUID);
	} else if (playerCache.get(playerUUID).inTeam()) {
	    // Try to get the leader's 
	    UUID leader = playerCache.get(playerUUID).getTeamLeader();
	    if (leader != null) {
		addPlayer(leader);
		return playerCache.get(leader).isBanned(targetUUID);
	    }
	}
	return false;
    }

    /**
     * @param playerUUID
     * @return ban list for player
     */
    public List<UUID> getBanList(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getBanList();
    }

    /**
     * Clears resets for online players or players in the cache
     * @param resetLimit
     */
    public void clearResets(int resetLimit) {
	for (Players player : playerCache.values()) {
	    player.setResetsLeft(resetLimit);
	}	
    }

    /**
     * Sets whether the player uses the control panel or not when doing /island
     * @param b
     */
    public void setControlPanel(UUID playerUUID, boolean b) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setControlPanel(b);

    }

    /**
     * Sets whether the player uses the control panel or not when doing /island
     * @param b
     */
    public boolean getControlPanel(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getControlPanel();

    }
}
