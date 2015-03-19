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

    public final static GameType GAMETYPE = GameType.ASKYBLOCK;
    // Permission prefix
    public final static String PERMPREFIX = "askyblock.";
    // The island command
    public final static String ISLANDCOMMAND = "island";
    // The challenge command
    public static final String CHALLENGECOMMAND = "asc";
    // The spawn command (Essentials spawn for example)
    public final static String SPAWNCOMMAND = "spawn";

    /*
     * AcidIsland
     * // Permission prefix
     * public final static String PERMPREFIX = "acidisland.";
     * // The island command
     * public final static String ISLANDCOMMAND = "ai";
     * // The challenge command
     * public static final String CHALLENGECOMMAND = "aic";
     */

    public static Set<String> challengeList;
    public static int waiverAmount;
    public static List<String> challengeLevels;
    public static double acidDamage;
    public static double mobAcidDamage;
    public static double rainDamage;
    public static int resetWait;
    public static int resetLimit;
    public static int maxTeamSize;
    public static int maxTeamSizeVIP;
    public static int maxTeamSizeVIP2;
    public static String worldName;
    public static int monsterSpawnLimit;
    public static int animalSpawnLimit;
    public static int waterAnimalSpawnLimit;
    // IslandGuard settings
    public static boolean allowPvP;
    public static boolean allowBreakBlocks;
    public static boolean allowPlaceBlocks;
    public static boolean allowBedUse;
    public static boolean allowBucketUse;
    public static boolean allowShearing;
    public static boolean allowEnderPearls;
    public static boolean allowDoorUse;
    public static boolean allowLeverButtonUse;
    public static boolean allowCropTrample;
    public static boolean allowChestAccess;
    public static boolean allowFurnaceUse;
    public static boolean allowRedStone;
    public static boolean allowMusic;
    public static boolean allowCrafting;
    public static boolean allowBrewing;
    public static boolean allowGateUse;
    public static boolean allowHurtMobs;

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
    public static boolean allowEndermanGriefing;
    public static boolean allowCreeperDamage;
    public static boolean allowTNTDamage;
    public static boolean allowMonsterEggs;
    public static boolean allowBreeding;
    public static boolean allowFire;
    public static boolean allowChestDamage;
    public static boolean allowLeashUse;
    public static boolean allowHurtMonsters;
    public static boolean allowEnchanting;
    public static boolean allowAnvilUse;
    public static boolean allowVisitorKeepInvOnDeath;
    public static boolean allowVisitorItemPickup;
    public static boolean allowVisitorItemDrop;
    public static boolean allowArmorStandUse;
    public static boolean allowBeaconAccess;
    public static boolean allowPortalUse;

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

    // Levels
    public static HashMap<Material, Integer> blockLimits;
    public static HashMap<Material, Integer> blockValues;

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

    // Schematic list (permission, filename)
    public static HashMap<String, String> schematics = new HashMap<String, String>();

    // Wait between being invited to same team island
    public static int inviteWait;

    // Use physics when pasting schematic blocks
    public static boolean usePhysics;

    // Falling blocked commands
    public static List<String> fallingCommandBlockList;
    public static List<String> leaveCommands;
    public static int levelWait;
    public static long resetConfirmWait;
    public static boolean allowSpawnMonsterEggs;
    public static boolean loginLevel;
    public static boolean allowSpawnBreakBlocks;
    public static boolean allowSpawnPlaceBlocks;
    public static boolean allowSpawnEggs;
    public static boolean resetEnderChest;
    public static EntityType islandCompanion;
    public static boolean updateCheck;
    public static List<String> companionNames;
    public static long islandStartX;
    public static long islandStartZ;
    public static boolean allowNetherPvP;
    public static int maxHomes;
    public static boolean immediateTeleport;
    public static boolean makeIslandIfNone;
    public static boolean setTeamName;

}