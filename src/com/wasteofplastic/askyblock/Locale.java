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

import java.io.File;
import java.io.InputStream;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


/**
 * All the text strings in the game sent to players
 * This version enables different players to have different locales.
 * 
 * @author tastybento
 */
public class Locale {
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    private ASkyBlock plugin;

    public String changingObsidiantoLava;
    public String acidLore;
    public String acidBucket;
    public String acidBottle;
    public String drankAcidAndDied;
    public String drankAcid;
    // Errors
    public String errorUnknownPlayer;
    public String errorNoPermission;
    public String errorNoIsland;
    public String errorNoIslandOther;
    public String errorCommandNotReady;
    public String errorOfflinePlayer;
    public String errorUnknownCommand;
    public String errorNoTeam;

    // IslandGuard
    public String islandProtected;

    // LavaCheck
    public String lavaTip;

    // WarpSigns
    public String warpswelcomeLine;
    public String warpswarpTip;
    public String warpssuccess;
    public String warpsremoved;
    public String warpssignRemoved;
    public String warpsdeactivate;
    public String warpserrorNoRemove;
    public String warpserrorNoPerm;
    public String warpserrorNoPlace;
    public String warpserrorDuplicate;
    public String warpserrorDoesNotExist;
    public String warpserrorNotReadyYet;
    public String warpserrorNotSafe;
    // island warp help
    public String warpswarpToPlayersSign;
    public String warpserrorNoWarpsYet;
    public String warpswarpsAvailable;
    public String warpsPlayerWarped;

    // ASkyBlock
    public String topTenheader;
    public String topTenerrorNotReady;
    public String levelislandLevel;
    public String levelerrornotYourIsland;
    // sethome
    public String setHomehomeSet;
    public String setHomeerrorNotOnIsland;
    public String setHomeerrorNoIsland;

    // Challenges
    public String challengesyouHaveCompleted;
    public String challengesnameHasCompleted;
    public String challengesyouRepeated;
    public String challengestoComplete;
    public String challengeshelp1;
    public String challengeshelp2;
    public String challengescolors;
    public String challengescomplete;
    public String challengesincomplete;
    public String challengescompleteNotRepeatable;
    public String challengescompleteRepeatable;
    public String challengesname;
    public String challengeslevel;
    public String challengesitemTakeWarning;
    public String challengesnotRepeatable;
    public String challengesfirstTimeRewards;
    public String challengesrepeatRewards;
    public String challengesexpReward;
    public String challengesmoneyReward;
    public String challengestoCompleteUse;
    public String challengesinvalidChallengeName;
    public String challengesrewards;
    public String challengesyouHaveNotUnlocked;
    public String challengesunknownChallenge;
    public String challengeserrorNotEnoughItems;
    public String challengeserrorNotOnIsland;
    public String challengeserrorNotCloseEnough;
    public String challengeserrorItemsNotThere;
    public String challengeserrorIslandLevel;
    public String challengeserrorYouAreMissing;

    // /island
    public String islandteleport;
    public String islandnew;
    public String islanderrorCouldNotCreateIsland;
    public String islanderrorYouDoNotHavePermission;

    // /island reset
    public String islandresetOnlyOwner;
    public String islandresetMustRemovePlayers;
    public String islandresetPleaseWait;
    public String islandresetConfirm;
    public String resetYouHave;
    public String islandResetNoMore;
    // Cool down warning - [time] is number of seconds left
    public String islandresetWait;
    // /island help
    // /island
    public String islandhelpIsland;
    // island cp
    public String islandhelpControlPanel;
    // /island restart
    public String islandhelpRestart;
    public String islandDeletedLifeboats;
    // /island sethome
    public String islandhelpSetHome;
    // /island level
    public String islandhelpLevel;
    // /island level <player>
    public String islandhelpLevelPlayer;
    // /island top;
    public String islandhelpTop;
    // /island warps;
    public String islandhelpWarps;
    // /island warp <player>
    public String islandhelpWarp;
    // /island team
    public String islandhelpTeam;
    // /island invite <player>;
    public String islandhelpInvite;
    // /island leave;
    public String islandhelpLeave;
    // /island kick <player>
    public String islandhelpKick;
    // /island <accept/reject>
    public String islandhelpAcceptReject;
    // /island makeLeader<player>
    public String islandhelpMakeLeader;
    // Level
    public String islanderrorLevelNotReady;
    public String islanderrorInvalidPlayer;
    public String islandislandLevelis;
    // Spawn
    public String islandhelpSpawn;
    // Teleport go
    public String islandhelpTeleport;
    // Expel
    public String islandhelpExpel;

    // ////////////////////////////////////
    // /island commands //
    // ////////////////////////////////////

    // invite
    public String invitehelp;
    public String inviteyouCanInvite;
    public String inviteyouCannotInvite;
    // "Only the island's owner may invite new players."
    public String inviteonlyIslandOwnerCanInvite;
    public String inviteyouHaveJoinedAnIsland;
    public String invitehasJoinedYourIsland;
    public String inviteerrorCantJoinIsland;
    public String inviteerrorYouMustHaveIslandToInvite;
    public String inviteerrorYouCannotInviteYourself;
    public String inviteremovingInvite;
    public String inviteinviteSentTo;
    public String invitenameHasInvitedYou;
    public String invitetoAcceptOrReject;
    public String invitewarningYouWillLoseIsland;
    public String inviteerrorYourIslandIsFull;
    // "That player is already with a group on an island."
    public String inviteerrorThatPlayerIsAlreadyInATeam;
    public String inviteerrorCoolDown;

    // reject
    public String rejectyouHaveRejectedInvitation;
    public String rejectnameHasRejectedInvite;
    public String rejectyouHaveNotBeenInvited;

    // leave
    public String leaveerrorYouAreTheLeader;
    public String leaveyouHaveLeftTheIsland;
    public String leavenameHasLeftYourIsland;
    public String leaveerrorYouCannotLeaveIsland;
    public String leaveerrorYouMustBeInWorld;
    public String leaveerrorLeadersCannotLeave;

    // team
    public String teamlistingMembers;

    // kick / remove
    public String kickerrorPlayerNotInTeam;
    public String kicknameRemovedYou;
    public String kicknameRemoved;
    public String kickerrorNotPartOfTeam;
    public String kickerrorOnlyLeaderCan;
    public String kickerrorNoTeam;

    // makeleader
    public String makeLeadererrorPlayerMustBeOnline;
    public String makeLeadererrorYouMustBeInTeam;
    public String makeLeadererrorRemoveAllPlayersFirst;
    public String makeLeaderyouAreNowTheOwner;
    public String makeLeadernameIsNowTheOwner;
    public String makeLeadererrorThatPlayerIsNotInTeam;
    public String makeLeadererrorNotYourIsland;
    public String makeLeadererrorGeneralError;

    // //////////////////////////////////////////////////////////////
    // Admin commands that use /acid //
    // //////////////////////////////////////////////////////////////

    // Help
    public String adminHelpHelp;
    public String adminHelpreload;
    // /acid top ten;
    public String adminHelptopTen;
    // /acid register <player>;
    public String adminHelpregister;
    // /acid delete <player>;
    public String adminHelpdelete;
    // /acid completechallenge <challengename> <player>
    public String adminHelpcompleteChallenge;
    // /acid resetchallenge <challengename> <player>
    public String adminHelpresetChallenge;
    // /acid resetallchallenges <player>;
    public String adminHelpresetAllChallenges;
    // /acid purge [TimeInDays];
    public String adminHelppurge;
    public String adminHelppurgeAllowDisallow;
    public String adminHelppurgeUnowned;
    // /acid info <player>;
    public String adminHelpinfo;
    public String adminHelpclearReset;

    public String adminHelptp;

    // acid reload
    public String reloadconfigReloaded;
    // topten
    public String adminTopTengenerating;
    public String adminTopTenfinished;

