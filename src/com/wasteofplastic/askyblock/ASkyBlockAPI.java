package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Provides a programming interface
 * 
 * @author tastybento
 */
public class ASkyBlockAPI {
    private static ASkyBlockAPI instance = new ASkyBlockAPI(ASkyBlock.getPlugin());

    /**
     * @return the instance
     */
    public static ASkyBlockAPI getInstance() {
	return instance;
    }

    private ASkyBlock plugin;

    private ASkyBlockAPI(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * @param playerUUID
     * @return HashMap of all of the known challenges with a boolean marking
     *         them as complete (true) or incomplete (false)
     */
    public HashMap<String, Boolean> getChallengeStatus(UUID playerUUID) {
	return plugin.getPlayers().getChallengeStatus(playerUUID);
    }

    public Location getHomeLocation(UUID playerUUID) {
	return plugin.getPlayers().getHomeLocation(playerUUID,1);
    }

    /**
     * Returns the island level from the last time it was calculated. Note this
     * does not calculate the island level.
     * 
     * @param playerUUID
     * @return the last level calculated for the island or zero if none.
     */
    public int getIslandLevel(UUID playerUUID) {
	return plugin.getPlayers().getIslandLevel(playerUUID);
    }

    /**
     * Provides the location of the player's island, either the team island or
     * their own
     * 
     * @param playerUUID
     * @return Location of island
     */
    public Location getIslandLocation(UUID playerUUID) {
	if (plugin.getPlayers().inTeam(playerUUID)) {
	    return plugin.getPlayers().getTeamIslandLocation(playerUUID);
	}
	return plugin.getPlayers().getIslandLocation(playerUUID);
    }

    /**
     * Returns the owner of an island from the location. This is an expensive
     * call as it may have to hunt through the file system to find offline
     * players.
     * 
     * @param location
     * @return UUID of owner
     */
    public UUID getOwner(Location location) {
	return plugin.getPlayers().getPlayerFromIslandLocation(location);
    }

    /**
     * Get Team Leader
     * 
     * @param playerUUID
     * @return UUID of Team Leader or null if there is none. Use inTeam to
     *         check.
     */
    public UUID getTeamLeader(UUID playerUUID) {
	return plugin.getPlayers().getTeamLeader(playerUUID);
    }

    /**
     * Get team members.
     * 
     * @param playerUUID
     * @return List of team members, including the player. Empty if there are
     *         none.
     */
    public List<UUID> getTeamMembers(UUID playerUUID) {
	return plugin.getPlayers().getMembers(playerUUID);
    }

    /**
     * Provides location of the player's warp sign
     * 
     * @param playerUUID
     * @return Location of sign or null if one does not exist
     */
    public Location getWarp(UUID playerUUID) {
	return plugin.getWarpSignsListener().getWarp(playerUUID);
    }

    /**
     * Get the owner of the warp at location
     * 
     * @param location
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
     * @param playerUUID
     * @return true if player has an island, false if the player does not.
     */
    public boolean hasIsland(UUID playerUUID) {
	return plugin.getPlayers().hasIsland(playerUUID);
    }

    /**
     * @param playerUUID
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
     * @param location
     * @return true if there is an island in that location, false if not
     */
    public boolean islandAtLocation(Location location) {
	return plugin.getGrid().islandAtLocation(location);
    }

    /**
     * Checks to see if a player is trespassing on another player's island Both
     * players must be online.
     * 
     * @param owner
     *            - owner or team member of an island
     * @param target
     * @return true if they are on the island otherwise false.
     */
    public boolean isOnIsland(Player owner, Player target) {
	return plugin.getGrid().isOnIsland(owner, target);
    }

    /**
     * Lists all the known warps. As each player can have only one warp, the
     * player's UUID is used. It can be displayed however you like to other
     * users.
     * 
     * @return String set of warps
     */
    public Set<UUID> listWarps() {
	return plugin.getWarpSignsListener().listWarps();
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
     * @param player
     * @param loc
     * @return true if the location is on an island owner by player
     */
    public boolean locationIsOnIsland(final Player player, final Location loc) {
	return plugin.getGrid().locationIsOnIsland(player, loc);
    }

    /**
     * Finds out if location is within a set of island locations and returns the
     * one that is there or null if not
     * 
     * @param islandTestLocations
     * @param loc
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
     * @param player
     *            - the player who is being checked
     * @return - true if they are on their island, otherwise false
     */
    public boolean playerIsOnIsland(Player player) {
	return plugin.getGrid().playerIsOnIsland(player);
    }

    /**
     * Sets all blocks in an island to a specified biome type
     * 
     * @param islandLoc
     * @param biomeType
     * @return true if the setting was successful
     */
    public boolean setIslandBiome(Location islandLoc, Biome biomeType) {
	return plugin.getBiomes().setIslandBiome(islandLoc, biomeType);
    }

    /**
     * Sets a message for the player to receive next time they login
     * 
     * @param playerUUID
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
	return plugin.getMessages().setMessage(playerUUID, message);
    }

    /**
     * Sends a message to every player in the team that is offline If the player
     * is not in a team, nothing happens.
     * 
     * @param playerUUID
     * @param message
     */
    public void tellOfflineTeam(UUID playerUUID, String message) {
	plugin.getMessages().tellOfflineTeam(playerUUID, message);
    }

    /**
     * Player is in a coop or not
     * 
     * @param player
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
     * @param player
     * @return set of locations of islands or empty if none
     */
    public Set<Location> getCoopIslands(Player player) {
	return CoopPlay.getInstance().getCoopIslands(player);
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
     * @param location
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
     * Get the top ten list
     * @return Top ten list
     */
    public Map<UUID, Integer> getTopTen() {
	return TopTen.getTopTenList();
    }
    
}
