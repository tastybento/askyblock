package com.wasteofplastic.askyblock;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * @author tastybento
 * Provides a programming interface to Island
 */
public class ASkyBlockAPI {
    private static ASkyBlockAPI instance = new ASkyBlockAPI(ASkyBlock.getPlugin());
    private ASkyBlock plugin;

    private ASkyBlockAPI(ASkyBlock plugin) {
	this.plugin = plugin;
    }

    /**
     * @return the instance
     */
    public static ASkyBlockAPI getInstance() {
	return instance;
    }

    /*
     * Get information about (island):
Level
Owner
Party members
Rank (Something with the top-ten ?)
Bedrock coordinates
Island bounds
Biome

Playing with challenges would be interesting: 
Number of completed challenges 
Challenge names / list 

Now, we could be able to: 
Play with party members (add, kick, set owner) 
Change island biome 
Complete a challenge 
Send someone to the Welcome Sign of an island
     */

    /**
     * @param player
     * @return HashMap of all of the known challenges with a boolean marking them as complete (true) or incomplete (false)
     */
    public HashMap<String,Boolean> getChallengeStatus(UUID playerUUID) {
	return plugin.getPlayers().getChallengeStatus(playerUUID);
    }

    public Location getHomeLocation(UUID playerUUID) {
	return plugin.getPlayers().getHomeLocation(playerUUID);
    }
    
    /**
     * Returns the island level from the last time it was calculated. Note this does not calculate the island level.
     * @param player
     * @return island level
     */
    public int getIslandLevel(UUID playerUUID) {
	return plugin.getPlayers().getIslandLevel(playerUUID);
    }

    /**
     * Provides the location of the player's island, either the team island or their own
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
     * Get team members.
     * @param playerUUID
     * @return List of team members, including the player. Empty if there are none.
     */
    public List<UUID> getTeamMembers(UUID playerUUID) {
	return plugin.getPlayers().getMembers(playerUUID);
    }
        
    /**
     * Returns the owner of an island from the location.
     * This is an expensive call as it may have to hunt through the file system
     * to find offline players.
     * @param loc
     * @return UUID of owner
     */
    public UUID getOwner(Location location) {
	return plugin.getPlayers().getPlayerFromIslandLocation(location);
    }
    
    /**
     * Get Team Leader
     * @param playerUUID
     * @return UUID of Team Leader or null if there is none. Use inTeam to check.
     */
    public UUID getTeamLeader(UUID playerUUID) {
	return plugin.getPlayers().getTeamLeader(playerUUID);
    }
    
    /**
     * Status of island ownership. Team members do not have islands of their own, only leaders do.
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
     * Provides location of the player's warp sign
     * @param playerUUID
     * @return Location of sign or null if one does not exist
     */
    public Location getWarp(UUID playerUUID) {
	return plugin.getWarp(playerUUID);
    }
    
    /**
     * Lists all the known warps. As each player can have only one warp, the player's UUID is used.
     * It can be displayed however you like to other users.
     * @return String set of warps
     */
    public Set<UUID> listWarps() {
	return plugin.listWarps();
    }
    
    /**
     * Get the owner of the warp at location
     * @param location
     * @return Returns name of player or empty string if there is no warp at that spot
     */
    public String getWarpOwner(Location location) {
 	return plugin.getWarpOwner(location);
    }
    
    /**
     * Checks if an online player is on their island, on a team island or on a coop island
     * 
     * @param player
     *            - the player who is being checked
     * @return - true if they are on their island, otherwise false
     */
    public boolean playerIsOnIsland(Player player) {
	return plugin.playerIsOnIsland(player);
    }
    
    /**
     * Determines if an island is at a location in this area
     * location. Also checks if the spawn island is in this area.
     * 
     * @param loc
     * @return
     */
    public boolean islandAtLocation(Location location) {
	return plugin.islandAtLocation(location);
    }
    
