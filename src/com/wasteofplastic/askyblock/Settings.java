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

import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Messages.HistoryMessageType;
import com.wasteofplastic.askyblock.util.ASBParser.ASBSound;

/**
 * Where all the settings are
 * 
 * @author tastybento
 */
public class Settings {
	
    /*		CONSTANTS		*/
	
    // Game Type ASKYBLOCK or ACIDISLAND
    public enum GameType {ASKYBLOCK, ACIDISLAND}
    
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

    /*		MAIN SETTINGS		*/
    public static boolean updateCheck;
    public static boolean metrics;
    
    public static long backupDuration;
    
    public static boolean purgeRemoveUserfiles;
    
    public static String worldName;
    public static String defaultLanguage;
    
    public static int maxIslands;
    public static int islandDistance;
    public static int islandProtectionRange;
    public static int islandXOffset;
    public static int islandZOffset;
    public static int islandHeight;
    public static int seaHeight;

    public static double startingMoney;
    public static boolean useEconomy;
    public static boolean useMinishop;
    public static boolean allowGlobalMinishop;
    
    public static boolean recoverSuperFlat;
    
    public static boolean disableOfflineRedstone;
    
    public static ItemStack[] chestItems;
    
    public static boolean useWarpPanel;
    public static boolean useControlPanel;

    public static boolean useMagicCobbleGen;
    public static boolean magicCobbleGenOnlyAtSpawn;
    public static TreeMap<Integer,TreeMap<Double,Material>> magicCobbleGenChances;
    
    /*		PROTECTION		*/
    public static HashMap<SettingsFlag, Boolean> defaultWorldSettings = new HashMap<SettingsFlag, Boolean>();
    public static HashMap<SettingsFlag, Boolean> defaultIslandSettings = new HashMap<SettingsFlag, Boolean>();
    public static HashMap<SettingsFlag, Boolean> defaultSpawnSettings = new HashMap<SettingsFlag, Boolean>();
    
    //System settings
    public static boolean allowAutoActivator;
    public static boolean allowChestDamage;
    public static boolean allowCreeperGriefing;
    public static boolean allowEndermanGriefing;
    public static boolean allowMobDamageToItemFrames;
    public static boolean allowObsidianScooping;
    public static boolean allowPistonPush;
    public static boolean allowPushing;
    public static boolean allowVisitorKeepInvOnDeath;
    public static boolean endermanDeathDrop;
    public static boolean restrictWither;
    
    //Nether
    public static double netherSpawnRadius;
    
    //Visitors
    public static HashMap<SettingsFlag, Boolean> visitorSettings = new HashMap<SettingsFlag, Boolean>();
    public static boolean invincibleVisitors;
    public static HashSet<DamageCause> visitorDamagePrevention;
    public static List<String> visitorCommandBlockList;
    
    public static long pvpRestartCooldown;
    
    /*		ISLAND CHANGING		*/
    public static boolean resetChallenges;
    public static boolean resetMoney;
    public static boolean resetEnderChest;
    public static boolean clearInventory;
    
    public static List<String> startCommands;
    public static List<String> teamStartCommands;
    public static List<String> leaveCommands;
    public static List<String> resetCommands;

    public static int resetLimit;
    public static int resetWait;
    public static int inviteWait;
    
    public static boolean leaversLoseReset;
    public static boolean kickedKeepInv;
    
    public static boolean immediateTeleport;
    public static boolean makeIslandIfNone;
    public static boolean useSchematicPanel;
    public static boolean chooseIslandRandomly;
    
    public static int abandonedIslandLevel;
    
    public static boolean confirmKick;
    public static long confirmKickWait;
    public static boolean confirmLeave;
    public static long confirmLeaveWait;
    public static boolean confirmRestart;
    public static long confirmRestartWait;
    
