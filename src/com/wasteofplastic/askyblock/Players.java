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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.util.Util;

/**
 * Tracks the following info on the player
 * 
 * @author tastybento
 */
public class Players {
    private ASkyBlock plugin;
    private YamlConfiguration playerInfo;
    private HashMap<String, Boolean> challengeList;
    private HashMap<String, Integer> challengeListTimes;
    private boolean hasIsland;
    private boolean inTeam;
    //private String homeLocation;
    private HashMap<Integer, Location> homeLocations;
    private int islandLevel;
    private String islandLocation;
    private List<UUID> members;
    private String teamIslandLocation;
    private UUID teamLeader;
    private UUID uuid;
    private String playerName;
    private int resetsLeft;
    private HashMap<Location, Date> kickedList;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final ASkyBlock aSkyBlock, final UUID uuid) {
	this.plugin = aSkyBlock;
	this.uuid = uuid;
	this.members = new ArrayList<UUID>();
	this.hasIsland = false;
	this.islandLocation = null;
	//this.homeLocation = null;
	this.homeLocations = new HashMap<Integer,Location>();
	this.inTeam = false;
	this.teamLeader = null;
	this.teamIslandLocation = null;
	this.challengeList = new HashMap<String, Boolean>();
	this.challengeListTimes = new HashMap<String, Integer>();
	this.islandLevel = 0;
	this.playerName = "";
	this.resetsLeft = Settings.resetLimit;
	this.kickedList = new HashMap<Location, Date>();
	load(uuid);
    }

    /**
     * Loads a player from file system and if they do not exist, then it is
     * created
     * 
     * @param uuid
     */
    public void load(UUID uuid) {
	playerInfo = Util.loadYamlFile("players/" + uuid.toString() + ".yml");
	// Load in from YAML file
	this.playerName = playerInfo.getString("playerName", "");
	if (playerName.isEmpty()) {
	    try {
		playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
		playerName = "";
	    }
	    if (playerName == null) {
		plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
		playerName = "";
	    }
	}
	// plugin.getLogger().info("Loading player..." + playerName);
	this.hasIsland = playerInfo.getBoolean("hasIsland", false);
	// plugin.getLogger().info("DEBUG: hasIsland load = " + this.hasIsland);
	this.islandLocation = playerInfo.getString("islandLocation", "");
	// Old home location storage
	Location homeLocation = Util.getLocationString(playerInfo.getString("homeLocation",""));
	// New home location storage
	if (homeLocation != null) {
	    // Transfer the old into the new
	    this.homeLocations.put(1,homeLocation);
	} else {
	    // Import
	    if (playerInfo.contains("homeLocations")) {
		// Import to hashmap
		for (String number : playerInfo.getConfigurationSection("homeLocations").getValues(false).keySet()) {
		    try {
			int num = Integer.valueOf(number);
			Location loc = Util.getLocationString(playerInfo.getString("homeLocations." + number));
			homeLocations.put(num, loc);
		    } catch (Exception e) {
			plugin.getLogger().warning("Error importing home locations for " + playerName);
		    }
		}
	    }
	}
	this.inTeam = playerInfo.getBoolean("hasTeam", false);
	final String teamLeaderString = playerInfo.getString("teamLeader", "");
	if (!teamLeaderString.isEmpty()) {
	    this.teamLeader = UUID.fromString(teamLeaderString);
	} else {
	    this.teamLeader = null;
	}
	this.teamIslandLocation = playerInfo.getString("teamIslandLocation", "");
	this.islandLevel = playerInfo.getInt("islandLevel", 0);
	List<String> temp = playerInfo.getStringList("members");
	for (String s : temp) {
	    this.members.add(UUID.fromString(s));
	}
	// Challenges
	// Run through all challenges available
	for (String challenge : Settings.challengeList) {
	    // If they are in the list, then use the value, otherwise use false
	    challengeList.put(challenge, playerInfo.getBoolean("challenges.status." + challenge, false));
	    challengeListTimes.put(challenge, playerInfo.getInt("challenges.times." + challenge, 0));
	}
	// Load reset limit
	this.resetsLeft = playerInfo.getInt("resetsLeft", Settings.resetLimit);
	// Check what the global limit is and raise it if it was changed
	if (Settings.resetLimit > 0 && this.resetsLeft == -1) {
	    resetsLeft = Settings.resetLimit;
	}
	// Load the invite cool downs
	if (playerInfo.contains("invitecooldown")) {
	    // plugin.getLogger().info("DEBUG: cooldown found");
	    for (String timeIndex : playerInfo.getConfigurationSection("invitecooldown").getKeys(false)) {
		try {
		    // plugin.getLogger().info("DEBUG: index is " + timeIndex);
		    String locationString = playerInfo.getString("invitecooldown." + timeIndex, "");
		    // plugin.getLogger().info("DEBUG: location string is " +
		    // locationString);
		    Location l = Util.getLocationString(locationString);
		    // plugin.getLogger().info("DEBUG: location is " + l);
		    long timeInMillis = Long.valueOf(timeIndex);
		    // plugin.getLogger().info("DEBUG: time in millis is " +
		    // timeInMillis);
		    if (l != null && timeInMillis > 0) {
			Date date = new Date();
			date.setTime(timeInMillis);
			// plugin.getLogger().info("DEBUG: date is " + date);
			// Insert into hashmap
			kickedList.put(l, date);
		    }
		} catch (Exception e) {
		    plugin.getLogger().severe("Error in player " + playerName + "'s yml config when loading invite timeout - skipping");
		}
	    }
	}
    }

    /**
     * Saves the player info to the file system
     */
    public void save() {
	// plugin.getLogger().info("Saving player..." + playerName);
	// Save the variables
	playerInfo.set("playerName", playerName);
	playerInfo.set("hasIsland", hasIsland);
	playerInfo.set("islandLocation", islandLocation);
	playerInfo.set("homeLocation", null);
	// Only store the new way
	for (int num : homeLocations.keySet()) {
	    playerInfo.set("homeLocations." + num, Util.getStringLocation(homeLocations.get(num)));
	}
	playerInfo.set("hasTeam", inTeam);
	if (teamLeader == null) {
	    playerInfo.set("teamLeader", "");
	} else {
	    playerInfo.set("teamLeader", teamLeader.toString());
	}
	playerInfo.set("teamIslandLocation", teamIslandLocation);
	playerInfo.set("islandLevel", islandLevel);
	// Serialize UUIDs
	List<String> temp = new ArrayList<String>();
	for (UUID m : members) {
	    temp.add(m.toString());
	}
	playerInfo.set("members", temp);
	// Save the challenges
	for (String challenge : challengeList.keySet()) {
	    playerInfo.set("challenges.status." + challenge, challengeList.get(challenge));
	}
	for (String challenge : challengeListTimes.keySet()) {
	    playerInfo.set("challenges.times." + challenge, challengeListTimes.get(challenge));
	}
	// Check what the global limit is
	if (Settings.resetLimit < this.resetsLeft) {
	    this.resetsLeft = Settings.resetLimit;
	}
	playerInfo.set("resetsLeft", this.resetsLeft);
	// Save invite cooldown timers
	playerInfo.set("invitecooldown", null);
	for (Entry<Location, Date> en : kickedList.entrySet()) {
	    // Convert location and date to string (time in millis)
	    Calendar coolDownTime = Calendar.getInstance();
	    coolDownTime.setTime(en.getValue());
	    playerInfo.set("invitecooldown." + coolDownTime.getTimeInMillis(), Util.getStringLocation(en.getKey()));
	}

	Util.saveYamlFile(playerInfo, "players/" + uuid.toString() + ".yml");

    }

    /**
     * @param member
     *            Adds a member to the the player's list
     */
    public void addTeamMember(final UUID member) {
	members.add(member);
    }

    /**
     * A maintenance function. Rebuilds the challenge list for this player.
     * Should be used when the challenges change, e.g. config.yml changes.
     */
    public void updateChallengeList() {
	// If it does not exist, then make it
	if (challengeList == null) {
	    challengeList = new HashMap<String, Boolean>();
	}
	// Iterate through all the challenges in the config.yml and if they are
	// not in the list the add them as yet to be done
	final Iterator<?> itr = Settings.challengeList.iterator();
	while (itr.hasNext()) {
	    final String current = (String) itr.next();
	    if (!challengeList.containsKey(current.toLowerCase())) {
		challengeList.put(current.toLowerCase(), Boolean.valueOf(false));
	    }
	}
	// If the challenge list is bigger than the number of challenges in the
	// config.yml (some were removed?)
	// then remove the old ones - the ones that are no longer in Settings
	if (challengeList.size() > Settings.challengeList.size()) {
	    final Object[] challengeArray = challengeList.keySet().toArray();
	    for (int i = 0; i < challengeArray.length; i++) {
		if (!Settings.challengeList.contains(challengeArray[i].toString())) {
		    challengeList.remove(challengeArray[i].toString());
		}
	    }
	}
    }

    /**
     * Checks if a challenge exists in the player's challenge list
     * 
     * @param challenge
     * @return true if challenge is listed in the player's challenge list,
     *         otherwise false
     */
    public boolean challengeExists(final String challenge) {
	if (challengeList.containsKey(challenge.toLowerCase())) {
	    return true;
	}
	// for (String s : challengeList.keySet()) {
	// ASkyBlock.getInstance().getLogger().info("DEBUG: challenge list: " +
	// s);
	// }
	return false;
    }

    /**
     * Checks if a challenge is recorded as completed in the player's challenge
     * list or not
     * 
     * @param challenge
     * @return true if the challenge is listed as complete, false if not
     */
    public boolean checkChallenge(final String challenge) {
	if (challengeList.containsKey(challenge.toLowerCase())) {
	    // plugin.getLogger().info("DEBUG: " + challenge + ":" +
	    // challengeList.get(challenge.toLowerCase()).booleanValue() );
	    return challengeList.get(challenge.toLowerCase()).booleanValue();
	}
	return false;
    }

    /**
     * Checks how many times a challenge has been done
     * 
     * @param challenge
     * @return
     */
    public int checkChallengeTimes(final String challenge) {
	if (challengeListTimes.containsKey(challenge.toLowerCase())) {
	    // plugin.getLogger().info("DEBUG: check " + challenge + ":" +
	    // challengeListTimes.get(challenge.toLowerCase()).intValue() );
	    return challengeListTimes.get(challenge.toLowerCase()).intValue();
	}
	return 0;
    }

    public HashMap<String, Boolean> getChallengeStatus() {
	return challengeList;
    }

    /**
     * Records the challenge as being complete in the player's list If the
     * challenge is not listed in the player's challenge list already, then it
     * will not be recorded! TODO: Possible systemic bug here as a result
     * 
     * @param challenge
     */
    public void completeChallenge(final String challenge) {
	// plugin.getLogger().info("DEBUG: Complete challenge");
	if (challengeList.containsKey(challenge)) {
	    challengeList.remove(challenge);
	    challengeList.put(challenge, Boolean.valueOf(true));
	    // Count how many times the challenge has been done
	    int times = 0;
	    if (challengeListTimes.containsKey(challenge)) {
		times = challengeListTimes.get(challenge);
	    }
	    times++;
	    challengeListTimes.put(challenge, times);
	    // plugin.getLogger().info("DEBUG: complete " + challenge + ":" +
	    // challengeListTimes.get(challenge.toLowerCase()).intValue() );
	}
    }

    public boolean hasIsland() {
	// Check if the player really has an island
	if (hasIsland && islandLocation.isEmpty()) {
	    hasIsland = false;
	    plugin.getLogger().warning(playerName + " apparently had an island, but the location is unknown.");
	}
	return hasIsland;
    }

    /**
     * 
     * @return boolean - true if player is in a team
     */
    public boolean inTeam() {
	// Check if this player really has a team island
	if (inTeam && teamIslandLocation.isEmpty()) {
	    // Something odd is going on
	    // See if the player has a team leader
	    if (teamLeader == null) {
		// No, so just clear everything
		inTeam = false;
		plugin.getLogger().warning(playerName + " was listed as in a team, but has no team island or team leader. Removing from team.");
	    } else {
		// See if the team leader thinks this player is on their team
		if (plugin.getPlayers().getMembers(teamLeader).contains(uuid)) {
		    // Try and get the team leader's island
		    if (plugin.getPlayers().getTeamIslandLocation(teamLeader) != null) {
			teamIslandLocation = Util.getStringLocation(plugin.getPlayers().getTeamIslandLocation(teamLeader));
			plugin.getLogger().warning(playerName + " was listed as in a team, but has no team island. Fixed.");
		    }
		} else {
		    inTeam = false;
		    teamLeader = null;
		    plugin.getLogger()
		    .warning(playerName + " was listed as in a team, but the team leader does not have them on the team. Removing from team.");
		}
	    }
	}
	if (members == null) {
	    members = new ArrayList<UUID>();
	}
	return inTeam;
    }

    /**
     * Gets the default home location.
     * @return
     */
    public Location getHomeLocation() {
	return getHomeLocation(1); // Default
    }

    /**
     * Gets the home location by number. Note that the number is a string (to avoid conversion)
     * @param number
     * @return Location of this home or null if not available
     */
    public Location getHomeLocation(int number) {
	if (homeLocations.containsKey(number)) {
	    return homeLocations.get(number);
	} else {
	    return null;
	}
    }

    /**
     * Provides a list of all home locations - used when searching for a safe spot to place someone
     * @return List of home locations
     */
    public HashMap<Integer,Location> getHomeLocations() {
	HashMap<Integer,Location> result = new HashMap<Integer,Location>();
	for (int number : homeLocations.keySet()) {
	    result.put(number, homeLocations.get(number));
	}
	return result;
    }

    /**
     * @return The island level int. Note this function does not calculate the
     *         island level
     */
    public int getIslandLevel() {
	return islandLevel;
    }

    /**
     * @return the location of the player's island in Location form
     */
    public Location getIslandLocation() {
	// TODO: Enable this, but check the implications
	// if (islandLocation.isEmpty() && inTeam) {
	// return getLocationString(teamIslandLocation);
	// }
	return Util.getLocationString(islandLocation);
    }

    public List<UUID> getMembers() {
	return members;
    }

    public Location getTeamIslandLocation() {
	// return teamIslandLoc.getLocation();
	if (teamIslandLocation == null || teamIslandLocation.isEmpty()) {
	    return null;
	}
	Location l = Util.getLocationString(teamIslandLocation);
	return l;
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @return
     */
    public UUID getTeamLeader() {
	return teamLeader;
    }

    public Player getPlayer() {
	return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
	return uuid;
    }

    public String getPlayerName() {
	return playerName;
    }

    public void setPlayerN(String playerName) {
	this.playerName = playerName;
    }

    /**
     * @return the resetsLeft
     */
    public int getResetsLeft() {
	// Check what the global limit is
	if (Settings.resetLimit < resetsLeft) {
	    // Lower to the limit, which may be -1
	    resetsLeft = Settings.resetLimit;
	}
	if (Settings.resetLimit > 0 && resetsLeft == -1) {
	    // Set to the new limit if it has been raised from previously being
	    // unlimited
	    resetsLeft = Settings.resetLimit;
	}
	return resetsLeft;
    }

    /**
     * @param resetsLeft
     *            the resetsLeft to set
     */
    public void setResetsLeft(int resetsLeft) {
	this.resetsLeft = resetsLeft;
    }

    /**
     * Removes member from player's member list
     * 
     * @param member
     */
    public void removeMember(final UUID member) {
	members.remove(member);
    }

    /**
     * Resets all the challenges for the player and rebuilds the challenge list
     */
    public void resetAllChallenges() {
	challengeList.clear();
	challengeListTimes.clear();
	updateChallengeList();
    }

    /**
     * Resets a specific challenge. Will not reset a challenge that does not
     * exist in the player's list TODO: Add a success or failure return
     * 
     * @param challenge
     */
    public void resetChallenge(final String challenge) {
	if (challengeList.containsKey(challenge)) {
	    challengeList.put(challenge, Boolean.valueOf(false));
	    challengeListTimes.put(challenge, 0);
	}
    }

    public void setHasIsland(final boolean b) {
	hasIsland = b;
    }

    /**
     * Stores the home location of the player in a String format
     * 
     * @param l
     *            a Bukkit location
     */
    public void setHomeLocation(final Location l) {
	setHomeLocation(l, 1);
    }

    /**
     * Stores the numbered home location of the player. Numbering starts at 1. 
     * @param location
     * @param number
     */
    public void setHomeLocation(final Location location, int number) {
	if (location == null) {
	    homeLocations.clear();
	} else {
	    homeLocations.put(number, new Location(location.getWorld(),location.getBlockX(),location.getBlockY(),location.getBlockZ()));
	}
    }

    /**
     * Records the island's level. Does not calculate it
     * 
     * @param i
     */
    public void setIslandLevel(final int i) {
	islandLevel = i;
	if (Settings.setTeamName) {
	    Scoreboards.getInstance().setLevel(uuid, i);
	}
    }

    /**
     * Records the player's island location in a string form
     * 
     * @param l
     *            a Bukkit Location
     */
    public void setIslandLocation(final Location l) {
	islandLocation = Util.getStringLocation(l);
    }

    /**
     * Records that a player is now in a team
     * 
     * @param leader
     *            - a String of the leader's name
     * @param l
     *            - the Bukkit location of the team's island (converted to a
     *            String in this function)
     */
    public void setJoinTeam(final UUID leader, final Location l) {
	inTeam = true;
	teamLeader = leader;
	teamIslandLocation = Util.getStringLocation(l);
    }

    /**
     * Called when a player leaves a team Resets inTeam, teamLeader,
     * islandLevel, teamIslandLocation and members array
     */

    public void setLeaveTeam() {
	inTeam = false;
	teamLeader = null;
	islandLevel = 0;
	teamIslandLocation = null;
	members = new ArrayList<UUID>();
    }

    /**
     * @param l
     *            a Bukkit Location of the team island
     */
    public void setTeamIslandLocation(final Location l) {
	teamIslandLocation = Util.getStringLocation(l);
    }

    /**
     * @param leader
     *            a String name of the team leader
     */
    public void setTeamLeader(final UUID leader) {
	teamLeader = leader;
    }

    /**
     * @param s
     *            a String name of the player
     */
    public void setPlayerUUID(final UUID s) {
	uuid = s;
    }

    /**
     * Can invite or still waiting for cool down to end
     * 
     * @param location
     *            to check
     * @return number of mins/hours left until cool down ends
     */
    public long getInviteCoolDownTime(Location location) {
	// Check the hashmap
	if (location != null && kickedList.containsKey(location)) {
	    // plugin.getLogger().info("DEBUG: Location is known");
	    // The location is in the list
	    // Check the date/time
	    Date kickedDate = kickedList.get(location);
	    // plugin.getLogger().info("DEBUG: kicked date = " + kickedDate);
	    Calendar coolDownTime = Calendar.getInstance();
	    coolDownTime.setTime(kickedDate);
	    // coolDownTime.add(Calendar.HOUR_OF_DAY, Settings.inviteWait);
	    coolDownTime.add(Calendar.MINUTE, Settings.inviteWait);
	    // Add the invite cooldown period
	    Calendar timeNow = Calendar.getInstance();
	    // plugin.getLogger().info("DEBUG: date now = " + timeNow);
	    if (coolDownTime.before(timeNow)) {
		// The time has expired
		kickedList.remove(location);
		return 0;
	    } else {
		// Still not there yet
		// long hours = (coolDownTime.getTimeInMillis() -
		// timeNow.getTimeInMillis())/(1000 * 60 * 60);
		// Temp minutes
		long hours = (coolDownTime.getTimeInMillis() - timeNow.getTimeInMillis()) / (1000 * 60);
		return hours;
	    }
	}
	return 0;
    }

    /**
     * Stores the location that the player has been kicked from along with the
     * current time
     * 
     * @param kickedList
     *            the kickedList to set
     */
    public void startInviteCoolDownTimer(Location location) {
	if (location != null) {
	    kickedList.put(location, new Date());
	}
    }

    /**
     * @return the challengeListTimes
     */
    public HashMap<String, Integer> getChallengeCompleteTimes() {
	return challengeListTimes;
    }

    /**
     * Clears all home Locations
     */
    public void clearHomeLocations() {
	homeLocations.clear();
    }

}