    /**
     * Checks to see if a player is trespassing on another player's island
     * Both players must be online.
     * @param owner - owner or team member of an island
     * @param target
     * @return true if they are on the island otherwise false.
     */
    public boolean isOnIsland(Player owner, Player target) {
	return plugin.isOnIsland(owner, target);
    }
    
    /**
     * Checks if a specific location is within the protected range of an island owned by the player
     * @param player
     * @param loc
     * @return
     */
    public boolean locationIsOnIsland(final Player player, final Location loc) {
	return plugin.locationIsOnIsland(player, loc);
    }
    
    /**
     * Finds out if location is within a set of island locations and returns the one that is there or null if not
     * @param islandTestLocations
     * @param loc
     * @return
     */
    public Location locationIsOnIsland(final Set<Location> islandTestLocations, final Location loc) {
	return plugin.locationIsOnIsland(islandTestLocations, loc);
    }
    
    /**
     * Sets all blocks in an island to a specified biome type
     * @param islandLoc
     * @param biomeType
     * @return true if the setting was successful
     */
    public boolean setIslandBiome(Location islandLoc, Biome biomeType) {
	return plugin.setIslandBiome(islandLoc, biomeType);
    }
    
    /**
     * Sets a message for the player to receive next time they login
     * @param player
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
	return plugin.setMessage(playerUUID, message);
    }
    
    /**
     * Sends a message to every player in the team that is offline
     * If the player is not in a team, nothing happens.
     * @param playerUUID
     * @param message
     */
    public void tellOfflineTeam(UUID playerUUID, String message) {
	plugin.tellOfflineTeam(playerUUID, message);
    }
    // Settings
    /**
     * @return the challengeList
     */
    public static Set<String> getChallengeList() {
        return Settings.challengeList;
    }

    /**
     * @return the waiverAmount
     */
    public static int getWaiverAmount() {
        return Settings.waiverAmount;
    }

    /**
     * @return the challengeLevels
     */
    public static List<String> getChallengeLevels() {
        return Settings.challengeLevels;
    }

    /**
     * @return the acidDamage
     */
    public static double getAcidDamage() {
        return Settings.acidDamage;
    }

    /**
     * @return the mobAcidDamage
     */
    public static double getMobAcidDamage() {
        return Settings.mobAcidDamage;
    }

    /**
     * @return the rainDamage
     */
    public static double getRainDamage() {
        return Settings.rainDamage;
    }

    /**
     * @return the resetWait
     */
    public static int getResetWait() {
        return Settings.resetWait;
    }

    /**
     * @return the resetLimit
     */
    public static int getResetLimit() {
        return Settings.resetLimit;
    }

    /**
     * @return the maxTeamSize
     */
    public static int getMaxTeamSize() {
        return Settings.maxTeamSize;
    }

    /**
     * @return the maxTeamSizeVIP
     */
    public static int getMaxTeamSizeVIP() {
        return Settings.maxTeamSizeVIP;
    }

    /**
     * @return the maxTeamSizeVIP2
     */
    public static int getMaxTeamSizeVIP2() {
        return Settings.maxTeamSizeVIP2;
    }

    /**
     * @return the worldName
     */
    public static String getWorldName() {
        return Settings.worldName;
    }

    /**
     * @return the monsterSpawnLimit
     */
    public static int getMonsterSpawnLimit() {
        return Settings.monsterSpawnLimit;
    }

    /**
     * @return the animalSpawnLimit
     */
    public static int getAnimalSpawnLimit() {
        return Settings.animalSpawnLimit;
    }

    /**
     * @return the waterAnimalSpawnLimit
     */
    public static int getWaterAnimalSpawnLimit() {
        return Settings.waterAnimalSpawnLimit;
    }

    /**
     * @return the allowPvP
     */
    public static boolean isAllowPvP() {
        return Settings.allowPvP;
    }

    /**
     * @return the allowBreakBlocks
     */
    public static boolean isAllowBreakBlocks() {
        return Settings.allowBreakBlocks;
    }