    // purge
    public String purgealreadyRunning;
    public String purgeusage;
    public String purgecalculating;
    public String purgenoneFound;
    public String purgethisWillRemove;
    public String purgewarning;
    public String purgetypeConfirm;
    public String purgepurgeCancelled;
    public String purgefinished;
    public String purgeremovingName;
    public String adminHelppurgeholes;
    public String adminAllowPurge;
    public String adminPreventPurge;

    // confirm
    public String confirmerrorTimeLimitExpired;

    // delete
    public String deleteremoving;

    // register
    public String registersettingIsland;
    public String registererrorBedrockNotFound;

    // info
    public String adminInfoislandLocation;
    public String adminInfoerrorNotPartOfTeam;
    public String adminInfoerrorNullTeamLeader;
    public String adminInfoerrorTeamMembersExist;
    public String adminHelpinfoIsland;
    public String adminHelpSetSpawn;

    // resetallchallenges
    public String resetChallengessuccess;

    // checkteam
    public String checkTeamcheckingTeam;

    // completechallenge
    public String completeChallengeerrorChallengeDoesNotExist;
    public String completeChallengechallangeCompleted;

    // resetchallenge
    public String resetChallengeerrorChallengeDoesNotExist;
    public String resetChallengechallengeReset;

    // ASkyBlock news
    public String newsHeadline;

    // Nether
    public String netherSpawnIsProtected;

    // Minishop & other Control Panels
    public String islandhelpMiniShop;
    public String islandMiniShopTitle;
    public String controlPanelTitle;
    public String challengesguiTitle;
    public String minishopBuy;
    public String minishopSell;
    public String minishopOutOfStock;

    // Ultra safe boats
    public String boatWarningItIsUnsafe;
    public String clearedResetLimit;

    public String minishopYouBought;
    public String minishopSellProblem;
    public String minishopYouSold;
    public String minishopBuyProblem;
    public String minishopYouCannotAfford;

    // Sign
    public String signLine1;
    public String signLine2;
    public String signLine3;
    public String signLine4;

    // Biomes
    public String islandhelpBiome;
    public String biomeSet;
    public String biomeUnknown;
    public String biomeYouBought;
    public String biomePanelTitle;

    // Expel
    public String expelSuccess;
    public String expelNotOnIsland;
    public String expelExpelled;
    public String expelFail;
    public String expelNotYourself;

    // Ban
    public String banSuccess;
    public String banLifted;
    public String banBanned;
    public String banFail;
    public String banNotYourself;
    public String banNotBanned;
    public String banAlreadyBanned;
    public String banLiftedSuccess;

    // Mob limits
    public String moblimitsError;

    // Coop
    public String coopRemoved;
    public String coopSuccess;
    public String coopRemoveSuccess;
    public String coopMadeYouCoop;
    public String coopOnYourTeam;
    public String islandhelpCoop;
    public String coopInvited;
    public String coopUseExpel;

    public String errorWrongWorld;
    public String islandcannotTeleport;
    public String levelCalculating;
    public String prefix;
    // Lock
    public String lockIslandLocked;
    public String lockNowEntering;
    public String lockNowLeaving;
    public String lockLocking;
    public String lockUnlocking;
    public String islandHelpLock;
    public String helpColor;
    public String lockPlayerLocked;
    public String lockPlayerUnlocked;
    public String lockEnteringSpawn;
    public String lockLeavingSpawn;
    // Titles
    public String islandSubTitle;
    public String islandTitle;
    public String islandDonate;
    public String islandURL;
    public String adminHelpunregister;
    public String adminHelpSetRange;
    public String challengeserrorRewardProblem;
    public String challengesNavigation;
    public String islandHelpSettings;
    public String islandHelpChallenges;
    public String challengesmaxreached;
    public String challengescompletedtimes;
    public String targetInNoPVPArea;
    // Island Guard Settings
    public String igsTitle;
    public String igsAllowed;
    public String igsDisallowed;
    public String igsArmorStand;
    public String igsBeacon;
    public String igsBed;
    public String igsBreakBlocks;
    public String igsBreeding;
    public String igsBrewing;
    public String igsBucket;
    public String igsChest;
    public String igsChestDamage;
    public String igsWorkbench;
    public String igsCropTrampling;
    public String igsDoor;
    public String igsEnchanting;
    public String igsEnderPearl;
    public String igsFire;
    public String igsFurnace;
    public String igsGate;
    public String igsHurtAnimals;
    public String igsHurtMobs;
    public String igsCreeperDamage;
    public String igsCreeperGriefing;
    public String igsWitherDamage;
    public String igsLeash;
    public String igsLever;
    public String igsSpawnEgg;
    public String igsJukebox;
    public String igsPlaceBlocks;
    public String igsPortalUse;
    public String igsPVP;
    public String igsRedstone;
    public String igsShears;
    public String igsTeleport;
    public String igsTNT;
    public String igsVisitorDrop;
    public String igsVisitorPickUp;
    public String igsVisitorKeep;
    public String igsNetherPVP;
    public String igsAnvil;
    public String setHomeerrorNumHomes;
    public String schematicsTitle;
    public String islandhelpBan;
    public String islandhelpUnban;
    public String banNotTeamMember;
    public String teamChatPrefix;
    public String teamChatHelp;
    public String teamChatStatusOff;
    public String teamChatStatusOn;
    public String teamChatNoTeamAround;
    public String teamChatNoTeam;
    public String warpsPrevious;
    public String warpsNext;
    public String warpsTitle;
    public String villagerLimitError;
    public String hopperLimit;
    public String adminHelpsetBiome;
    public String adminHelptopBreeders;
    public String adminHelplock;
    public String adminHelpkick;
    public String adminHelpadd;
    public String adminHelptpNether;
    public String adminLockerrorInGame;
    public String errorNotOnIsland;
    public String adminLockadminUnlockedIsland;
    public String adminLockadminLockedIsland;
    public String adminTopBreedersFinding;
    public String adminTopBreedersChecking;
    public String adminDeleteIslandError;
    public String errorUseInGame;
    public String adminSetSpawnsetting;
    public String adminSetSpawncenter;
    public String adminSetSpawnlimits;
    public String adminSetSpawnrange;
    public String adminSetSpawncoords;
    public String adminSetSpawnlocked;
    public String adminSetSpawnset;
    public String adminSetSpawnownedBy;
    public String adminSetSpawnmove;
    public String adminInfotitle;
    public String adminInfounowned;
    public String adminDeleteIslandnoid;
    public String adminDeleteIslanduse;
    public String adminHelpResetHome;
    public String adminHelpSetHome;
    public String adminSetHomeNoneFound;
    public String adminSetHomeHomeSet;
    public String adminSetHomeNotOnPlayersIsland;
    public String adminHelpResetSign;
    public String adminResetSignNoSign;
    public String adminResetSignFound;
    public String adminResetSignRescued;
    public String adminResetSignErrorExists;
    public String adminSetRangeInvalid;
    public String adminSetRangeTip;
    public String adminSetRangeSet;
    public String adminSetRangeWarning;
    public String adminSetRangeWarning2;
    public String adminTpManualWarp;
    public String adminUnregisterOnTeam;
    public String adminUnregisterKeepBlocks;
    public String adminInfoPlayer;
    public String adminInfoLastLogin;
    public String adminInfoTeamLeader;
    public String adminInfoTeamMembers;
    public String adminInfoIsSpawn;
    public String adminInfoIsLocked;
    public String adminInfoIsUnlocked;
    public String adminInfoIsProtected;
    public String adminInfoIsUnprotected;
    public String adminInfoBannedPlayers;
    public String adminInfoHoppers;
    public String adminTeamKickLeader;
    public String adminTeamAddLeaderToOwn;
    public String adminTeamAddLeaderNoIsland;
    public String adminTeamAddedLeader;
    public String adminTeamNowUnowned;
    public String adminTeamSettingHome;
    public String adminTeamAddingPlayer;
    public String adminTeamAlreadyOnTeam;
    public String purgeRemovingAt;
    public String purgeNowWaiting;
    public String purgeCountingUnowned;
    public String purgeStillChecking;
    public String purgeSkyBlockFound;
    public String purgeAcidFound;
    public String adminRegisterNotSpawn;
    public String adminRegisterLeadsTeam;
    public String adminRegisterTaking;
    public String adminRegisterHadIsland;
    public String adminRegisterNoIsland;
    public String adminTopBreedersNothing;
    public String adminHelpTeamChatSpy;
    public String coopNotInCoop;
    public String islandhelpUnCoop;

