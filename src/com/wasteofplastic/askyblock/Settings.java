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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;

import com.wasteofplastic.askyblock.Island.Flags;

/**
 * Where all the settings are
 * 
 * @author tastybento
 */
public class Settings {
    // Constants
    // Game Type ASKYBLOCK or ACIDISLAND
    public enum GameType {
        ASKYBLOCK, ACIDISLAND
    }
    /*
    public final static GameType GAMETYPE = GameType.ACIDISLAND;
    // The spawn command (Essentials spawn for example)
    public final static String SPAWNCOMMAND = "spawn";
    // Permission prefix
    public final static String PERMPREFIX = "acidisland.";
    // The island command
    public final static String ISLANDCOMMAND = "ai";
    // The challenge command
    public static final String CHALLENGECOMMAND = "aic";
    // Admin command
    public static final String ADMINCOMMAND = "acid";
    */
    public final static GameType GAMETYPE = GameType.ASKYBLOCK;
    // Permission prefix
    public final static String PERMPREFIX = "askyblock.";
    // The island command
    public final static String ISLANDCOMMAND = "island";
    // The challenge command
    public static final String CHALLENGECOMMAND = "asc";
    // The spawn command (Essentials spawn for example)
    public final static String SPAWNCOMMAND = "spawn";
    // Admin command
    public static final String ADMINCOMMAND = "asadmin";

    public static Set<String> challengeList;
    public static int waiverAmount;
    public static List<String> challengeLevels;
    public static double acidDamage;
    public static double mobAcidDamage;
    public static double rainDamage;
    public static int resetWait;
    public static int resetLimit;
    public static int maxTeamSize;
    public static String worldName;
    public static int monsterSpawnLimit;
    public static int animalSpawnLimit;
    public static int waterAnimalSpawnLimit;
    // IslandGuard settings
    public static HashMap<Flags, Boolean> defaultIslandSettings = new HashMap<Flags, Boolean>();
    public static boolean allowAnvilUse;
    public static boolean allowArmorStandUse;
    public static boolean allowBeaconAccess;
    public static boolean allowBedUse;
    public static boolean allowBreakBlocks;
    public static boolean allowBreeding;
    public static boolean allowBrewing;
    public static boolean allowBucketUse;
    public static boolean allowChestAccess;
    public static boolean allowCrafting;
    public static boolean allowCropTrample;
    public static boolean allowDoorUse;
    public static boolean allowEnchanting;
    public static boolean allowEnderPearls;
    public static boolean allowFurnaceUse;
    public static boolean allowGateUse;
    public static boolean allowHorseInvAccess;
    public static boolean allowHorseRiding;
    public static boolean allowHurtMobs;
    public static boolean allowLeashUse;
    public static boolean allowLeverButtonUse;
    public static boolean allowMusic;
    public static boolean allowPlaceBlocks;
    public static boolean allowPortalUse;
    public static boolean allowPressurePlate;
    public static boolean allowPvP;
    public static boolean allowNetherPvP;
    public static boolean allowRedStone;
    public static boolean allowShearing;
    public static boolean allowVillagerTrading;
    public static boolean allowChorusFruit;
    public static boolean enableJoinAndLeaveIslandMessages;
    public static boolean allowMobSpawning;

    // System settings
    public static boolean allowChestDamage;
    public static boolean allowCreeperDamage;
    public static boolean allowCreeperGriefing;
    public static boolean allowEndermanGriefing;
    public static boolean allowFire;
    public static boolean allowFireSpread;
    public static boolean allowHurtMonsters;
    public static boolean allowMonsterEggs;
    public static boolean allowPistonPush;
    public static boolean allowTNTDamage;
    public static boolean allowVisitorItemDrop;
    public static boolean allowVisitorItemPickup;
    public static boolean allowVisitorKeepInvOnDeath;
    public static boolean restrictWither;

    public static ItemStack[] chestItems;
    public static int islandDistance;
    public static int islandXOffset;
    public static int islandZOffset;
    public static int sea_level;
    public static int island_protectionRange;
    public static int abandonedIslandLevel;
    public static Double startingMoney;
    public static double netherSpawnRadius;
    public static List<PotionEffectType> acidDamageType = new ArrayList<PotionEffectType>();
    public static boolean resetMoney;
    public static boolean damageOps;
    public static boolean endermanDeathDrop;
    
    // Invincible visitor
    public static boolean invincibleVisitors;
    public static HashSet<DamageCause> visitorDamagePrevention;