    /**
     * @return the allowPlaceBlocks
     */
    public static boolean isAllowPlaceBlocks() {
        return Settings.allowPlaceBlocks;
    }

    /**
     * @return the allowBedUse
     */
    public static boolean isAllowBedUse() {
        return Settings.allowBedUse;
    }

    /**
     * @return the allowBucketUse
     */
    public static boolean isAllowBucketUse() {
        return Settings.allowBucketUse;
    }

    /**
     * @return the allowShearing
     */
    public static boolean isAllowShearing() {
        return Settings.allowShearing;
    }

    /**
     * @return the allowEnderPearls
     */
    public static boolean isAllowEnderPearls() {
        return Settings.allowEnderPearls;
    }

    /**
     * @return the allowDoorUse
     */
    public static boolean isAllowDoorUse() {
        return Settings.allowDoorUse;
    }

    /**
     * @return the allowLeverButtonUse
     */
    public static boolean isAllowLeverButtonUse() {
        return Settings.allowLeverButtonUse;
    }

    /**
     * @return the allowCropTrample
     */
    public static boolean isAllowCropTrample() {
        return Settings.allowCropTrample;
    }

    /**
     * @return the allowChestAccess
     */
    public static boolean isAllowChestAccess() {
        return Settings.allowChestAccess;
    }

    /**
     * @return the allowFurnaceUse
     */
    public static boolean isAllowFurnaceUse() {
        return Settings.allowFurnaceUse;
    }

    /**
     * @return the allowRedStone
     */
    public static boolean isAllowRedStone() {
        return Settings.allowRedStone;
    }

    /**
     * @return the allowMusic
     */
    public static boolean isAllowMusic() {
        return Settings.allowMusic;
    }

    /**
     * @return the allowCrafting
     */
    public static boolean isAllowCrafting() {
        return Settings.allowCrafting;
    }

    /**
     * @return the allowBrewing
     */
    public static boolean isAllowBrewing() {
        return Settings.allowBrewing;
    }

    /**
     * @return the allowGateUse
     */
    public static boolean isAllowGateUse() {
        return Settings.allowGateUse;
    }

    /**
     * @return the allowHurtMobs
     */
    public static boolean isAllowHurtMobs() {
        return Settings.allowHurtMobs;
    }

    /**
     * @return the chestItems
     */
    public static ItemStack[] getChestItems() {
        return Settings.chestItems;
    }

    /**
     * @return the islandDistance
     */
    public static int getIslandDistance() {
        return Settings.islandDistance;
    }

    /**
     * @return the islandXOffset
     */
    public static int getIslandXOffset() {
        return Settings.islandXOffset;
    }

    /**
     * @return the islandZOffset
     */
    public static int getIslandZOffset() {
        return Settings.islandZOffset;
    }

    /**
     * @return the sea_level
     */
    public static int getSea_level() {
        return Settings.sea_level;
    }

    /**
     * @return the island_protectionRange
     */
    public static int getIsland_protectionRange() {
        return Settings.island_protectionRange;
    }

    /**
     * @return the abandonedIslandLevel
     */
    public static int getAbandonedIslandLevel() {
        return Settings.abandonedIslandLevel;
    }

    /**
     * @return the startingMoney
     */
    public static Double getStartingMoney() {
        return Settings.startingMoney;
    }

    /**
     * @return the netherSpawnRadius
     */
    public static double getNetherSpawnRadius() {
        return Settings.netherSpawnRadius;
    }

    /**
     * @return the acidDamageType
     */
    public static List<PotionEffectType> getAcidDamageType() {
        return Settings.acidDamageType;
    }

    /**
     * @return the resetMoney
     */
    public static boolean isResetMoney() {
        return Settings.resetMoney;
    }

    /**
     * @return the damageOps
     */
    public static boolean isDamageOps() {
        return Settings.damageOps;
    }

