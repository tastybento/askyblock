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
    /**
     * Default world protection settings
     */
    public static HashMap<SettingsFlag, Boolean> defaultWorldSettings = new HashMap<SettingsFlag, Boolean>();

    /**
     * Default island protection settings
     */
    public static HashMap<SettingsFlag, Boolean> defaultIslandSettings = new HashMap<SettingsFlag, Boolean>();
    /**
     * Default spawn protection settings
     */
    public static HashMap<SettingsFlag, Boolean> defaultSpawnSettings = new HashMap<SettingsFlag, Boolean>();
    /**
     * Visitors settings to show in the GUI
     */
    public static HashMap<SettingsFlag, Boolean> visitorSettings = new HashMap<SettingsFlag, Boolean>();
    // Flymode
    public static int flyTimeOutside;
    
    // Temporary Permissions
    public static List<String> temporaryPermissions;

    // System settings
    public static boolean allowChestDamage;
    public static boolean allowCreeperDamage;
    public static boolean allowCreeperGriefing;
    public static boolean allowEndermanGriefing;
    public static boolean allowPistonPush;
    public static boolean allowTNTDamage;
    public static boolean allowVisitorKeepInvOnDeath;
    public static boolean restrictWither;

    public static ItemStack[] chestItems;
    public static int islandDistance;
    public static int islandXOffset;
    public static int islandZOffset;
    public static int seaHeight;
    public static int islandProtectionRange;
    public static int abandonedIslandLevel;
    public static Double startingMoney;
    public static double netherSpawnRadius;
    public static List<PotionEffectType> acidDamageType = new ArrayList<PotionEffectType>();
    public static boolean resetMoney;
    public static boolean damageOps;
    public static boolean endermanDeathDrop;
    public static boolean onlyLeaderCanCoop;
    
    // Invincible visitor
    public static boolean invincibleVisitors;
    public static HashSet<DamageCause> visitorDamagePrevention;

    // public static boolean ultraSafeBoats;
    public static boolean logInRemoveMobs;
    public static boolean islandRemoveMobs;
    public static int islandHeight;
    public static boolean resetChallenges;

    // Levels
    public static HashMap<MaterialData, Integer> blockLimits;
    public static HashMap<MaterialData, Integer> blockValues;

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
    public static boolean magicCobbleGenOnlyAtSpawn;
    public static TreeMap<Long,TreeMap<Double,Material>> magicCobbleGenChances;
    
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
    public static int hopperLimit;
    public static List<String> visitorCommandBlockList;
    public static boolean muteDeathMessages;
    public static int maxIslands;
    public static HashMap<String,Integer> limitedBlocks;
    public static long pvpRestartCooldown;
    public static long backupDuration;
    public static boolean acidBottle;
    public static boolean useOwnGenerator;
    public static List<String> freeLevels = new ArrayList<String>();
    public static int cleanRate;
    public static boolean allowPushing;
    public static boolean recoverSuperFlat;
    protected static boolean levelLogging;
    public static boolean persistantCoops;
    //public static boolean allowSpawnCreeperPain;
    public static List<String> teamStartCommands;
    public static int minNameLength;
    public static int maxNameLength;
    public static int deathpenalty;
    public static boolean sumTeamDeaths;
    public static int maxDeaths;
    public static boolean islandResetDeathReset;
    public static boolean teamJoinDeathReset;
    public static List<String> allowedFakePlayers;
    public static boolean netherRoof;
    //public static boolean allowSpawnVillagerTrading;
    public static String chatLevelPrefix;
    public static String chatChallengeLevelPrefix;
    public static String chatIslandPlayer;
    public static boolean allowObsidianScooping;
    public static boolean allowFireExtinguish;
    //public static boolean allowSpawnFireExtinguish;
    public static boolean allowMobDamageToItemFrames;
    public static boolean kickedKeepInv;
    public static boolean hackSkeletonSpawners;
    public static HashMap<EntityType, Integer> entityLimits;
    public static long acidItemDestroyTime;
    public static boolean helmetProtection;
    public static boolean fullArmorProtection;
    public static String defaultLanguage;
    public static boolean showInActionBar;
    public static boolean leaversLoseReset;
    public static int maxPurge;
    public static boolean allowTNTPushing;
    public static boolean silenceCommandFeedback;
    public static long inviteTimeout;
    public static boolean warpHeads;
    public static boolean saveEntities;
}