    // public static boolean ultraSafeBoats;
    public static boolean logInRemoveMobs;
    public static boolean islandRemoveMobs;
    public static int island_level;
    public static boolean resetChallenges;
    // Spawn fields
    public static boolean allowSpawnDoorUse;
    public static boolean allowSpawnLeverButtonUse;
    public static boolean allowSpawnChestAccess;
    public static boolean allowSpawnFurnaceUse;
    public static boolean allowSpawnRedStone;
    public static boolean allowSpawnMusic;
    public static boolean allowSpawnCrafting;
    public static boolean allowSpawnBrewing;
    public static boolean allowSpawnGateUse;
    public static boolean allowSpawnMobSpawn;
    public static boolean allowSpawnNoAcidWater;
    public static boolean allowSpawnEnchanting;
    public static boolean allowSpawnAnvilUse;
    public static boolean allowSpawnBeaconAccess;
    public static boolean allowSpawnAnimalSpawn;
    public static boolean allowSpawnAnimalKilling;
    public static boolean allowSpawnMobKilling;
    public static boolean allowSpawnMilking;
    public static boolean allowSpawnBreakBlocks;
    public static boolean allowSpawnPlaceBlocks;
    public static boolean allowSpawnEggs;
    public static boolean allowSpawnPVP;
    public static boolean allowSpawnLavaCollection;
    public static boolean allowSpawnWaterCollection;
    public static boolean allowSpawnMonsterEggs;
    public static boolean allowSpawnHorseInvAccess;
    public static boolean allowSpawnHorseRiding;
    public static boolean allowSpawnPressurePlate;
    public static boolean allowSpawnVisitorItemDrop;
    public static boolean allowSpawnVisitorItemPickup;
    public static boolean allowSpawnArmorStandUse;
    public static boolean allowSpawnBedUse;
    public static boolean allowSpawnBreeding;
    public static boolean allowSpawnCropTrample;
    public static boolean allowSpawnEnderPearls;
    public static boolean allowSpawnLeashUse;
    public static boolean allowSpawnShearing;
    public static boolean allowSpawnChorusFruit;

    // Levels
    public static HashMap<MaterialData, Integer> blockLimits;
    public static HashMap<MaterialData, Integer> blockValues;
    public static boolean fastLevelCalc;

    // Challenge completion broadcast
    public static boolean broadcastMessages;
    // Nether world
    public static boolean createNether;
    public static boolean clearInventory;
    // Use control panel for /island
    public static boolean useControlPanel;
    // Prevent /island when falling
    public static boolean allowTeleportWhenFalling;
    // Biomes
    public static double biomeCost;
    public static Biome defaultBiome;

    // Island reset commands
    public static List<String> resetCommands = new ArrayList<String>();
    // Mob limits
    public static int breedingLimit;
    
    // Console shows teamchat messages
    public static boolean logTeamChat;

    // Challenges - show or remove completed on-time challenges
    public static boolean removeCompleteOntimeChallenges;
    public static boolean addCompletedGlow;

    // New nether
    public static boolean newNether;

    // Animal Damage
    public static double animalAcidDamage;
    public static boolean damageChickens;

    // Use Economy
    public static boolean useEconomy;
    
    // Use Minishop
    public static boolean useMinishop;

    // Wait between being invited to same team island
    public static int inviteWait;

    // Use physics when pasting schematic blocks
    public static boolean usePhysics;
    
    // Use old display (chat instead of GUI) for Island top ten
    public static boolean displayIslandTopTenInChat;

    // Need a certain amount of island levels to create a warp sign
    public static int warpLevelsRestriction;
    
    // Magic Cobble Generator
    public static boolean useMagicCobbleGen;
    public static TreeMap<Integer,TreeMap<Double,Material>> magicCobbleGenChances;
    
    // Disable offline redstone
    public static boolean disableOfflineRedstone;
    
    // Fancy island level display
    public static boolean fancyIslandLevelDisplay;
    
    // Falling blocked commands
    public static List<String> fallingCommandBlockList;
    public static List<String> leaveCommands;
    public static int levelWait;
    public static long resetConfirmWait;
    public static boolean loginLevel;
    public static boolean resetEnderChest;
    public static EntityType islandCompanion;
    public static boolean updateCheck;
    public static List<String> companionNames;
    public static long islandStartX;
    public static long islandStartZ;
    public static int maxHomes;
    public static boolean immediateTeleport;
    public static boolean makeIslandIfNone;
    public static boolean setTeamName;
    public static int debug;
    public static boolean useSchematicPanel;
    public static boolean chooseIslandRandomly;
    public static double underWaterMultiplier;
    public static String teamSuffix;
    public static int levelCost;
    public static boolean respawnOnIsland;
    public static boolean netherTrees;
    public static int maxTeamSizeVIP;
    public static int maxTeamSizeVIP2;
    public static boolean teamChat;
    public static List<String> startCommands;
    public static boolean useWarpPanel;
    public static List<EntityType> mobWhiteList = new ArrayList<EntityType>();
    public static int villagerLimit;
    public static int mobLimit;
    public static int hopperLimit;
    public static List<String> visitorCommandBlockList;
    public static boolean muteDeathMessages;
    public static int maxIslands;
    public static HashMap<String,Integer> limitedBlocks;
    public static long pvpRestartCooldown;
    public static long backupDuration;
    public static boolean cleanUpBlocks;
    public static boolean acidBottle;
    public static boolean useOwnGenerator;
    public static List<String> freeLevels = new ArrayList<String>();
    public static int cleanRate;
    public static boolean allowPushing;
    public static boolean recoverSuperFlat;
    protected static boolean levelLogging;
    public static boolean persistantCoops;
    public static boolean allowSpawnCreeperPain;
    public static List<String> teamStartCommands;
    public static int minNameLength;
    public static int maxNameLength;
    public static int deathpenalty;
    public static boolean sumTeamDeaths;
    public static int maxDeaths;
    public static boolean islandResetDeathReset;
    public static boolean teamJoinDeathReset;
    public static boolean allowAutoActivator;
    public static boolean netherRoof;
    public static boolean allowSpawnVillagerTrading;
    public static String chatLevelPrefix;
    public static String chatChallengeLevelPrefix;
    public static String chatIslandPlayer;
    public static boolean allowObsidianScooping;
    public static boolean allowFireExtinguish;
    public static boolean allowSpawnFireExtinguish;
    public static boolean allowMobDamageToItemFrames;
    public static boolean kickedKeepInv;
}