    /**
     * @return the endermanDeathDrop
     */
    public static boolean isEndermanDeathDrop() {
        return Settings.endermanDeathDrop;
    }

    /**
     * @return the allowEndermanGriefing
     */
    public static boolean isAllowEndermanGriefing() {
        return Settings.allowEndermanGriefing;
    }

    /**
     * @return the allowCreeperDamage
     */
    public static boolean isAllowCreeperDamage() {
        return Settings.allowCreeperDamage;
    }

    /**
     * @return the allowTNTDamage
     */
    public static boolean isAllowTNTDamage() {
        return Settings.allowTNTDamage;
    }

    /**
     * @return the allowSpawnEggs
     */
    public static boolean isAllowSpawnEggs() {
        return Settings.allowSpawnEggs;
    }

    /**
     * @return the allowBreeding
     */
    public static boolean isAllowBreeding() {
        return Settings.allowBreeding;
    }

    /**
     * @return the allowFire
     */
    public static boolean isAllowFire() {
        return Settings.allowFire;
    }

    /**
     * @return the allowChestDamage
     */
    public static boolean isAllowChestDamage() {
        return Settings.allowChestDamage;
    }

    /**
     * @return the allowLeashUse
     */
    public static boolean isAllowLeashUse() {
        return Settings.allowLeashUse;
    }

    /**
     * @return the allowHurtMonsters
     */
    public static boolean isAllowHurtMonsters() {
        return Settings.allowHurtMonsters;
    }

    /**
     * @return the allowEnchanting
     */
    public static boolean isAllowEnchanting() {
        return Settings.allowEnchanting;
    }

    /**
     * @return the allowAnvilUse
     */
    public static boolean isAllowAnvilUse() {
        return Settings.allowAnvilUse;
    }

    /**
     * @return the allowVisitorKeepInvOnDeath
     */
    public static boolean isAllowVisitorKeepInvOnDeath() {
        return Settings.allowVisitorKeepInvOnDeath;
    }

    /**
     * @return the allowVisitorItemPickup
     */
    public static boolean isAllowVisitorItemPickup() {
        return Settings.allowVisitorItemPickup;
    }

    /**
     * @return the allowVisitorItemDrop
     */
    public static boolean isAllowVisitorItemDrop() {
        return Settings.allowVisitorItemDrop;
    }

    /**
     * @return the allowArmorStandUse
     */
    public static boolean isAllowArmorStandUse() {
        return Settings.allowArmorStandUse;
    }

    /**
     * @return the logInRemoveMobs
     */
    public static boolean isLogInRemoveMobs() {
        return Settings.logInRemoveMobs;
    }

    /**
     * @return the islandRemoveMobs
     */
    public static boolean isIslandRemoveMobs() {
        return Settings.islandRemoveMobs;
    }

    /**
     * @return the island_level
     */
    public static int getIsland_level() {
        return Settings.island_level;
    }

    /**
     * @return the resetChallenges
     */
    public static boolean isResetChallenges() {
        return Settings.resetChallenges;
    }

    /**
     * @return the allowSpawnDoorUse
     */
    public static boolean isAllowSpawnDoorUse() {
        return Settings.allowSpawnDoorUse;
    }

    /**
     * @return the allowSpawnLeverButtonUse
     */
    public static boolean isAllowSpawnLeverButtonUse() {
        return Settings.allowSpawnLeverButtonUse;
    }

    /**
     * @return the allowSpawnChestAccess
     */
    public static boolean isAllowSpawnChestAccess() {
        return Settings.allowSpawnChestAccess;
    }

    /**
     * @return the allowSpawnFurnaceUse
     */
    public static boolean isAllowSpawnFurnaceUse() {
        return Settings.allowSpawnFurnaceUse;
    }

    /**
     * @return the allowSpawnRedStone
     */
    public static boolean isAllowSpawnRedStone() {
        return Settings.allowSpawnRedStone;
    }

