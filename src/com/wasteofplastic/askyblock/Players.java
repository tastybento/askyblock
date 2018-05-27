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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.events.TeamJoinEvent;
import com.wasteofplastic.askyblock.events.TeamLeaveEvent;
import com.wasteofplastic.askyblock.util.Util;

/**
 * Tracks the following info on the player
 * 
 * @author tastybento
 */
public class Players {
    private final ASkyBlock plugin;
    private YamlConfiguration playerInfo;
    private Map<String, Boolean> challengeList;
    private Map<String, Integer> challengeListTimes;
    private Map<String, Long> challengeListTimestamp;
    private boolean hasIsland;
    private boolean inTeam;
    //private String homeLocation;
    private Map<Integer, Location> homeLocations;
    private long islandLevel;
    private String islandLocation;
    private List<UUID> members;
    private String teamIslandLocation;
    private UUID teamLeader;
    private UUID uuid;
    private String playerName;
    private int resetsLeft;
    private Map<Location, Date> kickedList;
    private List<UUID> banList;
    private String locale;
    private int startIslandRating;
    private boolean useControlPanel;
    private int deaths;

    /**
     * @param aSkyBlock - plugin
     * @param uuid - player's uuid
     * @throws IOException - if uuid is null
     */
    public Players(final ASkyBlock aSkyBlock, final UUID uuid) throws IOException {
        this.plugin = aSkyBlock;
        this.uuid = uuid;
        if (uuid == null) {
            throw new IOException("UUID is null");
        }
        this.members = new ArrayList<>();
        this.hasIsland = false;
        this.islandLocation = null;
        //this.homeLocation = null;
        this.homeLocations = new HashMap<>();
        this.inTeam = false;
        this.teamLeader = null;
        this.teamIslandLocation = null;
        this.challengeList = new HashMap<>();
        this.challengeListTimes = new HashMap<>();
        this.challengeListTimestamp = new HashMap<>();
        this.islandLevel = 0;
        this.playerName = "";
        this.resetsLeft = Settings.resetLimit;
        this.kickedList = new HashMap<>();
        this.locale = "";
        this.startIslandRating = 50;
        this.banList = new ArrayList<>();
        this.useControlPanel = Settings.useControlPanel;
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
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.hasMetadata("NPC")) {
                //plugin.getLogger().info("DEBUG: Entity is NPC");
                playerName = player.getUniqueId().toString();
            } else {
                playerName = uuid.toString();
                //plugin.getLogger().info("DEBUG: Entity is player");
                try {
                    OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
                    if (offlinePlayer != null && offlinePlayer.getName() != null) {
                        playerName = offlinePlayer.getName();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
                    playerName = uuid.toString();
                }
            }

        }
        // Start island rating - how difficult the start island was. Default if 50/100
        this.startIslandRating = playerInfo.getInt("startIslandRating", 50);
        // Locale
        this.locale = playerInfo.getString("locale","");
        // Ban list
        List<String> banListString = playerInfo.getStringList("banList");
        for (String uuidString : banListString) {
            try {
                banList.add(UUID.fromString(uuidString));
            } catch (Exception ignored) {}
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
            challengeList.put(challenge.toLowerCase(), playerInfo.getBoolean("challenges.status." + challenge.toLowerCase().replace(".", "[dot]"), false));
            challengeListTimes.put(challenge.toLowerCase(), playerInfo.getInt("challenges.times." + challenge.toLowerCase().replace(".", "[dot]"), 0));
            challengeListTimestamp.put(challenge.toLowerCase(), playerInfo.getLong("challenges.timestamp." + challenge.toLowerCase().replace(".", "[dot]"), 0));
        }
        for (String challenge : Settings.challengeLevels) {
            // If they are in the list, then use the value, otherwise use false
            challengeList.put(challenge.toLowerCase(), playerInfo.getBoolean("challenges.status." + challenge.toLowerCase().replace(".", "[dot]"), false));
            challengeListTimes.put(challenge.toLowerCase(), playerInfo.getInt("challenges.times." + challenge.toLowerCase().replace(".", "[dot]"), 0));
            challengeListTimestamp.put(challenge.toLowerCase(), playerInfo.getLong("challenges.timestamp." + challenge.toLowerCase().replace(".", "[dot]"), 0));
        }
        // Load reset limit
        this.resetsLeft = playerInfo.getInt("resetsLeft", Settings.resetLimit);
        // Check what the global limit is and raise it if it was changed
        if (Settings.resetLimit > 0 && this.resetsLeft == -1) {
            resetsLeft = Settings.resetLimit;
        }
        // Deaths
        this.deaths = playerInfo.getInt("deaths", 0);
        // Load control panel setting
        useControlPanel = playerInfo.getBoolean("useControlPanel", Settings.useControlPanel);
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
    public void save(boolean async) {
        //plugin.getLogger().info("Saving player..." + playerName);
        // Save the variables
        playerInfo.set("playerName", playerName);
        playerInfo.set("hasIsland", hasIsland);
        if (hasIsland && !banList.isEmpty()) {
            List<String> banListString = new ArrayList<>();
            for (UUID bannedUUID : banList) {
                banListString.add(bannedUUID.toString());
            }
            playerInfo.set("banList", banListString);
        } else {
            // Clear
            playerInfo.set("banList", null);
        }
        playerInfo.set("islandLocation", islandLocation);
        playerInfo.set("homeLocation", null);
        // Only store the new way
        // Clear any old home locations
        playerInfo.set("homeLocations",null);
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
        playerInfo.set("challenges",null);
        for (String challenge : challengeList.keySet()) {
            if (!challenge.isEmpty())
                playerInfo.set("challenges.status." + challenge.replace(".","[dot]"), challengeList.get(challenge));
        }
        for (String challenge : challengeListTimes.keySet()) {
            if (!challenge.isEmpty())
                playerInfo.set("challenges.times." + challenge.replace(".","[dot]"), challengeListTimes.get(challenge));
        }
        for (String challenge : challengeListTimestamp.keySet()) {
            if (!challenge.isEmpty())
                playerInfo.set("challenges.timestamp." + challenge.replace(".","[dot]"), challengeListTimestamp.get(challenge));
        }
        // Check what the global limit is
        if (Settings.resetLimit < this.resetsLeft) {
            this.resetsLeft = Settings.resetLimit;
        }
        playerInfo.set("resetsLeft", this.resetsLeft);
        playerInfo.set("deaths", deaths);
        // Save invite cooldown timers
        playerInfo.set("invitecooldown", null);
        for (Entry<Location, Date> en : kickedList.entrySet()) {
            // Convert location and date to string (time in millis)
            Calendar coolDownTime = Calendar.getInstance();
            coolDownTime.setTime(en.getValue());
            playerInfo.set("invitecooldown." + coolDownTime.getTimeInMillis(), Util.getStringLocation(en.getKey()));
        }
        // Locale
        playerInfo.set("locale", locale);
        // Start island rating
        if (startIslandRating < 1){
            // Remove it if the rating is 0 or less
            playerInfo.set("startIslandRating", null);
        } else {
            playerInfo.set("startIslandRating", startIslandRating);
        }
        // Island info - to be used if the island.yml file is removed
        playerInfo.set("islandInfo",null);
        if (hasIsland) {
            if (plugin.getGrid() != null) {
                Island island = plugin.getGrid().getIsland(uuid);
                if (island != null) {
                    playerInfo.set("islandInfo", island.save());
                } 
            }
        }
        // Control panel
        playerInfo.set("useControlPanel", useControlPanel);

        //playerInfo.set("coops", value);

        // Actually save the file
        Util.saveYamlFile(playerInfo, "players/" + uuid.toString() + ".yml", async);
    }

