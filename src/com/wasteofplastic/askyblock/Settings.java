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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * @author tastybento
 * Where all the settings are
 */
public class Settings {
    // Constants
    // Game Type ASKYBLOCK or ACIDISLAND
    protected enum GameType {
	ASKYBLOCK, ACIDISLAND
	}
    protected final static GameType GAMETYPE = GameType.ASKYBLOCK;
    // Permission prefix
    protected final static String PERMPREFIX = "askyblock.";
    // The island command
    protected final static String ISLANDCOMMAND = "island";
    // The challenge command
    protected static final String CHALLENGECOMMAND = "asc";
    // The spawn command (Essentials spawn for example)
    protected final static String SPAWNCOMMAND = "spawn";    

    /* Acid Island
     *
    // Permission prefix
    protected final static String PERMPREFIX = "acidisland.";
    // The island command
    protected final static String ISLANDCOMMAND = "ai";
    // The challenge command
    protected static final String CHALLENGECOMMAND = "aic";
     */
  
    protected static Set<String> challengeList;
    protected static int waiverAmount;
    protected static List<String> challengeLevels;
    protected static double acidDamage;
    protected static double mobAcidDamage;   
    protected static double rainDamage;
    protected static int resetWait;
    protected static int resetLimit;
    protected static int maxTeamSize;
    protected static int maxTeamSizeVIP;
    protected static int maxTeamSizeVIP2;
    protected static String worldName;
    protected static int monsterSpawnLimit;
    protected static int animalSpawnLimit;
    protected static int waterAnimalSpawnLimit;
    // IslandGuard settings
    protected static boolean allowPvP;
    protected static boolean allowBreakBlocks;
    protected static boolean allowPlaceBlocks;
    protected static boolean allowBedUse;
    protected static boolean allowBucketUse;
    protected static boolean allowShearing;
    protected static boolean allowEnderPearls;
    protected static boolean allowDoorUse;
    protected static boolean allowLeverButtonUse;
    protected static boolean allowCropTrample;
    protected static boolean allowChestAccess;
    protected static boolean allowFurnaceUse;
    protected static boolean allowRedStone;
    protected static boolean allowMusic;
    protected static boolean allowCrafting;
    protected static boolean allowBrewing;
    protected static boolean allowGateUse;
    protected static boolean allowHurtMobs;
    
    protected static ItemStack[] chestItems;
    protected static int islandDistance;
    protected static int islandXOffset;
    protected static int islandZOffset;
    protected static int sea_level;
    protected static int island_protectionRange;
    protected static int abandonedIslandLevel;
    protected static Double startingMoney;
    protected static double netherSpawnRadius;
    protected static List<PotionEffectType> acidDamageType = new ArrayList<PotionEffectType>();
    protected static boolean resetMoney;
    protected static boolean damageOps;
    protected static boolean endermanDeathDrop;
    protected static boolean allowEndermanGriefing;
    protected static boolean allowCreeperDamage;
    protected static boolean allowTNTDamage;
    protected static boolean allowMonsterEggs;
    protected static boolean allowBreeding;
    protected static boolean allowFire;
    protected static boolean allowChestDamage;
    protected static boolean allowLeashUse;
    protected static boolean allowHurtMonsters;
    protected static boolean allowEnchanting;
    protected static boolean allowAnvilUse;
    protected static boolean allowVisitorKeepInvOnDeath;
    protected static boolean allowVisitorItemPickup;
    protected static boolean allowVisitorItemDrop;
    protected static boolean allowArmorStandUse;
    protected static boolean allowBeaconAccess;
    protected static boolean allowPortalUse;
    
    //protected static boolean ultraSafeBoats;
    protected static boolean logInRemoveMobs;
    protected static boolean islandRemoveMobs;
    protected static int island_level;
    protected static boolean resetChallenges;
    // Spawn fields
    protected static boolean allowSpawnDoorUse;
    protected static boolean allowSpawnLeverButtonUse;
    protected static boolean allowSpawnChestAccess;
    protected static boolean allowSpawnFurnaceUse;
    protected static boolean allowSpawnRedStone;
    protected static boolean allowSpawnMusic;
    protected static boolean allowSpawnCrafting;
    protected static boolean allowSpawnBrewing;
    protected static boolean allowSpawnGateUse;
    protected static boolean allowSpawnMobSpawn;
    protected static boolean allowSpawnNoAcidWater;
    protected static boolean allowSpawnEnchanting;
    protected static boolean allowSpawnAnvilUse;
    protected static boolean allowSpawnBeaconAccess;
    protected static boolean allowSpawnAnimalSpawn;
    protected static boolean allowSpawnAnimalKilling;
    protected static boolean allowSpawnMobKilling;
    
    // Levels
    protected static HashMap<Material,Integer> blockLimits;
    protected static HashMap<Material,Integer> blockValues;
    
   // Challenge completion broadcast
    protected static boolean broadcastMessages;
    // Nether world
    protected static boolean createNether;
    protected static boolean clearInventory;
    // Use control panel for /island
    protected static boolean useControlPanel;
    // Prevent /island when falling
    protected static boolean allowTeleportWhenFalling;
    // Biomes
    protected static double biomeCost;
    protected static Biome defaultBiome;
    
    // Island reset commands
    protected static List<String> resetCommands = new ArrayList<String>();
    // Mob limits
    protected static int breedingLimit;
    
    // Challenges - show or remove completed on-time challenges
    protected static boolean removeCompleteOntimeChallenges;
    protected static boolean addCompletedGlow;
    
    // New nether
    protected static boolean newNether;
    
    // Animal Damage
    protected static double animalAcidDamage;
    protected static boolean damageChickens;
    
    // Use Economy
    protected static boolean useEconomy;
    
    // Schematic list (permission, filename)
    protected static HashMap<String,String> schematics = new HashMap<String,String>();
    
    // Wait between being invited to same team island
    protected static int inviteWait;
    
    // Use physics when pasting schematic blocks
    protected static boolean usePhysics;

    // Falling blocked commands
    protected static List<String> fallingCommandBlockList;
    public static List<String> leaveCommands;
    public static int levelWait;
    public static long resetConfirmWait;
    public static boolean allowSpawnMonsterEggs;
    public static boolean loginLevel;
    public static boolean allowSpawnBreakBlocks;
    public static boolean allowSpawnPlaceBlocks;
    public static boolean allowSpawnEggs;
    public static boolean resetEnderChest;
    protected static EntityType islandCompanion;
    public static boolean updateCheck;

}