    /**
     * @return the allowSpawnMusic
     */
    public static boolean isAllowSpawnMusic() {
        return Settings.allowSpawnMusic;
    }

    /**
     * @return the allowSpawnCrafting
     */
    public static boolean isAllowSpawnCrafting() {
        return Settings.allowSpawnCrafting;
    }

    /**
     * @return the allowSpawnBrewing
     */
    public static boolean isAllowSpawnBrewing() {
        return Settings.allowSpawnBrewing;
    }

    /**
     * @return the allowSpawnGateUse
     */
    public static boolean isAllowSpawnGateUse() {
        return Settings.allowSpawnGateUse;
    }

    /**
     * @return the allowSpawnMobSpawn
     */
    public static boolean isAllowSpawnMobSpawn() {
        return Settings.allowSpawnMobSpawn;
    }

    /**
     * @return the allowSpawnNoAcidWater
     */
    public static boolean isAllowSpawnNoAcidWater() {
        return Settings.allowSpawnNoAcidWater;
    }

    /**
     * @return the allowSpawnEnchanting
     */
    public static boolean isAllowSpawnEnchanting() {
        return Settings.allowSpawnEnchanting;
    }

    /**
     * @return the allowSpawnAnvilUse
     */
    public static boolean isAllowSpawnAnvilUse() {
        return Settings.allowSpawnAnvilUse;
    }

    /**
     * @return the blockLimits
     */
    public static HashMap<Material, Integer> getBlockLimits() {
        return Settings.blockLimits;
    }

    /**
     * @return the blockValues
     */
    public static HashMap<Material, Integer> getBlockValues() {
        return Settings.blockValues;
    }

    /**
     * @return the broadcastMessages
     */
    public static boolean isBroadcastMessages() {
        return Settings.broadcastMessages;
    }

    /**
     * @return the createNether
     */
    public static boolean isCreateNether() {
        return Settings.createNether;
    }

    /**
     * @return the clearInventory
     */
    public static boolean isClearInventory() {
        return Settings.clearInventory;
    }

    /**
     * @return the useControlPanel
     */
    public static boolean isUseControlPanel() {
        return Settings.useControlPanel;
    }

    /**
     * @return the allowTeleportWhenFalling
     */
    public static boolean isAllowTeleportWhenFalling() {
        return Settings.allowTeleportWhenFalling;
    }

    /**
     * @return the biomeCost
     */
    public static double getBiomeCost() {
        return Settings.biomeCost;
    }

    /**
     * @return the defaultBiome
     */
    public static Biome getDefaultBiome() {
        return Settings.defaultBiome;
    }

    /**
     * @return the resetCommands
     */
    public static List<String> getResetCommands() {
        return Settings.resetCommands;
    }

    /**
     * @return the breedingLimit
     */
    public static int getBreedingLimit() {
        return Settings.breedingLimit;
    }

    /**
     * @return the removeCompleteOntimeChallenges
     */
    public static boolean isRemoveCompleteOntimeChallenges() {
        return Settings.removeCompleteOntimeChallenges;
    }

    /**
     * @return the addCompletedGlow
     */
    public static boolean isAddCompletedGlow() {
        return Settings.addCompletedGlow;
    }

    /**
     * @return the newNether
     */
    public static boolean isNewNether() {
        return Settings.newNether;
    }

    /**
     * @return the animalAcidDamage
     */
    public static double getAnimalAcidDamage() {
        return Settings.animalAcidDamage;
    }

    /**
     * @return the damageChickens
     */
    public static boolean isDamageChickens() {
        return Settings.damageChickens;
    }

    /**
     * @return the useEconomy
     */
    public static boolean isUseEconomy() {
        return Settings.useEconomy;
    }

    /**
     * @return the schematics
     */
    public static HashMap<String, String> getSchematics() {
        return Settings.schematics;
    }

    /**
     * @return the inviteWait
     */
    public static int getInviteWait() {
        return Settings.inviteWait;
    }
    

  
} 