    /**
     * @param member
     *            Adds a member to the the player's list
     */
    public void addTeamMember(final UUID member) {
        members.add(member);
    }

    /**
     * Checks if a challenge exists in the player's challenge list
     * 
     * @param challenge
     * @return true if challenge is listed in the player's challenge list,
     *         otherwise false
     */
    public boolean challengeExists(final String challenge) {
        return challengeList.containsKey(challenge.toLowerCase());
    }

    /**
     * Checks if a challenge is recorded as completed in the player's challenge
     * list or not
     * 
     * @param challenge
     * @return true if the challenge is listed as complete, false if not
     */
    public boolean checkChallenge(String challenge) {
        challenge = challenge.toLowerCase();
        if (challengeList.containsKey(challenge)) {
            // Check if the challenge has been globally reset
            long timestamp = challengeListTimestamp.get(challenge);
            if (plugin.getChallenges().isChallengeReset(challenge, timestamp)) {
                this.resetChallenge(challenge);
                return false;
            }
            // It has not been globally reset
            // plugin.getLogger().info("DEBUG: " + challenge + ":" +
            // challengeList.get(challenge.toLowerCase()).booleanValue() );
            return challengeList.get(challenge.toLowerCase());
        }
        return false;
    }

    /**
     * Checks how many times a challenge has been done
     * 
     * @param challenge
     * @return number of times
     */
    public int checkChallengeTimes(final String challenge) {
        if (challengeListTimes.containsKey(challenge.toLowerCase())) {
            // plugin.getLogger().info("DEBUG: check " + challenge + ":" +
            // challengeListTimes.get(challenge.toLowerCase()).intValue() );
            return challengeListTimes.get(challenge.toLowerCase());
        }
        return 0;
    }