    /**
     * Creates a locale object full of localized strings for a language
     * @param plugin
     * @param localeName - name of the yml file that will be used
     */
    public Locale(ASkyBlock plugin, String localeName) {
	this.plugin = plugin;
	getLocale(localeName);
	loadLocale();
    }

    /**
     * @return locale FileConfiguration object
     */
    public FileConfiguration getLocale(String localeName) {
	if (this.locale == null) {
	    reloadLocale(localeName);
	}
	return locale;
    }

    /**
     * Reloads the locale file
     */
    public void reloadLocale(String localeName) {
	//plugin.getLogger().info("DEBUG: loading local file " + localeName + ".yml");
	// Make directory if it doesn't exist
	File localeDir = new File(plugin.getDataFolder() + File.separator + "locale");
	if (!localeDir.exists()) {
	    localeDir.mkdir();
	}
	if (localeFile == null) {
	    localeFile = new File(localeDir.getPath(), localeName + ".yml");
	}
	if (localeFile.exists()) {
	    //plugin.getLogger().info("DEBUG: File exists!");
	    locale = YamlConfiguration.loadConfiguration(localeFile);
	} else {
	    // Look for defaults in the jar
	    InputStream defLocaleStream = plugin.getResource("locale/" + localeName + ".yml");
	    if (defLocaleStream != null) {
		//plugin.getLogger().info("DEBUG: Saving from jar");
		plugin.saveResource("locale/" + localeName + ".yml", true);
		localeFile = new File(plugin.getDataFolder() + File.separator + "locale", localeName + ".yml");
		locale = YamlConfiguration.loadConfiguration(localeFile);
		//locale.setDefaults(defLocale);
	    } else {
		// Use the default file
		localeFile = new File(plugin.getDataFolder() + File.separator + "locale", "locale.yml");
		if (localeFile.exists()) {
		    locale = YamlConfiguration.loadConfiguration(localeFile);
		} else {
		    // Look for defaults in the jar
		    defLocaleStream = plugin.getResource("locale/locale.yml");
		    if (defLocaleStream != null) {
			plugin.saveResource("locale/locale.yml", true);
			localeFile = new File(plugin.getDataFolder() + File.separator + "locale", "locale.yml");
			locale = YamlConfiguration.loadConfiguration(localeFile);
		    } else {
			plugin.getLogger().severe("Could not find any locale file!");
		    }
		}
	    }
	}
    }

