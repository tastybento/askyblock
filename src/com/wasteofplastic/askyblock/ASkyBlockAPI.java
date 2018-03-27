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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.commands.Challenges;
import com.wasteofplastic.askyblock.panels.SetBiome;

/**
 * Provides a programming interface. The aim of this class is to provide a fairly static set of calls for other plugins to use.
 * If you try to hook into other classes in the code, they may change and break your code.
 * 
 * @author tastybento
 */
public class ASkyBlockAPI {
    private static ASkyBlockAPI instance;

    /**
     * @return the instance
     */
    public static ASkyBlockAPI getInstance() {
        if (instance == null)
            // Initialize the API
            new ASkyBlockAPI(ASkyBlock.getPlugin());
        return instance;
    }


    private ASkyBlock plugin;

    protected ASkyBlockAPI(ASkyBlock plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /**
     * @param playerUUID - the player's UUID - player's UUID
     * @return Map of all of the known challenges with a boolean marking
     *         them as complete (true) or incomplete (false). This is a view of the
     *         challenges map that only allows read operations.
     */
    public Map<String, Boolean> getChallengeStatus(UUID playerUUID) {
        return Collections.unmodifiableMap(plugin.getPlayers().getChallengeStatus(playerUUID));
    }

    /**
     * @param playerUUID - the player's UUID - player's UUID
     * @return Map of all of the known challenges and how many times each
     *         one has been completed. This is a view of the challenges
     *         map that only allows read operations.
     */
    public Map<String, Integer> getChallengeTimes(UUID playerUUID) {
        return Collections.unmodifiableMap(plugin.getPlayers().getChallengeTimes(playerUUID));
    }

    public Location getHomeLocation(UUID playerUUID) {
        return plugin.getPlayers().getHomeLocation(playerUUID,1);
    }

    /**
     * @deprecated
     * Island level is now a long and the int value may not be accurate for very large values.
     * Use getLongIslandLevel(UUID playerUUID) instead.
     * 
     * Returns the island level from the last time it was calculated. Note this
     * does not calculate the island level.
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return the last level calculated for the island or zero if none.
     */
    public int getIslandLevel(UUID playerUUID) {
        return (int)plugin.getPlayers().getIslandLevel(playerUUID);
    }
    
    /**
     * Returns the island level from the last time it was calculated. Note this
     * does not calculate the island level.
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return the last level calculated for the island or zero if none.
     */
    public long getLongIslandLevel(UUID playerUUID) {
        return plugin.getPlayers().getIslandLevel(playerUUID);
    }

    /**
     * Sets the player's island level. Does not calculate it and does not set the level of any team members.
     * You will need to check if the player is in a team and individually set the level of each team member.
     * This value will be overwritten if the players run the build-in level command or if the island level
     * is calculated some other way, e.g. at login or via an admin command.
     * @param playerUUID - the player's UUID - player's UUID
     * @param level - level to set
     */
    public void setIslandLevel(UUID playerUUID, int level) {
        plugin.getPlayers().setIslandLevel(playerUUID, level);
    }

    /**
     * Calculates the island level. Only the fast calc is supported.
     * The island calculation runs async and fires an IslandLevelEvent when completed
     * or use getIslandLevel(playerUUID). See https://gist.github.com/tastybento/e81d2403c03f2fe26642
     * for example code.
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return true if player has an island, false if not
     */
    public boolean calculateIslandLevel(UUID playerUUID) {
        if (plugin.getPlayers().hasIsland(playerUUID) || plugin.getPlayers().inTeam(playerUUID)) {		
            new LevelCalcByChunk(plugin, plugin.getGrid().getIsland(playerUUID), playerUUID, null, false);
            return true;
        }
        return false;
    }

    /**
     * Provides the location of the player's island, either the team island or
     * their own
     * 
     * @param playerUUID - the player's UUID - players UUID
     * @return Location of island
     */
    public Location getIslandLocation(UUID playerUUID) {
        return plugin.getPlayers().getIslandLocation(playerUUID);
    }

    /**
     * Returns the owner of an island from the location.
     * Uses the grid lookup and is quick
     * 
     * @param location - location to check
     * @return UUID of owner
     */
    public UUID getOwner(Location location) {
        return plugin.getPlayers().getPlayerFromIslandLocation(location);
    }

    /**
     * Get Team Leader
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return UUID of Team Leader or null if there is none. Use inTeam to
     *         check.
     */
    public UUID getTeamLeader(UUID playerUUID) {
        return plugin.getPlayers().getTeamLeader(playerUUID);
    }

    /**
     * Get a list of team members. This is a copy and changing the return value
     * will not affect the membership.
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return List of team members, including the player. Empty if there are
     *         none.
     */
    public List<UUID> getTeamMembers(UUID playerUUID) {
        return new ArrayList<UUID>(plugin.getPlayers().getMembers(playerUUID));
    }

    /**
     * Provides location of the player's warp sign
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return Location of sign or null if one does not exist
     */
    public Location getWarp(UUID playerUUID) {
        return plugin.getWarpSignsListener().getWarp(playerUUID);
    }

    /**
     * Get the owner of the warp at location
     * 
     * @param location - location of the warp
     * @return Returns name of player or empty string if there is no warp at
     *         that spot
     */
    public String getWarpOwner(Location location) {
        return plugin.getWarpSignsListener().getWarpOwner(location);
    }

    /**
     * Status of island ownership. Team members do not have islands of their
     * own, only leaders do.
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @return true if player has an island, false if the player does not.
     */
    public boolean hasIsland(UUID playerUUID) {
        return plugin.getPlayers().hasIsland(playerUUID);
    }

    /**
     * @param playerUUID - the player's UUID - player's UUID
     * @return true if in a team
     */
    public boolean inTeam(UUID playerUUID) {
        return plugin.getPlayers().inTeam(playerUUID);
    }

    /**
     * Determines if an island is at a location in this area location. Also
     * checks if the spawn island is in this area. Checks for bedrock within
     * limits and also looks in the file system. Quite processor intensive.
     * 
     * @param location - location to check
     * @return true if there is an island in that location, false if not
     */
    public boolean islandAtLocation(Location location) {
        return plugin.getGrid().islandAtLocation(location);
    }

    /**
     * Checks to see if a player is trespassing on another player's island. Both
     * players must be online.
     * 
     * @param owner - player owner or team member of an island
     * @param target - player who may be doing the trespassing
     * @return true if they are on the island otherwise false.
     */
    public boolean isOnIsland(Player owner, Player target) {
        return plugin.getGrid().isOnIsland(owner, target);
    }

    /**
     * Lists all the known warps. As each player can have only one warp, the
     * player's UUID is used. It can be displayed however you like to other
     * users. This is a copy of the set and changing it will not affect the
     * actual set of warps.
     * 
     * @return String set of warps
     */
    public Set<UUID> listWarps() {
        return new HashSet<UUID>(plugin.getWarpSignsListener().listWarps());
    }

    /**
     * Forces the warp panel to update and the warp list event to fire so that
     * the warps can be sorted how you like.
     */
    public void updateWarpPanel() {
        plugin.getWarpPanel().updatePanel();
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * owned by the player
     * 
     * @param player - player object
     * @param location - location to check
     * @return true if the location is on an island owner by player
     */
    public boolean locationIsOnIsland(final Player player, final Location location) {
        return plugin.getGrid().locationIsOnIsland(player, location);
    }

    /**
     * Finds out if location is within a set of island locations and returns the
     * one that is there or null if not. The islandTestLocations should be the center
     * location of an island. The check is done to see if loc is inside the protected
     * range of any of the islands given. 
     * 
     * @param islandTestLocations - Set of island locations to check
     * @param loc - location to check
     * @return the island location that is in the set of locations or null if
     *         none
     */
    public Location locationIsOnIsland(final Set<Location> islandTestLocations, final Location loc) {
        return plugin.getGrid().locationIsOnIsland(islandTestLocations, loc);
    }

    /**
     * Checks if an online player is on their island, on a team island or on a
     * coop island
     * 
     * @param player - the player who is being checked
     * @return - true if they are on their island, otherwise false
     */
    public boolean playerIsOnIsland(Player player) {
        return plugin.getGrid().playerIsOnIsland(player);
    }

    /**
     * Sets all blocks in an island to a specified biome type
     * 
     * @param islandLoc - island location
     * @param biomeType - biome type
     * @return true if the setting was successful
     */
    public boolean setIslandBiome(Location islandLoc, Biome biomeType) {
        Island island = plugin.getGrid().getIslandAt(islandLoc);
        if (island != null) {
            new SetBiome(plugin, island, biomeType, null);
            return true;
        }
        return false;
    }

    /**
     * Sets a message for the player to receive next time they login
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @param message - message to set
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
        return plugin.getMessages().setMessage(playerUUID, message);
    }

    /**
     * Sends a message to every player in the team that is offline If the player
     * is not in a team, nothing happens.
     * 
     * @param playerUUID - the player's UUID - player's UUID
     * @param message - message to send
     */
    public void tellOfflineTeam(UUID playerUUID, String message) {
        plugin.getMessages().tellOfflineTeam(playerUUID, message);
    }

    /**
     * Player is in a coop or not
     * 
     * @param player - player object to check
     * @return true if player is in a coop, otherwise false
     */
    public boolean isCoop(Player player) {
        if (CoopPlay.getInstance().getCoopIslands(player).isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Find out which coop islands player is a part of
     * 
     * @param player - player object to check
     * @return set of locations of islands or empty if none
     */
    public Set<Location> getCoopIslands(Player player) {
        return new HashSet<Location>(CoopPlay.getInstance().getCoopIslands(player));
    }

    /**
     * Provides spawn location
     * @return Location of spawn's central point
     */
    public Location getSpawnLocation() {
        return plugin.getGrid().getSpawn().getCenter();
    }

    /**
     * Provides the spawn range
     * @return spawn range
     */
    public int getSpawnRange() {
        return plugin.getGrid().getSpawn().getProtectionSize();
    }

    /**
     * Checks if a location is at spawn or not
     * @param location - location to check
     * @return true if at spawn
     */
    public boolean isAtSpawn(Location location) {
        return plugin.getGrid().isAtSpawn(location);
    }

    /**
     * Get the island overworld
     * @return the island overworld
     */
    public World getIslandWorld() {
        return ASkyBlock.getIslandWorld();
    }

    /**
     * Get the nether world
     * @return the nether world
     */
    public World getNetherWorld() {
        return ASkyBlock.getNetherWorld();
    }

    /**
     * Whether the new nether is being used or not
     * @return true if new nether is being used
     */
    public boolean isNewNether() {
        return Settings.newNether;
    }

    /**
     * @deprecated
     * Island levels are now stored as longs, so the int value may not be accurate
     * 
     * Get the top ten list
     * @return Top ten list
     */
    public Map<UUID, Integer> getTopTen() {
        HashMap<UUID, Integer> result = new HashMap<UUID, Integer>();
        for (Entry<UUID, Long> en : plugin.getTopTen().getTopTenList().entrySet()) {
           result.put(en.getKey(), en.getValue().intValue()); 
        }
        return result;
    }
    
    /**
     * Get the top ten list
     * @return Top ten list
     */
    public Map<UUID, Long> getLongTopTen() {
        return new HashMap<UUID, Long>(plugin.getTopTen().getTopTenList());
    }

    /**
     * Obtains a copy of the island object owned by playerUUID
     * @param playerUUID - the player's UUID - player's UUID
     * @return copy of Island object
     */
    public Island getIslandOwnedBy(UUID playerUUID) {
        return plugin.getGrid().getIsland(playerUUID);
    }

    /**
     * Returns the Island object for an island at this location or null if one does not exist
     * @param location - location requested
     * @return copy of Island object
     */
    public Island getIslandAt(Location location) {
        return plugin.getGrid().getIslandAt(location);
    }

    /**
     * @return how many islands are in the world (that the plugin knows of)
     */
    public int getIslandCount() {
        return plugin.getGrid().getIslandCount();
    }

    /**
     * Get a copy of the ownership map of islands
     * @return Hashmap of owned islands with owner UUID as a key
     */
    public HashMap<UUID, Island> getOwnedIslands() {
        //System.out.println("DEBUG: getOwnedIslands");
        if (plugin.getGrid() != null) {
            HashMap<UUID, Island> islands = plugin.getGrid().getOwnedIslands();
            if (islands != null) {
                //plugin.getLogger().info("DEBUG: getOwnedIslands is not null");
                return new HashMap<UUID, Island>(islands);
            }
            //plugin.getLogger().info("DEBUG: getOwnedIslands is null");
        }
        return new HashMap<UUID, Island>();

    }

    /**
     * Get name of the island owned by owner
     * @param owner - player's UUID
     * @return Returns the name of owner's island, or the owner's name if there is none.
     */
    public String getIslandName(UUID owner) {
        return plugin.getGrid().getIslandName(owner);
    }

    /**
     * Set the island name
     * @param owner - player's UUID
     * @param name - name to set
     */
    public void setIslandName(UUID owner, String name) {
        plugin.getGrid().setIslandName(owner, name);
    }

    /**
     * Get all the challenges
     * @return challenges per level
     */
    public LinkedHashMap<String, List<String>> getAllChallenges(){
        return Challenges.getChallengeList();
    }

    /**
     * Get the number of resets left for this player
     * @param playerUUID - the player's UUID - player's UUID
     * @return Number of resets left
     */
    public int getResetsLeft(UUID playerUUID) {
        return plugin.getPlayers().getResetsLeft(playerUUID);
    }

    /**
     * Set the number of resets left for this player
     * @param playerUUID - the player's UUID - player's UUID
     * @param resets - number to set
     */
    public void setResetsLeft(UUID playerUUID, int resets) {
        plugin.getPlayers().setResetsLeft(playerUUID, resets);
    }
    
    /**
     * Find out if this player is a team leader or not. If the player is not in a team, the result will always be false.
     * @param playerUUID - the player's UUID - player's UUID
     * @return true if the player is in a team and is the leader
     */
    public boolean isLeader(UUID playerUUID) {
        UUID leader = plugin.getPlayers().getTeamLeader(playerUUID);
        if (leader != null && leader.equals(playerUUID)) {
            return true;
        }
        return false;
        
    }
}
