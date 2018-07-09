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

package com.wasteofplastic.askyblock.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.wasteofplastic.askyblock.ASLocale;
import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.CoopPlay;
import com.wasteofplastic.askyblock.DeleteIslandChunk;
import com.wasteofplastic.askyblock.FileLister;
import com.wasteofplastic.askyblock.GridManager;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.PluginConfig;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.Settings.GameType;
import com.wasteofplastic.askyblock.listeners.LavaCheck;
import com.wasteofplastic.askyblock.panels.ControlPanel;
import com.wasteofplastic.askyblock.panels.SetBiome;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;
import com.wasteofplastic.askyblock.util.teleport.SafeTeleportBuilder;

/**
 * This class handles admin commands
 *
 */
public class AdminCmd implements CommandExecutor, TabCompleter {
    private ASkyBlock plugin;
    private List<UUID> removeList = new ArrayList<UUID>();
    private boolean purgeFlag = false;
    private boolean confirmReq = false;
    private boolean confirmOK = false;
    private int confirmTimer = 0;
    private boolean purgeUnownedConfirm = false;
    private HashMap<String, Island> unowned = new HashMap<String,Island>();
    private boolean asyncPending = false;

    public AdminCmd(ASkyBlock aSkyBlock) {
        this.plugin = aSkyBlock;
    }