    public void loadLocale() {
	// Localization Locale Setting
	// Command prefix - can be added to the beginning of any message
	prefix = ChatColor.translateAlternateColorCodes('&', ChatColor.translateAlternateColorCodes('&', locale.getString("prefix", "")));

	if (Settings.GAMETYPE.equals(Settings.GameType.ASKYBLOCK)) {
	    signLine1 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign.line1", "&1[A Skyblock]"));
	    signLine2 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign.line2", "[player]"));
	    signLine3 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign.line3", "Do not fall!"));
	    signLine4 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign.line4", "Beware!"));
	    islandhelpSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpIslandSpawn", "go to ASkyBlock spawn."));
	    newsHeadline = ChatColor.translateAlternateColorCodes('&', locale.getString("news.headline", "[ASkyBlock News] While you were offline..."));

	} else {
	    // AcidIsland
	    signLine1 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign-acidisland.line1", "&1[Acid Island]"));
	    signLine2 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign-acidisland.line2", "[player]"));
	    signLine3 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign-acidisland.line3", "Water is acid!"));
	    signLine4 = ChatColor.translateAlternateColorCodes('&', locale.getString("sign-acidisland.line4", "Beware!"));
	    islandhelpSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpIslandSpawn", "go to AcidIsland spawn."));
	    newsHeadline = ChatColor.translateAlternateColorCodes('&', locale.getString("news.headline", "[AcidIsland News] While you were offline..."));

	}
	changingObsidiantoLava = ChatColor.translateAlternateColorCodes('&',
		locale.getString("changingObsidiantoLava", "Changing obsidian back into lava. Be careful!"));
	acidLore = ChatColor.translateAlternateColorCodes('&', locale.getString("acidLore", "Poison!\nBeware!\nDo not drink!"));
	acidBucket = ChatColor.translateAlternateColorCodes('&', locale.getString("acidBucket", "Acid Bucket"));
	acidBottle = ChatColor.translateAlternateColorCodes('&', locale.getString("acidBottle", "Bottle O' Acid"));
	drankAcidAndDied = ChatColor.translateAlternateColorCodes('&', locale.getString("drankAcidAndDied", "drank acid and died."));
	drankAcid = ChatColor.translateAlternateColorCodes('&', locale.getString("drankAcid", "drank acid."));
	errorUnknownPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("error.unknownPlayer", "That player is unknown."));
	errorNoPermission = ChatColor.translateAlternateColorCodes('&',
		locale.getString("error.noPermission", "You don't have permission to use that command!"));
	errorNoIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("error.noIsland", "You do not have an island!"));
	errorNoIslandOther = ChatColor
		.translateAlternateColorCodes('&', locale.getString("error.noIslandOther", "That player does not have an island!"));
	// "You must be on your island to use this command."
	errorCommandNotReady = ChatColor.translateAlternateColorCodes('&',
		locale.getString("error.commandNotReady", "You can't use that command right now."));
	errorOfflinePlayer = ChatColor.translateAlternateColorCodes('&',
		locale.getString("error.offlinePlayer", "That player is offline or doesn't exist."));
	errorUnknownCommand = ChatColor.translateAlternateColorCodes('&', locale.getString("error.unknownCommand", "Unknown command."));
	errorNoTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("error.noTeam", "That player is not in a team."));
	errorWrongWorld = ChatColor.translateAlternateColorCodes('&', locale.getString("error.wrongWorld", "You cannot do that in this world."));
	islandProtected = ChatColor.translateAlternateColorCodes('&', locale.getString("islandProtected", "Island protected."));
	targetInNoPVPArea = ChatColor.translateAlternateColorCodes('&', locale.getString("targetInPVPArea", "Target is in a no-PVP area!"));
	igsTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.title", "Island Guard Settings"));
	igsAnvil = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.anvil", "Anvil Use"));
	igsAllowed = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.allowed", "Allowed"));
	igsDisallowed = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.disallowed", "Disallowed"));
	igsArmorStand = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.armorstand", "Armor Stand use"));
	igsBeacon = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.beacon", "Beacon use"));
	igsBed = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.bed", "Bed use"));
	igsBreakBlocks = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.breakblocks", "Break blocks"));
	igsBreeding = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.breeding", "Breeding"));
	igsBrewing = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.brewingstand", "Potion Brewing"));
	igsBucket = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.bucket", "Bucket use"));
	igsChest = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.chest", "Chest use"));
	igsChestDamage = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.chestdamage", "Chest damage by TNT"));
	igsWorkbench = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.workbench", "Workbench use"));
	igsCropTrampling = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.croptrample", "Crop trampling"));
	igsDoor = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.door", "Door use"));
	igsEnchanting = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.enchantingtable", "Enchanting table use"));
	igsEnderPearl = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.enderpearl", "Enderpearl use"));
	igsFire = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.fire", "Fire"));
	igsFurnace = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.furnace", "Furnace use"));
	igsGate = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.gate", "Gate use"));
	igsHurtAnimals = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.hurtanimals", "Hurting animals"));
	igsHurtMobs = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.hurtmonsters", "Hurting monsters"));
	igsCreeperDamage = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.creeperdamage", "Creeper damage"));
	igsCreeperGriefing = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.creepergriefing", "Creeper griefing"));
	igsWitherDamage = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.withergriefing", "Wither griefing"));
	igsLeash = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.leash", "Leash use"));
	igsLever = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.lever", "Lever or Button Use"));
	igsSpawnEgg = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.spawnegg", "Spawn egg use"));
	igsJukebox = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.jukebox", "Jukebox use"));
	igsPlaceBlocks = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.placeblocks", "Place blocks"));
	igsPortalUse = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.portaluse", "Portal use"));
	igsPVP = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.pvp", "PvP"));
	igsNetherPVP = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.netherpvp", "Nether PvP"));
	igsRedstone = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.redstone", "Redstone use"));
	igsShears = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.shears", "Shears use"));
	igsTeleport = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.teleportwhenfalling", "Teleport when falling"));
	igsTNT = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.TNTdamage", "TNT Damage"));
	igsVisitorDrop = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.visitordrop", "Visitor item dropping"));
	igsVisitorPickUp = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.visitorpickup", "Visitor item pick-up"));
	igsVisitorKeep = ChatColor.translateAlternateColorCodes('&', locale.getString("islandguardsettings.visitorkeepitems", "Visitor keep item on death"));	
	lavaTip = ChatColor.translateAlternateColorCodes('&', locale.getString("lavaTip", "Changing obsidian back into lava. Be careful!"));
	warpswelcomeLine = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.welcomeLine", "[WELCOME]"));
	warpswarpTip = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.warpTip", "Create a warp by placing a sign with [WELCOME] at the top."));
	warpssuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.success", "Welcome sign placed successfully!"));
	warpsremoved = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.removed", "Welcome sign removed!"));
	warpssignRemoved = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.signRemoved", "Your welcome sign was removed!"));
	warpsdeactivate = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.deactivate", "Deactivating old sign!"));
	warpserrorNoRemove = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorNoRemove", "You can only remove your own Welcome Sign!"));
	warpserrorNoPerm = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorNoPerm", "You do not have permission to place Welcome Signs yet!"));
	warpserrorNoPlace = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorNoPlace", "You must be on your island to place a Welcome Sign!"));
	warpserrorDuplicate = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorDuplicate", "Sorry! There is a sign already in that location!"));
	warpserrorDoesNotExist = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorDoesNotExist", "That warp doesn't exist!"));
	warpserrorNotReadyYet = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorNotReadyYet", "That warp is not ready yet. Try again later."));
	warpserrorNotSafe = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorNotSafe", "That warp is not safe right now. Try again later."));
	warpswarpToPlayersSign = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.warpToPlayersSign", "Warping to <player>'s welcome sign."));
	warpserrorNoWarpsYet = ChatColor.translateAlternateColorCodes('&',
		locale.getString("warps.errorNoWarpsYet", "There are no warps available yet!"));
	warpswarpsAvailable = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.warpsAvailable", "The following warps are available"));
	warpsPlayerWarped = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.playerWarped", "[name] &2warped to your island!"));
	warpsPrevious = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.previous", "Previous"));
	warpsNext = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.next", "Next"));
	warpsTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.title", "Island warps"));
	topTenheader = ChatColor.translateAlternateColorCodes('&', locale.getString("topTen.header", "These are the Top 10 islands:"));
	topTenerrorNotReady = ChatColor.translateAlternateColorCodes('&', locale.getString("topTen.errorNotReady", "Top ten list not generated yet!"));
	levelislandLevel = ChatColor.translateAlternateColorCodes('&', locale.getString("level.islandLevel", "Island level"));
	levelerrornotYourIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("level.errornotYourIsland", "Only the island owner can do that."));
	levelCalculating = ChatColor.translateAlternateColorCodes('&',
		locale.getString("level.calculating", "Calculating island level. This will take a few seconds..."));
	setHomehomeSet = ChatColor.translateAlternateColorCodes('&',
		locale.getString("sethome.homeSet", "Your island home has been set to your current location."));
	setHomeerrorNotOnIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("sethome.errorNotOnIsland", "You must be within your island boundaries to set home!"));
	setHomeerrorNumHomes = ChatColor.translateAlternateColorCodes('&',
		locale.getString("sethome.errorNumHomes", "Homes can be 1 to [max]"));
	setHomeerrorNoIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("sethome.errorNoIsland", "You are not part of an island. Returning you the spawn area!"));
	challengesyouHaveCompleted = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.youHaveCompleted", "You have completed the [challenge] challenge!"));
	challengesnameHasCompleted = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.nameHasCompleted", "[name] has completed the [challenge] challenge!"));
	challengesyouRepeated = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.youRepeated", "You repeated the [challenge] challenge!"));
	challengestoComplete = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.toComplete", "Complete [challengesToDo] more [thisLevel] challenges to unlock this level!"));
	challengeshelp1 = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.help1", "Use /c <name> to view information about a challenge."));
	challengeshelp2 = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.help2", "Use /c complete <name> to attempt to complete that challenge."));
	challengescolors = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.colors", "Challenges will have different colors depending on if they are:"));
	challengescomplete = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.complete", "Complete"));
	challengesincomplete = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.incomplete", "Incomplete"));
	challengescompleteNotRepeatable = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.completeNotRepeatable", "Completed(not repeatable)"));
	challengescompleteRepeatable = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.completeRepeatable", "Completed(repeatable)"));
	challengesname = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.name", "Challenge Name"));
	challengeslevel = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.level", "Level"));
	challengesitemTakeWarning = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.itemTakeWarning", "All required items are taken when you complete this challenge!"));
	challengesnotRepeatable = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.notRepeatable", "This Challenge is not repeatable!"));
	challengesfirstTimeRewards = ChatColor
		.translateAlternateColorCodes('&', locale.getString("challenges.firstTimeRewards", "First time reward(s)"));
	challengesrepeatRewards = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.repeatRewards", "Repeat reward(s)"));
	challengesexpReward = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.expReward", "Exp reward"));
	challengesmoneyReward = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.moneyReward", "Money reward"));
	challengestoCompleteUse = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.toCompleteUse", "To complete this challenge, use"));
	challengesinvalidChallengeName = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.invalidChallengeName", "Invalid challenge name! Use /c help for more information"));
	challengesrewards = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.rewards", "Reward(s)"));
	challengesyouHaveNotUnlocked = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.youHaveNotUnlocked", "You have not unlocked this challenge yet!"));
	challengesunknownChallenge = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.unknownChallenge", "Unknown challenge name (check spelling)!"));
	challengeserrorNotEnoughItems = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.errorNotEnoughItems", "You do not have enough of the required item(s)"));
	challengeserrorNotOnIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.errorNotOnIsland", "You must be on your island to do that!"));
	challengeserrorNotCloseEnough = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.errorNotCloseEnough", "You must be standing within 10 blocks of all required items."));
	challengeserrorItemsNotThere = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.errorItemsNotThere", "All required items must be close to you on your island!"));
	challengeserrorIslandLevel = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.errorIslandLevel", "Your island must be level [level] to complete this challenge!"));
	challengeserrorRewardProblem = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.errorRewardProblem", "There was a problem giving your reward. Ask Admin to check log!"));
	challengesguiTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.guititle", "Challenges"));
	challengeserrorYouAreMissing = ChatColor.translateAlternateColorCodes('&', locale.getString("challenges.erroryouaremissing", "You are missing"));
	challengesNavigation = ChatColor
		.translateAlternateColorCodes('&', locale.getString("challenges.navigation", "Click to see [level] challenges!"));
	challengescompletedtimes = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.completedtimes", "Completed [donetimes] out of [maxtimes]"));
	challengesmaxreached = ChatColor.translateAlternateColorCodes('&',
		locale.getString("challenges.maxreached", "Max reached [donetimes] out of [maxtimes]"));
	islandteleport = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.teleport", "Teleporting you to your island. (/island help for more info)"));
	islandcannotTeleport = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.cannotTeleport", "You cannot teleport when falling!"));
	islandnew = ChatColor.translateAlternateColorCodes('&', locale.getString("island.new", "Creating a new island for you..."));
	islandSubTitle = locale.getString("island.subtitle", "by tastybento");
	islandDonate = locale.getString("island.donate", "ASkyBlock by tastybento, click here to donate via PayPal!");
	islandTitle = locale.getString("island.title", "A SkyBlock");
	islandURL = locale.getString("island.url", "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ZSBJG5J2E3B7U");
	islanderrorCouldNotCreateIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.errorCouldNotCreateIsland", "Could not create your Island. Please contact a server moderator."));
	islanderrorYouDoNotHavePermission = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.errorYouDoNotHavePermission", "You do not have permission to use that command!"));
	islandresetOnlyOwner = ChatColor.translateAlternateColorCodes('&', locale.getString("island.resetOnlyOwner",
		"Only the owner may restart this island. Leave this island in order to start your own (/island leave)."));
	islandresetMustRemovePlayers = ChatColor
		.translateAlternateColorCodes(
			'&',
			locale.getString(
				"island.resetMustRemovePlayers",
				"You must remove all players from your island before you can restart it (/island kick <player>). See a list of players currently part of your island using /island team."));
	islandresetPleaseWait = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.resetPleaseWait", "Please wait, generating new island"));
	islandresetWait = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.resetWait", "You have to wait [time] seconds before you can do that again."));
	islandresetConfirm = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.resetConfirm", "Type /island confirm within [seconds] seconds to delete your island and restart!"));
	islandhelpIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.helpIsland", "start an island, or teleport to your island."));
	islandhelpTeleport = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpTeleport", "teleport to your island."));
	islandhelpControlPanel = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpControlPanel", "open the island GUI."));
	islandhelpRestart = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.helpRestart", "restart your island and remove the old one."));
	islandDeletedLifeboats = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.islandDeletedLifeboats", "Island deleted! Head to the lifeboats!"));
	islandhelpSetHome = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpSetHome", "set your teleport point for /island."));
	islandhelpLevel = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpLevel", "calculate your island level"));
	islandhelpLevelPlayer = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.helpLevelPlayer", "see another player's island level."));
	islandhelpTop = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpTop", "see the top ranked islands."));
	islandhelpWarps = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpWarps", "Lists all available welcome-sign warps."));
	islandhelpWarp = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpWarp", "Warp to <player>'s welcome sign."));
	islandhelpTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpTeam", "view your team information."));
	islandhelpInvite = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpInvite", "invite a player to join your island."));
	islandhelpLeave = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpLeave", "leave another player's island."));
	islandhelpKick = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpKick", "remove a team member from your island."));
	islandhelpExpel = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpExpel", "force a player from your island."));
	islandhelpBan = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.helpBan", "ban a player from your island."));
	islandhelpUnban = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.helpUnban", "un-ban a player from your island."));
	islandHelpSettings = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.helpSettings", "see island protection and game settings"));
	islandHelpChallenges = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpChallenges", "/challenges: &fshow challenges"));
	adminHelpHelp = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.help", "Acid Admin Commands:"));
	islandhelpAcceptReject = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.helpAcceptReject", "accept or reject an invitation."));
	islandhelpMakeLeader = ChatColor
		.translateAlternateColorCodes('&', locale.getString("island.helpMakeLeader", "transfer the island to <player>."));
	islanderrorLevelNotReady = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.errorLevelNotReady", "Can't use that command right now! Try again in a few seconds."));
	islanderrorInvalidPlayer = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.errorInvalidPlayer", "That player is invalid or does not have an island!"));
	islandislandLevelis = ChatColor.translateAlternateColorCodes('&', locale.getString("island.islandLevelis", "Island level is"));
	invitehelp = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.help", "Use [/island invite <playername>] to invite a player to your island."));
	inviteyouCanInvite = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.youCanInvite", "You can invite [number] more players."));
	inviteyouCannotInvite = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.youCannotInvite", "You can't invite any more players."));
	inviteonlyIslandOwnerCanInvite = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.onlyIslandOwnerCanInvite", "Only the island's owner can invite!"));
	inviteyouHaveJoinedAnIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.youHaveJoinedAnIsland", "You have joined an island! Use /island team to see the other members."));
	invitehasJoinedYourIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.hasJoinedYourIsland", "[name] has joined your island!"));
	inviteerrorCantJoinIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.errorCantJoinIsland", "You couldn't join the island, maybe it's full."));
	inviteerrorYouMustHaveIslandToInvite = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.errorYouMustHaveIslandToInvite", "You must have an island in order to invite people to it!"));
	inviteerrorYouCannotInviteYourself = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.errorYouCannotInviteYourself", "You can not invite yourself!"));
	inviteremovingInvite = ChatColor.translateAlternateColorCodes('&', locale.getString("invite.removingInvite", "Removing your previous invite."));
	inviteinviteSentTo = ChatColor.translateAlternateColorCodes('&', locale.getString("invite.inviteSentTo", "Invite sent to [name]"));
	invitenameHasInvitedYou = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.nameHasInvitedYou", "[name] has invited you to join their island!"));
	invitetoAcceptOrReject = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.toAcceptOrReject", "to accept or reject the invite."));
	invitewarningYouWillLoseIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.warningYouWillLoseIsland", "WARNING: You will lose your current island if you accept!"));
	inviteerrorYourIslandIsFull = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.errorYourIslandIsFull", "Your island is full, you can't invite anyone else."));
	inviteerrorThatPlayerIsAlreadyInATeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.errorThatPlayerIsAlreadyInATeam", "That player is already in a team."));
	inviteerrorCoolDown = ChatColor.translateAlternateColorCodes('&',
		locale.getString("invite.errorCoolDown", "You can invite that player again in [time] minutes"));
	rejectyouHaveRejectedInvitation = ChatColor.translateAlternateColorCodes('&',
		locale.getString("reject.youHaveRejectedInvitation", "You have rejected the invitation to join an island."));
	rejectnameHasRejectedInvite = ChatColor.translateAlternateColorCodes('&',
		locale.getString("reject.nameHasRejectedInvite", "[name] has rejected your island invite!"));
	rejectyouHaveNotBeenInvited = ChatColor.translateAlternateColorCodes('&',
		locale.getString("reject.youHaveNotBeenInvited", "You had not been invited to join a team."));
	leaveerrorYouAreTheLeader = ChatColor.translateAlternateColorCodes('&',
		locale.getString("leave.errorYouAreTheLeader", "You are the leader, use /island remove <player> instead."));
	leaveyouHaveLeftTheIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("leave.youHaveLeftTheIsland", "You have left the island and returned to the player spawn."));
	leavenameHasLeftYourIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("leave.nameHasLeftYourIsland", "[name] has left your island!"));
	leaveerrorYouCannotLeaveIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("leave.errorYouCannotLeaveIsland",
		"You can't leave your island if you are the only person. Try using /island restart if you want a new one!"));
	leaveerrorYouMustBeInWorld = ChatColor.translateAlternateColorCodes('&',
		locale.getString("leave.errorYouMustBeInWorld", "You must be in the island world to leave your team!"));
	leaveerrorLeadersCannotLeave = ChatColor.translateAlternateColorCodes('&', locale.getString("leave.errorLeadersCannotLeave",
		"Leaders cannot leave an island. Make someone else the leader fist using /island makeleader <player>"));
	teamlistingMembers = ChatColor.translateAlternateColorCodes('&', locale.getString("team.listingMembers", "Listing your island members"));
	kickerrorPlayerNotInTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("kick.errorPlayerNotInTeam", "That player is not in your team!"));
	kicknameRemovedYou = ChatColor.translateAlternateColorCodes('&',
		locale.getString("kick.nameRemovedYou", "[name] has removed you from their island!"));
	kicknameRemoved = ChatColor.translateAlternateColorCodes('&', locale.getString("kick.nameRemoved", "[name] has been removed from the island."));
	kickerrorNotPartOfTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("kick.errorNotPartOfTeam", "That player is not part of your island team!"));
	kickerrorOnlyLeaderCan = ChatColor.translateAlternateColorCodes('&',
		locale.getString("kick.errorOnlyLeaderCan", "Only the island's owner may remove people from the island!"));
	kickerrorNoTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("kick.errorNoTeam", "No one else is on your island, are you seeing things?"));
	makeLeadererrorPlayerMustBeOnline = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.errorPlayerMustBeOnline", "That player must be online to transfer the island."));
	makeLeadererrorYouMustBeInTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.errorYouMustBeInTeam", "You must be in a team to transfer your island."));
	makeLeadererrorRemoveAllPlayersFirst = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.errorRemoveAllPlayersFirst", "Remove all players from your team other than the player you are transferring to."));
	makeLeaderyouAreNowTheOwner = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.youAreNowTheOwner", "You are now the owner of your island."));
	makeLeadernameIsNowTheOwner = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.nameIsNowTheOwner", "[name] is now the owner of your island!"));
	makeLeadererrorThatPlayerIsNotInTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.errorThatPlayerIsNotInTeam", "That player is not part of your island team!"));
	makeLeadererrorNotYourIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.errorNotYourIsland", "This isn't your island, so you can't give it away!"));
	makeLeadererrorGeneralError = ChatColor.translateAlternateColorCodes('&',
		locale.getString("makeleader.errorGeneralError", "Could not make leader!"));
	adminHelpHelp = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.help", "Could not change leaders."));
	adminHelpreload = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.reload", "reload configuration from file."));
	adminHelptopTen = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.topTen", "manually update the top 10 list"));
	adminHelpregister = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.register", "set a player's island to your location"));
	adminHelpunregister = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.unregister", "deletes a player without deleting the island blocks"));
	adminHelpdelete = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.delete", "delete an island (removes blocks)"));
	adminHelpcompleteChallenge = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.completeChallenge", "marks a challenge as complete"));
	adminHelpresetChallenge = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.resetChallenge", "marks a challenge as incomplete"));
	adminHelpresetAllChallenges = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.resetAllChallenges", "resets all of the player's challenges"));
	adminHelppurge = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.purge", "delete inactive islands older than [TimeInDays]."));
	adminHelppurgeAllowDisallow = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.purgeallowdisallow", "allow/disallow island to be purged if it meets purge criteria"));
	adminHelppurgeUnowned = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.purgeunowned", "remove unowned islands"));
	adminHelppurgeholes = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.purgeholes", "free up island holes for reuse"));
	adminHelpinfo = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.info", "check information on the given player"));
	adminHelpSetSpawn = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.setspawn", "sets the island world spawn to a location close to you"));
	adminHelpSetRange = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.setrange", "changes the island's protection range"));
	adminHelpsetBiome = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.setbiome", "sets leader's island biome"));
	adminHelptopBreeders = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.topbreeders", "lists most populated islands currently loaded"));
	adminHelplock = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.lock", "locks/unlocks player's island"));
	adminHelpkick = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.kick", "removes player from any team"));
	adminHelpadd = ChatColor
		.translateAlternateColorCodes('&', locale.getString("adminHelp.add", "adds player to leader's team"));



	adminHelpinfoIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.infoisland", "provide info on the nearest island."));
	adminHelptp = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.tp", "Teleport to a player's island."));
	adminHelptpNether = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.tpnether", "Teleport to a player's nether island."));
	reloadconfigReloaded = ChatColor.translateAlternateColorCodes('&',
		locale.getString("reload.configReloaded", "Configuration reloaded from file."));
	adminTopTengenerating = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTopTen.generating", "Generating the Top Ten list"));
	adminTopTenfinished = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminTopTen.finished", "Finished generation of the Top Ten list"));
	purgealreadyRunning = ChatColor.translateAlternateColorCodes('&',
		locale.getString("purge.alreadyRunning", "Purge is already running, please wait for it to finish!"));
	purgeusage = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.usage", "Usage: /[label] purge [TimeInDays]"));
	purgecalculating = ChatColor.translateAlternateColorCodes('&',
		locale.getString("purge.calculating", "Calculating which islands have been inactive for more than [time] days."));
	purgenoneFound = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.noneFound", "No inactive islands to remove."));
	purgethisWillRemove = ChatColor.translateAlternateColorCodes('&',
		locale.getString("purge.thisWillRemove", "[number] inactive islands found. Islands with level < [level] will be removed."));
	purgewarning = ChatColor.translateAlternateColorCodes('&',
		locale.getString("purge.warning", "DANGER! Do not run this with players on the server! MAKE BACKUP OF WORLD!"));
	purgetypeConfirm = ChatColor.translateAlternateColorCodes('&',
		locale.getString("purge.typeConfirm", "Type [label] confirm to proceed within 10 seconds"));
	purgepurgeCancelled = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.purgeCancelled", "Purge cancelled."));
	purgefinished = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.finished", "Finished purging of inactive islands."));
	purgeremovingName = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.removingName", "Purge: Removing [name]'s island"));
	purgeRemovingAt = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.removingAt", "Removing island at location [location]"));
	purgeNowWaiting = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.nowWaiting", "Now waiting..."));
	purgeCountingUnowned = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.countingUnowned", "Counting unowned islands and checking player files. This could take some time..."));
	purgeStillChecking = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.stillChecking", "Still checking player files..."));
	purgeSkyBlockFound = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.skyblockFound", "There are [number] unowned islands. Do '/asadmin purge unowned confirm' to delete them within 20 seconds."));
	purgeAcidFound = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.acidFound", "There are [number] unowned islands. Do '/acid purge unowned confirm' to delete them within 20 seconds."));
	adminAllowPurge = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.preventName", "Purge protection removed"));
	adminPreventPurge = ChatColor.translateAlternateColorCodes('&', locale.getString("purge.allowName", "Island is protected from purging"));
	confirmerrorTimeLimitExpired = ChatColor.translateAlternateColorCodes('&',
		locale.getString("confirm.errorTimeLimitExpired", "Time limit expired! Issue command again."));
	deleteremoving = ChatColor.translateAlternateColorCodes('&', locale.getString("delete.removing", "Removing [name]'s island."));
	registersettingIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("register.settingIsland", "Set [name]'s island to the bedrock nearest you."));
	registererrorBedrockNotFound = ChatColor.translateAlternateColorCodes('&',
		locale.getString("register.errorBedrockNotFound", "Error: unable to set the island!"));
	adminInfoislandLocation = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.islandLocation", "Island Location"));
	adminInfoerrorNotPartOfTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminInfo.errorNotPartOfTeam", "That player is not a member of an island team."));
	adminInfoerrorNullTeamLeader = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminInfo.errorNullTeamLeader", "Team leader should be null!"));
	adminInfoerrorTeamMembersExist = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminInfo.errorTeamMembersExist", "Player has team members, but shouldn't!"));
	resetChallengessuccess = ChatColor.translateAlternateColorCodes('&',
		locale.getString("resetallchallenges.success", "[name] has had all challenges reset."));
	checkTeamcheckingTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("checkTeam.checkingTeam", "Checking Team of [name]"));
	completeChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',
		locale.getString("completechallenge.errorChallengeDoesNotExist", "Challenge doesn't exist or is already completed"));
	completeChallengechallangeCompleted = ChatColor.translateAlternateColorCodes('&',
		locale.getString("completechallenge.challangeCompleted", "[challengename] has been completed for [name]"));
	resetChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',
		locale.getString("resetchallenge.errorChallengeDoesNotExist", "[challengename] has been reset for [name]"));
	confirmerrorTimeLimitExpired = ChatColor.translateAlternateColorCodes('&',
		locale.getString("confirm.errorTimeLimitExpired", "Time limit expired! Issue command again."));
	deleteremoving = ChatColor.translateAlternateColorCodes('&', locale.getString("delete.removing", "Removing [name]'s island."));
	registersettingIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("register.settingIsland", "Set [name]'s island to the bedrock nearest you."));
	registererrorBedrockNotFound = ChatColor.translateAlternateColorCodes('&',
		locale.getString("register.errorBedrockNotFound", "Error: unable to set the island!"));
	adminInfoislandLocation = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.islandLocation", "Island Location"));
	adminInfoerrorNotPartOfTeam = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminInfo.errorNotPartOfTeam", "That player is not a member of an island team."));
	adminInfoerrorNullTeamLeader = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminInfo.errorNullTeamLeader", "Team leader should be null!"));
	adminInfoerrorTeamMembersExist = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminInfo.errorTeamMembersExist", "Player has team members, but shouldn't!"));
	resetChallengessuccess = ChatColor.translateAlternateColorCodes('&',
		locale.getString("resetallchallenges.success", "[name] has had all challenges reset."));
	checkTeamcheckingTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("checkTeam.checkingTeam", "Checking Team of [name]"));
	completeChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',
		locale.getString("completechallenge.errorChallengeDoesNotExist", "Challenge doesn't exist or is already completed"));
	completeChallengechallangeCompleted = ChatColor.translateAlternateColorCodes('&',
		locale.getString("completechallenge.challangeCompleted", "[challengename] has been completed for [name]"));
	resetChallengeerrorChallengeDoesNotExist = ChatColor.translateAlternateColorCodes('&',
		locale.getString("resetchallenge.errorChallengeDoesNotExist", "Challenge doesn't exist or isn't yet completed"));
	resetChallengechallengeReset = ChatColor.translateAlternateColorCodes('&',
		locale.getString("resetchallenge.challengeReset", "[challengename] has been reset for [name]"));
	netherSpawnIsProtected = ChatColor.translateAlternateColorCodes('&',
		locale.getString("nether.spawnisprotected", "The Nether spawn area is protected."));
	islandhelpMiniShop = ChatColor.translateAlternateColorCodes('&', locale.getString("minishop.islandhelpMiniShop", "Opens the MiniShop"));
	islandMiniShopTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("minishop.title", "MiniShop"));
	minishopBuy = ChatColor.translateAlternateColorCodes('&', locale.getString("minishop.buy", "Buy(Left Click)"));
	minishopSell = ChatColor.translateAlternateColorCodes('&', locale.getString("minishop.sell", "Sell(Right Click)"));
	minishopYouBought = ChatColor.translateAlternateColorCodes('&',
		locale.getString("minishop.youbought", "You bought [number] [description] for [price]"));
	minishopSellProblem = ChatColor.translateAlternateColorCodes('&',
		locale.getString("minishop.sellproblem", "You do not have enough [description] to sell."));
	minishopYouSold = ChatColor.translateAlternateColorCodes('&',
		locale.getString("minishop.yousold", "You sold [number] [description] for [price]"));
	minishopBuyProblem = ChatColor.translateAlternateColorCodes('&',
		locale.getString("minishop.buyproblem", "There was a problem purchasing [description]"));
	minishopYouCannotAfford = ChatColor.translateAlternateColorCodes('&',
		locale.getString("minishop.youcannotafford", "You cannot afford [description]!"));
	minishopOutOfStock = ChatColor.translateAlternateColorCodes('&', locale.getString("minishop.outofstock", "Out Of Stock"));
	boatWarningItIsUnsafe = ChatColor.translateAlternateColorCodes('&',
		locale.getString("boats.warning", "It's unsafe to exit the boat right now..."));
	adminHelpclearReset = ChatColor.translateAlternateColorCodes('&',
		locale.getString("adminHelp.clearreset", "resets the island reset limit for player."));
	resetYouHave = ChatColor.translateAlternateColorCodes('&', locale.getString("island.resetYouHave", "You have [number] resets left."));
	islandResetNoMore = ChatColor.translateAlternateColorCodes('&',
		locale.getString("island.resetNoMore", "No more resets are allowed for your island!"));
	clearedResetLimit = ChatColor.translateAlternateColorCodes('&', locale.getString("resetTo", "Cleared reset limit"));

	islandhelpBiome = ChatColor.translateAlternateColorCodes('&', locale.getString("biome.help", "open the biome GUI."));
	biomeSet = ChatColor.translateAlternateColorCodes('&', locale.getString("biome.set", "Island biome set to [biome]!"));
	biomeUnknown = ChatColor.translateAlternateColorCodes('&', locale.getString("biome.unknown", "Unknown biome!"));
	biomeYouBought = ChatColor.translateAlternateColorCodes('&', locale.getString("biome.youbought", "Purchased for [cost]!"));
	biomePanelTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("biome.paneltitle", "Select A Biome"));
	expelNotOnIsland = ChatColor.translateAlternateColorCodes('&',
		locale.getString("expel.notonisland", "Player is not trespassing on your island!"));
	expelSuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("expel.success", "You expelled [name]!"));
	expelExpelled = ChatColor.translateAlternateColorCodes('&', locale.getString("expel.expelled", "You were expelled from that island!"));
	expelFail = ChatColor.translateAlternateColorCodes('&', locale.getString("expel.fail", "[name] cannot be expelled!"));
	expelNotYourself = ChatColor.translateAlternateColorCodes('&', locale.getString("expel.notyourself", "You cannot expel yourself!"));
	banSuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.success", "[name] is banned from the island!"));
	banBanned = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.banned", "You are banned from [name]'s island!"));
	banLifted = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.lifted", "Ban lifted from [name]'s island!"));
	banLiftedSuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.liftedsuccess", "Ban lifted for [name]!"));
	banFail = ChatColor.translateAlternateColorCodes('&', locale.getString("banned.fail", "[name] cannot be banned!"));
	banNotYourself = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.notyourself", "You cannot do that to yourself!"));
	banNotTeamMember = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.notteammember", "You cannot ban a team member!"));
	banNotBanned = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.notbanned", "[name] is not banned!"));
	banAlreadyBanned = ChatColor.translateAlternateColorCodes('&', locale.getString("ban.alreadybanned", "[name] is already banned!"));
	moblimitsError = ChatColor.translateAlternateColorCodes('&', locale.getString("moblimits.error", "Island animal breeding limit of [number] reached!"));
	villagerLimitError = ChatColor.translateAlternateColorCodes('&', locale.getString("moblimits.villager", "Island villager breeding limit of [number] reached!"));
	hopperLimit = ChatColor.translateAlternateColorCodes('&', locale.getString("moblimits.hopper", "Island hopper limit of [number] reached!"));
	coopRemoved = ChatColor.translateAlternateColorCodes('&', locale.getString("coop.removed", "[name] remove your coop status!"));
	coopRemoveSuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("coop.removesuccess", "[name] is no longer a coop player."));
	coopSuccess = ChatColor.translateAlternateColorCodes('&',
		locale.getString("coop.success", "[name] is now a coop player until they log out or you expel them."));
	coopMadeYouCoop = ChatColor.translateAlternateColorCodes('&',
		locale.getString("coop.madeyoucoopy", "[name] made you a coop player until you log out or they expel you."));
	coopOnYourTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("coop.onyourteam", "Player is already on your team!"));
	coopNotInCoop = ChatColor.translateAlternateColorCodes('&', locale.getString("coop.notincoop", "[name] is not in your coop!"));
	islandhelpCoop = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.help", "temporarily give a player full access to your island"));
	islandhelpUnCoop = ChatColor.translateAlternateColorCodes('&',locale.getString("coop.uncoop", "remove full island access from player"));
	coopInvited = ChatColor.translateAlternateColorCodes('&', locale.getString("coop.invited", "[name] made [player] a coop player!"));
	coopUseExpel = ChatColor.translateAlternateColorCodes('&', locale.getString("coop.useexpel", "Use expel to remove."));
	lockIslandLocked = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.islandlocked", "Island is locked to visitors"));
	lockNowEntering = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.nowentering", "Now entering [name]'s island"));
	lockNowLeaving = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.nowleaving", "Now leaving [name]'s island"));
	lockLocking = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.locking", "Locking island"));
	lockUnlocking = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.unlocking", "Unlocking island"));
	islandHelpLock = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpLock", "Locks island so visitors cannot enter it"));
	helpColor = ChatColor.translateAlternateColorCodes('&', locale.getString("island.helpColor", "&e"));
	lockPlayerLocked = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.playerlocked", "[name] locked the island"));
	lockPlayerUnlocked = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.playerunlocked", "[name] unlocked the island"));
	lockEnteringSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.enteringspawn", "Entering Spawn"));
	lockLeavingSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("lock.leavingspawn", "Leaving Spawn"));
	schematicsTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("schematics.title", "Choose island..."));
	teamChatPrefix = ChatColor.translateAlternateColorCodes('&', locale.getString("teamchat.prefix", "[Team Chat]<{ISLAND_PLAYER}> "));
	teamChatHelp = ChatColor.translateAlternateColorCodes('&', locale.getString("teamchat.helpChat", "turn on/off team chat"));
	teamChatStatusOff = ChatColor.translateAlternateColorCodes('&', locale.getString("teamchat.statusOff", "Team chat is off"));
	teamChatStatusOn = ChatColor.translateAlternateColorCodes('&', locale.getString("teamchat.statusOn", "Team chat is on"));
	teamChatNoTeamAround = ChatColor.translateAlternateColorCodes('&', locale.getString("teamchat.noTeamAround", "None of your team are online!"));
	teamChatNoTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("teamchat.noTeam", "You are not in a team!"));
	adminLockerrorInGame = ChatColor.translateAlternateColorCodes('&', locale.getString("adminLock.errorInGame", "Must use command in-game while on an island!"));
	errorNotOnIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("error.notonisland", "You are not in an island space!"));
	adminLockadminUnlockedIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("adminLock.adminUnlockedIsland", "Admin unlocked your island"));
	adminLockadminLockedIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("adminLock.adminLockedIsland", "Admin locked your island"));
	adminTopBreedersFinding = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTopBreeders.finding", "Finding top breeders..."));
	adminTopBreedersChecking = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTopBreeders.checking", "Checking [number] islands..."));
	adminTopBreedersNothing = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTopBreeders.nothing", "No creatures found."));
	adminDeleteIslandError = ChatColor.translateAlternateColorCodes('&', locale.getString("adminDeleteIsland.error", "Use &ldeleteisland confirm &r&cto delete the island you are on."));
	adminDeleteIslandnoid = ChatColor.translateAlternateColorCodes('&', locale.getString("adminDeleteIsland.noid", "Cannot identify island."));
	adminDeleteIslanduse = ChatColor.translateAlternateColorCodes('&', locale.getString("adminDeleteIsland.use", "Use &ldelete [name] &r&cto delete the player instead."));
	errorUseInGame = ChatColor.translateAlternateColorCodes('&', locale.getString("error.useInGame", "This command must be used in-game."));
	adminSetSpawnsetting = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.setting", "Setting island spawn to your location [location]"));
	adminSetSpawncenter = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.center", "Spawn island center [location]"));
	adminSetSpawnlimits = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.limits", "Spawn island limits [min] to [max]"));
	adminSetSpawnrange = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.range", "Spawn protection range = [number]"));
	adminSetSpawncoords = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.coords", "Spawn protection coords [min] to [max]"));
	adminSetSpawnlocked = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.locked", "Spawn is locked!"));
	adminSetSpawnset = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.set", "Set island spawn to your location."));
	adminSetSpawnownedBy = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.ownedBy", "This island space is owned by [name]"));
	adminSetSpawnmove = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetSpawn.move", "Move further away or unregister the owner."));
	adminInfotitle = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.title", "This is spawn island"));
	adminInfounowned = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.unowned", "This island is not owned by anyone right now."));
	adminHelpResetHome = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.resethome", "Clears all home spots for player"));
	adminHelpSetHome = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.resethome", "Sets player's home to your position"));
	adminSetHomeNoneFound = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetHome.noneFound", "No safe location found!"));
	adminSetHomeHomeSet = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetHome.homeSet", "Home set to [location]"));
	adminSetHomeNotOnPlayersIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetHome.notOnPlayersIsland", "You are not on the player's island"));
	adminHelpResetSign = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.resetSign", "Resets the sign you are looking at to the island owner"));
	adminResetSignNoSign = ChatColor.translateAlternateColorCodes('&', locale.getString("adminResetSign.noSign", "You must be looking at a sign post to run this command."));
	adminResetSignFound = ChatColor.translateAlternateColorCodes('&', locale.getString("adminResetSign.found", "Warp Sign found!"));
	adminResetSignRescued = ChatColor.translateAlternateColorCodes('&', locale.getString("adminResetSign.rescued", "Warp sign rescued and assigned to [name]"));
	adminResetSignErrorExists = ChatColor.translateAlternateColorCodes('&', locale.getString("adminResetSign.errorExists", "That warp sign is already active and owned by [name]"));
	adminSetRangeInvalid = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetRange.invalid", "Invalid range!"));
	adminSetRangeTip = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetRange.tip", "Use 10 to [max]"));
	adminSetRangeSet = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetRange.set", "Set range to [number]"));
	adminSetRangeWarning = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetRange.warning", "Warning - range is greater than island range [max]"));
	adminSetRangeWarning2 = ChatColor.translateAlternateColorCodes('&', locale.getString("adminSetRange.warning2", "Overlapped islands will act like spawn!"));
	adminTpManualWarp = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTp.manualWarp", "No safe spot found. Manually warp to somewhere near [location]."));
	adminUnregisterOnTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("adminUnregsiter.onTeam", "Player is in a team - disband it first."));
	adminUnregisterKeepBlocks = ChatColor.translateAlternateColorCodes('&', locale.getString("adminUnregsiter.KeepBlocks", "Removing player from world, but keeping island at [location]"));
	adminInfoPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.player","Player"));
	adminInfoLastLogin = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.lastLogin","Last Login"));
	adminInfoTeamLeader = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.teamLeader","Team Leader"));
	adminInfoTeamMembers = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.teamMembers","Team Members"));
	adminInfoIsSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.isSpawn","Island is spawn"));
	adminInfoIsLocked = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.isLocked","Island is locked"));
	adminInfoIsUnlocked = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.isUnlocked","Island is unlocked"));
	adminInfoIsProtected = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.isProtected","Island is purge protected"));
	adminInfoIsUnprotected = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.isUnprotected","Island is not purge protected"));
	adminInfoBannedPlayers = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.bannedPlayers", "Banned players"));
	adminInfoHoppers = ChatColor.translateAlternateColorCodes('&', locale.getString("adminInfo.hoppers", "Island has [number] hoppers"));
	adminTeamKickLeader = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.kickLeader", "That player is a team leader. Remove team members first. Use '/[label] info [name]' to find team members."));
	adminTeamAddLeaderToOwn = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.addLeaderToOwn", "Cannot add a leader to their own team."));
	adminTeamAddLeaderNoIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.addLeaderNoIsland", "Team leader does not have their own island so cannot have a team!"));
	adminTeamAddedLeader = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.addedLeader", "Added the leader to this team!"));
	adminTeamNowUnowned = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.nowUnowned", "[name] had an island at [location] that will become unowned now. You may want to delete it manually." ));
	adminTeamSettingHome = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.settingHome", "Setting player's home to the leader's home location"));
	adminTeamAddingPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.addingPlayer", "Adding player to team."));
	adminTeamAlreadyOnTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("adminTeam.alreadyOnTeam", "Player was already on this team!"));
	adminRegisterNotSpawn = ChatColor.translateAlternateColorCodes('&', locale.getString("adminRegister.notSpawn", "You cannot take ownership of spawn!"));
	adminRegisterLeadsTeam = ChatColor.translateAlternateColorCodes('&', locale.getString("adminRegister.leadsTeam", "[name] leads a team. Kick players from it first."));
	adminRegisterTaking = ChatColor.translateAlternateColorCodes('&', locale.getString("adminRegister.taking", "Taking ownership away from [name]"));
	adminRegisterHadIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("adminRegister.hadIsland", "[name] had an island at [location]"));
	adminRegisterNoIsland = ChatColor.translateAlternateColorCodes('&', locale.getString("adminRegister.noIsland", "There is no known island in this area!"));
	adminHelpTeamChatSpy = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.teamChatSpy", "Spy on team chats (on/off)"));
    }
}