    /*		ENTITIES		*/
    public static HashMap<EntityType, Integer> entityLimits;
    public static int monsterSpawnLimit;
    public static int animalSpawnLimit;
    public static int waterAnimalSpawnLimit;
    public static int breedingLimit;
    public static int villagerLimit;
    
    public static HashMap<String,Integer> limitedBlocks;
    public static int hopperLimit;
    
    public static List<EntityType> mobWhiteList = new ArrayList<EntityType>();
    public static boolean logInRemoveMobs;
    public static boolean islandRemoveMobs;
    
    public static boolean hackSkeletonSpawners;
    
    /*		NETHER		*/
    public static boolean createNether;
    
    public static boolean newNether;
    public static boolean netherRoof;
    public static boolean netherTrees;

    /*		CHAT		*/
    public static boolean fancyIslandLevelDisplay;
    public static boolean teamChat;
    public static boolean logTeamChat;
    public static boolean teamChatIncludeCoop;
    public static boolean muteDeathMessages;
    public static boolean showInActionBar;
    
    public static List<HistoryMessageType> historyMessagesTypes;
    
    /*		ISLAND		*/
    public static int maxTeamSize;
    public static int maxTeamSizeVIP;
    public static int maxTeamSizeVIP2;
    
    public static EntityType islandCompanion;
    public static List<String> companionNames;
    
    public static boolean persistantCoops;
    public static boolean onlyLeaderCanCoop;
    
    public static int flyTimeOutside;
    public static List<String> temporaryPermissions;
    
    public static int maxHomes;
        
    //Island name
    public static int minNameLength;
    public static int maxNameLength;
    
    public static List<String> fallingCommandBlockList;
    public static boolean allowTeleportWhenFalling;
    
    public static boolean respawnOnIsland;
    
    public static ASBSound warpSound;
    
    /*		ACID		*/
    public static double acidDamage;
    public static List<PotionEffectType> acidDamageType = new ArrayList<PotionEffectType>();
    public static double rainDamage;
    
    public static boolean damageOps;
    public static boolean helmetProtection;
    public static boolean fullArmorProtection;
    
    public static double mobAcidDamage;
    public static double animalAcidDamage;
    public static boolean damageChickens;
    public static long acidItemDestroyTime;
    
    public static List<String> acidCommandBlockList;
    public static boolean allowTeleportWhenInAcid;
    
    /*		CHALLENGES		*/
    public static List<String> challengeLevels;
    public static List<String> freeLevels = new ArrayList<String>();
    public static Set<String> challengeList;
    
    public static int waiverAmount;
    
    public static boolean removeCompleteOntimeChallenges;
    public static boolean addCompletedGlow;
    
    public static boolean broadcastMessages;
    
    /*		LEVELS		*/
    public static int levelCost;
    public static HashMap<MaterialData, Integer> blockLimits;
    public static HashMap<MaterialData, Integer> blockValues;
    
    public static int levelWait;

    public static boolean loginLevel;
    
    public static double underWaterMultiplier;
    
    public static int deathpenalty;
    public static boolean sumTeamDeaths;
    public static int maxDeaths;
    public static boolean islandResetDeathReset;
    public static boolean teamJoinDeathReset;
    
    public static boolean displayIslandTopTenInChat;

    public static int warpLevelsRestriction;
    
    /*		BIOMES		*/
    public static double biomeCost;
    public static Biome defaultBiome;
    
    /*		"SECRET" SETTINGS		*/
    protected static boolean levelLogging;
    public static boolean useOwnGenerator;
    public static boolean acidBottle;
    // Use physics when pasting schematic blocks
    public static boolean usePhysics;
    
    public static int cleanRate;
    
    public static long islandStartX;
    public static long islandStartZ;
    
    //=======================================================    
    
    //public static boolean ultraSafeBoats;
    //public static boolean allowSpawnCreeperPain;
    //public static boolean allowSpawnVillagerTrading;
    //public static boolean allowSpawnFireExtinguish;

}