    private void help(CommandSender sender, String label) {
        if (!(sender instanceof Player)) {
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " clearchallengereset <challenge>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpclearChallengeReset);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " clearreset <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpclearReset);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " clearresetall:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpclearResetAll);
            if (Settings.useMagicCobbleGen) {
                Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " cobblestats: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelpcobbleStats);
            }
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " completechallenge <player> <challenge>:" + ChatColor.WHITE + " "
                    + plugin.myLocale().adminHelpcompleteChallenge);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " delete <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpdelete);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " info <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfo);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " info challenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfo);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " info:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpinfoIsland);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " level <player>: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelplevel);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " listchallengeresets: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelplistChallengeResets);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " lock <player>: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelplock);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelppurge);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " name <player> <island name>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpName);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpreload);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " resetallchallenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpresetAllChallenges);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " resetchallenge <player> <challenge>:" + ChatColor.WHITE + " "
                    + plugin.myLocale().adminHelpresetChallenge);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " resetchallengeforall <challenge> [time][m/h/d]:" + ChatColor.WHITE + " "
                    + plugin.myLocale().adminHelpresetChallengeForAll);           
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " resethome <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpResetHome);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " resetname <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpResetName);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetBiome);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " setdeaths <player> <number>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetDeaths);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " setlanguage <locale>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetLanguage);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " settingsreset [help | all | flag]:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpSettingsReset);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " team add <player> <leader>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpadd);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " team kick <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpkick);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " topbreeders: " + ChatColor.WHITE + " " + plugin.myLocale().adminHelptopBreeders);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " topten:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelptopTen);
            Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " unregister <player>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpunregister);

        } else {
            // Only give help if the player has permissions
            // Permissions are split into admin permissions and mod permissions
            // Listed in alphabetical order
            Player player = (Player) sender;
            List<String> helpMessages = new ArrayList<String>();
            helpMessages.add(plugin.myLocale(player.getUniqueId()).adminHelpHelp);
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " clearchallengereset <challenge>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpclearChallengeReset);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " clearreset <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpclearReset);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.clearresetall") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " clearresetall:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpclearReset);
            }
            if (Settings.useMagicCobbleGen && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.cobblestats") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor  + "/" + label + " cobblestats: " + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpcobbleStats);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " completechallenge <player> <challengename>:" + ChatColor.WHITE + " "
                        + plugin.myLocale(player.getUniqueId()).adminHelpcompleteChallenge);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " delete <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpdelete);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.deleteisland") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " deleteisland confirm:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpdelete);
            }

            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " info:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfoIsland);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " info <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfo);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " info challenges <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpinfo);

            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " listchallengeresets: " + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelplistChallengeResets);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.lock") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " lock <player>: " + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelplock);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.name") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor  + "/" + label + " name <player> <island name>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpName);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.purge") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " purge [TimeInDays]:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurge);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " purge unowned:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurgeUnowned);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " purge allow/disallow:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelppurgeAllowDisallow);
            }

            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reload") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpreload);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.register") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " register <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpregister);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.resethome") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " resethome <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetHome);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " resetchallenge <player> <challengename>:" + ChatColor.WHITE + " "
                        + plugin.myLocale(player.getUniqueId()).adminHelpresetChallenge);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " resetchallengeforall <challenge> [time][m/h/d]:" + ChatColor.WHITE + " "
                        + plugin.myLocale(player.getUniqueId()).adminHelpresetChallengeForAll);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " resetallchallenges <player>:" + ChatColor.WHITE + " "
                        + plugin.myLocale(player.getUniqueId()).adminHelpresetAllChallenges);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.resetname") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor  + "/" + label + " resetname <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetName);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " resetsign:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetSign);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " resetsign <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpResetSign);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reserve") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " reserve <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpReserve);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " setbiome <leader> <biome>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpsetBiome);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setdeaths") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor  + "/" +label + " setdeaths <player> <number>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetDeaths);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setlanguage") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor  + "/" + label + " setlanguage <locale>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetLanguage);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.resethome") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " sethome <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetHome);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setspawn") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " setspawn:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetSpawn);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setrange") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " setrange <number>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSetRange);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " addrange <+/- number>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpAddRange);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.settingsreset") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " settingsreset [help | all | flag]:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSettingsReset);
            }
            if (Settings.teamChat && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.spy") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " spy:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpTeamChatSpy);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.switch") && !player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " switch:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpSwitch);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " team kick <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpkick);
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " team add <player> <leader>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpadd);
                // Util.sendMessage(sender, plugin.myLocale(player.getUniqueId()).helpColor + "/" + label +
                // " team delete <leader>:" + ChatColor.WHITE +
                // " Removes the leader's team compeletely.");
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topten") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " topten:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptopTen);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topbreeders") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " topbreeders: " + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptopBreeders);
            }
            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " tp <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptp);
            }
            if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null && (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tpnether") || player.isOp())) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " tpnether <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelptpNether);
            }

            if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp()) {
                helpMessages.add(plugin.myLocale(player.getUniqueId()).helpColor + "/" + label + " unregister <player>:" + ChatColor.WHITE + " " + plugin.myLocale(player.getUniqueId()).adminHelpunregister);
            }
            // Send out the help. If the player does not have permission for any commands, tell them they have no permission
            if (helpMessages.size() == 1) {
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
            } else {
                for (String line : helpMessages) {
                    Util.sendMessage(player, line);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
        // Console commands
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
            if (player.getUniqueId() == null) {
                return false;
            }
            if (split.length > 0) {
                // Admin-only commands : reload, register, delete and purge
                if (split[0].equalsIgnoreCase("reload") || split[0].equalsIgnoreCase("register") || split[0].equalsIgnoreCase("delete")
                        || split[0].equalsIgnoreCase("purge") || split[0].equalsIgnoreCase("confirm") || split[0].equalsIgnoreCase("setspawn")
                        || split[0].equalsIgnoreCase("deleteisland") || split[0].equalsIgnoreCase("setrange")
                        || split[0].equalsIgnoreCase("reserve") || split[0].equalsIgnoreCase("addrange")
                        || split[0].equalsIgnoreCase("unregister") || split[0].equalsIgnoreCase("clearresetall")
                        || split[0].equalsIgnoreCase("settingsreset") || split[0].equalsIgnoreCase("cobblestats")
                        || split[0].equalsIgnoreCase("setlanguage")) {
                    if (!checkAdminPerms(player, split)) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
                        return true;
                    }
                } else {
                    // Mod commands
                    if (!checkModPerms(player, split)) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
                        return true;
                    }
                }
            }
        }
        // Island name (can have spaces)
        if (split.length > 1 && split[0].equalsIgnoreCase("name")) {
            final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
            // plugin.getLogger().info("DEBUG: console player info UUID = "
            // + playerUUID);
            if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                return true;
            } else {
                if (split.length == 2) {
                    // Say the island name
                    Util.sendMessage(sender, plugin.getGrid().getIslandName(playerUUID));                    
                } else {
                    String name = split[2];
                    for (int i = 3; i < split.length; i++) {
                        name = name + " " + split[i];
                    }
                    if (name.length() < Settings.minNameLength) {
                        Util.sendMessage(sender, ChatColor.RED + (plugin.myLocale().errorTooShort).replace("[length]", String.valueOf(Settings.minNameLength)));
                        return true;
                    }
                    if (name.length() > Settings.maxNameLength) {
                        Util.sendMessage(sender, ChatColor.RED + (plugin.myLocale().errorTooLong).replace("[length]", String.valueOf(Settings.maxNameLength)));
                        return true;
                    }
                    plugin.getGrid().setIslandName(playerUUID, ChatColor.translateAlternateColorCodes('&', name));
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().generalSuccess);
                }
                return true;
            } 
        }


        // Check for zero parameters e.g., /asadmin
        switch (split.length) {
        case 0:
            help(sender, label);
            return true;
        case 1:
            if (split[0].equalsIgnoreCase("switch")) {
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUseInGame);
                    return true;
                }
                player = (Player) sender;
                if (player.isOp()) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminSwitchOp);
                    return true;
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.switch")) {
                    if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminSwitchRemoving);
                        //Util.sendMessage(sender, "Removing protection bypass");
                        VaultHelper.addPerm(player, "-" + Settings.PERMPREFIX + "mod.bypassprotect");
                        VaultHelper.removePerm(player, Settings.PERMPREFIX + "mod.bypassprotect");
                        if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
                            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).generalSuccess);
                        }
                    } else {
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminSwitchAdding);
                        VaultHelper.addPerm(player, Settings.PERMPREFIX + "mod.bypassprotect");
                        VaultHelper.removePerm(player, "-" + Settings.PERMPREFIX + "mod.bypassprotect");
                        if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
                            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).generalSuccess);
                        }
                    }
                }
                return true;
            }
            if (split[0].equalsIgnoreCase("setlanguage")) {
                Util.sendMessage(sender, plugin.myLocale().helpColor + plugin.myLocale().adminHelpsetLanguage);                
                return true;
            }
            if (split[0].equalsIgnoreCase("listchallengeresets")) {
                // Reset the challenge now
                for (String challenge : plugin.getChallenges().getRepeatingChallengeResets()) {
                    Util.sendMessage(sender, ChatColor.GREEN + challenge);
                }
                return true;
            } else
                if (split[0].equalsIgnoreCase("cobblestats")) {
                    if (LavaCheck.getStats().size() == 0) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().banNone);
                        return true;
                    }
                    // Display by level
                    for (Long level : LavaCheck.getStats().keySet()) {
                        if (level == Long.MIN_VALUE) {
                            Util.sendMessage(sender, plugin.myLocale().challengeslevel + ": Default");
                        } else {
                            Util.sendMessage(sender, plugin.myLocale().challengeslevel + ": " + level);
                        }
                        // Collect and sort
                        Collection<String> result = new TreeSet<String>(Collator.getInstance());
                        for (Material mat : LavaCheck.getStats().get(level).elementSet()) {
                            result.add("   " + Util.prettifyText(mat.toString()) + ": " + LavaCheck.getStats().get(level).count(mat) + "/" + LavaCheck.getStats().get(level).size() + " or " 
                                    + ((long)((double)LavaCheck.getStats().get(level).count(mat)/LavaCheck.getStats().get(level).size()*100)) 
                                    + "% (config = " + String.valueOf(LavaCheck.getConfigChances(level, mat)) + "%)");
                        }
                        // Send to player
                        for (String r: result) {
                            Util.sendMessage(sender,r);
                        }
                    }
                    return true;
                }
            if (split[0].equalsIgnoreCase("setdeaths")) {
                Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " setdeaths <player> <number>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetDeaths);
                return true;
            } else
                if (split[0].equalsIgnoreCase("settingsreset")) {
                    Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " settingsreset help");
                    return true;
                } else 
                    if (Settings.teamChat && split[0].equalsIgnoreCase("spy")) {
                        if (!(sender instanceof Player)) {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
                            return true;
                        }
                        player = (Player) sender;
                        if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.spy") || player.isOp()) {
                            if (plugin.getChatListener().toggleSpy(player.getUniqueId())) {
                                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().teamChatSpyStatusOn);
                            } else {
                                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().teamChatSpyStatusOff);
                            }
                            return true;
                        }
                    } else if (split[0].equalsIgnoreCase("lock")) {
                        // Just /asadmin lock
                        if (!(sender instanceof Player)) {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
                            return true;
                        }
                        player = (Player) sender;
                        Island island = plugin.getGrid().getIslandAt(player.getLocation());
                        // Check if island exists
                        if (island == null) {
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNotOnIsland);
                            return true;
                        } else {
                            Player owner = plugin.getServer().getPlayer(island.getOwner());
                            if (island.isLocked()) {
                                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().lockUnlocking);
                                island.setLocked(false);
                                if (owner != null) {
                                    Util.sendMessage(owner, plugin.myLocale(owner.getUniqueId()).adminLockadminUnlockedIsland);
                                } else {
                                    plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminUnlockedIsland);
                                }
                            } else {
                                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().lockLocking);
                                island.setLocked(true);
                                if (owner != null) {
                                    Util.sendMessage(owner, plugin.myLocale(owner.getUniqueId()).adminLockadminLockedIsland);
                                } else {
                                    plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminLockedIsland);
                                }
                            }
                            return true;
                        }
                    } else
                        // Find farms
                        if (split[0].equalsIgnoreCase("topbreeders")) {
                            // Go through each island and find how many farms there are
                            Util.sendMessage(sender, plugin.myLocale().adminTopBreedersFinding);
                            //TreeMap<Integer, List<UUID>> topEntityIslands = new TreeMap<Integer, List<UUID>>();
                            // Generate the stats
                            Util.sendMessage(sender, plugin.myLocale().adminTopBreedersChecking.replace("[number]",String.valueOf(plugin.getGrid().getOwnershipMap().size())));
                            // Try just finding every entity
                            final List<Entity> allEntities = ASkyBlock.getIslandWorld().getEntities();
                            final World islandWorld = ASkyBlock.getIslandWorld();
                            final World netherWorld = ASkyBlock.getNetherWorld();
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                                @Override
                                public void run() {
                                    Map<UUID, Multiset<EntityType>> result = new HashMap<UUID, Multiset<EntityType>>();
                                    // Find out where the entities are
                                    for (Entity entity: allEntities) {
                                        //System.out.println("DEBUG " + entity.getType().toString());
                                        if (entity.getLocation().getWorld().equals(islandWorld) || entity.getLocation().getWorld().equals(netherWorld)) {
                                            //System.out.println("DEBUG in world");
                                            if (entity instanceof Creature && !(entity instanceof Player)) {
                                                //System.out.println("DEBUG creature");
                                                // Find out where it is
                                                Island island = plugin.getGrid().getIslandAt(entity.getLocation());
                                                if (island != null && !island.isSpawn()) {
                                                    //System.out.println("DEBUG on island");
                                                    // Add to result
                                                    UUID owner = island.getOwner();
                                                    Multiset<EntityType> count = result.get(owner);
                                                    if (count == null) {
                                                        // New entry for owner
                                                        //System.out.println("DEBUG new entry for owner");
                                                        count = HashMultiset.create();
                                                    }
                                                    count.add(entity.getType());
                                                    result.put(owner, count);
                                                }
                                            }
                                        }
                                    }
                                    // Sort by the number of entities on each island
                                    TreeMap<Integer, List<UUID>> topEntityIslands = new TreeMap<Integer, List<UUID>>();
                                    for (Entry<UUID, Multiset<EntityType>> entry : result.entrySet()) {
                                        int numOfEntities = entry.getValue().size();
                                        List<UUID> players = topEntityIslands.get(numOfEntities);
                                        if (players == null) {
                                            players = new ArrayList<UUID>();
                                        }
                                        players.add(entry.getKey());
                                        topEntityIslands.put(numOfEntities, players);
                                    }
                                    final TreeMap<Integer, List<UUID>> topBreeders = topEntityIslands;
                                    final Map<UUID, Multiset<EntityType>> finalResult = result;
                                    // Now display results in sync thread
                                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                                        @Override
                                        public void run() {
                                            if (topBreeders.isEmpty()) {
                                                Util.sendMessage(sender, plugin.myLocale().adminTopBreedersNothing);
                                                return;
                                            }
                                            int rank = 1;
                                            // Display, largest first
                                            for (int numOfEntities : topBreeders.descendingKeySet()) {
                                                // Only bother if there's more that 5 animals
                                                if (numOfEntities > 5) {
                                                    // There can be multiple owners in the same position
                                                    List<UUID> owners = topBreeders.get(numOfEntities);
                                                    // Go through the owners one by one
                                                    for (UUID owner : owners) {
                                                        Util.sendMessage(sender, "#" + rank + " " + plugin.getPlayers().getName(owner) + " = " + numOfEntities);
                                                        String content = "";
                                                        Multiset<EntityType> entityCount = finalResult.get(owner);
                                                        for (EntityType entity: entityCount.elementSet()) {
                                                            int num = entityCount.count(entity);
                                                            String color = ChatColor.GREEN.toString();
                                                            if (num > 10 && num <= 20) {
                                                                color = ChatColor.YELLOW.toString();
                                                            } else if (num > 20 && num <= 40) {
                                                                color = ChatColor.GOLD.toString();
                                                            } else if (num > 40) {
                                                                color = ChatColor.RED.toString();
                                                            }
                                                            content += Util.prettifyText(entity.toString()) + " x " + color + num + ChatColor.WHITE + ", ";
                                                        }
                                                        int lastComma = content.lastIndexOf(",");
                                                        // plugin.getLogger().info("DEBUG: last comma " +
                                                        // lastComma);
                                                        if (lastComma > 0) {
                                                            content = content.substring(0, lastComma);
                                                        }
                                                        Util.sendMessage(sender, "  " + content);

                                                    }
                                                    rank++;
                                                    if (rank > 10) {
                                                        break;
                                                    }
                                                }
                                            }
                                            // If we didn't show anything say so
                                            if (rank == 1) {
                                                Util.sendMessage(sender, plugin.myLocale().adminTopBreedersNothing);
                                            }

                                        }});

                                }});
                            return true;
                        }
            // Delete island
            if (split[0].equalsIgnoreCase("deleteisland")) {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminDeleteIslandError);
                return true;
            }
            // Set spawn
            if (split[0].equalsIgnoreCase("setspawn")) {
                // Find the closest island
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUseInGame);
                    return true;
                }
                player = (Player) sender;
                // Island spawn must be in the island world
                if (!player.getLocation().getWorld().getName().equals(Settings.worldName)) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
                    return true;
                }
                // The island location is calculated based on the grid
                Location closestIsland = getClosestIsland(player.getLocation());
                Island oldSpawn = plugin.getGrid().getSpawn();
                Island newSpawn = plugin.getGrid().getIslandAt(closestIsland);
                if (newSpawn != null && newSpawn.isSpawn()) {
                    // Already spawn, so just set the world spawn coords
                    plugin.getGrid().setSpawnPoint(player.getLocation());
                    //ASkyBlock.getIslandWorld().setSpawnLocation(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminSetSpawnset);
                    return true;
                }
                // Space otherwise occupied - find if anyone owns it
                if (newSpawn != null && newSpawn.getOwner() != null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetSpawnownedBy.replace("[name]",plugin.getPlayers().getName(newSpawn.getOwner())));
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetSpawnmove);
                    return true;
                }
                if (oldSpawn != null) {
                    Util.sendMessage(sender, ChatColor.GOLD + "Changing spawn island location. Warning: old spawn island location at "
                            + oldSpawn.getCenter().getBlockX() + "," + oldSpawn.getCenter().getBlockZ()
                            + " will be at risk of being overwritten with new islands. Recommend to clear that old area.");
                    plugin.getGrid().deleteSpawn();
                }
                // New spawn site is free, so make it official
                if (newSpawn == null) {
                    // Make the new spawn
                    newSpawn = plugin.getGrid().addIsland(closestIsland.getBlockX(), closestIsland.getBlockZ());
                    // Set the default spawn island settings
                    newSpawn.setSpawnDefaults();
                }
                plugin.getGrid().setSpawn(newSpawn);
                plugin.getGrid().setSpawnPoint(player.getLocation());
                //ASkyBlock.getIslandWorld().setSpawnLocation(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
                Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale().adminSetSpawnsetting.replace("[location]", player.getLocation().getBlockX() + "," + player.getLocation().getBlockZ()));
                Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", newSpawn.getCenter().getBlockX() + "," + newSpawn.getCenter().getBlockZ()));
                Util.sendMessage(player, ChatColor.YELLOW + (plugin.myLocale().adminSetSpawnlimits.replace("[min]", newSpawn.getMinX() + "," + newSpawn.getMinZ())).replace("[max]",
                        (newSpawn.getMinX() + newSpawn.getIslandDistance() - 1) + "," + (newSpawn.getMinZ() + newSpawn.getIslandDistance() - 1)));
                Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(newSpawn.getProtectionSize())));
                Util.sendMessage(player, ChatColor.YELLOW + (plugin.myLocale().adminSetSpawncoords.replace("[min]",  newSpawn.getMinProtectedX() + ", " + newSpawn.getMinProtectedZ())).replace("[max]",
                        + (newSpawn.getMinProtectedX() + newSpawn.getProtectionSize() - 1) + ", "
                                + (newSpawn.getMinProtectedZ() + newSpawn.getProtectionSize() - 1)));
                if (newSpawn.isLocked()) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
                }
                // Save grid async
                plugin.getGrid().saveGrid(true);
                return true;
            } else if (split[0].equalsIgnoreCase("info") || split[0].equalsIgnoreCase("setrange")) {
                // Find the closest island
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUseInGame);
                    return true;
                }
                Location closestIsland = getClosestIsland(((Player) sender).getLocation());
                if (closestIsland == null) {
                    Util.sendMessage(sender, ChatColor.RED + "Sorry, could not find an island. Move closer?");
                    return true;
                }
                Island island = plugin.getGrid().getIslandAt(closestIsland);
                if (island != null && island.isSpawn()) {
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminInfotitle);
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
                    Util.sendMessage(sender, ChatColor.YELLOW + (plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ())).replace("[max]",
                            (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
                    Util.sendMessage(sender, ChatColor.YELLOW + (plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ())).replace("[max]",
                            + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
                                    + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
                    if (island.isLocked()) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
                    }
                    return true;
                }
                if (island == null) {
                    plugin.getLogger().info("Get island at was null" + closestIsland);
                }
                UUID target = plugin.getPlayers().getPlayerFromIslandLocation(closestIsland);
                if (target == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminInfounowned);
                    return true;
                }
                showInfo(target, sender);
                return true;
            } else if (split[0].equalsIgnoreCase("resetsign")) {
                // Find the closest island
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUseInGame);
                    return true;
                }
                player = (Player) sender;
                if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") && !player.isOp()) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
                    return true;
                }
                // Find out whether the player is looking at a warp sign
                // Look at what the player was looking at
                BlockIterator iter = new BlockIterator(player, 10);
                Block lastBlock = iter.next();
                while (iter.hasNext()) {
                    lastBlock = iter.next();
                    if (lastBlock.getType() == Material.AIR)
                        continue;
                    break;
                }
                if (!lastBlock.getType().equals(Material.SIGN_POST)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminResetSignNoSign);
                    return true;
                }
                // Check if it is a warp sign
                Sign sign = (Sign) lastBlock.getState();
                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminResetSignFound);
                // Find out whose island this is
                // plugin.getLogger().info("DEBUG: closest bedrock: " +
                // closestBedRock.toString());
                UUID target = plugin.getPlayers().getPlayerFromIslandLocation(player.getLocation());
                if (target == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminInfounowned);
                    return true;
                }
                if (plugin.getWarpSignsListener().addWarp(target, lastBlock.getLocation())) {
                    // Change sign color to green
                    sign.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
                    sign.update(true, false);
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminResetSignRescued.replace("[name]", plugin.getPlayers().getName(target)));
                    return true;
                }
                // Warp already exists
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminResetSignErrorExists.replace("[name]", plugin.getWarpSignsListener().getWarpOwner(lastBlock.getLocation())));
                return true;

            } else if (split[0].equalsIgnoreCase("reload")) {
                // Remove temp permissions
                plugin.getPlayerEvents().removeAllTempPerms();
                plugin.reloadConfig();
                PluginConfig.loadPluginConfig(plugin);
                plugin.getChallenges().reloadChallengeConfig();
                if (Settings.useEconomy && VaultHelper.setupEconomy()) {
                    ControlPanel.loadShop();
                } else {
                    Settings.useEconomy = false;
                }
                ControlPanel.loadControlPanel();
                if (Settings.updateCheck) {
                    plugin.checkUpdates();
                } else {
                    plugin.setUpdateCheck(null);
                }
                plugin.getIslandCmd().loadSchematics();
                if (plugin.getAcidTask() != null)
                    plugin.getAcidTask().runAcidItemRemovalTask();
                // Give back any temporary permissions
                plugin.getPlayerEvents().giveAllTempPerms();
                // Reset resets if the admin changes it to or from unlimited
                for (Player players: plugin.getServer().getOnlinePlayers()) {
                    UUID playerUUID = players.getUniqueId();
                    if (plugin.getPlayers().hasIsland(playerUUID) || plugin.getPlayers().inTeam(playerUUID)) {
                        if (Settings.resetLimit < plugin.getPlayers().getResetsLeft(playerUUID) || (Settings.resetLimit >= 0 && plugin.getPlayers().getResetsLeft(playerUUID) < 0)) {
                            plugin.getPlayers().setResetsLeft(playerUUID, Settings.resetLimit);
                        }
                    }
                }
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().reloadconfigReloaded);
                return true;
            } else if (split[0].equalsIgnoreCase("topten")) {
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminTopTengenerating);
                plugin.getTopTen().topTenCreate(sender);
                return true;
            } else if (split[0].equalsIgnoreCase("purge")) {
                if (purgeFlag) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().purgealreadyRunning);
                    return true;
                }
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgeusage.replace("[label]", label));
                return true;
            } else if (split[0].equalsIgnoreCase("confirm")) {
                if (!confirmReq) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().confirmerrorTimeLimitExpired);
                    return true;
                } else {
                    // Tell purge routine to go
                    confirmOK = true;
                    confirmReq = false;
                }
                return true;
            } else
                // clearesetall - clears all player resets
                if (split[0].equalsIgnoreCase("clearresetall")) {
                    if (asyncPending) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorCommandNotReady);
                        return true;
                    }
                    // Do online players first
                    plugin.getPlayers().clearResets(Settings.resetLimit);
                    // Do offline players
                    final File playerFolder = plugin.getPlayersFolder();
                    // Set the pending flag
                    asyncPending = true;
                    // Check against player files
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                        @Override
                        public void run() {
                            //System.out.println("DEBUG: Running async task");
                            int done = 0;
                            // Check files against potentialUnowned
                            FilenameFilter ymlFilter = new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String name) {
                                    String lowercaseName = name.toLowerCase();
                                    if (lowercaseName.endsWith(".yml")) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            };
                            for (File file: playerFolder.listFiles(ymlFilter)) {
                                List<String> playerFileContents = new ArrayList<String>();
                                done++;
                                try {
                                    Scanner scanner = new Scanner(file);
                                    while (scanner.hasNextLine()) {
                                        final String lineFromFile = scanner.nextLine();
                                        if (lineFromFile.contains("resetsLeft:")) {
                                            playerFileContents.add("resetsLeft: " + Settings.resetLimit);
                                        } else {
                                            playerFileContents.add(lineFromFile);
                                        }
                                    }
                                    scanner.close();
                                    // Write file
                                    try (FileWriter writer = new FileWriter(file)) {
                                        for(String str: playerFileContents) {
                                            writer.write(str + "\n");
                                        }
                                    }
                                    if (done % 500 == 0) {
                                        final int update = done;
                                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                                            @Override
                                            public void run() {
                                                // Tell player
                                                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().clearedResetLimit + " [" + update + " players]...");
                                            }});
                                    }
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            //System.out.println("DEBUG: scanning done");
                            asyncPending = false;
                            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().clearedResetLimit + " [" + done + " players] completed.");
                        }});
                    return true;
                } else {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                    return false;
                }
        case 2:
            if (split[0].equalsIgnoreCase("setlanguage")) {
                if (asyncPending) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorCommandNotReady);
                    return true;
                }
                if (plugin.getAvailableLocales().keySet().contains(split[1])) {
                    // Change the config.yml setting without removing comments
                    try {
                        Util.setConfig("defaultlanguage", Settings.defaultLanguage, split[1]);
                    } catch (IOException e) {
                        Util.sendMessage(sender, ChatColor.RED + e.getMessage());
                        return true;
                        //e.printStackTrace();
                    }
                    plugin.getConfig().set("general.defaultlanguage", split[1]);
                    Settings.defaultLanguage = split[1];

                    // Load languages
                    HashMap<String,ASLocale> availableLocales = new HashMap<String,ASLocale>();
                    FileLister fl = new FileLister(plugin);
                    try {
                        int index = 1;
                        for (String code: fl.list()) {
                            //plugin.getLogger().info("DEBUG: lang file = " + code);
                            availableLocales.put(code, new ASLocale(plugin, code, index++));
                        }
                    } catch (IOException e1) {
                        plugin.getLogger().severe("Could not add locales!");
                    }
                    if (!availableLocales.containsKey(Settings.defaultLanguage)) {
                        plugin.getLogger().severe("'" + Settings.defaultLanguage + ".yml' not found in /locale folder. Using /locale/en-US.yml");
                        Settings.defaultLanguage = "en-US";
                        availableLocales.put(Settings.defaultLanguage, new ASLocale(plugin, Settings.defaultLanguage, 0));
                    }
                    plugin.setAvailableLocales(availableLocales);
                    // Run through all the players and set their languages
                    for (UUID onlinePlayer : plugin.getPlayers().getOnlineCachedPlayers()) {
                        plugin.getPlayers().setLocale(onlinePlayer, Settings.defaultLanguage);
                    }
                    // Prepare for the async check - make final
                    final File playerFolder = plugin.getPlayersFolder();
                    // Set the pending flag
                    asyncPending = true;
                    // Change player files
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                        @Override
                        public void run() {
                            try {
                                Util.setPlayerYamlConfig(playerFolder, "locale", Settings.defaultLanguage);

                                // Run sync task
                                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                                    @Override
                                    public void run() {
                                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().generalSuccess);
                                        asyncPending = false;
                                    }} );
                            } catch (final IOException e) {
                                // Run sync task
                                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                                    @Override
                                    public void run() {
                                        Util.sendMessage(sender, ChatColor.RED + e.getMessage());
                                        asyncPending = false;
                                    }} );        
                            }
                            //System.out.println("DEBUG: scanning done");

                        }});

                    Util.sendMessage(sender, ChatColor.RED + plugin.getAvailableLocales().keySet().toString());
                }
                return true;

            }

            else if (split[0].equalsIgnoreCase("level")) {                   
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                // plugin.getLogger().info("DEBUG: console player info UUID = "
                // + playerUUID);
                if (playerUUID == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    if (sender instanceof Player) {
                        plugin.getIslandCmd().calculateIslandLevel(sender, playerUUID, false); 
                    } else {
                        plugin.getIslandCmd().calculateIslandLevel(sender, playerUUID, true);
                    }
                    return true;
                }
            }

            if (split[0].equalsIgnoreCase("clearchallengereset")) {
                split[1] = split[1].toLowerCase();
                if (!Settings.challengeList.contains(split[1])) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().resetChallengeerrorChallengeDoesNotExist);
                    return true;
                }

                // Clear challenge reset
                plugin.getChallenges().clearChallengeReset(split[1]);
                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().generalSuccess);

                return true;
            } else if (split[0].equalsIgnoreCase("resetchallengeforall")) {
                if (!Settings.challengeList.contains(split[1].toLowerCase())) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().resetChallengeerrorChallengeDoesNotExist);
                    return true;
                }

                // Reset the challenge now
                plugin.getChallenges().resetChallengeForAll(split[1].toLowerCase(), 0L, "");
                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().generalSuccess);

                return true;
            } else
                if (split[0].equalsIgnoreCase("settingsreset")) {
                    plugin.reloadConfig();
                    PluginConfig.loadPluginConfig(plugin);
                    if (split[1].equalsIgnoreCase("all")) {
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().settingsResetInProgress);
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                            @Override
                            public void run() {
                                for (Island island : plugin.getGrid().getOwnedIslands().values()) {
                                    island.setIgsDefaults();
                                }
                                for (Island island : plugin.getGrid().getUnownedIslands().values()) {
                                    island.setIgsDefaults();
                                }
                                plugin.getGrid().saveGrid();
                                // Go back to non-async world
                                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                                    @Override
                                    public void run() {
                                        // Reset any warp signs
                                        plugin.getWarpPanel().updateAllWarpText();
                                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().settingsResetDone);

                                    }});
                            }});
                        return true;
                    } else {
                        // Check if there is a flag here
                        for (SettingsFlag flag: SettingsFlag.values()) {
                            if (split[1].equalsIgnoreCase(flag.toString())) {
                                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().settingsResetInProgress);
                                final SettingsFlag flagToSet = flag;
                                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                                    @Override
                                    public void run() {
                                        for (Island island : plugin.getGrid().getOwnedIslands().values()) {
                                            island.setIgsFlag(flagToSet, Settings.defaultIslandSettings.get(flagToSet));
                                        }
                                        for (Island island : plugin.getGrid().getUnownedIslands().values()) {
                                            island.setIgsFlag(flagToSet, Settings.defaultIslandSettings.get(flagToSet));
                                        }
                                        plugin.getGrid().saveGrid();
                                        // Go back to non-async world
                                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                                            @Override
                                            public void run() {
                                                if (flagToSet.equals(SettingsFlag.PVP) || flagToSet.equals(SettingsFlag.NETHER_PVP)) {
                                                    // Reset any warp signs
                                                    plugin.getWarpPanel().updateAllWarpText();
                                                }
                                                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().settingsResetDone);

                                            }});
                                    }});
                                return true;
                            }
                        }
                        // Show help
                        Util.sendMessage(sender, plugin.myLocale().helpColor + "/" + label + " settingsreset [help | all | flag]:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpSettingsReset);
                        Util.sendMessage(sender, ChatColor.GREEN + "flag options: ");
                        String commaList = "all";
                        for (SettingsFlag flag: SettingsFlag.values()) {
                            commaList += ", " + flag.toString();
                        }
                        Util.sendMessage(sender, commaList);
                        return true;
                    }
                }
            // Resetsign <player> - makes a warp sign for player
            if (split[0].equalsIgnoreCase("resetsign")) {
                // Find the closest island
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUseInGame);
                    return true;
                }
                Player p = (Player) sender;
                if (!VaultHelper.checkPerm(p, Settings.PERMPREFIX + "mod.signadmin") && !p.isOp()) {
                    Util.sendMessage(p, ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorNoPermission);
                    return true;
                }
                // Convert target name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                } else {
                    // Check if this player has an island
                    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
                        // No island
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                        return true;
                    }
                    // Has an island
                    // Find out whether the player is looking at a warp sign
                    // Look at what the player was looking at
                    BlockIterator iter = new BlockIterator(p, 10);
                    Block lastBlock = iter.next();
                    while (iter.hasNext()) {
                        lastBlock = iter.next();
                        if (lastBlock.getType() == Material.AIR)
                            continue;
                        break;
                    }
                    // Check if it is a sign
                    if (!lastBlock.getType().equals(Material.SIGN_POST)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(p.getUniqueId()).adminResetSignNoSign);
                        return true;
                    }
                    Sign sign = (Sign) lastBlock.getState();
                    // Check if the sign is within the right island boundary
                    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
                    Island island = plugin.getGrid().getIslandAt(islandLoc);
                    if (island == null) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorNoIsland);
                        return true;
                    }
                    if (!island.inIslandSpace(sign.getLocation())) {
                        Util.sendMessage(p, ChatColor.RED + plugin.myLocale(p.getUniqueId()).adminSetHomeNotOnPlayersIsland);
                    } else {
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminResetSignFound);
                        // Find out if this player is allowed to have a sign on this island
                        if (plugin.getWarpSignsListener().addWarp(playerUUID, lastBlock.getLocation())) {
                            // Change sign color to green
                            sign.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
                            sign.update();
                            Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminResetSignRescued.replace("[name]", plugin.getPlayers().getName(playerUUID)));
                            return true;
                        }
                        // Warp already exists
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(p.getUniqueId()).adminResetSignErrorExists.replace("[name]", plugin.getWarpSignsListener().getWarpOwner(lastBlock.getLocation())));
                    }
                }
                return true;
            }
            // Delete the island you are on
            else if (split[0].equalsIgnoreCase("deleteisland")) {
                if (!split[1].equalsIgnoreCase("confirm")) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminDeleteIslandError);
                    return true;
                }
                // Get the island I am on
                Island island = plugin.getGrid().getIslandAt(((Player) sender).getLocation());
                if (island == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminDeleteIslandnoid);
                    return true;
                }
                // Try to get the owner of this island
                UUID owner = island.getOwner();
                String name = "unknown";
                if (owner != null) {
                    name = plugin.getPlayers().getName(owner);
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetSpawnownedBy.replace("[name]", name));
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminDeleteIslanduse.replace("[name]",name));
                    return true;
                } else {
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().deleteremoving.replace("[name]", name));
                    deleteIslands(island, sender);
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("resetname")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                } else {
                    // Check if this player has an island
                    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
                        // No island
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                        return true;
                    }
                    // Has an island
                    plugin.getGrid().setIslandName(playerUUID, null);
                    Util.sendMessage(sender, plugin.myLocale().generalSuccess);
                }
                return true;
            } else if (split[0].equalsIgnoreCase("resethome")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                } else {
                    // Check if this player has an island
                    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
                        // No island
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                        return true;
                    }
                    // Has an island
                    Location safeHome = plugin.getGrid().getSafeHomeLocation(playerUUID, 1);
                    if (safeHome == null) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetHomeNoneFound);
                    } else {
                        plugin.getPlayers().setHomeLocation(playerUUID, safeHome);
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminSetHomeHomeSet.replace("[location]", safeHome.getBlockX() + ", " + safeHome.getBlockY() + "," + safeHome.getBlockZ()));
                    }
                }
                return true;
            } else if (split[0].equalsIgnoreCase("sethome")) {
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
                    return true;
                }
                player = (Player)sender;
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                } else {
                    // Check if this player has an island
                    if (!plugin.getPlayers().hasIsland(playerUUID) && !plugin.getPlayers().inTeam(playerUUID)) {
                        // No island
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIslandOther);
                        return true;
                    }
                    // Has an island
                    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
                    Island island = plugin.getGrid().getIslandAt(islandLoc);
                    // Check the player is within the island boundaries
                    if (island == null || !island.inIslandSpace(player.getLocation())) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminSetHomeNotOnPlayersIsland);
                    } else {
                        // Check that the location is safe
                        if (!GridManager.isSafeLocation(player.getLocation())) {
                            // Not safe
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminSetHomeNoneFound);
                        } else {
                            // Success
                            plugin.getPlayers().setHomeLocation(playerUUID, player.getLocation());
                            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).adminSetHomeHomeSet.replace("[location]", player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ()));
                        }
                    }
                }
                return true;
            } else
                // Set protection for the island the player is on
                if (split[0].equalsIgnoreCase("setrange")) {
                    if (!(sender instanceof Player)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
                        return true;
                    }
                    player = (Player)sender;
                    UUID playerUUID = player.getUniqueId();
                    Island island = plugin.getGrid().getIslandAt(player.getLocation());
                    // Check if island exists
                    if (island == null) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale().errorNotOnIsland);
                        return true;
                    } else {
                        int newRange = 10;
                        int maxRange = Settings.islandDistance;
                        // If spawn do something different
                        if (island.isSpawn()) {
                            try {
                                newRange = Integer.valueOf(split[1]);
                            } catch (Exception e) {
                                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid);
                                return true;
                            }
                            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(playerUUID).adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
                            if (newRange > maxRange) {
                                Util.sendMessage(player, ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(playerUUID).adminSetRangeWarning.replace("[max]",String.valueOf(maxRange)));
                                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeWarning2);
                            }
                            island.setProtectionSize(newRange);
                            Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
                            Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ()).replace("[max]",
                                    (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
                            Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
                            Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ()).replace("[max]",
                                    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
                                            + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
                            if (island.isLocked()) {
                                Util.sendMessage(player, ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
                            }
                        } else {
                            try {
                                newRange = Integer.valueOf(split[1]);
                            } catch (Exception e) {
                                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid + " "  + plugin.myLocale(playerUUID).adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));
                                return true;
                            }
                            if (newRange < 10 || newRange > maxRange) {
                                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid + " "  + plugin.myLocale(playerUUID).adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));
                                return true;
                            }
                            island.setProtectionSize(newRange);
                            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(playerUUID).adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
                            showInfo(island.getOwner(), sender);
                        }
                        return true;
                    }
                } else
                    // Add/remove protection for the island the player is on
                    if (split[0].equalsIgnoreCase("addrange")) {
                        if (!(sender instanceof Player)) {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
                            return true;
                        }
                        player = (Player)sender;
                        UUID playerUUID = player.getUniqueId();
                        Island island = plugin.getGrid().getIslandAt(player.getLocation());
                        // Check if island exists
                        if (island == null) {
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale().errorNotOnIsland);
                            return true;
                        } else {
                            int newRange = island.getProtectionSize();
                            int maxRange = Settings.islandDistance;
                            // If spawn do something different
                            if (island.isSpawn()) {
                                try {
                                    newRange = Integer.valueOf(split[1]) + island.getProtectionSize();
                                } catch (Exception e) {
                                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid);
                                    return true;
                                }
                                Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(playerUUID).adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
                                if (newRange > maxRange) {
                                    Util.sendMessage(player, ChatColor.RED + "" + ChatColor.BOLD + plugin.myLocale(playerUUID).adminSetRangeWarning.replace("[max]"
                                            ,String.valueOf(maxRange)));
                                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeWarning2);
                                }
                                island.setProtectionSize(newRange);
                                Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
                                Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ()).replace("[max]",
                                        (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
                                Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
                                Util.sendMessage(player, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ()).replace("[max]",
                                        + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
                                                + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
                                if (island.isLocked()) {
                                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale().adminSetSpawnlocked);
                                }
                            } else {
                                try {
                                    newRange = Integer.valueOf(split[1]) + island.getProtectionSize();
                                } catch (Exception e) {
                                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid + " "  + plugin.myLocale(playerUUID).adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));
                                    return true;
                                }
                                if (newRange < 10 || newRange > maxRange) {
                                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).adminSetRangeInvalid + " "  + plugin.myLocale(playerUUID).adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));
                                    return true;
                                }
                                island.setProtectionSize(newRange);
                                Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(playerUUID).adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
                                showInfo(island.getOwner(), sender);
                            }
                            return true;
                        }
                    }
            if (split[0].equalsIgnoreCase("purge")) {
                // PURGE Command
                // Check for "allow" or "disallow" flags
                // Protect island from purging
                if (split[1].equalsIgnoreCase("allow") || split[1].equalsIgnoreCase("disallow")) {
                    // Find the closest island
                    if (!(sender instanceof Player)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminLockerrorInGame);
                        return true;
                    }
                    Player p = (Player) sender;
                    // Island spawn must be in the island world
                    if (!p.getLocation().getWorld().equals(ASkyBlock.getIslandWorld()) && !p.getLocation().getWorld().equals(ASkyBlock.getNetherWorld())) {
                        Util.sendMessage(p, ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorWrongWorld);
                        return true;
                    }
                    Island island = plugin.getGrid().getIslandAt(p.getLocation());
                    if (island == null) {
                        Util.sendMessage(p, ChatColor.RED + plugin.myLocale(p.getUniqueId()).errorNoIslandOther);
                        return true;
                    }
                    if (split[1].equalsIgnoreCase("disallow")) {
                        island.setPurgeProtected(true);
                    } else {
                        island.setPurgeProtected(false);
                    }
                    if (island.isPurgeProtected()) {
                        Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminPreventPurge);
                    } else {
                        Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(p.getUniqueId()).adminAllowPurge);
                    }
                    return true;
                }

                // Purge runs in the background so if one is already running
                // this flag stops a repeat
                if (purgeFlag) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().purgealreadyRunning);
                    return true;
                }

                if (split[1].equalsIgnoreCase("unowned")) {
                    countUnowned(sender);
                    return true;
                }

                if (split[1].equalsIgnoreCase("players")) {
                    purgePlayers(sender);
                    return true;
                }
                // Set the flag
                purgeFlag = true;
                // See if this purge unowned

                // Convert days to hours - no other limit checking?
                final int time;
                try {
                    time = Integer.parseInt(split[1]) * 24;
                } catch (Exception e) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().purgeusage.replace("[label]", label));
                    purgeFlag = false;
                    return true;
                }
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgecalculating.replace("[time]", split[1]));
                // Check who has not been online since the time
                for (Entry<UUID, Island> entry: plugin.getGrid().getOwnershipMap().entrySet()) {
                    //plugin.getLogger().info("UUID = " + entry.getKey());
                    // Only do this if it isn't protected
                    if (entry.getKey() != null && !entry.getValue().isPurgeProtected()) {
                        if (Bukkit.getOfflinePlayer(entry.getKey()).hasPlayedBefore()) {
                            long offlineTime = Bukkit.getOfflinePlayer(entry.getKey()).getLastPlayed();
                            offlineTime = (System.currentTimeMillis() - offlineTime) / 3600000L;
                            if (offlineTime > time && plugin.getPlayers().getIslandLevel(entry.getKey()) < Settings.abandonedIslandLevel) {
                                removeList.add(entry.getKey());
                            }
                        } else {
                            removeList.add(entry.getKey());
                        }
                    }
                }
                if (removeList.isEmpty()) {
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgenoneFound);
                    purgeFlag = false;
                    return true;
                }
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgethisWillRemove.replace("[number]", String.valueOf(removeList.size())).replace("[level]", String.valueOf(Settings.abandonedIslandLevel)));
                long runtime = removeList.size() * 2L;
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgeEstimatedRunTime.replace("[time]", String.format("%d h %02d m %02d s", runtime / 3600, (runtime % 3600) / 60, (runtime % 60))));
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().purgewarning);
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().purgetypeConfirm.replace("[label]", label));
                if (removeList.size() > Settings.maxPurge) {
                    Util.sendMessage(sender, plugin.myLocale().purgeLimit.replace("[number]",String.valueOf(Settings.maxPurge)));
                    Iterator<UUID> it = removeList.iterator();
                    int count = 1;
                    while (it.hasNext()) {
                        it.next();
                        if (count++ > Settings.maxPurge) {
                            it.remove();
                        }
                    }
                }
                confirmReq = true;
                confirmOK = false;
                confirmTimer = 0;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // This waits for 10 seconds and if no
                        // confirmation received, then it
                        // cancels
                        if (confirmTimer++ > 10) {
                            // Ten seconds is up!
                            confirmReq = false;
                            confirmOK = false;
                            purgeFlag = false;
                            removeList.clear();
                            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgepurgeCancelled);
                            this.cancel();
                        } else if (confirmOK) {
                            // Set up a repeating task to run every 2
                            // seconds to remove
                            // islands one by one and then cancel when
                            // done
                            final int total = removeList.size();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (removeList.isEmpty() && purgeFlag) {
                                        purgeFlag = false;
                                        Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgefinished);
                                        this.cancel();
                                    }

                                    if (removeList.size() > 0 && purgeFlag) {
                                        // Check if the player is online
                                        if (plugin.getServer().getPlayer(removeList.get(0)) == null) {
                                            //plugin.getLogger().info("DEBUG: player is offline");
                                            // Check the level
                                            if (plugin.getPlayers().getIslandLevel(removeList.get(0)) < Settings.abandonedIslandLevel) {
                                                Util.sendMessage(sender, ChatColor.YELLOW + "[" + (total - removeList.size() + 1) + "/" + total + "] "
                                                        + plugin.myLocale().purgeremovingName.replace("[name]", plugin.getPlayers().getName(removeList.get(0))));
                                                plugin.deletePlayerIsland(removeList.get(0), true);
                                            }
                                        } else {
                                            Util.sendMessage(sender, ChatColor.YELLOW + "[" + (total - removeList.size() + 1) + "/" + total + "] "
                                                    + "Skipping online player...");
                                        }
                                        removeList.remove(0);
                                    }
                                    //Util.sendMessage(sender, "Now waiting...");
                                }
                            }.runTaskTimer(plugin, 0L, 20L);
                            confirmReq = false;
                            confirmOK = false;
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 40L);
                return true;
            } else if (split[0].equalsIgnoreCase("lock")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    Island island = plugin.getGrid().getIsland(playerUUID);
                    if (island != null) {
                        Player owner = plugin.getServer().getPlayer(island.getOwner());
                        if (island.isLocked()) {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().lockUnlocking);
                            island.setLocked(false);
                            if (owner != null) {
                                owner.sendMessage(plugin.myLocale(owner.getUniqueId()).adminLockadminUnlockedIsland);
                            } else {
                                plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminUnlockedIsland);
                            }
                        } else {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().lockLocking);
                            island.setLocked(true);
                            if (owner != null) {
                                Util.sendMessage(owner, plugin.myLocale(owner.getUniqueId()).adminLockadminLockedIsland);
                            } else {
                                plugin.getMessages().setMessage(island.getOwner(), plugin.myLocale(island.getOwner()).adminLockadminLockedIsland);
                            }
                        }
                    } else {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                    }
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("setdeaths")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.getPlayers().getName(playerUUID) + " " + plugin.getPlayers().getDeaths(playerUUID) + " " + plugin.myLocale().deaths);
                    Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " setdeaths <player> <number>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetDeaths);
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("clearreset")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    plugin.getPlayers().setResetsLeft(playerUUID, Settings.resetLimit);
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().clearedResetLimit + " [" + Settings.resetLimit + "]");
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("tp")) {
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                    return true;
                }
                player = (Player)sender;
                // Convert name to a UUID
                final UUID targetUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(targetUUID)) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
                    return true;
                } else {
                    if (plugin.getPlayers().hasIsland(targetUUID) || plugin.getPlayers().inTeam(targetUUID)) {
                        // Teleport to the over world
                        Location warpSpot = plugin.getPlayers().getIslandLocation(targetUUID).toVector().toLocation(ASkyBlock.getIslandWorld());
                        String failureMessage = ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminTpManualWarp.replace("[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
                                + warpSpot.getBlockZ());
                        // Try the player's home first
                        Location home = plugin.getPlayers().getHomeLocation(targetUUID);
                        plugin.getGrid();
                        if (home != null && home.getWorld().equals(ASkyBlock.getIslandWorld()) && GridManager.isSafeLocation(home)) {
                            player.teleport(home);
                            return true;
                        }
                        // Other wise, go to a safe spot
                        new SafeTeleportBuilder(plugin)
                        .entity(player)
                        .location(warpSpot)
                        .failureMessage(failureMessage)
                        .build();
                        return true;
                    }
                    Util.sendMessage(sender, plugin.myLocale().errorNoIslandOther);
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("tpnether")) {
                if (!Settings.createNether || !Settings.newNether || ASkyBlock.getNetherWorld() == null) {
                    return false;
                }
                if (!(sender instanceof Player)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                    return true;
                }
                player = (Player)sender;
                // Convert name to a UUID
                final UUID targetUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(targetUUID)) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorUnknownPlayer);
                    return true;
                } else {
                    if (plugin.getPlayers().hasIsland(targetUUID) || plugin.getPlayers().inTeam(targetUUID)) {
                        // Teleport to the nether
                        Location warpSpot = plugin.getPlayers().getIslandLocation(targetUUID).toVector().toLocation(ASkyBlock.getNetherWorld());
                        String failureMessage = ChatColor.RED + plugin.myLocale(player.getUniqueId()).adminTpManualWarp.replace("[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
                                + warpSpot.getBlockZ());
                        // Try the player's home first
                        Location home = plugin.getPlayers().getHomeLocation(targetUUID);
                        plugin.getGrid();
                        if (home != null && home.getWorld().equals(ASkyBlock.getNetherWorld()) && GridManager.isSafeLocation(home)) {
                            player.teleport(home);
                            return true;
                        }
                        new SafeTeleportBuilder(plugin)
                        .entity(player)
                        .location(warpSpot)
                        .failureMessage(failureMessage)
                        .build();
                        return true;
                    }
                    Util.sendMessage(sender, plugin.myLocale().errorNoIslandOther);
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("delete")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    // This now deletes the player and cleans them up even if
                    // they don't have an island
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().deleteremoving.replace("[name]", split[1]));
                    // If they are online and in ASkyBlock then delete their
                    // stuff too
                    Player target = plugin.getServer().getPlayer(playerUUID);
                    if (target != null) {
                        // Clear any coop inventories
                        // CoopPlay.getInstance().returnAllInventories(target);
                        // Remove any of the target's coop invitees and grab
                        // their stuff
                        CoopPlay.getInstance().clearMyInvitedCoops(target);
                        CoopPlay.getInstance().clearMyCoops(target);
                        plugin.resetPlayer(target);
                    }
                    // plugin.getLogger().info("DEBUG: deleting player");
                    plugin.deletePlayerIsland(playerUUID, true);
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("reserve")) {
                // Reserves a spot for the player's next island
                if (sender instanceof Player) {
                    player = (Player)sender;
                    // Convert name to a UUID
                    final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                        return true;
                    } else {
                        // Check the spot
                        Location islandLoc = plugin.getGrid().getClosestIsland(player.getLocation());
                        Island island = plugin.getGrid().getIslandAt(islandLoc);
                        if (island == null) {
                            // Empty spot, reserve it!
                            plugin.getIslandCmd().reserveLocation(playerUUID, islandLoc);
                            Util.sendMessage(sender, ChatColor.GREEN + " [" + islandLoc.getBlockX() + ", " + islandLoc.getBlockZ() + "] " + plugin.myLocale().generalSuccess);
                        } else {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminReserveIslandExists);
                        }
                        return true;
                    }
                } else {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                }
                return true;
            } else if (split[0].equalsIgnoreCase("register")) {
                if (sender instanceof Player) {
                    // Convert name to a UUID
                    final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                    //plugin.getLogger().info("DEBUG: UUID is " + playerUUID);
                    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                        return true;
                    } else {
                        if (adminSetPlayerIsland(sender, ((Player) sender).getLocation(), playerUUID)) {
                            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().registersettingIsland.replace("[name]", split[1]));
                            plugin.getGrid().saveGrid();
                        } else {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().registererrorBedrockNotFound);
                        }
                        return true;
                    }
                } else {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                }
                return true;
            } else if (split[0].equalsIgnoreCase("unregister")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    if (plugin.getPlayers().inTeam(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminUnregisterOnTeam);
                        return true;
                    }
                    Location island = plugin.getPlayers().getIslandLocation(playerUUID);
                    if (island == null) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                        return true;
                    }
                    // Delete player, but keep blocks
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminUnregisterKeepBlocks.replace("[location]",
                            + plugin.getPlayers().getIslandLocation(playerUUID).getBlockX() + ","
                                    + plugin.getPlayers().getIslandLocation(playerUUID).getBlockZ()));
                    plugin.deletePlayerIsland(playerUUID, false);
                    plugin.getGrid().saveGrid();
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("info")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                // plugin.getLogger().info("DEBUG: console player info UUID = "
                // + playerUUID);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    showInfo(playerUUID, sender);
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("resetallchallenges")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                }
                plugin.getPlayers().resetAllChallenges(playerUUID, true);
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().resetChallengessuccess.replace("[name]", split[1]));
                return true;
            } else {
                return false;
            }
        case 3:
            if (split[0].equalsIgnoreCase("resetchallengeforall")) {
                if (!Settings.challengeList.contains(split[1].toLowerCase())) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().resetChallengeerrorChallengeDoesNotExist);
                    return true;
                }
                // Convert repeat to time in millis
                split[2] = split[2].trim();
                //plugin.getLogger().info("DEBUG: " + split[2]);
                if (split[2].length() > 1 && (split[2].toLowerCase().endsWith("m") || split[2].toLowerCase().endsWith("h") || split[2].toLowerCase().endsWith("d"))) {
                    char unit = split[2].charAt(split[2].length()-1);
                    String value = split[2].substring(0, split[2].length()-1);
                    try {
                        long repeat = 0;
                        int number = Integer.valueOf(value);
                        switch (unit) {
                        case 'm':
                            // Minutes
                            repeat = 60000L * number;
                            break;
                        case 'h':
                            repeat = 60000L * 60 * number;
                            break;
                        case 'd':
                            repeat = 60000L * 60 * 24 * number;
                            break;
                        }
                        // Reset all the players online
                        plugin.getChallenges().resetChallengeForAll(split[1].toLowerCase(), repeat, split[2]);
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().generalSuccess);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminResetChallengeForAllError);
                        return true; 
                    }
                } else {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminResetChallengeForAllError);
                    return true;
                }

                return true;
            }
            // Confirm purge unowned
            else if (split[0].equalsIgnoreCase("purge")) {
                if (purgeFlag) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().purgealreadyRunning);
                    return true;
                }
                // Check if this is purge unowned
                if (split[1].equalsIgnoreCase("unowned") && split[2].equalsIgnoreCase("confirm")) {
                    if (!purgeUnownedConfirm) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().confirmerrorTimeLimitExpired);
                        return true;
                    } else {
                        purgeUnownedConfirm = false;
                        // Purge the unowned islands
                        purgeUnownedIslands(sender);
                        return true;
                    }
                }

            }
            // Add or remove protection range
            if (split[0].equalsIgnoreCase("addrange")) {
                // Convert name to a UUID
                UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                // Check if player exists
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                }
                // Check if the target is in a team and if so, the leader needs to be adjusted
                if (plugin.getPlayers().inTeam(playerUUID)) {
                    playerUUID = plugin.getPlayers().getTeamLeader(playerUUID);
                }
                // Get the range that this player has now
                Island island = plugin.getGrid().getIsland(playerUUID);
                if (island == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                    return true;
                } else {
                    int newRange = 0;
                    int maxRange = Settings.islandDistance;
                    try {
                        newRange = Integer.valueOf(split[2]) + island.getProtectionSize();
                    } catch (Exception e) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetRangeInvalid + " "  + plugin.myLocale().adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));

                        return true;
                    }
                    if (newRange < 10 || newRange > maxRange) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetRangeInvalid + " "  + plugin.myLocale().adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));

                        return true;
                    }
                    island.setProtectionSize(newRange);
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
                    showInfo(playerUUID, sender);
                    plugin.getGrid().saveGrid();
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("setrange")) {
                // Convert name to a UUID
                UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                // Check if player exists
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                }
                // Check if the target is in a team and if so, the leader needs to be adjusted
                if (plugin.getPlayers().inTeam(playerUUID)) {
                    playerUUID = plugin.getPlayers().getTeamLeader(playerUUID);
                }
                // Get the range that this player has now
                Island island = plugin.getGrid().getIsland(playerUUID);
                if (island == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
                    return true;
                } else {
                    int newRange = 0;
                    int maxRange = Settings.islandDistance;
                    try {
                        newRange = Integer.valueOf(split[2]);
                    } catch (Exception e) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetRangeInvalid + " "  + plugin.myLocale().adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));

                        return true;
                    }
                    if (newRange < 10 || newRange > maxRange) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminSetRangeInvalid + " "  + plugin.myLocale().adminSetRangeTip.replace("[max]", String.valueOf(maxRange)));

                        return true;
                    }
                    island.setProtectionSize(newRange);
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminSetRangeSet.replace("[number]",String.valueOf(newRange)));
                    showInfo(playerUUID, sender);
                    plugin.getGrid().saveGrid();
                    return true;
                }
            } else if (split[0].equalsIgnoreCase("setdeaths")) {
                // Convert name to a UUID
                final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                } else {
                    try {
                        int newDeaths = Integer.valueOf(split[2]);
                        int oldDeaths = plugin.getPlayers().getDeaths(playerUUID);
                        plugin.getPlayers().setDeaths(playerUUID, newDeaths);
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.getPlayers().getName(playerUUID) + " " + oldDeaths + " >>> " + newDeaths + " " + plugin.myLocale().deaths);
                    } catch (Exception e) {
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.getPlayers().getName(playerUUID) + " " + plugin.getPlayers().getDeaths(playerUUID) + " " + plugin.myLocale().deaths);
                        Util.sendMessage(sender, plugin.myLocale().helpColor  + label + " setdeaths <player> <number>:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpsetDeaths);
                        return true;
                    }
                    return true;
                }
            }
            // Change biomes
            if (split[0].equalsIgnoreCase("setbiome")) {
                // Convert name to a UUID
                UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                // Check if player exists
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                }
                // Check if the target is in a team and if so, the leader
                if (plugin.getPlayers().inTeam(playerUUID)) {
                    playerUUID = plugin.getPlayers().getTeamLeader(playerUUID);
                }
                Island island = plugin.getGrid().getIsland(playerUUID);
                if (island == null) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIsland);
                    return true;
                }
                // Check if biome is valid
                Biome biome = null;
                String biomeName = split[2].toUpperCase();
                try {
                    biome = Biome.valueOf(biomeName);
                    biomeName = biome.name();
                    if (!plugin.getConfig().contains("biomes." + biomeName)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().biomeUnknown);
                        // Doing it this way ensures that only valid biomes are
                        // shown
                        for (Biome b : Biome.values()) {
                            if (plugin.getConfig().contains("biomes." + b.name())) {
                                Util.sendMessage(sender, b.name());
                            }
                        }
                        return true;
                    }
                    // Get friendly name
                    biomeName = plugin.getConfig().getString("biomes." + biomeName + ".friendlyname", Util.prettifyText(biomeName));

                } catch (Exception e) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().biomeUnknown);
                    for (Biome b : Biome.values()) {
                        if (plugin.getConfig().contains("biomes." + b.name())) {
                            Util.sendMessage(sender, b.name());
                        }
                    }
                    return true;
                }
                // Okay clear to set biome
                // Actually set the biome
                new SetBiome(plugin, island, biome, sender);
                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().biomeSet.replace("[biome]", biomeName));
                Player targetPlayer = plugin.getServer().getPlayer(playerUUID);
                if (targetPlayer != null) {
                    // Online
                    Util.sendMessage(targetPlayer, "[Admin] " + ChatColor.GREEN + plugin.myLocale(playerUUID).biomeSet.replace("[biome]", biomeName));
                } else {
                    plugin.getMessages().setMessage(playerUUID, "[Admin] " + ChatColor.GREEN + plugin.myLocale(playerUUID).biomeSet.replace("[biome]", biomeName));
                }
                return true;
            } else
                // team kick <player> and team delete <leader>
                if (split[0].equalsIgnoreCase("team")) {
                    // Convert name to a UUID
                    final UUID playerUUID = plugin.getPlayers().getUUID(split[2], true);
                    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                        return true;
                    }
                    if (split[1].equalsIgnoreCase("kick")) {
                        // Remove player from team
                        if (!plugin.getPlayers().inTeam(playerUUID)) {
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoTeam);
                            return true;
                        }
                        UUID teamLeader = plugin.getPlayers().getTeamLeader(playerUUID);
                        if (teamLeader == null) {
                            // Player is apparently in a team, but there is no team leader
                            // Remove their team status
                            // Clear the player of all team-related items
                            if (!plugin.getPlayers().setLeaveTeam(playerUUID)) {
                                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorBlockedByAPI);
                                return true;
                            }
                            plugin.getPlayers().setHomeLocation(playerUUID, null);
                            plugin.getPlayers().setIslandLocation(playerUUID, null);
                            // Remove any warps
                            plugin.getWarpSignsListener().removeWarp(playerUUID);
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().kicknameRemoved.replace("[name]", split[2]));
                            plugin.getPlayers().save(playerUUID);
                            return true;
                        }
                        // Payer is not a team leader
                        if (!teamLeader.equals(playerUUID)) {
                            // Clear the player of all team-related items
                            if (!plugin.getPlayers().setLeaveTeam(playerUUID)) {
                                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorBlockedByAPI);
                                return true;
                            }
                            plugin.getPlayers().setHomeLocation(playerUUID, null);
                            plugin.getPlayers().setIslandLocation(playerUUID, null);
                            // Clear the leader of this player and if they now have
                            // no team, remove the team
                            plugin.getPlayers().removeMember(teamLeader, playerUUID);
                            if (plugin.getPlayers().getMembers(teamLeader).size() < 2) {
                                if (!plugin.getPlayers().setLeaveTeam(teamLeader)) {
                                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorBlockedByAPI);
                                    return true;
                                }
                            }
                            // Remove any warps
                            plugin.getWarpSignsListener().removeWarp(playerUUID);
                            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().kicknameRemoved.replace("[name]", split[2]));
                            plugin.getPlayers().save(playerUUID);
                            return true;
                        } else {
                            Util.sendMessage(sender, ChatColor.RED + (plugin.myLocale().adminTeamKickLeader.replace("[label]",label)).replace("[name]",split[2]));
                            return true;
                        }
                    } else {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                        return false;
                    }
                } else if (split[0].equalsIgnoreCase("completechallenge")) {
                    // Convert name to a UUID
                    final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                        return true;
                    }
                    if (plugin.getPlayers().checkChallenge(playerUUID, split[2].toLowerCase())
                            || !plugin.getPlayers().get(playerUUID).challengeExists(split[2].toLowerCase())) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().completeChallengeerrorChallengeDoesNotExist);
                        return true;
                    }
                    plugin.getPlayers().get(playerUUID).completeChallenge(split[2].toLowerCase());
                    plugin.getPlayers().save(playerUUID);
                    Util.sendMessage(sender, ChatColor.YELLOW
                            + plugin.myLocale().completeChallengechallangeCompleted.replace("[challengename]", split[2].toLowerCase()).replace("[name]", split[1]));
                    return true;
                } else if (split[0].equalsIgnoreCase("resetchallenge")) {
                    // Convert name to a UUID
                    final UUID playerUUID = plugin.getPlayers().getUUID(split[1], true);
                    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                        return true;
                    }
                    if (!plugin.getPlayers().checkChallenge(playerUUID, split[2].toLowerCase())
                            || !plugin.getPlayers().get(playerUUID).challengeExists(split[2].toLowerCase())) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().resetChallengeerrorChallengeDoesNotExist);
                        return true;
                    }
                    plugin.getPlayers().resetChallenge(playerUUID, split[2].toLowerCase());
                    plugin.getPlayers().save(playerUUID);
                    Util.sendMessage(sender, ChatColor.YELLOW
                            + plugin.myLocale().resetChallengechallengeReset.replace("[challengename]", split[2].toLowerCase()).replace("[name]", split[1]));
                    return true;
                } else if (split[0].equalsIgnoreCase("info") && split[1].equalsIgnoreCase("challenges")) {
                    // Convert name to a UUID
                    final UUID playerUUID = plugin.getPlayers().getUUID(split[2], true);
                    // plugin.getLogger().info("DEBUG: console player info UUID = "
                    // + playerUUID);
                    if (!plugin.getPlayers().isAKnownPlayer(playerUUID)) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                        return true;
                    } else {
                        showInfoChallenges(playerUUID, sender);
                        return true;
                    }
                }
            return false;
        case 4:
            // Team add <player> <leader>
            if (split[0].equalsIgnoreCase("team") && split[1].equalsIgnoreCase("add")) {
                // Convert names to UUIDs
                final UUID playerUUID = plugin.getPlayers().getUUID(split[2], true);
                final Player targetPlayer = plugin.getServer().getPlayer(playerUUID);
                final UUID teamLeader = plugin.getPlayers().getUUID(split[3], true);
                if (!plugin.getPlayers().isAKnownPlayer(playerUUID) || !plugin.getPlayers().isAKnownPlayer(teamLeader)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownPlayer);
                    return true;
                }
                if (playerUUID.equals(teamLeader)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminTeamAddLeaderToOwn);
                    return true;
                }
                // See if leader has an island
                if (!plugin.getPlayers().hasIsland(teamLeader)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminTeamAddLeaderNoIsland);
                    return true;
                }
                // Check to see if this player is already in a team
                if (plugin.getPlayers().inTeam(playerUUID)) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().inviteerrorThatPlayerIsAlreadyInATeam);
                    return true;
                }
                // If the leader's member list does not contain their own name
                // then add it
                if (!plugin.getPlayers().getMembers(teamLeader).contains(teamLeader)) {
                    // Set up the team leader
                    if (!plugin.getPlayers().setJoinTeam(teamLeader, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader))) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorBlockedByAPI);
                        return true;
                    }
                    plugin.getPlayers().addTeamMember(teamLeader, teamLeader);
                    Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale().adminTeamAddedLeader);
                }
                // This is a hack to clear any pending invitations
                if (targetPlayer != null) {
                    Util.runCommand(targetPlayer, Settings.ISLANDCOMMAND + " decline");
                }
                // If the invitee has an island of their own
                if (plugin.getPlayers().hasIsland(playerUUID)) {
                    Location islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
                    if (islandLoc != null) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminTeamNowUnowned.replace("[name]", plugin.getPlayers().getName(playerUUID)).replace("[location]", islandLoc.getBlockX() + " "
                                + islandLoc.getBlockZ()));
                    }
                }
                // Remove their old island affiliation - do not delete the
                // island just in case
                plugin.deletePlayerIsland(playerUUID, false);
                // Join the team and set the team island location and leader
                plugin.getPlayers().setJoinTeam(playerUUID, teamLeader, plugin.getPlayers().getIslandLocation(teamLeader));
                // Configure the best home location for this player
                if (plugin.getPlayers().getHomeLocation(teamLeader) != null) {
                    plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getHomeLocation(teamLeader));
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminTeamSettingHome);
                } else {
                    plugin.getPlayers().setHomeLocation(playerUUID, plugin.getPlayers().getIslandLocation(teamLeader));
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminTeamSettingHome);
                }
                // If the leader's member list does not contain player then add
                // it
                if (!plugin.getPlayers().getMembers(teamLeader).contains(playerUUID)) {
                    plugin.getPlayers().addTeamMember(teamLeader, playerUUID);
                    Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminTeamAddingPlayer);
                } else {
                    Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale().adminTeamAlreadyOnTeam);
                }
                // Teleport the player if they are online
                if (targetPlayer != null) {
                    plugin.getGrid().homeTeleport(targetPlayer);
                }
                plugin.getGrid().saveGrid();
                return true;
            } else {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorUnknownCommand);
                return false;
            }
        default:
            return false;
        }
    }

    private void purgePlayers(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // This map is a list of owner and island level
            YamlConfiguration player = new YamlConfiguration();
            File oldPlayers = new File(plugin.getPlayersFolder(), "oldplayers");
            if (!oldPlayers.exists()) {
                oldPlayers.mkdirs();
            }
            Path targetPath = oldPlayers.toPath();
            int index = 0;
            for (final File f : plugin.getPlayersFolder().listFiles()) {
                // Need to remove the .yml suffix
                String fileName = f.getName();
                if (fileName.endsWith(".yml")) {
                    try {
                        player.load(f);
                        index++;
                        if (index % 1000 == 0) {
                            plugin.getLogger().info("Processed " + index + " players");
                        }
                        if (!player.getBoolean("hasIsland") && !player.getBoolean("hasTeam")) {
                            Path fileToMovePath = f.toPath();
                            Files.move(fileToMovePath, targetPath.resolve(fileToMovePath.getFileName()));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error when moving player file. File is " + fileName);
                        plugin.getLogger().severe("Look at the stack trace and edit the file - it probably has broken YAML in it for some reason.");
                        e.printStackTrace();
                    }
                }
            }
            plugin.getLogger().info("Processed " + index + " players for top ten");

            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (sender != null) {
                        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().generalSuccess);
                    } else {
                        plugin.getLogger().warning("Completed player purge.");
                    }

                }});
        });

    }

    /**
     * Deletes the overworld and nether islands together
     * @param island
     * @param sender
     */
    private void deleteIslands(Island island, CommandSender sender) {
        plugin.getGrid().removePlayersFromIsland(island,null);
        // Reset the biome
        new SetBiome(plugin, island, Settings.defaultBiome, null);
        new DeleteIslandChunk(plugin, island);
        //new DeleteIslandByBlock(plugin, island);
        plugin.getGrid().saveGrid();
    }

    /**
     * Purges the unowned islands upon direction from sender
     * @param sender
     */

    private void purgeUnownedIslands(final CommandSender sender) {
        purgeFlag = true;
        final int total = unowned.size();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (unowned.isEmpty()) {
                    purgeFlag = false;
                    Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().purgefinished);
                    this.cancel();
                    plugin.getGrid().saveGrid();
                }
                if (unowned.size() > 0) {
                    Iterator<Entry<String, Island>> it = unowned.entrySet().iterator();
                    Entry<String,Island> entry = it.next();
                    if (entry.getValue().getOwner() == null) {
                        Util.sendMessage(sender, ChatColor.YELLOW + "[" + (total - unowned.size() + 1) + "/" + total + "] " + plugin.myLocale().purgeRemovingAt.replace("[location]",
                                entry.getValue().getCenter().getWorld().getName() + " " + entry.getValue().getCenter().getBlockX()
                                + "," + entry.getValue().getCenter().getBlockZ()));
                        deleteIslands(entry.getValue(),sender);
                    }
                    // Remove from the list
                    it.remove();
                }
                Util.sendMessage(sender, plugin.myLocale().purgeNowWaiting);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    /**
     * Counts unowned islands
     * @param sender
     */
    private void countUnowned(final CommandSender sender) {
        unowned = plugin.getGrid().getUnownedIslands();
        if (!unowned.isEmpty()) {
            purgeFlag = true;
            Util.sendMessage(sender, plugin.myLocale().purgeCountingUnowned);
            // Prepare for the async check - make final
            final File playerFolder = plugin.getPlayersFolder();
            // Set the pending flag
            asyncPending = true;
            // Check against player files
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    //System.out.println("DEBUG: Running async task");
                    // Check files against potentialUnowned
                    FilenameFilter ymlFilter = new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            String lowercaseName = name.toLowerCase();
                            if (lowercaseName.endsWith(".yml")) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    };
                    for (File file: playerFolder.listFiles(ymlFilter)) {
                        try {
                            Scanner scanner = new Scanner(file);
                            while (scanner.hasNextLine()) {
                                final String lineFromFile = scanner.nextLine();
                                if (lineFromFile.contains("islandLocation:")) {
                                    // Check against potentialUnowned list
                                    String loc = lineFromFile.substring(lineFromFile.indexOf(' ')).trim();
                                    //System.out.println("DEBUG: Location in player file is " + loc);
                                    if (unowned.containsKey(loc)) {
                                        //System.out.println("DEBUG: Location found in player file - do not delete");
                                        unowned.remove(loc);
                                    }
                                    break;
                                }
                            }
                            scanner.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    //System.out.println("DEBUG: scanning done");
                    asyncPending = false;
                }});
            // Create a repeating task to check if the async task has completed

            new BukkitRunnable() {

                @Override
                public void run() {
                    if (asyncPending) {
                        // Still waiting
                        Util.sendMessage(sender, plugin.myLocale().purgeStillChecking);
                    } else {
                        // Done
                        //plugin.getLogger().info("DEBUG: unowned size = " + unowned.size());
                        if (unowned.size() > 0) {
                            if (Settings.GAMETYPE.equals(GameType.ASKYBLOCK)) {
                                Util.sendMessage(sender, plugin.myLocale().purgeSkyBlockFound.replace("[number]", String.valueOf(unowned.size())));
                            } else {
                                Util.sendMessage(sender, plugin.myLocale().purgeAcidFound.replace("[number]", String.valueOf(unowned.size())));
                            }
                            if (unowned.size() > Settings.maxPurge) {
                                Util.sendMessage(sender, plugin.myLocale().purgeLimit.replace("[number]", String.valueOf(Settings.maxPurge)));
                                Iterator<Entry<String, Island>> it = unowned.entrySet().iterator();
                                int count = 1;
                                while (it.hasNext()) {
                                    it.next();
                                    if (count++ > Settings.maxPurge) {
                                        //plugin.getLogger().info("DEBUG: removing record");
                                        it.remove();
                                    }
                                }
                            }
                            //plugin.getLogger().info("DEBUG: unowned size after = " + unowned.size());
                            purgeUnownedConfirm = true;
                            purgeFlag = false;
                            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

                                @Override
                                public void run() {
                                    if (purgeUnownedConfirm) {
                                        purgeUnownedConfirm = false;
                                        Util.sendMessage(sender, plugin.myLocale().purgepurgeCancelled);
                                    }
                                }}, 400L);
                        } else {
                            Util.sendMessage(sender, plugin.myLocale().purgenoneFound);
                            purgeFlag = false;
                        }
                        this.cancel();
                        plugin.getGrid().saveGrid();
                    }

                }
            }.runTaskTimer(plugin,20L,20L);
        } else {
            Util.sendMessage(sender, plugin.myLocale().purgenoneFound);
        }
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     *
     * @param location
     * @return Location of where an island should be on a grid in this world
     */
    public static Location getClosestIsland(Location location) {
        long x = Math.round((double) location.getBlockX() / Settings.islandDistance) * Settings.islandDistance + Settings.islandXOffset;
        long z = Math.round((double) location.getBlockZ() / Settings.islandDistance) * Settings.islandDistance + Settings.islandZOffset;
        long y = Settings.islandHeight;
        return new Location(location.getWorld(), x, y, z);
    }

    /**
     * Shows info on a player
     *
     * @param playerUUID - the player's UUID
     * @param sender
     */
    private void showInfo(UUID playerUUID, CommandSender sender) {
        Util.sendMessage(sender, plugin.myLocale().adminInfoPlayer + ": " + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
        Util.sendMessage(sender, ChatColor.WHITE + "UUID: " + playerUUID.toString());
        // Display island level
        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().levelislandLevel + ": " + plugin.getPlayers().getIslandLevel(playerUUID));
        // Last login
        try {
            Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed());
            Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale().adminInfoLastLogin + ": " + d.toString());
        } catch (Exception e) {
        }
        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().deaths + ": " + plugin.getPlayers().getDeaths(playerUUID));
        String resetsLeft = plugin.myLocale().unlimited;
        if (plugin.getPlayers().getResetsLeft(playerUUID) >= 0) {
            resetsLeft = String.valueOf(plugin.getPlayers().getResetsLeft(playerUUID)) + " / " + String.valueOf(Settings.resetLimit);
        }
        Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().resetsLeft + ": " + resetsLeft);
        Location islandLoc = null;
        // Teams
        if (plugin.getPlayers().inTeam(playerUUID)) {
            final UUID leader = plugin.getPlayers().getTeamLeader(playerUUID);
            final List<UUID> pList = plugin.getPlayers().getMembers(leader);
            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminInfoTeamLeader + ": " + plugin.getPlayers().getName(leader));
            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminInfoTeamMembers + ":");
            for (UUID member : pList) {
                if (!member.equals(leader)) {
                    Util.sendMessage(sender, ChatColor.WHITE + " - " + plugin.getPlayers().getName(member));
                }
            }
            islandLoc = plugin.getPlayers().getTeamIslandLocation(playerUUID);
        } else {
            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().errorNoTeam);
            if (plugin.getPlayers().hasIsland(playerUUID)) {
                islandLoc = plugin.getPlayers().getIslandLocation(playerUUID);
            }
            if (!(plugin.getPlayers().getTeamLeader(playerUUID) == null)) {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminInfoerrorNullTeamLeader);
            }
            if (!plugin.getPlayers().getMembers(playerUUID).isEmpty()) {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminInfoerrorTeamMembersExist);
            }
        }
        if (islandLoc != null) {
            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminInfoislandLocation + ":" + ChatColor.WHITE + " (" + islandLoc.getBlockX() + ","
                    + islandLoc.getBlockY() + "," + islandLoc.getBlockZ() + ")");
            Island island = plugin.getGrid().getIslandAt(islandLoc);
            if (island == null) {
                if (plugin.getGrid().onGrid(islandLoc)) {
                    plugin.getLogger().warning("Player has an island, but it is not in the grid. Adding it now...");
                    island = plugin.getGrid().addIsland(islandLoc.getBlockX(), islandLoc.getBlockZ(), playerUUID);
                } else {
                    plugin.getLogger().severe("Player file says they have an island, but it is not in the grid and has the wrong coordinates to be added! Use register to correct!");
                    Util.sendMessage(sender, ChatColor.RED + "See console for error!");
                    return;
                }
            }
            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminSetSpawncenter.replace("[location]", island.getCenter().getBlockX() + "," + island.getCenter().getBlockZ()));
            Util.sendMessage(sender, ChatColor.YELLOW + (plugin.myLocale().adminSetSpawnlimits.replace("[min]", island.getMinX() + "," + island.getMinZ())).replace("[max]",
                    (island.getMinX() + island.getIslandDistance() - 1) + "," + (island.getMinZ() + island.getIslandDistance() - 1)));
            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminSetSpawnrange.replace("[number]",String.valueOf(island.getProtectionSize())));
            Util.sendMessage(sender, ChatColor.YELLOW + (plugin.myLocale().adminSetSpawncoords.replace("[min]",  island.getMinProtectedX() + ", " + island.getMinProtectedZ())).replace("[max]",
                    + (island.getMinProtectedX() + island.getProtectionSize() - 1) + ", "
                            + (island.getMinProtectedZ() + island.getProtectionSize() - 1)));
            if (island.isSpawn()) {
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminInfoIsSpawn);
            }
            if (island.isLocked()) {
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminInfoIsLocked);
            } else {
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminInfoIsUnlocked);
            }
            if (island.isPurgeProtected()) {
                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminInfoIsProtected);
            } else {
                Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().adminInfoIsUnprotected);
            }
            List<UUID> banList = plugin.getPlayers().getBanList(playerUUID);
            if (!banList.isEmpty()) {
                Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminInfoBannedPlayers + ":");
                String list = "";
                for (UUID uuid : banList) {
                    Player target = plugin.getServer().getPlayer(uuid);
                    if (target != null) {
                        //online
                        list += target.getName() + ", ";
                    } else {
                        list += plugin.getPlayers().getName(uuid) + ", ";
                    }
                }
                if (!list.isEmpty()) {
                    Util.sendMessage(sender, ChatColor.RED + list.substring(0, list.length()-2));
                }
            }
            // Number of hoppers
            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminInfoHoppers.replace("[number]", String.valueOf(island.getHopperCount())));
        } else {
            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().errorNoIslandOther);
        }
    }

    /**
     * Shows info on the challenge situation for player
     *
     * @param playerUUID - the player's UUID
     * @param sender
     */
    private void showInfoChallenges(UUID playerUUID, CommandSender sender) {
        Util.sendMessage(sender, "Name:" + ChatColor.GREEN + plugin.getPlayers().getName(playerUUID));
        Util.sendMessage(sender, ChatColor.WHITE + "UUID: " + playerUUID.toString());
        // Completed challenges
        Util.sendMessage(sender, ChatColor.WHITE + plugin.myLocale().challengesguiTitle + ":");
        Map<String, Boolean> challenges = plugin.getPlayers().getChallengeStatus(playerUUID);
        Map<String, Integer> challengeTimes = plugin.getPlayers().getChallengeTimes(playerUUID);
        for (String c : challenges.keySet()) {
            if (challengeTimes.containsKey(c)) {
                Util.sendMessage(sender, c + ": "
                        + ((challenges.get(c)) ? ChatColor.GREEN + plugin.myLocale().challengescomplete : ChatColor.AQUA + plugin.myLocale().challengesincomplete) + "("
                        + plugin.getPlayers().checkChallengeTimes(playerUUID, c) + ")");

            } else {
                Util.sendMessage(sender, c + ": "
                        + ((challenges.get(c)) ? ChatColor.GREEN + plugin.myLocale().challengescomplete : ChatColor.AQUA + plugin.myLocale().challengesincomplete));
            }
        }
    }

    private boolean checkAdminPerms(Player player2, String[] split) {
        // Check perms quickly for this command
        if (player2.isOp()) {
            return true;
        }
        String check = split[0];
        if (check.equalsIgnoreCase("confirm"))
            check = "purge";
        if (VaultHelper.checkPerm(player2, Settings.PERMPREFIX + "admin." + check.toLowerCase())) {
            return true;
        }
        return false;
    }

    private boolean checkModPerms(Player player2, String[] split) {
        // Check perms quickly for this command
        if (player2.isOp()) {
            return true;
        }
        String check = split[0];
        // Check special cases
        if (check.contains("challenge".toLowerCase())) {
            check = "challenges";
        }
        if (VaultHelper.checkPerm(player2, Settings.PERMPREFIX + "mod." + check.toLowerCase())) {
            return true;
        }
        return false;
    }

    /**
     * Assigns player to an island
     *
     * @param sender
     *            - the player requesting the assignment
     * @param l
     *            - the location of sender
     * @param newOwner
     *            - the assignee
     * @return - true if successful, false if not
     */
    public boolean adminSetPlayerIsland(final CommandSender sender, final Location l, final UUID newOwner) {
        // Location island = getClosestIsland(l);
        // Check what the grid thinks
        Island island = plugin.getGrid().getIslandAt(l);
        if (island == null) {
            // Try to find it and create it if it isn't known
            Location closestIsland = plugin.getGrid().getClosestIsland(l);
            // Double check this is not taken already
            island = plugin.getGrid().getIslandAt(closestIsland);
            if (island == null) {
                // Still not known - make an island
                island = plugin.getGrid().addIsland(closestIsland.getBlockX(), closestIsland.getBlockZ());
            }
        }
        if (island.isSpawn()) {
            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminRegisterNotSpawn);
            return false;
        }
        UUID oldOwner = island.getOwner();
        if (oldOwner != null) {
            if (plugin.getPlayers().inTeam(oldOwner)) {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminRegisterLeadsTeam.replace("[name]", plugin.getPlayers().getName(oldOwner)));
                return false;
            }
            Util.sendMessage(sender, ChatColor.RED + plugin.myLocale().adminRegisterTaking.replace("[name]", plugin.getPlayers().getName(oldOwner)));
            plugin.getPlayers().setIslandLevel(newOwner, plugin.getPlayers().getIslandLevel(oldOwner));
            plugin.getPlayers().setTeamIslandLocation(oldOwner, null);
            plugin.getPlayers().setHasIsland(oldOwner, false);
            plugin.getPlayers().setIslandLocation(oldOwner, null);
            plugin.getPlayers().setIslandLevel(oldOwner, 0);
            plugin.getPlayers().setTeamIslandLocation(oldOwner, null);
            // plugin.topTenChangeOwner(oldOwner, newOwner);
        }
        // Check if the assigned player already has an island
        Island playersIsland = plugin.getGrid().getIsland(newOwner);
        if (playersIsland != null) {
            Util.sendMessage(sender, ChatColor.RED + (plugin.myLocale().adminRegisterHadIsland.replace("[name]", plugin.getPlayers().getName(playersIsland.getOwner())).replace("[location]",
                    playersIsland.getCenter().getBlockX() + "," + playersIsland.getCenter().getBlockZ())));
            plugin.getGrid().setIslandOwner(playersIsland, null);
        }
        if (sender instanceof Player && Settings.createNether && Settings.newNether && 
                ((Player)sender).getWorld().equals(ASkyBlock.getNetherWorld())) {
            // Island in new nether
            plugin.getPlayers().setHomeLocation(newOwner, island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld()));
            plugin.getPlayers().setIslandLocation(newOwner, island.getCenter().toVector().toLocation(ASkyBlock.getNetherWorld()));
        } else {
            // Island in overworld
            plugin.getPlayers().setHomeLocation(newOwner, island.getCenter());
            plugin.getPlayers().setIslandLocation(newOwner, island.getCenter());
        }
        plugin.getPlayers().setHasIsland(newOwner, true);

        // Change the grid
        plugin.getGrid().setIslandOwner(island, newOwner);
        return true;
    }


    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        final List<String> options = new ArrayList<String>();
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");

        if (!(sender instanceof Player)) {
            //Server console or something else; doesn't have
            //permission checking.

            switch (args.length) {
            case 0:
            case 1:
                options.addAll(Arrays.asList("reload", "topten", "unregister",
                        "delete", "completechallenge", "resetchallenge", "resetchallengeforall",
                        "resetallchallenges", "purge", "info", "info", "info", "listchallengeresets",
                        "clearreset", "clearresetall", "setbiome", "topbreeders", "team",
                        "name", "setdeaths", "settingsreset", "setrange", "addrange",
                        "resetname", "register", "cobblestats", "clearchallengereset",
                        "setlanguage"));
                break;
            case 2:
                if (args[0].equalsIgnoreCase("setlanguage")) {
                    options.addAll(plugin.getAvailableLocales().keySet());
                }
                if (args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("resetname") || args[0].equalsIgnoreCase("setdeaths")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("setrange") || args[0].equalsIgnoreCase("addrange")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("resetsign")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("unregister") || args[0].equalsIgnoreCase("register")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("resetchallenge")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("completechallenge") || args[0].equalsIgnoreCase("resetallchallenges")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("info")) {
                    options.add("challenges");
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("clearreset")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("setbiome")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("team")) {
                    options.add("add");
                    options.add("kick");
                }
                if (args[0].equalsIgnoreCase("settingsreset")) {
                    options.add("help");
                    options.add("all");
                    for (SettingsFlag flag: SettingsFlag.values()) {
                        options.add(flag.toString());
                    }
                }
                if (args[0].equalsIgnoreCase("resetchallengeforall")) {
                    options.addAll(Settings.challengeList);
                }
                if (args[0].equalsIgnoreCase("clearchallengereset")) {
                    options.addAll(plugin.getChallenges().getRepeatingChallengeResetsRaw());
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("completechallenge")) {
                    UUID uuid = plugin.getPlayers().getUUID(args[1]);
                    //plugin.getLogger().info("DEBUG: uuid = " + uuid);
                    if (uuid != null)  {
                        options.addAll(plugin.getPlayers().getChallengesNotDone(uuid));
                    } else {
                        options.addAll(plugin.getChallenges().getAllChallenges());  
                    }
                }
                if (args[0].equalsIgnoreCase("resetchallenge")) {
                    UUID uuid = plugin.getPlayers().getUUID(args[1]);
                    //plugin.getLogger().info("DEBUG: uuid = " + uuid);
                    if (uuid != null)  {
                        options.addAll(plugin.getPlayers().getChallengesDone(uuid));
                    } else {
                        options.addAll(plugin.getChallenges().getAllChallenges());  
                    }
                }
                if (args[0].equalsIgnoreCase("info")
                        && args[1].equalsIgnoreCase("challenges")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if (args[0].equalsIgnoreCase("setbiome")) {
                    final Biome[] biomes = Biome.values();
                    for (Biome b : biomes) {
                        if (plugin.getConfig().contains("biomes." + b.name())) {
                            options.add(b.name());
                        }
                    }
                }
                if (args[0].equalsIgnoreCase("team")
                        && (args[1].equalsIgnoreCase("add")
                                || args[1].equalsIgnoreCase("kick"))) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                break;
            case 4:
                if (args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("add")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
            }
        } else {
            final Player player = (Player) sender;

            switch (args.length) {
            case 0:
            case 1:
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setlanguage") || player.isOp()) {
                    options.add("setlanguage");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reload") || player.isOp()) {
                    options.add("reload");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.register") || player.isOp()) {
                    options.add("register");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp()) {
                    options.add("unregister");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reserve") || player.isOp()) {
                    options.add("reserve");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp()) {
                    options.add("delete");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.deleteisland") || player.isOp()) {
                    options.add("deleteisland");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.purge") || player.isOp()) {
                    options.add("purge");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topten") || player.isOp()) {
                    options.add("topten");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.topbreeders") || player.isOp()) {
                    options.add("topbreeders");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp()) {
                    options.add("completechallenge");
                    options.add("resetchallenge");
                    options.add("resetallchallenges");
                    options.add("listchallengeresets");
                    options.add("resetchallengeforall");
                    options.add("clearchallengereset");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp()) {
                    options.add("info");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp()) {
                    options.add("clearreset");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setdeaths") || player.isOp()) {
                    options.add("setdeaths");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.clearresetall") || player.isOp()) {
                    options.add("clearresetall");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setspawn") || player.isOp()) {
                    options.add("setspawn");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setrange") || player.isOp()) {
                    options.add("setrange");
                    options.add("addrange");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp()) {
                    options.add("tp");
                }
                if (Settings.createNether && Settings.newNether && ASkyBlock.getNetherWorld() != null && (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tpnether") || player.isOp())) {
                    options.add("tpnether");
                }

                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp()) {
                    options.add("setbiome");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp()) {
                    options.add("team");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.lock") || player.isOp()) {
                    options.add("lock");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") || player.isOp()) {
                    options.add("resetsign");
                }
                if (Settings.teamChat && VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.spy") || player.isOp()) {
                    options.add("spy");
                }
                if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.switch")) {
                    options.add("switch");
                }
                break;
            case 2:
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setlanguage") || player.isOp())
                        && args[0].equalsIgnoreCase("setlanguage")) {
                    options.addAll(plugin.getAvailableLocales().keySet());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.setrange") || player.isOp()) 
                        && (args[0].equalsIgnoreCase("setrange") || args[0].equalsIgnoreCase("addrange"))) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.reserve") || player.isOp()) 
                        && args[0].equalsIgnoreCase("reserve")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.lock") || player.isOp())
                        && args[0].equalsIgnoreCase("lock")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.signadmin") || player.isOp())
                        && args[0].equalsIgnoreCase("signadmin")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.unregister") || player.isOp())
                        && args[0].equalsIgnoreCase("unregister")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "admin.delete") || player.isOp())
                        && args[0].equalsIgnoreCase("delete")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
                        && (args[0].equalsIgnoreCase("completechallenge") || args[0].equalsIgnoreCase("resetchallenge"))) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
                        && args[0].equalsIgnoreCase("resetallchallenges")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
                        && args[0].equalsIgnoreCase("resetchallengeforall")) {
                    options.addAll(Settings.challengeList);
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())
                        && args[0].equalsIgnoreCase("clearchallengereset")) {
                    options.addAll(plugin.getChallenges().getRepeatingChallengeResetsRaw());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp())
                        && args[0].equalsIgnoreCase("info")) {
                    options.add("challenges");
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.clearreset") || player.isOp())
                        && args[0].equalsIgnoreCase("clearreset")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setdeaths") || player.isOp())
                        && args[0].equalsIgnoreCase("setdeaths")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.tp") || player.isOp())
                        && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("tpnether"))) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp())
                        && args[0].equalsIgnoreCase("setbiome")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp())
                        && args[0].equalsIgnoreCase("team")) {
                    options.add("kick");
                    options.add("add");
                }
                break;
            case 3:
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.challenges") || player.isOp())) {
                    if (args[0].equalsIgnoreCase("completechallenge")) {
                        UUID uuid = plugin.getPlayers().getUUID(args[1]);
                        //plugin.getLogger().info("DEBUG: uuid = " + uuid);
                        if (uuid != null)  {
                            options.addAll(plugin.getPlayers().getChallengesNotDone(uuid));
                        } else {
                            options.addAll(plugin.getChallenges().getAllChallenges());  
                        }
                    }
                    if (args[0].equalsIgnoreCase("resetchallenge")) {
                        UUID uuid = plugin.getPlayers().getUUID(args[1]);
                        //plugin.getLogger().info("DEBUG: uuid = " + uuid);
                        if (uuid != null)  {
                            options.addAll(plugin.getPlayers().getChallengesDone(uuid));
                        } else {
                            options.addAll(plugin.getChallenges().getAllChallenges());  
                        }
                    }
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.info") || player.isOp())
                        && args[0].equalsIgnoreCase("info")
                        && args[1].equalsIgnoreCase("challenges")) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.setbiome") || player.isOp())
                        && args[0].equalsIgnoreCase("setbiome")) {
                    final Biome[] biomes = Biome.values();
                    for (Biome b : biomes) {
                        if (plugin.getConfig().contains("biomes." + b.name())) {
                            options.add(b.name());
                        }
                    }
                }
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp())
                        && args[0].equalsIgnoreCase("team")
                        && (args[1].equalsIgnoreCase("add")
                                || args[1].equalsIgnoreCase("kick"))) {
                    options.addAll(Util.getOnlinePlayerList());
                }
                break;
            case 4:
                if ((VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.team") || player.isOp())
                        && args[0].equalsIgnoreCase("team")
                        && args[1].equalsIgnoreCase("add")) {
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        options.add(p.getName());
                    }
                }
            }
        }

        return Util.tabLimit(options, lastArg);
    }
}