    public Map<String, Boolean> getChallengeStatus() {
        return challengeList;
    }

    /**
     * Records the challenge as being complete in the player's list. If the
     * challenge is not listed in the player's challenge list already, then it
     * will be added.
     * 
     * @param challenge
     */
    public void completeChallenge(final String challenge) {
        //plugin.getLogger().info("DEBUG: Complete challenge");
        challengeList.put(challenge.toLowerCase(), true);
        // Count how many times the challenge has been done
        int times = 0;
        if (challengeListTimes.containsKey(challenge.toLowerCase())) {
            times = challengeListTimes.get(challenge.toLowerCase());
        }
        times++;
        challengeListTimes.put(challenge.toLowerCase(), times);
        // plugin.getLogger().info("DEBUG: complete " + challenge + ":" +
        // challengeListTimes.get(challenge.toLowerCase()).intValue() );
        // Add timestamp
        challengeListTimestamp.put(challenge, System.currentTimeMillis());
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
            members = new ArrayList<>();
        }
        return inTeam;
    }

    /**
     * Gets the default home location.
     * @return Location
     */
    public Location getHomeLocation() {
        return getHomeLocation(1); // Default
    }

    /**
     * Gets the home location by number.
     * @param number
     * @return Location of this home or null if not available
     */
    public Location getHomeLocation(int number) {
        return homeLocations.getOrDefault(number, null);
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
    public long getIslandLevel() {
        return islandLevel;
    }

    /**
     * @return the location of the player's island in Location form
     */
    public Location getIslandLocation() {
        if (islandLocation.isEmpty() && inTeam) {
            return Util.getLocationString(teamIslandLocation);
        }
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
    }

    /**
     * Resets a specific challenge.
     * @param challenge
     */
    public void resetChallenge(final String challenge) {
        //plugin.getLogger().info("DEBUG: reset challenge");
        challengeList.put(challenge, false);
        challengeListTimes.put(challenge, 0);
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
            // Make the location x,y,z integer, but keep the yaw and pitch
            homeLocations.put(number, new Location(location.getWorld(),location.getBlockX(),location.getBlockY(),location.getBlockZ(),location.getYaw(), location.getPitch()));
        }
    }

    /**
     * Records the island's level. Does not calculate it
     * 
     * @param l
     */
    public void setIslandLevel(final long l) {
        islandLevel = l;
        if (Settings.setTeamName) {
            Scoreboards.getInstance().setLevel(uuid, l);
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
     * @return - true if successful
     */
    public boolean setJoinTeam(final UUID leader, final Location l) {
        if(inTeam) {
            TeamLeaveEvent teamLeaveEvent = new TeamLeaveEvent(uuid, teamLeader);
            Bukkit.getPluginManager().callEvent(teamLeaveEvent);
            if (teamLeaveEvent.isCancelled()) {
                return false;
            }
        }
        // Fire the event and give a chance for it to be cancelled.
        TeamJoinEvent teamJoinEvent = new TeamJoinEvent(uuid, leader);
        Bukkit.getPluginManager().callEvent(teamJoinEvent);
        if (teamJoinEvent.isCancelled()) {
            return false;
        }
        // Success
        inTeam = true;
        teamLeader = leader;
        teamIslandLocation = Util.getStringLocation(l);

        return true;
    }

    /**
     * Called when a player leaves a team Resets inTeam, teamLeader,
     * islandLevel, teamIslandLocation and members array
     * @return true if successful, false if not
     */
    public boolean setLeaveTeam() {
        if(inTeam) {
            TeamLeaveEvent teamLeaveEvent = new TeamLeaveEvent(uuid, teamLeader);
            Bukkit.getPluginManager().callEvent(teamLeaveEvent);
            if (teamLeaveEvent.isCancelled()) {
                return false;
            }
        }

        inTeam = false;
        teamLeader = null;
        islandLevel = 0;
        teamIslandLocation = null;
        members = new ArrayList<>();
        return true;
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
     * @return true if successful
     */
    public boolean setTeamLeader(final UUID leader) {
        if(inTeam) {
            // Changing team leader changes the team identifier
            TeamLeaveEvent teamLeaveEvent = new TeamLeaveEvent(uuid, teamLeader);
            Bukkit.getPluginManager().callEvent(teamLeaveEvent);
            if (teamLeaveEvent.isCancelled()) {
                return false;
            }
        }
        if(leader != null) {
            TeamJoinEvent teamJoinEvent = new TeamJoinEvent(uuid, leader);
            Bukkit.getPluginManager().callEvent(teamJoinEvent);
            if (teamJoinEvent.isCancelled()) {
                return false;
            }
        }
        teamLeader = leader;
        return true;
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
     * Starts the invite cooldown timer for location. Location should be the center of an island.
     * @param location
     */
    public void startInviteCoolDownTimer(Location location) {
        if (location != null) {
            kickedList.put(location, new Date());
        }
    }

    /**
     * @return the challengeListTimes
     */
    public Map<String, Integer> getChallengeCompleteTimes() {
        return challengeListTimes;
    }

    /**
     * Clears all home Locations
     */
    public void clearHomeLocations() {
        homeLocations.clear();
    }

    /**
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * @return the startIslandRating
     */
    public int getStartIslandRating() {
        return startIslandRating;
    }

    /**
     * @param startIslandRating the startIslandRating to set
     */
    public void setStartIslandRating(int startIslandRating) {
        this.startIslandRating = startIslandRating;
    }

    /**
     * @return the banList
     */
    public List<UUID> getBanList() {
        return banList;
    }

    /**
     * Ban a player
     * @param banned player's UUID
     */
    public void addToBanList(UUID banned) {
        this.banList.add(banned);
    }

    /**
     * Un ban a player
     * @param unbanned
     */
    public void unBan(UUID unbanned) {
        this.banList.remove(unbanned);
    }

    /**
     * @param targetUUID
     * @return true if this player is banned
     */
    public boolean isBanned(UUID targetUUID) {
        return this.banList.contains(targetUUID);
    }

    /**
     * Sets whether a player uses the control panel or not
     * @param b
     */
    public void setControlPanel(boolean b) {
        useControlPanel = b;
    }

    /**
     * @return useControlPanel
     */
    public boolean getControlPanel() {
        return useControlPanel;
    }

    /**
     * @return the deaths
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * @param deaths the deaths to set
     */
    public void setDeaths(int deaths) {
        this.deaths = deaths;
        if (this.deaths > Settings.maxDeaths) {
            this.deaths = Settings.maxDeaths;
        }
    }

    /**
     * Add death
     */
    public void addDeath() {
        this.deaths++;
        if (this.deaths > Settings.maxDeaths) {
            this.deaths = Settings.maxDeaths;
        }
    }

    /**
     * @return a list of challenges this player has completed
     * Used by the reset admin command
     */
    public List<String> getChallengesDone() {
        return challengeList.entrySet().stream().filter(Entry::getValue)
            .map(Entry::getKey).collect(Collectors.toList());
    }

    /**
     * @return a list of challenges this player has not completed
     * Used by the complete admin command
     */
    public List<String> getChallengesNotDone() {
        return challengeList.entrySet().stream().filter(en -> !en.getValue())
            .map(Entry::getKey).collect(Collectors.toList());
    }
}
