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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.wasteofplastic.askyblock.util.VaultHelper;

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
    private Set<UUID> inTeleport = new HashSet<UUID>();

    public PlayerCache(ASkyBlock plugin) {
        this.plugin = plugin;
        // final Collection<? extends Player> serverPlayers =
        // Bukkit.getServer().getOnlinePlayers();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getUniqueId() != null) {
                try {
                    final Players playerInf = new Players(plugin, p.getUniqueId());
                    // Make sure parties are working correctly
                    if (playerInf.inTeam() && playerInf.getTeamIslandLocation() == null) {
                        if (playerInf.getTeamLeader() == null) {
                            // Player cannot be in a team - try to clean up
                            playerInf.setLeaveTeam();
                        } else {
                            final Players leaderInf = new Players(plugin, playerInf.getTeamLeader());
                            playerInf.setTeamIslandLocation(leaderInf.getIslandLocation());
                        }
                        playerInf.save();
                    }
                    // Add this player to the online cache
                    //plugin.getLogger().info("DEBUG: added player " + p.getUniqueId());
                    playerCache.put(p.getUniqueId(), playerInf);
                } catch (Exception e) {
                    plugin.getLogger().severe("Player add tried for a player with null UUID");
                }
            }
        }
    }

    /**
     * @return list of all online cached players
     */
    public List<UUID> getOnlineCachedPlayers() {
        List<UUID> list = Lists.newArrayList();
        for (Player p: plugin.getServer().getOnlinePlayers()) {
            if (playerCache.containsKey(p.getUniqueId())) {
                list.add(p.getUniqueId());
            }
        }
        return Collections.unmodifiableList(list);
    }

    /*
     * Cache control methods
     */

    public void addPlayer(final UUID playerUUID) {
        //plugin.getLogger().info("DEBUG: added player " + playerUUID);
        if (!playerCache.containsKey(playerUUID)) {
            try {
                final Players player = new Players(plugin, playerUUID);
                playerCache.put(playerUUID, player);
            } catch (Exception e) {
                // Be silent.
                //plugin.getLogger().severe("Player add request for a null UUID");
            }
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
        Map<UUID, Players> map = Collections.unmodifiableMap(playerCache);
        for (UUID pl : map.keySet()) {
            map.get(pl).save();
        }
        playerCache.clear();
    }

    /*
     * Player info query methods
     */
    /**
     * Returns location of player's island from cache if available
     * 
     * @param playerUUID - the player's UUID
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
     * @param uniqueId - unique ID
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
            try {
                final File file = new File(plugin.getPlayersFolder(), uniqueID.toString() + ".yml");
                return file.exists();
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Returns the player object for the named player
     * 
     * @param playerUUID - the player's UUID
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
     * @param playerUUID - the player's UUID
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
     * @param playerUUID - the player's UUID
     * @return true if player in a team
     */
    public boolean inTeam(final UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).inTeam();
    }

    /**
     * Removes any island associated with this player and generally cleans up
     * the player
     * 
     * @param playerUUID - the player's UUID
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
        plugin.getTopTen().topTenRemoveEntry(playerUUID);
    }

    /**
     * Sets the home location for the player
     * @param playerUUID - the player's UUID
     * @param location
     * @param number - 1 is default. Can be any number.
     */
    public void setHomeLocation(UUID playerUUID, Location location, int number) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,number);
    }

    /**
     * Set the default home location for player
     * @param playerUUID - the player's UUID
     * @param location
     */
    public void setHomeLocation(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,1);
    }

    /**
     * Clears any home locations for player
     * @param playerUUID - the player's UUID
     */
    public void clearHomeLocations(UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).clearHomeLocations();
    }

    /**
     * Returns the home location, or null if none
     * 
     * @param playerUUID - the player's UUID
     * @param number 
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID, int number) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(number);
    }

    /**
     * Gets the default home location for player
     * @param playerUUID - the player's UUID
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(1);
    }

    /**
     * Provides all home locations for player
     * @param playerUUID - the player's UUID
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
     * @param playerUUID - the player's UUID
     * @return Location of player's island
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

    public long getIslandLevel(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getIslandLevel();
    }

    public void setIslandLevel(UUID playerUUID, long l) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setIslandLevel(l);
        plugin.getChatListener().setPlayerLevel(playerUUID, l);
    }

    public void setTeamIslandLocation(UUID playerUUID, Location islandLocation) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setTeamIslandLocation(islandLocation);
    }

    /**
     * Checks if a challenge has been completed or not
     * 
     * @param playerUUID - the player's UUID
     * @param challenge
     * @return true if complete
     */
    public boolean checkChallenge(UUID playerUUID, String challenge) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).checkChallenge(challenge);
    }

    /**
     * Checks how often a challenge has been completed
     * 
     * @param playerUUID - the player's UUID
     * @param challenge
     * @return number of times
     */
    public int checkChallengeTimes(UUID playerUUID, String challenge) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).checkChallengeTimes(challenge);
    }

    /**
     * Provides the status of all challenges for this player
     * 
     * @param playerUUID - the player's UUID
     * @return Hashmap of challenges as key, boolean as state
     */
    public HashMap<String, Boolean> getChallengeStatus(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getChallengeStatus();
    }

    /**
     * How many times a challenge has been completed
     * 
     * @param playerUUID - the player's UUID
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
     * @param playerUUID - the player's UUID
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

    /**
     * Puts a player in a team
     * @param playerUUID - the player's UUID
     * @param teamLeader
     * @param islandLocation
     * @return true if successful, false if not
     */
    public boolean setJoinTeam(UUID playerUUID, UUID teamLeader, Location islandLocation) {
        addPlayer(playerUUID);
        addPlayer(teamLeader);
        return playerCache.get(playerUUID).setJoinTeam(teamLeader, islandLocation);
    }

    /**
     * Called when a player leaves a team Resets inTeam, teamLeader,
     * islandLevel, teamIslandLocation, islandLocation and members array
     * @param playerUUID - the player's UUID - player's UUID
     * @return - true if successful
     */
    public boolean setLeaveTeam(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).setLeaveTeam();
    }

    /**
     * Returns a list of team member UUID's. If the player is not the leader,
     * then the leader's list is used
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return List of team UUIDs
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
        if (teamLeader != null) {
            addPlayer(teamLeader);
        }
        addPlayer(playerUUID);
        if (teamLeader != null) {
            playerCache.get(teamLeader).removeMember(playerUUID);
        }
        // Remove from team chat too
        plugin.getChatListener().unSetPlayer(playerUUID);
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @param playerUUID - the player's UUID - player's UUID
     * @return UUID of leader
     */
    public UUID getTeamLeader(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getTeamLeader();
    }

    /**
     * Saves the player's info to the file system
     * 
     * @param playerUUID - the player's UUID - player's UUID
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
     * Attempts to return a UUID for a given player's name. Only uses online or cached information.
     * @param string - player's name
     * @return UUID of player or null if unknown
     */
    public UUID getUUID(String string) {
        return getUUID(string, false);
    }

    /**
     * Attempts to return a UUID for a given player's name
     * @param string - player's name
     * @param adminCheck - if made via an admin call, this will go out to the 'net and grab - may cause lag
     * @return UUID of player or null if unknown
     */
    @SuppressWarnings("deprecation")
    public UUID getUUID(String string, boolean adminCheck) {
        // Look in the database if it ready
        if (plugin.getTinyDB() != null && plugin.getTinyDB().isDbReady()) {
            UUID result = plugin.getTinyDB().getPlayerUUID(string);
            if (result != null) {
                return result;
            }
        }
        // This goes after the database because it is possible for islands that have a duplicate name to be in
        // the cache. For example, Bill had an island but left. Bill changes his name to Bob. Then Alice changes
        // her name to Bill and logs into the game. There are now two islands with owner names called "Bill"
        // The name database will ensure the names are updated.
        for (UUID id : playerCache.keySet()) {
            String name = playerCache.get(id).getPlayerName();
            //plugin.getLogger().info("DEBUG: Testing name " + name);
            if (name != null && name.equalsIgnoreCase(string)) {
                //plugin.getLogger().info("DEBUG: found it! " + id);
                return id;
            }
        }
        // Try the server
        if (adminCheck && plugin.getServer().getOfflinePlayer(string) != null) {
            return plugin.getServer().getOfflinePlayer(string).getUniqueId();
        }
        return null;
    }

    /**
     * Sets the player's name and updates the name to UUID database so it is up to date
     * @param uniqueId - unique ID  - player's UUID
     * @param name - name to set
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
     * @param playerUUID - the player's UUID - player's UUID
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
     * @param loc - location to query
     * @return UUID of owner of island
     */
    public UUID getPlayerFromIslandLocation(Location loc) {
        if (loc == null)
            return null;
        // Look in the grid
        Island island = plugin.getGrid().getIslandAt(loc);
        if (island != null) {
            return island.getOwner();
        }
        return null;
    }

    /**
     * Gets how many island resets the player has left
     * 
     * @param playerUUID - the player's UUID  - player's UUID
     * @return number of resets
     */
    public int getResetsLeft(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getResetsLeft();
    }

    /**
     * Sets how many resets the player has left
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @param resets - value to set
     */
    public void setResetsLeft(UUID playerUUID, int resets) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setResetsLeft(resets);
    }

    /**
     * Returns how long the player must wait before they can be invited to an
     * island with the location
     * 
     * @param playerUUID - the player's UUID  - player's UUID
     * @param location - location to query
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
     * @param playerUUID - the player's UUID  - player's UUID
     * @param location - location to set
     */
    public void startInviteCoolDownTimer(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).startInviteCoolDownTimer(location);
    }

    /**
     * Returns the locale for this player. If missing, will return nothing
     * @param playerUUID - the player's UUID  - player's UUID
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
     * @param playerUUID - the player's UUID - player's UUID
     * @param localeName - locale to set
     */
    public void setLocale(UUID playerUUID, String localeName) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setLocale(localeName);
    }

    /**
     * The rating of the initial starter island out of 100. Default is 50
     * @param playerUUID - the player's UUID  - player's UUID
     * @return rating the rating
     */
    public int getStartIslandRating(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getStartIslandRating();
    }

    /**
     * Record the island rating that the player started with
     * @param playerUUID - the player's UUID - player's UUID
     * @param rating the rating
     */
    public void setStartIslandRating(UUID playerUUID, int rating) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setStartIslandRating(rating);
    }

    /**
     * Clear the starter island rating from the player's record
     * @param playerUUID - the player's UUID  - player's UUID
     */
    public void clearStartIslandRating(UUID playerUUID) {
        setStartIslandRating(playerUUID, 0);
    }

    /**
     * Ban target from a player's island
     * @param playerUUID - the player's UUID  - player's UUID
     * @param targetUUID - target's UUID
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
     * @param playerUUID - the player's UUID - player's UUID
     * @param targetUUID - target's UUID
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
     * @param playerUUID - the player's UUID - player's UUID
     * @param targetUUID - target's UUID
     * @return true if target is banned from player's island
     */
    public boolean isBanned(UUID playerUUID, UUID targetUUID) {
        if (playerUUID == null || targetUUID == null) {
            // If the island is unowned, then playerUUID could be null
            return false;
        }
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        // Check if the target player has a permission bypass (admin.noban)
        Player target = plugin.getServer().getPlayer(targetUUID);
        if (target != null && VaultHelper.checkPerm(target, Settings.PERMPREFIX + "admin.noban")) {
            return false;
        }
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
     * @param playerUUID - the player's UUID - player's UUID
     * @return ban list for player
     */
    public List<UUID> getBanList(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getBanList();
    }

    /**
     * Clears resets for online players or players in the cache
     * @param resetLimit - reset limit
     */
    public void clearResets(int resetLimit) {
        for (Players player : playerCache.values()) {
            player.setResetsLeft(resetLimit);
        }	
    }

    /**
     * Sets whether the player uses the control panel or not when doing /island
     * @param playerUUID - the player's UUID - player's UUID
     * @param b - true = user control panel
     */
    public void setControlPanel(UUID playerUUID, boolean b) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setControlPanel(b);

    }

    /**
     * Gets whether the player uses the control panel or not when doing /island
     * @param playerUUID - the player's UUID  - player's UUID
     * @return true if they use the control panel
     */
    public boolean getControlPanel(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getControlPanel();

    }

    /**
     * Map storing whether a player is officially teleporting or not
     * @param uniqueId - unique ID  - player's UUID
     * @param b - true if player is teleporting
     */
    public void setInTeleport(UUID uniqueId, boolean b) {
        if (b) {
            inTeleport.add(uniqueId);
        } else {
            inTeleport.remove(uniqueId);
        }	
    }

    /**
     * Checks whether player with UUID uniqueId is in an official teleport or not
     * @param uniqueId - unique ID  - player's UUID
     * @return true if in teleport
     */
    public boolean isInTeleport(UUID uniqueId) {
        return inTeleport.contains(uniqueId);
    }

    /**
     * Add death to player
     * @param playerUUID - the player's UUID  - player's UUID
     */
    public void addDeath(UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).addDeath();
    }

    /**
     * Set death number for player
     * @param playerUUID - the player's UUID - player's UUID
     * @param deaths - number of deaths
     */
    public void setDeaths(UUID playerUUID, int deaths) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setDeaths(deaths);
    }

    /**
     * Get number of times player has died in ASkyBlock worlds since counting began
     * @param playerUUID - the player's UUID - player's UUID
     * @return number of deaths
     */
    public int getDeaths(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getDeaths();
    }

    /**
     * @param playerUUID - the player's UUID - player's UUID
     * @return List of challenges or levels done
     */
    public List<String> getChallengesDone(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getChallengesDone();
    }

    /**
     * @param playerUUID - the player's UUID - player's UUID
     * @return List of challenges or levels not done
     */
    public List<String> getChallengesNotDone(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getChallengesNotDone();
    }
}
