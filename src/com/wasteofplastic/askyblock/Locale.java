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


/**
 * @author ben
 * All the text strings in the game sent to players
 */
public class Locale {
    public static String changingObsidiantoLava;
    public static String acidLore;
    public static String acidBucket;
    public static String acidBottle;
    public static String drankAcidAndDied;
    public static String drankAcid;
    // Errors
    public static String errorUnknownPlayer;
    public static String errorNoPermission;
    public static String errorNoIsland;
    public static String errorNoIslandOther;
    public static String errorCommandNotReady;
    public static String errorOfflinePlayer;
    public static String errorUnknownCommand;
    public static String errorNoTeam;

    //IslandGuard
    public static String islandProtected;

    //LavaCheck
    public static String lavaTip;

    //WarpSigns
    public static String warpswelcomeLine;
    public static String warpswarpTip;
    public static String warpssuccess;
    public static String warpsremoved;
    public static String warpssignRemoved;
    public static String warpsdeactivate;
    public static String warpserrorNoRemove;
    public static String warpserrorNoPerm;
    public static String warpserrorNoPlace;
    public static String warpserrorDuplicate;
    public static String warpserrorDoesNotExist;
    public static String warpserrorNotReadyYet;
    public static String warpserrorNotSafe;
    //island warp help
    public static String warpswarpToPlayersSign;
    public static String warpserrorNoWarpsYet;
    public static String warpswarpsAvailable;
    public static String warpsPlayerWarped;

    //ASkyBlock
    public static String topTenheader;
    public static String topTenerrorNotReady;
    public static String levelislandLevel;
    public static String levelerrornotYourIsland;
    //sethome
    public static String setHomehomeSet;
    public static String setHomeerrorNotOnIsland;
    public static String setHomeerrorNoIsland;


    //Challenges
    public static String challengesyouHaveCompleted;
    public static String challengesnameHasCompleted;
    public static String challengesyouRepeated;
    public static String challengestoComplete;
    public static String challengeshelp1;
    public static String challengeshelp2;
    public static String challengescolors;
    public static String challengescomplete;
    public static String challengesincomplete;
    public static String challengescompleteNotRepeatable;
    public static String challengescompleteRepeatable;
    public static String challengesname;
    public static String challengeslevel;
    public static String challengesitemTakeWarning;
    public static String challengesnotRepeatable;
    public static String challengesfirstTimeRewards;
    public static String challengesrepeatRewards;
    public static String challengesexpReward;
    public static String challengesmoneyReward;
    public static String challengestoCompleteUse;
    public static String challengesinvalidChallengeName;
    public static String challengesrewards;
    public static String challengesyouHaveNotUnlocked;
    public static String challengesunknownChallenge;
    public static String challengeserrorNotEnoughItems;
    public static String challengeserrorNotOnIsland;
    public static String challengeserrorNotCloseEnough;
    public static String challengeserrorItemsNotThere;
    public static String challengeserrorIslandLevel;
    public static String challengeserrorYouAreMissing;

    ///island
    public static String islandteleport;
    public static String islandnew;
    public static String islanderrorCouldNotCreateIsland;
    public static String islanderrorYouDoNotHavePermission;

    ///island reset
    public static String islandresetOnlyOwner;
    public static String islandresetMustRemovePlayers;
    public static String islandresetPleaseWait;
    public static String islandresetConfirm;
    public static String resetYouHave;
    public static String islandResetNoMore;
    //Cool down warning - [time] is number of seconds left
    public static String islandresetWait;
    ///island help
    ///island
    public static String islandhelpIsland;
    //island cp
    public static String islandhelpControlPanel;
    ///island restart
    public static String islandhelpRestart;
    public static String islandDeletedLifeboats;
    ///island sethome
    public static String islandhelpSetHome;
    ///island level
    public static String islandhelpLevel;
    ///island level <player>
    public static String islandhelpLevelPlayer;
    ///island top;
    public static String islandhelpTop;
    ///island warps;
    public static String islandhelpWarps;
    ///island warp <player>
    public static String islandhelpWarp;
    ///island team
    public static String islandhelpTeam;
    ///island invite <player>;
    public static String islandhelpInvite;
    ///island leave;
    public static String islandhelpLeave;
    ///island kick <player>
    public static String islandhelpKick;
    ///island <accept/reject>
    public static String islandhelpAcceptReject;
    ///island makeLeader<player>
    public static String islandhelpMakeLeader;
    //Level
    public static String islanderrorLevelNotReady;
    public static String islanderrorInvalidPlayer;
    public static String islandislandLevelis;
    // Spawn
    public static String islandhelpSpawn;
    // Teleport go
    public static String islandhelpTeleport;
    // Expel
    public static String islandhelpExpel;

    //////////////////////////////////////
    ///island commands //
    //////////////////////////////////////

    //invite
    public static String invitehelp;
    public static String inviteyouCanInvite;
    public static String inviteyouCannotInvite;
    //"Only the island's owner may invite new players."
    public static String inviteonlyIslandOwnerCanInvite;
    public static String inviteyouHaveJoinedAnIsland;
    public static String invitehasJoinedYourIsland;
    public static String inviteerrorCantJoinIsland;
    public static String inviteerrorYouMustHaveIslandToInvite;
    public static String inviteerrorYouCannotInviteYourself;
    public static String inviteremovingInvite;
    public static String inviteinviteSentTo;
    public static String invitenameHasInvitedYou;
    public static String invitetoAcceptOrReject;
    public static String invitewarningYouWillLoseIsland;
    public static String inviteerrorYourIslandIsFull;
    //"That player is already with a group on an island."
    public static String inviteerrorThatPlayerIsAlreadyInATeam;
    public static String inviteerrorCoolDown;

    //reject
    public static String rejectyouHaveRejectedInvitation;
    public static String rejectnameHasRejectedInvite;
    public static String rejectyouHaveNotBeenInvited;

    //leave
    public static String leaveerrorYouAreTheLeader;
    public static String leaveyouHaveLeftTheIsland;
    public static String leavenameHasLeftYourIsland;
    public static String leaveerrorYouCannotLeaveIsland;
    public static String leaveerrorYouMustBeInWorld;
    public static String leaveerrorLeadersCannotLeave;

    //team
    public static String teamlistingMembers;


    //kick / remove
    public static String kickerrorPlayerNotInTeam;
    public static String kicknameRemovedYou;
    public static String kicknameRemoved;
    public static String kickerrorNotPartOfTeam;
    public static String kickerrorOnlyLeaderCan;
    public static String kickerrorNoTeam;

    //makeleader
    public static String makeLeadererrorPlayerMustBeOnline;
    public static String makeLeadererrorYouMustBeInTeam;
    public static String makeLeadererrorRemoveAllPlayersFirst;
    public static String makeLeaderyouAreNowTheOwner;
    public static String makeLeadernameIsNowTheOwner;
    public static String makeLeadererrorThatPlayerIsNotInTeam;
    public static String makeLeadererrorNotYourIsland;
    public static String makeLeadererrorGeneralError;

    ////////////////////////////////////////////////////////////////
    //Admin commands that use /acid //
    ////////////////////////////////////////////////////////////////

    //Help
    public static String adminHelpHelp;
    public static String adminHelpreload;
    ///acid top ten;
    public static String adminHelptopTen;
    ///acid register <player>;
    public static String adminHelpregister;
    ///acid delete <player>;
    public static String adminHelpdelete;
    ///acid completechallenge <challengename> <player>
    public static String adminHelpcompleteChallenge;
    ///acid resetchallenge <challengename> <player>
    public static String adminHelpresetChallenge;
    ///acid resetallchallenges <player>;
    public static String adminHelpresetAllChallenges;
    ///acid purge [TimeInDays];
    public static String adminHelppurge;
    ///acid info <player>;
    public static String adminHelpinfo;
    public static String adminHelpclearReset;
    
    public static String adminHelptp;

    //acid reload
    public static String reloadconfigReloaded;
    //topten
    public static String adminTopTengenerating;
    public static String adminTopTenfinished;

    //purge
    public static String purgealreadyRunning;
    public static String purgeusage;
    public static String purgecalculating;
    public static String purgenoneFound;
    public static String purgethisWillRemove;
    public static String purgewarning;
    public static String purgetypeConfirm;
    public static String purgepurgeCancelled;
    public static String purgefinished;
    public static String purgeremovingName;
    public static String adminHelppurgeholes;

    //confirm
    public static String confirmerrorTimeLimitExpired;

    //delete
    public static String deleteremoving;

    //register
    public static String registersettingIsland;
    public static String registererrorBedrockNotFound;

    //info
    public static String adminInfoislandLocation;
    public static String adminInfoerrorNotPartOfTeam;
    public static String adminInfoerrorNullTeamLeader;
    public static String adminInfoerrorTeamMembersExist;
    public static String adminHelpinfoIsland;
    public static String adminHelpSetSpawn;

    //resetallchallenges
    public static String resetChallengessuccess;

    //checkteam
    public static String checkTeamcheckingTeam;

    //completechallenge
    public static String completeChallengeerrorChallengeDoesNotExist;
    public static String completeChallengechallangeCompleted;

    //resetchallenge
    public static String resetChallengeerrorChallengeDoesNotExist;
    public static String resetChallengechallengeReset;
    
    // ASkyBlock news
    public static String newsHeadline;
    
    // Nether
    public static String netherSpawnIsProtected;
    
    // Minishop & other Control Panels
    public static String islandhelpMiniShop;
    public static String islandMiniShopTitle;
    public static String controlPanelTitle;
    public static String challengesguiTitle;
    public static String minishopBuy;
    public static String minishopSell;
    public static String minishopOutOfStock;
    
    // Ultra safe boats
    public static String boatWarningItIsUnsafe;
    public static String clearedResetLimit;
    
    public static String minishopYouBought;
    public static String minishopSellProblem;
    public static String minishopYouSold;
    public static String minishopBuyProblem;
    public static String minishopYouCannotAfford;
    
    // Sign
    public static String signLine1;
    public static String signLine2;
    public static String signLine3;
    public static String signLine4;
    
    // Biomes
    public static String islandhelpBiome;
    public static String biomeSet;
    public static String biomeUnknown;
    public static String biomeYouBought;
    public static String biomePanelTitle;
    
    // Expel
    public static String expelSuccess;
    public static String expelNotOnIsland;
    public static String expelExpelled;
    public static String expelFail;
    public static String expelNotYourself;
    
    // Mob limits
    public static String moblimitsError;
    
    // Coop
    public static String coopRemoved;
    public static String coopSuccess;
    public static String coopRemoveSuccess;
    public static String coopMadeYouCoop;
    public static String coopOnYourTeam;
    public static String islandhelpCoop;
    public static String coopInvited;
    public static String coopUseExpel;
    
    public static String errorWrongWorld;
    public static String islandcannotTeleport;
      
}
