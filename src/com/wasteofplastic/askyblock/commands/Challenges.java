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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.events.ChallengeCompleteEvent;
import com.wasteofplastic.askyblock.events.ChallengeLevelCompleteEvent;
import com.wasteofplastic.askyblock.nms.NMSAbstraction;
import com.wasteofplastic.askyblock.panels.CPItem;
import com.wasteofplastic.askyblock.util.SpawnEgg1_9;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

import net.milkbowl.vault.economy.EconomyResponse;

/**
 * Handles challenge commands and related methods
 */
@SuppressWarnings("deprecation")
public class Challenges implements CommandExecutor, TabCompleter {
    private ASkyBlock plugin;
    private static final boolean DEBUG = false;
    // Database of challenges
    private static LinkedHashMap<String, List<String>> challengeList = new LinkedHashMap<String, List<String>>();
    private HashMap<UUID, List<CPItem>> playerChallengeGUI = new HashMap<UUID, List<CPItem>>();
    private HashMap<UUID, String> playerChallengeLevel = new HashMap<UUID, String>();
    private YamlConfiguration resettingChallenges;
    // Where challenges are stored
    private static FileConfiguration challengeFile = null;
    private static File challengeConfigFile = null;

    public Challenges(ASkyBlock plugin) {
        this.plugin = plugin;
        saveDefaultChallengeConfig();
        reloadChallengeConfig();
        resettingChallenges = Util.loadYamlFile("resettimers.yml");
    }

    /*
     * (non-Javadoc)
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] cmd) {
        if (!(sender instanceof Player)) {
            return false;
        }
        final Player player = (Player) sender;
        if (player.getUniqueId() == null) {
            return false;
        }
        /*
         * if
         * (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
         * Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
         * return true;
         * }
         */
        // Check permissions
        if (!VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.challenges")) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoPermission);
            return true;
        }
        // Check island
        if (plugin.getGrid().getIsland(player.getUniqueId()) == null) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorNoIsland);
            return true;
        }
        switch (cmd.length) {
        case 0:
            // Display panel
            player.openInventory(challengePanel(player));
            return true;
        case 1:
            if (cmd[0].equalsIgnoreCase("help") || cmd[0].equalsIgnoreCase("complete") || cmd[0].equalsIgnoreCase("c")) {
                Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengeshelp1);
                Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengeshelp2);
            } else if (isLevelAvailable(player, getChallengeConfig().getString("challenges.challengeList." + cmd[0].toLowerCase().replaceAll("\\.","") + ".level"))) {
                // Provide info on the challenge
                // Challenge Name
                // Description
                // Type
                // Items taken or not
                // island or not
                final String challenge = cmd[0].toLowerCase().toLowerCase().replaceAll("\\.","");
                Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesname + ": " + ChatColor.WHITE + challenge);
                Util.sendMessage(sender, ChatColor.WHITE + plugin.myLocale(player.getUniqueId()).challengeslevel + ": " + ChatColor.GOLD
                        + getChallengeConfig().getString("challenges.challengeList." + challenge + ".level", ""));
                String desc = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challenge + ".description", "").replace("[label]", Settings.ISLANDCOMMAND));
                List<String> result = new ArrayList<String>();
                if (desc.contains("|")) {
                    result.addAll(Arrays.asList(desc.split("\\|")));
                } else {
                    result.add(desc);
                }
                for (String line: result) {
                    Util.sendMessage(sender, ChatColor.GOLD + line);
                }
                final String type = getChallengeConfig().getString("challenges.challengeList." + challenge + ".type", "").toLowerCase();
                if (type.equals("inventory")) {
                    if (getChallengeConfig().getBoolean("challenges.challengeList." + cmd[0].toLowerCase() + ".takeItems")) {
                        Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesitemTakeWarning);
                    }
                } else if (type.equals("island")) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorItemsNotThere);
                }
                if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)
                        && (!type.equals("inventory") || !getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable", false))) {
                    Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
                    return true;
                }
                double moneyReward = 0;
                int expReward = 0;
                String rewardText = "";

                if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
                    // First time
                    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0D);
                    rewardText = ChatColor.translateAlternateColorCodes('&',
                            getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!"));
                    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".expReward", 0);
                    Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesfirstTimeRewards);
                } else {
                    // Repeat challenge
                    moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0D);
                    rewardText = ChatColor.translateAlternateColorCodes('&',
                            getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!"));
                    expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
                    Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesrepeatRewards);

                }
                Util.sendMessage(sender, ChatColor.WHITE + rewardText);
                if (expReward > 0) {
                    Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward);
                }
                if (Settings.useEconomy && moneyReward > 0) {
                    Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
                }
                Util.sendMessage(sender, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengestoCompleteUse + ChatColor.WHITE + " /" + label + " c " + challenge);
            } else {
                Util.sendMessage(sender, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesinvalidChallengeName);
            }
            return true;
        case 2:
            if (cmd[0].equalsIgnoreCase("complete") || cmd[0].equalsIgnoreCase("c")) {
                if (!player.getWorld().equals(ASkyBlock.getIslandWorld())) {
                    // Check if in new nether
                    if (!Settings.createNether || !Settings.newNether || ASkyBlock.getNetherWorld() == null || !player.getWorld().equals(ASkyBlock.getNetherWorld())) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorWrongWorld);
                        return true;
                    }
                }
                if (checkIfCanCompleteChallenge(player, cmd[1].toLowerCase().toLowerCase().replaceAll("\\.",""))) {
                    int oldLevel = getLevelDone(player);
                    giveReward(player, cmd[1].toLowerCase().toLowerCase().replaceAll("\\.",""));
                    //Save player
                    plugin.getPlayers().save(player.getUniqueId());
                    int newLevel = getLevelDone(player);
                    // Fire an event if they are different
                    //plugin.getLogger().info("DEBUG: " + oldLevel + " " + newLevel);
                    if (oldLevel < newLevel) {
                        // Update chat
                        plugin.getChatListener().setPlayerChallengeLevel(player);
                        // Run commands and give rewards but only if they haven't done it below
                        //plugin.getLogger().info("DEBUG: old level = " + oldLevel + " new level = " + newLevel);
                        String level = Settings.challengeLevels.get(newLevel);
                        if (!level.isEmpty() && !plugin.getPlayers().checkChallenge(player.getUniqueId(), level)) {
                            //plugin.getLogger().info("DEBUG: level name = " + level);
                            plugin.getPlayers().completeChallenge(player.getUniqueId(), level);
                            String message = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.levelUnlock." + level + ".message", ""));
                            if (!message.isEmpty()) {
                                Util.sendMessage(player, ChatColor.GREEN + message);
                            }

                            String[] itemReward = getChallengeConfig().getString("challenges.levelUnlock." + level + ".itemReward", "").split(" ");
                            String rewardDesc = getChallengeConfig().getString("challenges.levelUnlock." + level + ".rewardDesc", "");
                            if (!rewardDesc.isEmpty()) {
                                Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesrewards + ": " + ChatColor.WHITE + rewardDesc);
                            }
                            List<ItemStack> rewardedItems = giveItems(player, itemReward);
                            double moneyReward = getChallengeConfig().getDouble("challenges.levelUnlock." + level + ".moneyReward", 0D);
                            int expReward = getChallengeConfig().getInt("challenges.levelUnlock." + level + ".expReward", 0);
                            if (expReward > 0) {
                                Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward);
                                player.giveExp(expReward);
                            }
                            if (Settings.useEconomy && moneyReward > 0 && (VaultHelper.econ != null)) {
                                EconomyResponse e = VaultHelper.econ.depositPlayer(player, Settings.worldName, moneyReward);
                                if (e.transactionSuccess()) {
                                    Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
                                } else {
                                    plugin.getLogger().severe("Error giving player " + player.getUniqueId() + " challenge money:" + e.errorMessage);
                                    plugin.getLogger().severe("Reward was $" + moneyReward);
                                }
                            }
                            String[] permList = getChallengeConfig().getString("challenges.levelUnlock." + level + ".permissionReward", "").split(" ");

                            for (final String s : permList) {			
                                if (!s.isEmpty()) {
                                    VaultHelper.addPerm(player, s);
                                    plugin.getLogger().info("Added permission " + s + " to " + player.getName() + "");
                                }
                            }
                            List<String> commands = getChallengeConfig().getStringList("challenges.levelUnlock." + level + ".commands");
                            runCommands(player,commands);
                            // Fire event
                            ChallengeLevelCompleteEvent event = new ChallengeLevelCompleteEvent(player, oldLevel, newLevel, rewardedItems);
                            plugin.getServer().getPluginManager().callEvent(event);
                            // Save player
                            plugin.getPlayers().save(player.getUniqueId());
                        }
                    }
                }
                return true;
            }
            return false;
        default:
            return false;
        }
    }

    /**
     * Checks the highest level this player has achieved
     * @param player
     * @return level number
     */
    private int getLevelDone(Player player) {
        //plugin.getLogger().info("DEBUG: checking level completed");
        //plugin.getLogger().info("DEBUG: getting challenge level for " + player.getName());
        for (int result = 0; result < Settings.challengeLevels.size(); result++) {
            if (checkLevelCompletion(player, Settings.challengeLevels.get(result)) > 0) {
                return result;
            }
        }
        return (Math.max(0,Settings.challengeLevels.size()-1));
    }

    /**
     * Gives the reward for completing the challenge 
     * 
     * @param player
     * @param challenge
     * @return ture if reward given successfully
     */
    private boolean giveReward(final Player player, final String challenge) {
        // Grab the rewards from the config.yml file
        String[] permList;
        String[] itemRewards;
        double moneyReward = 0;
        int expReward = 0;
        String rewardText = "";
        // If the friendly name is available use it
        String challengeName = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challenge + ".friendlyname",
                challenge.substring(0, 1).toUpperCase() + challenge.substring(1)));

        // Gather the rewards due
        // If player has done a challenge already, the rewards are different
        if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
            // First time
            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).challengesyouHaveCompleted.replace("[challenge]", challengeName));
            if (Settings.broadcastMessages) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    Util.sendMessage(p, 
                            ChatColor.GOLD + plugin.myLocale(p.getUniqueId()).challengesnameHasCompleted.replace("[name]", player.getName()).replace("[challenge]", challengeName));
                }
            }
            plugin.getMessages().tellOfflineTeam(player.getUniqueId(),
                    ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesnameHasCompleted.replace("[name]", player.getName()).replace("[challenge]", challengeName));
            itemRewards = getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".itemReward", "").split(" ");
            moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0D);
            rewardText = ChatColor.translateAlternateColorCodes('&',
                    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!"));
            expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".expReward", 0);
        } else {
            // Repeat challenge
            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).challengesyouRepeated.replace("[challenge]", challengeName));
            itemRewards = getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatItemReward", "").split(" ");
            moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
            rewardText = ChatColor.translateAlternateColorCodes('&',
                    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!"));
            expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
        }
        // Report the rewards and give out exp, money and permissions if
        // appropriate
        Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesrewards + ": " + ChatColor.WHITE + rewardText);
        if (expReward > 0) {
            Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward);
            player.giveExp(expReward);
        }
        if (Settings.useEconomy && moneyReward > 0 && (VaultHelper.econ != null)) {
            EconomyResponse e = VaultHelper.econ.depositPlayer(player, Settings.worldName, moneyReward);
            if (e.transactionSuccess()) {
                Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
            } else {
                plugin.getLogger().severe("Error giving player " + player.getUniqueId() + " challenge money:" + e.errorMessage);
                plugin.getLogger().severe("Reward was $" + moneyReward);
            }
        }
        // Dole out permissions
        //plugin.getLogger().info("DEBUG: dole out permissions");
        permList = getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".permissionReward", "").split(" ");
        for (final String s : permList) {
            if (!s.isEmpty()) {
                VaultHelper.addPerm(player, s);
                plugin.getLogger().info("Added permission " + s + " to " + player.getName() + "");
            }
        }
        // Give items
        List<ItemStack> rewardedItems = giveItems(player, itemRewards);
        if (rewardedItems == null) {
            return false;
        }

        // Run reward commands
        if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
            // First time
            List<String> commands = getChallengeConfig().getStringList("challenges.challengeList." + challenge.toLowerCase() + ".rewardcommands");
            runCommands(player,commands);
        } else {
            // Repeat challenge
            List<String> commands = getChallengeConfig().getStringList("challenges.challengeList." + challenge.toLowerCase() + ".repeatrewardcommands");
            runCommands(player,commands);
        }

        // Mark the challenge as complete
        // if (!plugin.getPlayers().checkChallenge(player.getUniqueId(),challenge)) {
        plugin.getPlayers().completeChallenge(player.getUniqueId(), challenge);
        // }
        // Call the Challenge Complete Event
        final ChallengeCompleteEvent event = new ChallengeCompleteEvent(player, challenge, permList, itemRewards, moneyReward, expReward, rewardText, rewardedItems);
        plugin.getServer().getPluginManager().callEvent(event);
        return true;
    }

    private void runCommands(Player player, List<String> commands) {
        // Ignore commands with this perm
        if (player.hasPermission(Settings.PERMPREFIX + "command.challengeexempt") && !player.isOp()) {
            return;
        }
        for (String cmd : commands) {
            if (cmd.startsWith("[SELF]")) {
                plugin.getLogger().info("Running command '" + cmd + "' as " + player.getName());
                cmd = cmd.substring(6,cmd.length()).replace("[player]", player.getName()).trim();
                try {
                    player.performCommand(cmd);
                } catch (Exception e) {
                    plugin.getLogger().severe("Problem executing island command executed by player - skipping!");
                    plugin.getLogger().severe("Command was : " + cmd);
                    plugin.getLogger().severe("Error was: " + e.getMessage());
                    e.printStackTrace();
                }

                continue;
            }
            // Substitute in any references to player
            try {
                if (!plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("[player]", player.getName()))) {
                    plugin.getLogger().severe("Problem executing challenge reward commands - skipping!");
                    plugin.getLogger().severe("Command was : " + cmd);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Problem executing challenge reward commands - skipping!");
                plugin.getLogger().severe("Command was : " + cmd);
                plugin.getLogger().severe("Error was: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Gives player the reward items. 
     * @param player
     * @param itemRewards
     * @return List of ItemStacks that were given to the player or null if there was an error in the interpretation of the rewards
     */
    private List<ItemStack> giveItems(Player player, String[] itemRewards) {
        List<ItemStack> rewardedItems = new ArrayList<ItemStack>();
        Material rewardItem;
        int rewardQty;
        // Build the item stack of rewards to give the player
        for (final String s : itemRewards) {
            final String[] element = s.split(":");
            if (element.length == 2) {
                try {
                    if (StringUtils.isNumeric(element[0])) {
                        rewardItem = Material.getMaterial(Integer.parseInt(element[0]));
                    } else {
                        rewardItem = Material.getMaterial(element[0].toUpperCase());
                    }
                    rewardQty = Integer.parseInt(element[1]);
                    ItemStack item = new ItemStack(rewardItem, rewardQty);
                    rewardedItems.add(item);
                    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { item });
                    if (!leftOvers.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
                    }
                    if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                        player.getWorld().playSound(player.getLocation(), Sound.valueOf("ITEM_PICKUP"), 1F, 1F);
                    } else {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);
                    }
                } catch (Exception e) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorRewardProblem);
                    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " for challenge reward!");
                    String materialList = "";
                    boolean hint = false;
                    for (Material m : Material.values()) {
                        materialList += m.toString() + ",";
                        if (element[0].length() > 3) {
                            if (m.toString().startsWith(element[0].substring(0, 3))) {
                                plugin.getLogger().severe("Did you mean " + m.toString() + "? If so, put that in challenges.yml.");
                                hint = true;
                            }
                        }
                    }
                    if (!hint) {
                        plugin.getLogger().severe("Sorry, I have no idea what " + element[0] + " is. Pick from one of these:");
                        plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
                    }
                }
            } else if (element.length == 3) {
                try {
                    if (StringUtils.isNumeric(element[0])) {
                        rewardItem = Material.getMaterial(Integer.parseInt(element[0]));
                    } else {
                        rewardItem = Material.getMaterial(element[0].toUpperCase());
                    }
                    rewardQty = Integer.parseInt(element[2]);                    
                    // Check for POTION
                    if (rewardItem.equals(Material.POTION)) {
                        givePotion(player, rewardedItems, element, rewardQty);
                    } else {
                        ItemStack item = null;
                        // Normal item, not a potion, check if it is a Monster Egg
                        if (rewardItem.equals(Material.MONSTER_EGG)) {

                            try {                                
                                EntityType type = EntityType.valueOf(element[1].toUpperCase());
                                if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                                    item = new SpawnEgg(type).toItemStack(rewardQty);
                                } else {
                                    try {
                                        item = new SpawnEgg1_9(type).toItemStack(rewardQty);
                                    } catch (Exception ex) {
                                        item = new ItemStack(rewardItem);
                                        plugin.getLogger().severe("Monster eggs not supported with this server version.");
                                    }
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().severe("Spawn eggs must be described by name. Try one of these (not all are possible):");                          
                                for (EntityType type : EntityType.values()) {
                                    if (type.isSpawnable() && type.isAlive()) {
                                        plugin.getLogger().severe(type.toString());
                                    }
                                }
                            }
                        } else {
                            int rewMod = Integer.parseInt(element[1]);
                            item = new ItemStack(rewardItem, rewardQty, (short) rewMod);
                        }
                        if (item != null) {
                            rewardedItems.add(item);
                            final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(
                                    new ItemStack[] { item });
                            if (!leftOvers.isEmpty()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
                            }
                        }
                    }
                    if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                        player.getWorld().playSound(player.getLocation(), Sound.valueOf("ITEM_PICKUP"), 1F, 1F);
                    } else {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);
                    }
                } catch (Exception e) {
                    Util.sendMessage(player, ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
                    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " for challenge reward!");
                    /*
                    if (element[0].equalsIgnoreCase("POTION")) {
                        String potionList = "";
                        boolean hint = false;
                        for (PotionEffectType m : PotionEffectType.values()) {
                            potionList += m.toString() + ",";
                            if (element[1].length() > 3) {
                                if (m.toString().startsWith(element[1].substring(0, 3))) {
                                    plugin.getLogger().severe("Did you mean " + m.toString() + "?");
                                    hint = true;
                                }
                            }
                        }
                        if (!hint) {
                            plugin.getLogger().severe("Sorry, I have no idea what potion type " + element[1] + " is. Pick from one of these:");
                            plugin.getLogger().severe(potionList.substring(0, potionList.length() - 1));
                        }

                    } else {*/
                    String materialList = "";
                    boolean hint = false;
                    for (Material m : Material.values()) {
                        materialList += m.toString() + ",";
                        if (m.toString().startsWith(element[0].substring(0, 3))) {
                            plugin.getLogger().severe("Did you mean " + m.toString() + "? If so, put that in challenges.yml.");
                            hint = true;
                        }
                    }
                    if (!hint) {
                        plugin.getLogger().severe("Sorry, I have no idea what " + element[0] + " is. Pick from one of these:");
                        plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
                    }
                    //}
                    return null;
                }
            } else if (element.length == 6) {
                //plugin.getLogger().info("DEBUG: 6 element reward");
                // Potion format = POTION:name:level:extended:splash:qty
                try {
                    if (StringUtils.isNumeric(element[0])) {
                        rewardItem = Material.getMaterial(Integer.parseInt(element[0]));
                    } else {
                        rewardItem = Material.getMaterial(element[0].toUpperCase());
                    }
                    rewardQty = Integer.parseInt(element[5]);
                    // Check for POTION
                    if (rewardItem.equals(Material.POTION)) {
                        givePotion(player, rewardedItems, element, rewardQty);
                    }
                } catch (Exception e) {
                    Util.sendMessage(player, ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
                    plugin.getLogger().severe("Problem with reward potion: " + s);
                    plugin.getLogger().severe("Format POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY");
                    plugin.getLogger().severe("LEVEL, EXTENDED and SPLASH are optional");
                    plugin.getLogger().severe("LEVEL is a number");
                    plugin.getLogger().severe("Examples:");
                    plugin.getLogger().severe("POTION:STRENGTH:1:EXTENDED:SPLASH:1");
                    plugin.getLogger().severe("POTION:JUMP:2:NOTEXTENDED:NOSPLASH:1");
                    plugin.getLogger().severe("POTION:WEAKNESS:::::1   -  any weakness potion");
                    plugin.getLogger().severe("Available names are:");
                    String potionNames = "";
                    for (PotionType p : PotionType.values()) {
                        potionNames += p.toString() + ", ";
                    }
                    plugin.getLogger().severe(potionNames.substring(0, potionNames.length()-2));
                    return null;
                }
            }
        }
        return rewardedItems;
    }

    /**
     * Converts a serialized potion to a ItemStack of that potion
     * @param element
     * @param rewardQty
     * @param configFile that is being used
     * @return ItemStack of the potion
     */
    public static ItemStack getPotion(String[] element, int rewardQty, String configFile) {
        // Check for potion aspects
        boolean splash = false;
        boolean extended = false;
        boolean linger = false;
        int level = 1;

        if (element.length > 2) {
            // Add level etc.
            if (!element[2].isEmpty()) {
                try {
                    level = Integer.valueOf(element[2]);
                } catch (Exception e) {
                    level = 1;
                }
            }
        }
        if (element.length > 3) {
            //Bukkit.getLogger().info("DEBUG: level = " + Integer.valueOf(element[2]));
            if (element[3].equalsIgnoreCase("EXTENDED")) {
                //Bukkit.getLogger().info("DEBUG: Extended");
                extended = true;
            }
        }
        if (element.length > 4) {
            if (element[4].equalsIgnoreCase("SPLASH")) {
                //Bukkit.getLogger().info("DEBUG: splash");
                splash = true;                
            }
            if (element[4].equalsIgnoreCase("LINGER")) {
                //Bukkit.getLogger().info("DEBUG: linger");
                linger = true;
            } 
        }

        if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
            // Add the effect of the potion
            final PotionType potionType = PotionType.valueOf(element[1]);
            if (potionType == null) {
                Bukkit.getLogger().severe("Potion effect '" + element[1] + "' in " + configFile + " is unknown - skipping!");
                Bukkit.getLogger().severe("Use one of the following:");
                for (PotionType name : PotionType.values()) {
                    Bukkit.getLogger().severe(name.name());
                }
            } else {
                final Potion rewPotion = new Potion(potionType);
                if (potionType != PotionType.INSTANT_DAMAGE && potionType != PotionType.INSTANT_HEAL) {
                    // Instant potions cannot be extended
                    rewPotion.setHasExtendedDuration(extended);
                }
                rewPotion.setLevel(level);
                rewPotion.setSplash(splash);
                return rewPotion.toItemStack(rewardQty);
            }
        } else {
            // 1.9
            try {
                ItemStack result = new ItemStack(Material.POTION, rewardQty);
                if (splash) {
                    result = new ItemStack(Material.SPLASH_POTION, rewardQty);
                }
                if (linger) {
                    result = new ItemStack(Material.LINGERING_POTION, rewardQty);
                }
                PotionMeta potionMeta = (PotionMeta) result.getItemMeta();
                try {
                    PotionData potionData = new PotionData(PotionType.valueOf(element[1].toUpperCase()), extended, level > 1 ? true: false);
                    potionMeta.setBasePotionData(potionData); 
                } catch (IllegalArgumentException iae) {
                    Bukkit.getLogger().severe("Potion parsing problem with " + element[1] +": " + iae.getMessage());
                    potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                }
                result.setItemMeta(potionMeta);
                return result;
                /*
                Potion1_9 rewPotion = new Potion1_9(Potion1_9.PotionType.valueOf(element[1].toUpperCase()));
                rewPotion.setHasExtendedDuration(extended);                
                rewPotion.setStrong(level > 1 ? true : false);
                rewPotion.setSplash(splash);
                rewPotion.setLinger(linger);
                return rewPotion.toItemStack(rewardQty); 
                 */              
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getLogger().severe("Potion effect '" + element[1] + "' in " + configFile + " is unknown - skipping!");
                Bukkit.getLogger().severe("Use one of the following:");
                for (PotionType name : PotionType.values()) {
                    Bukkit.getLogger().severe(name.name());
                }
                return new ItemStack(Material.POTION, rewardQty);
            }
        } 
        return null;       
    }

    private void givePotion(Player player, List<ItemStack> rewardedItems, String[] element, int rewardQty) {
        ItemStack item = getPotion(element, rewardQty, "challenges.yml");
        rewardedItems.add(item);
        final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(item);
        if (!leftOvers.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
        }

    }

    /**
     * Returns a color formatted string for all the challenges of a particular
     * level for a player Repeatable challenges are AQUA, completed are Dark
     * Green and yet to be done are green.
     * 
     * @param player
     * @param level
     * @return string of challenges
     */
    /*
     * private String getChallengesByLevel(final Player player, final String
     * level) {
     * List<String> levelChallengeList = challengeList.get(level);
     * String response = "";
     * for (String challenge : levelChallengeList) {
     * if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
     * if (getChallengeConfig().getBoolean("challenges.challengeList." +
     * challenge + ".repeatable", false)) {
     * response += ChatColor.AQUA + challenge + ", ";
     * } else {
     * response += ChatColor.DARK_GREEN + challenge + ", ";
     * }
     * } else {
     * response += ChatColor.GREEN + challenge + ", ";
     * }
     * }
     * // Trim the final dash
     * if (response.length() > 3) {
     * response = response.substring(0, response.length() - 2);
     * }
     * return response;
     * }
     */
    /**
     * Returns the number of challenges that must still be completed to finish a
     * level Based on how many challenges there are in a level, how many have
     * been done and how many are okay to leave undone.
     * 
     * @param player
     * @param level
     * @return int of challenges that must still be completed to finish level.
     */
    public int checkLevelCompletion(final Player player, String level) {
        // Remove any dots from the level name
        level = level.replaceAll("\\.","");
        if (Settings.freeLevels.contains(level)) {
            return 0;
        }
        int challengesCompleted = 0;
        List<String> levelChallengeList = challengeList.get(level);
        int waiver = Settings.waiverAmount;
        if (levelChallengeList != null) {
            for (String challenge : levelChallengeList) {
                if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
                    challengesCompleted++;
                }
            }
            // If the number of challenges in a level is below the waiver amount, then they all need to be done
            if (levelChallengeList.size() <= Settings.waiverAmount) {
                waiver = 0;
            }
            return levelChallengeList.size() - waiver - challengesCompleted;
        }
        return 0;
    }

    /**
     * Checks if player can complete challenge
     * 
     * @param player
     * @param challenge
     * @return true if player can complete otherwise false
     */
    public boolean checkIfCanCompleteChallenge(final Player player, String challenge) {
        // plugin.getLogger().info("DEBUG: " + player.getDisplayName() + " " +
        // challenge);
        // plugin.getLogger().info("DEBUG: 1");
        // Check if the challenge exists
        /*
        if (!isLevelAvailable(player, getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".level"))) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesunknownChallenge + " '" + challenge + "'");
            return false;
        }*/
        // Remove any dots from the challenge name (can be used to exploit)
        challenge = challenge.replaceAll("\\.","");
        // Check if this challenge level is available
        String level = getChallengeConfig().getString("challenges.challengeList." + challenge + ".level");
        if (level == null) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesunknownChallenge + " '" + challenge + "'");
            return false;
        }
        // Only check if the challenge has a level, otherwise it's a free level
        if (!level.isEmpty()) {
            if (!isLevelAvailable(player, level)) {
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesyouHaveNotUnlocked);
                return false;
            }
        }
        // Check if the player has maxed out the challenge
        if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable")) {
            int maxTimes = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".maxtimes", 0);
            if (maxTimes > 0) {
                // There is a limit
                if (plugin.getPlayers().checkChallengeTimes(player.getUniqueId(), challenge) >= maxTimes) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
                    return false;
                }
            }
        }
        // plugin.getLogger().info("DEBUG: 2");
        // Check if it is repeatable
        if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)
                && !getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable")) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
            return false;
        }
        // plugin.getLogger().info("DEBUG: 3");
        // If the challenge is an island type and already done, then this too is
        // not repeatable
        if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)
                && getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("island")) {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengesnotRepeatable);
            return false;
        }
        // plugin.getLogger().info("DEBUG: 4");
        if (getChallengeConfig().getConfigurationSection("challenges.challengeList." + challenge).contains("type")) {
            // Check if this is an inventory challenge
            if (getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("inventory")) {
                // Check if the player has the required items
                if (!hasRequired(player, challenge, "inventory")) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotEnoughItems);
                    String desc = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challenge + ".description", "").replace("[label]", Settings.ISLANDCOMMAND));
                    List<String> result = new ArrayList<String>();
                    if (desc.contains("|")) {
                        result.addAll(Arrays.asList(desc.split("\\|")));
                    } else {
                        result.add(desc);
                    }
                    for (String line: result) {
                        Util.sendMessage(player, ChatColor.RED + line);
                    }
                    return false;
                }
                return true;
            }
            // plugin.getLogger().info("DEBUG: 5");
            // Check if this is an island-based challenge
            if (getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("island")) {
                // plugin.getLogger().info("DEBUG: 6");
                // Don't count coop islands
                if (!plugin.getGrid().playerIsOnIsland(player, false)) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotOnIsland);
                    return false;
                }
                if (!hasRequired(player, challenge, "island")) {
                    int searchRadius = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".searchRadius",10);
                    if (searchRadius < 10) {
                        searchRadius = 10;
                    } else if (searchRadius > 50) {
                        searchRadius = 50;
                    }
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotCloseEnough.replace("[number]", String.valueOf(searchRadius)));
                    String desc = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challenge + ".description").replace("[label]", Settings.ISLANDCOMMAND));    
                    List<String> result = new ArrayList<String>();
                    if (desc.contains("|")) {
                        result.addAll(Arrays.asList(desc.split("\\|")));
                    } else {
                        result.add(desc);
                    }
                    for (String line: result) {
                        Util.sendMessage(player, ChatColor.RED + line);
                    }
                    return false;
                }
                // plugin.getLogger().info("DEBUG: 7");
                return true;
            }
            // Island level check
            if (getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("level")) {
                if (plugin.getPlayers().getIslandLevel(player.getUniqueId()) >= getChallengeConfig().getInt("challenges.challengeList." + challenge + ".requiredItems")) {
                    return true;
                }

                Util.sendMessage(player, ChatColor.RED
                        + plugin.myLocale(player.getUniqueId()).challengeserrorIslandLevel.replace("[level]",
                                String.valueOf(getChallengeConfig().getInt("challenges.challengeList." + challenge + ".requiredItems"))));
                return false;
            }
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
            plugin.getLogger().severe(
                    "The challenge " + challenge + " is of an unknown type " + getChallengeConfig().getString("challenges.challengeList." + challenge + ".type"));
            plugin.getLogger().severe("Types should be 'island', 'inventory' or 'level'");
        }
        return false;
    }

    /**
     * Goes through all the challenges in the config.yml file and puts them into
     * the challenges list
     */
    public void populateChallengeList() {
        challengeList.clear();
        for (String s : Settings.challengeList) {
            String level = getChallengeConfig().getString("challenges.challengeList." + s + ".level", "");
            // Verify that this challenge's level is in the list of levels
            if (Settings.challengeLevels.contains(level) || level.isEmpty()) {
                if (challengeList.containsKey(level)) {
                    challengeList.get(level).add(s);
                } else {
                    List<String> t = new ArrayList<String>();
                    t.add(s);
                    challengeList.put(level, t);
                }
            } else {
                plugin.getServer().getLogger().severe("Level (" + level + ") for challenge " + s + " does not exist. Check challenges.yml.");
            }
        }
    }

    /**
     * Checks if a player has enough for a challenge. Supports two types of
     * checks, inventory and island. Removes items if required.
     * 
     * @param player
     * @param challenge
     * @param type
     * @return true if the player has everything required
     */

    public boolean hasRequired(final Player player, final String challenge, final String type) {
        // Check money
        double moneyReq = 0D;
        if (Settings.useEconomy) {
            moneyReq = getChallengeConfig().getDouble("challenges.challengeList." + challenge + ".requiredMoney", 0D);
            if (moneyReq > 0D) {
                if (!VaultHelper.econ.has(player, Settings.worldName, moneyReq)) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorNotEnoughItems);
                    String desc = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challenge + ".description").replace("[label]", Settings.ISLANDCOMMAND));    
                    List<String> result = new ArrayList<String>();
                    if (desc.contains("|")) {
                        result.addAll(Arrays.asList(desc.split("\\|")));
                    } else {
                        result.add(desc);
                    }
                    for (String line: result) {
                        Util.sendMessage(player, ChatColor.RED + line);
                    }
                    return false;
                }
            }
        }
        final String reqList = getChallengeConfig().getString("challenges.challengeList." + challenge + ".requiredItems");
        // The format of the requiredItems is as follows:
        // Material:Qty
        // or
        // Material:DamageModifier:Qty
        // This second one is so that items such as potions or variations on
        // standard items can be collected
        if (type.equalsIgnoreCase("inventory")) {
            List<ItemStack> toBeRemoved = new ArrayList<ItemStack>();
            Material reqItem;
            int reqAmount = 0;
            if (!reqList.isEmpty()) {
                for (final String s : reqList.split(" ")) {
                    final String[] part = s.split(":");
                    // Material:Qty
                    if (part.length == 2) {
                        try {
                            // Correct some common mistakes
                            if (part[0].equalsIgnoreCase("potato")) {
                                part[0] = "POTATO_ITEM";
                            } else if (part[0].equalsIgnoreCase("brewing_stand")) {
                                part[0] = "BREWING_STAND_ITEM";
                            } else if (part[0].equalsIgnoreCase("carrot")) {
                                part[0] = "CARROT_ITEM";
                            } else if (part[0].equalsIgnoreCase("cauldron")) {
                                part[0] = "CAULDRON_ITEM";
                            } else if (part[0].equalsIgnoreCase("skull")) {
                                part[0] = "SKULL_ITEM";
                            }
                            // TODO: add netherwart vs. netherstalk?
                            if (StringUtils.isNumeric(part[0])) {
                                reqItem = Material.getMaterial(Integer.parseInt(part[0]));
                            } else {
                                reqItem = Material.getMaterial(part[0].toUpperCase());
                            }
                            reqAmount = Integer.parseInt(part[1]);
                            ItemStack item = new ItemStack(reqItem);
                            if (DEBUG) {
                                plugin.getLogger().info("DEBUG: required item = " + reqItem.toString());
                                plugin.getLogger().info("DEBUG: item amount = " + reqAmount);
                            }
                            if (!player.getInventory().contains(reqItem)) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: item not in inventory");
                                return false;
                            } else {
                                // check amount
                                int amount = 0;
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: Amount in inventory = " + player.getInventory().all(reqItem).size());
                                // Go through all the inventory and try to find
                                // enough required items
                                for (Entry<Integer, ? extends ItemStack> en : player.getInventory().all(reqItem).entrySet()) {
                                    // Get the item
                                    ItemStack i = en.getValue();
                                    // If the item is enchanted, skip - it doesn't count
                                    if (!i.getEnchantments().isEmpty()) {
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: item has enchantment - doesn't count");
                                        continue;
                                    }
                                    // Map needs special handling because the
                                    // durability increments every time a new one is
                                    // made by the player
                                    // TODO: if there are any other items that act
                                    // in the same way, they need adding too...
                                    if (i.getDurability() == 0 || (reqItem == Material.MAP && i.getType() == Material.MAP)) {
                                        // Clear any naming, or lore etc.
                                        //i.setItemMeta(null);
                                        //player.getInventory().setItem(en.getKey(), i);
                                        // #1 item stack qty + amount is less than
                                        // required items - take all i
                                        // #2 item stack qty + amount = required
                                        // item -
                                        // take all
                                        // #3 item stack qty + amount > req items -
                                        // take
                                        // portion of i
                                        // amount += i.getAmount();
                                        if ((amount + i.getAmount()) < reqAmount) {
                                            // Remove all of this item stack - clone
                                            // otherwise it will keep a reference to
                                            // the
                                            // original
                                            toBeRemoved.add(i.clone());
                                            amount += i.getAmount();
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: amount is <= req Remove "+ i.toString() + ":" + i.getDurability() + " x " + i.getAmount());
                                        } else if ((amount + i.getAmount()) == reqAmount) {
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: amount is = req Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());
                                            toBeRemoved.add(i.clone());
                                            amount += i.getAmount();
                                            break;
                                        } else {
                                            // Remove a portion of this item
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: amount is > req Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());

                                            item.setAmount(reqAmount - amount);
                                            item.setDurability(i.getDurability());
                                            toBeRemoved.add(item);
                                            amount += i.getAmount();
                                            break;
                                        }
                                    }
                                }
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: amount "+amount);
                                if (amount < reqAmount) {
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
                            String materialList = "";
                            boolean hint = false;
                            for (Material m : Material.values()) {
                                materialList += m.toString() + ",";
                                if (m.toString().contains(s.substring(0, 3).toUpperCase())) {
                                    plugin.getLogger().severe("Did you mean " + m.toString() + "?");
                                    hint = true;
                                }
                            }
                            if (!hint) {
                                plugin.getLogger().severe("Sorry, I have no idea what " + s + " is. Pick from one of these:");
                                plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
                            } else {
                                plugin.getLogger().severe("Correct challenges.yml with the correct material.");
                            }
                            return false;
                        }
                    } else if (part.length == 3) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: Item with durability");
                        // This handles items with durability
                        // Correct some common mistakes
                        if (part[0].equalsIgnoreCase("potato")) {
                            part[0] = "POTATO_ITEM";
                        } else if (part[0].equalsIgnoreCase("brewing_stand")) {
                            part[0] = "BREWING_STAND_ITEM";
                        } else if (part[0].equalsIgnoreCase("carrot")) {
                            part[0] = "CARROT_ITEM";
                        } else if (part[0].equalsIgnoreCase("cauldron")) {
                            part[0] = "CAULDRON_ITEM";
                        } else if (part[0].equalsIgnoreCase("skull")) {
                            part[0] = "SKULL_ITEM";
                        }
                        if (StringUtils.isNumeric(part[0])) {
                            reqItem = Material.getMaterial(Integer.parseInt(part[0]));
                        } else {
                            reqItem = Material.getMaterial(part[0].toUpperCase());
                        }
                        reqAmount = Integer.parseInt(part[2]);

                        ItemStack item = new ItemStack(reqItem);
                        int reqDurability = 0;
                        boolean entityIsString = false;
                        if (StringUtils.isNumeric(part[1])) {
                            reqDurability = Integer.parseInt(part[1]);
                            item.setDurability((short) reqDurability);
                        } else if (reqItem.equals(Material.MONSTER_EGG)){
                            entityIsString = true;
                            reqDurability = -1; // non existent
                            try {
                                // Check if this is a string
                                EntityType entityType = EntityType.valueOf(part[1]);
                                NMSAbstraction nms = null;
                                nms = Util.checkVersion();
                                item = nms.getSpawnEgg(entityType, reqAmount);
                            } catch (Exception ex) {
                                plugin.getLogger().severe("Unknown entity type '" + part[1] + "' for MONSTER_EGG in challenge " + challenge);
                                return false;
                            }
                        }
                        // check amount
                        int amount = 0;
                        // Go through all the inventory and try to find
                        // enough required items
                        for (Entry<Integer, ? extends ItemStack> en : player.getInventory().all(reqItem).entrySet()) {
                            // Get the item
                            ItemStack i = en.getValue();
                            if (i.hasItemMeta() && !i.getType().equals(Material.MONSTER_EGG)) {
                                continue;
                            }
                            if (i.getDurability() == reqDurability || (entityIsString && i.getItemMeta().equals(item.getItemMeta()))) {
                                // #1 item stack qty + amount is less than
                                // required items - take all i
                                // #2 item stack qty + amount = required
                                // item -
                                // take all
                                // #3 item stack qty + amount > req items -
                                // take
                                // portion of i
                                // amount += i.getAmount();
                                if ((amount + i.getAmount()) < reqAmount) {
                                    // Remove all of this item stack - clone
                                    // otherwise it will keep a reference to
                                    // the
                                    // original
                                    toBeRemoved.add(i.clone());
                                    amount += i.getAmount();
                                    if (DEBUG)
                                        plugin.getLogger().info("DEBUG: amount is <= req Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());
                                } else if ((amount + i.getAmount()) == reqAmount) {
                                    toBeRemoved.add(i.clone());
                                    amount += i.getAmount();
                                    break;
                                } else {
                                    // Remove a portion of this item
                                    if (DEBUG)
                                        plugin.getLogger().info("DEBUG: amount is > req Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());

                                    item.setAmount(reqAmount - amount);
                                    item.setDurability(i.getDurability());
                                    toBeRemoved.add(item);
                                    amount += i.getAmount();
                                    break;
                                }
                            }
                        }
                        if (DEBUG) {
                            plugin.getLogger().info("DEBUG: amount is " + amount);
                            plugin.getLogger().info("DEBUG: req amount is " + reqAmount);
                        }
                        if (amount < reqAmount) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: Failure! Insufficient amount of " + item.toString() + " required = " + reqAmount + " actual = " + amount);
                            return false;
                        }
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: before set amount " + item.toString() + ":" + item.getDurability() + " x " + item.getAmount());

                    } else if (part.length == 6 && part[0].contains("POTION")) {
                        // Run through player's inventory for the item
                        ItemStack[] playerInv = player.getInventory().getContents();
                        try {
                            reqAmount = Integer.parseInt(part[5]);
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: required amount is " + reqAmount);
                        } catch (Exception e) {
                            plugin.getLogger().severe("Could not parse the quantity of the potion item " + s);
                            return false;
                        }
                        int count = reqAmount;
                        for (ItemStack i : playerInv) {
                            // Catches all POTION, LINGERING_POTION and SPLASH_POTION
                            if (i != null && i.getType().toString().contains("POTION")) {
                                //plugin.getLogger().info("DEBUG:6 part potion check!");
                                // POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY
                                if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                                    // Test potion
                                    Potion potion = null;
                                    try {
                                        // This may fail if there are custom potions in the player's inventory
                                        // If so, just skip this item stack.
                                        potion = Potion.fromItemStack(i);
                                    } catch (Exception e) {
                                        potion = null;
                                    }
                                    if (potion != null) {
                                        PotionType potionType = potion.getType();
                                        boolean match = true;                                    
                                        if (DEBUG) {
                                            plugin.getLogger().info("DEBUG: name check " + part[1]);
                                            plugin.getLogger().info("DEBUG: potion = " + potion);
                                            plugin.getLogger().info("DEBUG: potionType = " + potionType);
                                            plugin.getLogger().info("DEBUG: part[1] = " + part[1]);
                                        }
                                        // Name check
                                        if (potionType != null && !part[1].isEmpty()) {
                                            // There is a name
                                            // Custom potions may not have names
                                            if (potionType.name() != null) {
                                                if (!part[1].equalsIgnoreCase(potionType.name())) {
                                                    match = false;
                                                    if (DEBUG)
                                                        plugin.getLogger().info("DEBUG: name does not match");
                                                } else {
                                                    if (DEBUG)
                                                        plugin.getLogger().info("DEBUG: name matches");
                                                }
                                            } else {
                                                plugin.getLogger().severe("Potion type is unknown. Please pick from the following:");
                                                for (PotionType pt: PotionType.values()) {
                                                    plugin.getLogger().severe(pt.name());
                                                }
                                                match = false;
                                            }                                        
                                        } else {
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: potionType = null");
                                            match = false;
                                        }
                                        // Level check (upgraded)
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: level check " + part[2]);
                                        if (!part[2].isEmpty()) {
                                            // There is a level declared - check it
                                            if (StringUtils.isNumeric(part[2])) {
                                                int level = Integer.valueOf(part[2]);
                                                if (level != potion.getLevel()) {
                                                    if (DEBUG)
                                                        plugin.getLogger().info("DEBUG: level does not match");
                                                    match = false;
                                                }                                     
                                            }
                                        }
                                        // Extended check
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: extended check " + part[3]);
                                        if (!part[3].isEmpty()) {
                                            if (part[3].equalsIgnoreCase("EXTENDED") && !potion.hasExtendedDuration()) {
                                                match = false;
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: extended does not match");
                                            }
                                            if (part[3].equalsIgnoreCase("NOTEXTENDED") && potion.hasExtendedDuration()) {
                                                match = false;
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: extended does not match");
                                            }
                                        }
                                        // Splash check
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: splash/linger check " + part[4]);
                                        if (!part[4].isEmpty()) {
                                            if (part[4].equalsIgnoreCase("SPLASH") && !potion.isSplash()) {
                                                match = false;
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: not splash");
                                            }
                                            if (part[4].equalsIgnoreCase("NOSPLASH") && potion.isSplash()) {
                                                match = false;
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: not no splash");
                                            }                                    
                                        }
                                        // Quantity check
                                        if (match) {
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: potion matches!");
                                            ItemStack removeItem = i.clone();
                                            if (removeItem.getAmount() > reqAmount) {
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: found " + removeItem.getAmount() + " qty in inv");
                                                removeItem.setAmount(reqAmount);
                                            }
                                            count = count - removeItem.getAmount();
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: " + count + " left");
                                            toBeRemoved.add(removeItem);
                                        }
                                    }
                                } else {
                                    // V1.9 and above
                                    PotionMeta potionMeta = (PotionMeta)i.getItemMeta();
                                    // If any of the settings above are missing, then any is okay
                                    boolean match = true;
                                    if (DEBUG)
                                        plugin.getLogger().info("DEBUG: name check " + part[1]);
                                    // Name check
                                    if (!part[1].isEmpty()) {
                                        // There is a name
                                        if (PotionType.valueOf(part[1]) != null) {
                                            if (!potionMeta.getBasePotionData().getType().name().equalsIgnoreCase(part[1])) {
                                                match = false;
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: name does not match");
                                            } else {
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: name matches");
                                            }
                                        } else {
                                            plugin.getLogger().severe("Potion type is unknown. Please pick from the following:");
                                            for (PotionType pt: PotionType.values()) {
                                                plugin.getLogger().severe(pt.name());
                                            }
                                            match = false;
                                        }
                                    }
                                    // Level check (upgraded)
                                    // plugin.getLogger().info("DEBUG: level check " + part[2]);
                                    if (!part[2].isEmpty()) {
                                        // There is a level declared - check it
                                        if (StringUtils.isNumeric(part[2])) {
                                            int level = Integer.valueOf(part[2]);
                                            if (level == 1 && potionMeta.getBasePotionData().isUpgraded()) {
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: level does not match");
                                                match = false;
                                            }
                                            if (level !=1 && !potionMeta.getBasePotionData().isUpgraded()) {
                                                match = false;
                                                if (DEBUG)
                                                    plugin.getLogger().info("DEBUG: level does not match");
                                            }
                                        }
                                    }
                                    // Extended check
                                    if (DEBUG)
                                        plugin.getLogger().info("DEBUG: extended check " + part[3]);
                                    if (!part[3].isEmpty()) {
                                        if (part[3].equalsIgnoreCase("EXTENDED") && !potionMeta.getBasePotionData().isExtended()) {
                                            match = false;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: extended does not match");
                                        }
                                        if (part[3].equalsIgnoreCase("NOTEXTENDED") && potionMeta.getBasePotionData().isExtended()) {
                                            match = false;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: extended does not match");
                                        }
                                    }
                                    // Splash or Linger check
                                    if (DEBUG)
                                        plugin.getLogger().info("DEBUG: splash/linger check " + part[4]);
                                    if (!part[4].isEmpty()) {
                                        if (part[4].equalsIgnoreCase("SPLASH") && !i.getType().equals(Material.SPLASH_POTION)) {
                                            match = false;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: not splash");
                                        }
                                        if (part[4].equalsIgnoreCase("NOSPLASH") && i.getType().equals(Material.SPLASH_POTION)) {
                                            match = false;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: not no splash");
                                        }
                                        if (part[4].equalsIgnoreCase("LINGER") && !i.getType().equals(Material.LINGERING_POTION)) {
                                            match = false;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: not linger");
                                        }
                                        if (part[4].equalsIgnoreCase("NOLINGER") && i.getType().equals(Material.LINGERING_POTION)) {
                                            match = false;
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: not no linger");
                                        }
                                    }
                                    // Quantity check
                                    if (match) {
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: potion matches!");
                                        ItemStack removeItem = i.clone();
                                        if (removeItem.getAmount() > reqAmount) {
                                            if (DEBUG)
                                                plugin.getLogger().info("DEBUG: found " + removeItem.getAmount() + " qty in inv");
                                            removeItem.setAmount(reqAmount);
                                        }
                                        count = count - removeItem.getAmount();
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: " + count + " left");
                                        toBeRemoved.add(removeItem);
                                    }
                                }
                            }
                            if (count <= 0) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: Player has enough");
                                break;
                            }
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: still need " + count + " to complete");
                        }
                        if (count > 0) {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: Player does not have enough");
                            return false;
                        }

                    } else {
                        plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
                        return false;
                    }                
                }
            }
            // Build up the items in the inventory and remove them if they are
            // all there.

            if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".takeItems")) {
                // checkChallengeItems(player, challenge);
                // int qty = 0;
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Removing items");
                for (ItemStack i : toBeRemoved) {
                    // qty += i.getAmount();
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Remove " + i.toString() + "::" + i.getDurability() + " x " + i.getAmount());
                    HashMap<Integer, ItemStack> leftOver = player.getInventory().removeItem(i);
                    if (!leftOver.isEmpty()) {
                        plugin.getLogger().warning(
                                "Exploit? Could not remove the following in challenge " + challenge + " for player " + player.getName() + ":");
                        for (ItemStack left : leftOver.values()) {
                            plugin.getLogger().info(left.toString());
                        }
                        return false;
                    }
                }
                // Remove money
                if (moneyReq > 0D) {
                    EconomyResponse er = VaultHelper.econ.withdrawPlayer(player, moneyReq);
                    if (!er.transactionSuccess()) {
                        plugin.getLogger().warning(
                                "Exploit? Could not remove " + VaultHelper.econ.format(moneyReq) + " from " + player.getName()
                                + " in challenge " + challenge);
                        plugin.getLogger().warning("Player's balance is " + VaultHelper.econ.format(VaultHelper.econ.getBalance(player)));
                    }
                }
                // plugin.getLogger().info("DEBUG: total = " + qty);
            }
            return true;
        }
        if (type.equalsIgnoreCase("island")) {
            final HashMap<MaterialData, Integer> neededItem = new HashMap<MaterialData, Integer>();
            final HashMap<EntityType, Integer> neededEntities = new HashMap<EntityType, Integer>();
            if (!reqList.isEmpty()) {
                for (int i = 0; i < reqList.split(" ").length; i++) {
                    final String[] sPart = reqList.split(" ")[i].split(":");
                    try {
                        // Find out if the needed item is a Material or an Entity
                        boolean isEntity = false;
                        for (EntityType entityType : EntityType.values()) {
                            if (entityType.toString().equalsIgnoreCase(sPart[0])) {
                                isEntity = true;
                                break;
                            }
                        }
                        if (isEntity) {
                            // plugin.getLogger().info("DEBUG: Item " + sPart[0].toUpperCase() + " is an entity");
                            EntityType entityType = EntityType.valueOf(sPart[0].toUpperCase());
                            if (entityType != null) {
                                neededEntities.put(entityType, Integer.parseInt(sPart[1]));
                                // plugin.getLogger().info("DEBUG: Needed entity is " + Integer.parseInt(sPart[1]) + " x " + EntityType.valueOf(sPart[0].toUpperCase()).toString());
                            }
                        } else {	
                            Material item;
                            if (StringUtils.isNumeric(sPart[0])) {
                                item = Material.getMaterial(Integer.parseInt(sPart[0]));
                            } else {
                                item = Material.getMaterial(sPart[0].toUpperCase());
                            }
                            if (item != null) {
                                // We have two cases : quantity only OR durability + quantity
                                final int quantity;
                                MaterialData md = new MaterialData(item);
                                if (sPart.length == 2) {
                                    // Only a quantity is specified
                                    quantity = Integer.parseInt(sPart[1]);
                                } else {
                                    quantity = Integer.parseInt(sPart[2]);
                                    md.setData(Byte.parseByte(sPart[1]));
                                }
                                neededItem.put(md, quantity);
                                if (DEBUG) {
                                    plugin.getLogger().info("DEBUG: Needed item is " + md.toString() + " x " + quantity);
                                }
                            } else {
                                plugin.getLogger().warning("Problem parsing required item for challenge " + challenge + " in challenges.yml!");
                                return false;
                            }
                        }
                    } catch (Exception intEx) {
                        plugin.getLogger().warning("Problem parsing required items for challenge " + challenge + " in challenges.yml - skipping");
                        return false;
                    }
                }
            }
            // We now have two sets of required items or entities
            // Check the items first
            final Location l = player.getLocation();
            // if (!neededItem.isEmpty()) {
            final int px = l.getBlockX();
            final int py = l.getBlockY();
            final int pz = l.getBlockZ();
            // Get search radius - min is 10, max is 50
            int searchRadius = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".searchRadius",10);
            if (searchRadius < 10) {
                searchRadius = 10;
            } else if (searchRadius > 50) {
                searchRadius = 50;
            }
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int y = -searchRadius; y <= searchRadius; y++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        final MaterialData b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock().getState().getData();
                        if (!b.getItemType().equals(Material.AIR)) {
                            neededItem.entrySet().stream().filter(e -> 
                                (e.getKey().getItemType().equals(b.getItemType()) && (
                                        e.getKey().getData() == 0 || e.getKey().getData() == b.getData())
                                        || (e.getKey().getItemType().toString().endsWith("_DOOR") && b.getItemType().toString().endsWith("_DOOR"))
                                        || (e.getKey().getItemType().toString().endsWith("_COMPARATOR") && b.getItemType().toString().endsWith("_COMPARATOR"))
                                        || (e.getKey().getItemType().toString().contains("_LAMP") && b.getItemType().toString().contains("_LAMP"))
                                        || (e.getKey().getItemType().toString().endsWith("FURNACE") && b.getItemType().toString().endsWith("FURNACE"))
                                        || (e.getKey().getItemType().toString().contains("DAYLIGHT") && b.getItemType().toString().contains("DAYLIGHT"))
                                        || (e.getKey().getItemType().toString().startsWith("BOAT") && b.getItemType().toString().startsWith("BOAT"))
                                        )
                                )
                            .forEach(e -> e.setValue(e.getValue() - 1));
                        }
                    }
                }
            }
            neededItem.values().removeIf(v -> v <= 0);
            // Check if all the needed items have been amassed
            if (!neededItem.isEmpty()) {
                // plugin.getLogger().info("DEBUG: Insufficient items around");
                
                for (MaterialData missing : neededItem.keySet()) {
                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorYouAreMissing + " " + neededItem.get(missing) + " x "
                            + Util.prettifyText(missing.getItemType().toString()) + ":" + missing.getData());
                }
                return false;
            } else {
                // plugin.getLogger().info("DEBUG: Items are there");
                // Check for needed entities
                for (Entity entity : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
                    // plugin.getLogger().info("DEBUG: Entity found:" +
                    // entity.getType().toString());
                    if (neededEntities.containsKey(entity.getType())) {
                        // plugin.getLogger().info("DEBUG: Entity in list");
                        if (neededEntities.get(entity.getType()) == 1) {
                            neededEntities.remove(entity.getType());
                            // plugin.getLogger().info("DEBUG: Entity qty satisfied");
                        } else {
                            neededEntities.put(entity.getType(), neededEntities.get(entity.getType()) - 1);
                            // plugin.getLogger().info("DEBUG: Entity qty reduced by 1");
                        }
                    } else {
                        // plugin.getLogger().info("DEBUG: Entity not in list");
                    }
                }
                if (neededEntities.isEmpty()) {
                    return true;
                } else {
                    for (EntityType missing : neededEntities.keySet()) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).challengeserrorYouAreMissing + " " + neededEntities.get(missing) + " x "
                                + Util.prettifyText(missing.toString()));
                    }
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the level is unlocked and false if not
     * 
     * @param player
     * @param level
     * @return true/false
     */
    public boolean isLevelAvailable(final Player player, final String level) {
        if (challengeList.size() < 2) {
            return true;
        }
        for (int i = 0; i < Settings.challengeLevels.size(); i++) {
            if (Settings.challengeLevels.get(i).equalsIgnoreCase(level)) {
                if (i == 0) {
                    return true;
                }

                if (checkLevelCompletion(player, Settings.challengeLevels.get(i - 1)) <= 0) {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * Dynamically creates an inventory of challenges for the player
     * 
     * @param player
     * @return inventory
     */
    public Inventory challengePanel(Player player) {
    	if(playerChallengeLevel.containsKey(player.getUniqueId())) {	
    		String level = playerChallengeLevel.get(player.getUniqueId());	  
    		return challengePanel(player, level);
        }else {
        	String maxLevel = "";
        	for (String level : Settings.challengeLevels) {
	            if (checkLevelCompletion(player, level) > 0) {
	                maxLevel = level;
	                break;
	            }
	        }
        	return challengePanel(player, maxLevel);
        }
        
    }

    /**
     * Dynamically creates an inventory of challenges for the player showing the
     * level
     * 
     * @param player
     * @param level
     * @return inventory
     */
    public Inventory challengePanel(Player player, String level) {
        //plugin.getLogger().info("DEBUG: level requested = " + level);
        // Create the challenges control panel
        // New panel map
        List<CPItem> cp = new ArrayList<CPItem>();

        // Do some checking
        // plugin.getLogger().severe("DEBUG: Opening level " + level);

        if (level.isEmpty() && !challengeList.containsKey("")) {
            if (!Settings.challengeLevels.isEmpty()) {
                level = Settings.challengeLevels.get(0);
            } else {
                // We have no challenges!
                plugin.getLogger().severe("There are no challenges to show!");
                Inventory error = Bukkit.createInventory(null, 9, plugin.myLocale(player.getUniqueId()).challengesguiTitle);
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).errorCommandNotReady);
                return error;
            }
        }
        if (challengeList.get(level) != null) {
            // Only show a control panel for the level requested.
            for (String challengeName : challengeList.get(level)) {
                CPItem item = createItem(challengeName, player);
                if (item != null) {
                    cp.add(item);
                }
            }
        }
        // Add the missing levels so player can navigate to them
        int levelDone = 0;
        for (; levelDone < Settings.challengeLevels.size(); levelDone++) {
            if (checkLevelCompletion(player, Settings.challengeLevels.get(levelDone)) > 0) {
                break;
            }
        }
        //plugin.getLogger().info("DEBUG: level done = " + levelDone);
        for (int i = 0; i < Settings.challengeLevels.size(); i++) {
            if (!level.equalsIgnoreCase(Settings.challengeLevels.get(i))) {
                // Add a navigation book
                List<String> lore = new ArrayList<String>();
                if (i <= levelDone) {
                    CPItem item = new CPItem(Material.BOOK_AND_QUILL, ChatColor.GOLD + Settings.challengeLevels.get(i), null, null);
                    lore = Util.chop(ChatColor.WHITE, plugin.myLocale(player.getUniqueId()).challengesNavigation.replace("[level]", Settings.challengeLevels.get(i)), 25);
                    item.setNextSection(Settings.challengeLevels.get(i));
                    item.setLore(lore);
                    cp.add(item);
                } else {
                    // Hint at what is to come
                    CPItem item = new CPItem(Material.BOOK, ChatColor.GOLD + Settings.challengeLevels.get(i), null, null);
                    // Add the level
                    int toDo = checkLevelCompletion(player, Settings.challengeLevels.get(i - 1));
                    lore = Util.chop(
                            ChatColor.WHITE,
                            plugin.myLocale(player.getUniqueId()).challengestoComplete.replace("[challengesToDo]", String.valueOf(toDo)).replace("[thisLevel]",
                                    Settings.challengeLevels.get(i - 1)), 25);
                    item.setLore(lore);
                    cp.add(item);
                }
            }
        }
        // Add the free challenges if not already shown (which can happen if all of the challenges are done!)
        if (!level.equals("") && challengeList.containsKey("")) {
            for (String freeChallenges: challengeList.get("")) {
                CPItem item = createItem(freeChallenges, player);
                if (item != null) {
                    cp.add(item);
                } 
            }
        }
        // Create the panel
        if (cp.size() > 0) {
            // Make sure size is a multiple of 9
            int size = cp.size() + 8;
            size -= (size % 9);
            Inventory newPanel = Bukkit.createInventory(null, size, plugin.myLocale(player.getUniqueId()).challengesguiTitle);
            // Store the panel details for retrieval later
            playerChallengeGUI.put(player.getUniqueId(), cp);
            playerChallengeLevel.put(player.getUniqueId(), level);
            // Fill the inventory and return
            int index = 0;
            for (CPItem i : cp) {
                newPanel.setItem(index++, i.getItem());
            }
            return newPanel;
        }
        return null;
    }

    /**
     * Creates an inventory item for the challenge
     * @param challengeName
     * @param player
     * @return Control Panel item
     */
    private CPItem createItem(String challengeName, Player player) {
        CPItem item = null;
        // Get the icon
        ItemStack icon = null;
        String iconName = getChallengeConfig().getString("challenges.challengeList." + challengeName + ".icon", "");
        if (!iconName.isEmpty()) {
            try {
                // Split if required
                String[] split = iconName.split(":");
                if (split.length == 1) {
                    // Some material does not show in the inventory
                    if (iconName.equalsIgnoreCase("potato")) {
                        iconName = "POTATO_ITEM";
                    } else if (iconName.equalsIgnoreCase("brewing_stand")) {
                        iconName = "BREWING_STAND_ITEM";
                    } else if (iconName.equalsIgnoreCase("carrot")) {
                        iconName = "CARROT_ITEM";
                    } else if (iconName.equalsIgnoreCase("cauldron")) {
                        iconName = "CAULDRON_ITEM";
                    } else if (iconName.equalsIgnoreCase("lava") || iconName.equalsIgnoreCase("stationary_lava")) {
                        iconName = "LAVA_BUCKET";
                    } else if (iconName.equalsIgnoreCase("water") || iconName.equalsIgnoreCase("stationary_water")) {
                        iconName = "WATER_BUCKET";
                    } else if (iconName.equalsIgnoreCase("portal")) {
                        iconName = "OBSIDIAN";
                    } else if (iconName.equalsIgnoreCase("PUMPKIN_STEM")) {
                        iconName = "PUMPKIN";
                    } else if (iconName.equalsIgnoreCase("skull")) {
                        iconName = "SKULL_ITEM";
                    } else if (iconName.equalsIgnoreCase("COCOA")) {
                        iconName = "INK_SACK:3";
                    } else if (iconName.equalsIgnoreCase("NETHER_WARTS")) {
                        iconName = "NETHER_STALK";
                    }
                    if (StringUtils.isNumeric(iconName)) {
                        icon = new ItemStack(Integer.parseInt(iconName));
                    } else {
                        icon = new ItemStack(Material.valueOf(iconName));
                    }
                    // Check POTION for V1.9 - for some reason, it must be declared as WATER otherwise comparison later causes an NPE
                    if (icon.getType().name().contains("POTION")) {
                        if (!plugin.getServer().getVersion().contains("(MC: 1.8") && !plugin.getServer().getVersion().contains("(MC: 1.7")) {                        
                            PotionMeta potionMeta = (PotionMeta)icon.getItemMeta();
                            potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                            icon.setItemMeta(potionMeta);
                        }
                    }
                } else if (split.length == 2) {
                    if (StringUtils.isNumeric(split[0])) {
                        icon = new ItemStack(Integer.parseInt(split[0]));
                    } else {
                        icon = new ItemStack(Material.valueOf(split[0]));
                    }
                    // Check POTION for V1.9 - for some reason, it must be declared as WATER otherwise comparison later causes an NPE
                    if (icon.getType().name().contains("POTION")) {
                        if (!plugin.getServer().getVersion().contains("(MC: 1.8") && !plugin.getServer().getVersion().contains("(MC: 1.7")) {                       
                            PotionMeta potionMeta = (PotionMeta)icon.getItemMeta();
                            try {
                                potionMeta.setBasePotionData(new PotionData(PotionType.valueOf(split[1].toUpperCase())));
                            } catch (Exception e) {
                                plugin.getLogger().severe("Challenges icon: Potion type of " + split[1] + " is unknown, setting to WATER. Valid types are:");
                                for (PotionType type: PotionType.values()) {
                                    plugin.getLogger().severe(type.name());
                                }
                                potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                            } 
                            icon.setItemMeta(potionMeta);
                        }
                    } else if (icon.getType().equals(Material.MONSTER_EGG)) {
                        // Handle monster egg icons
                        try {                                
                            EntityType type = EntityType.valueOf(split[1].toUpperCase());
                            if (Bukkit.getServer().getVersion().contains("(MC: 1.8") || Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                                icon = new SpawnEgg(type).toItemStack();
                            } else {
                                try {
                                    icon = new SpawnEgg1_9(type).toItemStack();
                                } catch (Exception ex) {
                                    plugin.getLogger().severe("Monster eggs not supported with this server version.");
                                }
                            }
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("Spawn eggs must be described by name. Try one of these (not all are possible):");                          
                            for (EntityType type : EntityType.values()) {
                                if (type.isSpawnable() && type.isAlive()) {
                                    plugin.getLogger().severe(type.toString());
                                }
                            }
                        }
                    } else {
                        icon.setDurability(Integer.valueOf(split[1]).shortValue());
                    }
                }
            } catch (Exception e) {
                // Icon was not well formatted
                plugin.getLogger().warning("Error in challenges.yml - icon format is incorrect for " + challengeName + ":" + iconName);
                plugin.getLogger().warning("Format should be 'icon: MaterialType:Damage' where Damage is optional");
            }
        }
        if (icon == null) {
            icon = new ItemStack(Material.PAPER);
        }
        // Handle spaces (AIR icon)
        if (icon.getType() == Material.AIR) {
            return new CPItem(icon, "");
        }
        String description = ChatColor.GREEN
                + ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challengeName + ".friendlyname",
                        challengeName.substring(0, 1).toUpperCase() + challengeName.substring(1)));
        // Remove extraneous info
        ItemMeta im = icon.getItemMeta();
        if (!plugin.getServer().getVersion().contains("1.7")) {
            im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            im.addItemFlags(ItemFlag.HIDE_DESTROYS);
            im.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        }
        // Check if completed or not
        boolean complete = false;
        if (Settings.addCompletedGlow && plugin.getPlayers().checkChallenge(player.getUniqueId(), challengeName)) {
            // Complete! Make the icon glow
            im.addEnchant(Enchantment.ARROW_DAMAGE, 0, true);
            if (!plugin.getServer().getVersion().contains("1.7")) {
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            complete = true;
        }
        icon.setItemMeta(im);
        boolean repeatable = false;
        if (getChallengeConfig().getBoolean("challenges.challengeList." + challengeName + ".repeatable", false)) {
            // Repeatable
            repeatable = true;
        }
        // Only show this challenge if it is not done or repeatable if the
        // setting Settings.removeCompleteOntimeChallenges
        if (!complete || ((complete && repeatable) || !Settings.removeCompleteOntimeChallenges)) {
            // Store the challenge panel item and the command that will be
            // called if it is activated.
            item = new CPItem(icon, description, Settings.CHALLENGECOMMAND + " c " + challengeName, null);
            // Get the challenge description, that changes depending on
            // whether the challenge is complete or not.
            List<String> lore = challengeDescription(challengeName, player);
            item.setLore(lore);
        }
        return item;
    }

    public List<CPItem> getCP(Player player) {
        return playerChallengeGUI.get(player.getUniqueId());
    }

    /**
     * Creates the challenge description for the "item" in the inventory
     * 
     * @param challenge
     * @param player
     * @return List of strings splitting challenge string into 25 chars long 
     */
    private List<String> challengeDescription(String challenge, Player player) {
        List<String> result = new ArrayList<String>();
        final int length = 25;
        // plugin.getLogger().info("DEBUG: challenge is '"+challenge+"'");
        // plugin.getLogger().info("challenges.challengeList." + challenge +
        // ".level");
        // plugin.getLogger().info(getChallengeConfig().getString("challenges.challengeList."
        // + challenge + ".level"));
        String level = getChallengeConfig().getString("challenges.challengeList." + challenge + ".level", "");
        if (!level.isEmpty()) {
            result.addAll(Util.chop(ChatColor.WHITE, plugin.myLocale(player.getUniqueId()).challengeslevel + ": " + level, length));
        }
        // Check if completed or not
        boolean complete = false;
        int maxTimes = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".maxtimes", 0);
        int doneTimes = plugin.getPlayers().checkChallengeTimes(player.getUniqueId(), challenge);
        if (plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
            // Complete!
            // result.add(ChatColor.AQUA + plugin.myLocale(player.getUniqueId()).challengescomplete);
            complete = true;
        }
        boolean repeatable = false;
        if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable", false)) {
            repeatable = true;
        }

        if (repeatable) {
            if (maxTimes == 0) {
                if (complete) {
                    result.add(ChatColor.AQUA + plugin.myLocale(player.getUniqueId()).challengescomplete);
                }
            } else {
                // Check if the player has maxed out the challenge
                if (doneTimes < maxTimes) {
                    result.add(plugin.myLocale(player.getUniqueId()).challengescompletedtimes.replace("[donetimes]", String.valueOf(doneTimes))
                            .replace("[maxtimes]", String.valueOf(maxTimes)));
                } else {
                    repeatable = false;
                    result.add(plugin.myLocale(player.getUniqueId()).challengesmaxreached.replace("[donetimes]", String.valueOf(doneTimes)).replace("[maxtimes]", String.valueOf(maxTimes)));
                }
            }
        } else if (complete) {
            result.add(ChatColor.AQUA + plugin.myLocale(player.getUniqueId()).challengescomplete);
        }

        final String type = getChallengeConfig().getString("challenges.challengeList." + challenge + ".type", "").toLowerCase();
        if (!complete || (complete && repeatable)) {
            String desc = ChatColor.translateAlternateColorCodes('&', getChallengeConfig().getString("challenges.challengeList." + challenge + ".description", "").replace("[label]", Settings.ISLANDCOMMAND));
            if (desc.contains("|")) {
                result.addAll(Arrays.asList(desc.split("\\|")));
            } else {
                result.addAll(Util.chop(ChatColor.GOLD, desc, length));
            }
            if (type.equals("inventory")) {
                if (getChallengeConfig().getBoolean("challenges.challengeList." + challenge.toLowerCase() + ".takeItems")) {
                    result.addAll(Util.chop(ChatColor.RED, plugin.myLocale(player.getUniqueId()).challengesitemTakeWarning, length));
                }
            } else if (type.equals("island")) {
                result.addAll(Util.chop(ChatColor.RED, plugin.myLocale(player.getUniqueId()).challengeserrorItemsNotThere, length));
            }
        }
        if (complete && (!type.equals("inventory") || !repeatable)) {
            result.addAll(Util.chop(ChatColor.RED, plugin.myLocale(player.getUniqueId()).challengesnotRepeatable, length));
            return result;
        }
        double moneyReward = 0;
        int expReward = 0;
        String rewardText = "";
        if (!plugin.getPlayers().checkChallenge(player.getUniqueId(), challenge)) {
            // First time
            moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0);
            rewardText = ChatColor.translateAlternateColorCodes('&',
                    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", ""));
            expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".expReward", 0);
            if (!rewardText.isEmpty()) {
                result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesfirstTimeRewards, length));
            }
        } else {
            // Repeat challenge
            moneyReward = getChallengeConfig().getDouble("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
            rewardText = ChatColor.translateAlternateColorCodes('&',
                    getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", ""));
            expReward = getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
            if (!rewardText.isEmpty()) {
                result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesrepeatRewards, length));
            }

        }
        if (!rewardText.isEmpty()) {
            result.addAll(Util.chop(ChatColor.WHITE, rewardText, length));
        }
        if (expReward > 0) {
            result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesexpReward + ": " + ChatColor.WHITE + expReward, length));
        }
        if (Settings.useEconomy && moneyReward > 0) {
            result.addAll(Util.chop(ChatColor.GOLD, plugin.myLocale(player.getUniqueId()).challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward), length));
        }
        return result;
    }

    /**
     * Saves the challenge.yml file if it does not exist
     */
    public void saveDefaultChallengeConfig() {
        if (challengeConfigFile == null) {
            challengeConfigFile = new File(plugin.getDataFolder(), "challenges.yml");
        }
        if (!challengeConfigFile.exists()) {
            plugin.saveResource("challenges.yml", false);
        }
    }

    /**
     * Reloads the challenge config file
     */
    public void reloadChallengeConfig() {
        if (challengeConfigFile == null) {
            challengeConfigFile = new File(plugin.getDataFolder(), "challenges.yml");
        }
        challengeFile = YamlConfiguration.loadConfiguration(challengeConfigFile);

        // Look for defaults in the jar
        /*
        if (plugin.getResource("challenges.yml") != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            challengeFile.setDefaults(defConfig);
        }*/
        Settings.challengeList = getChallengeConfig().getConfigurationSection("challenges.challengeList").getKeys(false);
        // This code below handles the edge case where the levels is set to ''
        if (getChallengeConfig().getString("challenges.levels","").isEmpty()) {
            Settings.challengeLevels = new ArrayList<String>();
        } else {
            Settings.challengeLevels = Arrays.asList(getChallengeConfig().getString("challenges.levels","").split(" "));
        }
        Settings.freeLevels = Arrays.asList(getChallengeConfig().getString("challenges.freelevels","").split(" "));
        Settings.waiverAmount = getChallengeConfig().getInt("challenges.waiveramount", 1);
        if (Settings.waiverAmount < 0) {
            Settings.waiverAmount = 0;
        }
        populateChallengeList();
    }

    /**
     * @return challenges FileConfiguration object
     */
    public FileConfiguration getChallengeConfig() {
        if (challengeFile == null) {
            reloadChallengeConfig();
        }
        return challengeFile;
    }

    /**
     * Saves challenges.yml
     */
    public void saveChallengeConfig() {
        if (challengeFile == null || challengeConfigFile == null) {
            return;
        }
        try {
            getChallengeConfig().save(challengeConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save config to " + challengeConfigFile);
        }
    }

    /**
     * Gets a list of all challenges in existence.
     */
    public List<String> getAllChallenges() {
        List<String> returned = new ArrayList<String>();
        for (List<String> challenges : challengeList.values()) {
            returned.addAll(challenges);
        }
        // Add levels
        for (String level : Settings.challengeLevels) {
            returned.add(level.toLowerCase());
        }
        return returned;
    }

    /**
     * Creates a list of challenges that the specified player is able to complete.
     * @param player
     * @return List of challenges
     */
    public List<String> getAvailableChallenges(Player player) {
        List<String> returned = new ArrayList<String>();
        for (Map.Entry<String, List<String>> e : challengeList.entrySet())
            if (isLevelAvailable(player, e.getKey())) {
                returned.addAll(e.getValue());
            }
        return returned;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        Player player = (Player) sender;

        List<String> options = new ArrayList<String>();

        switch (args.length) {
        case 0: 
        case 1:
            options.add("complete");
            options.addAll(getAvailableChallenges(player));
            break;
        case 2:
            options.addAll(getAvailableChallenges(player));
            break;
        }

        return Util.tabLimit(options, args.length != 0 ? args[args.length - 1] : "");
    }

    /**
     * Check if challenge can be reset according to challenges.yml file
     * @param challenge
     * @return true if this challenge can be reset, false if not
     */
    public boolean resetable(String challenge) {
        return getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".resetallowed", true);
    }

    /**
     * Gets the name of the highest challenge level the player has completed
     * @param player
     * @return challenge level
     */
    public String getChallengeLevel(Player player) {
        //plugin.getLogger().info("DEBUG: getting challenge level for " + player.getName());
        if (Settings.challengeLevels.isEmpty()) {
            return "";
        }      
        return Settings.challengeLevels.get(getLevelDone(player));
    }

    /**
     * Get the challenge list
     * @return challenge list
     */
    public static LinkedHashMap<String, List<String>> getChallengeList(){
        return challengeList;
    }

    /**
     * Records the reseting challenge
     * @param challenge - name of challenge
     * @param repeat - number of times it has been repeated
     * @param entry - when it was done
     */
    public void resetChallengeForAll(String challenge, long repeat, String entry) {
        resettingChallenges.set(challenge + ".resettime", System.currentTimeMillis());
        // TODO: store this entry
        resettingChallenges.set(challenge + ".repeat", repeat);
        resettingChallenges.set(challenge + ".duration", entry);
        Util.saveYamlFile(resettingChallenges, "resettimers.yml", true);
    }

    /**
     * Checks if a challenge has been reset or not since timestamp
     * @param challenge
     * @param timestamp of when challenge was last completed
     * @return true if reset, false if not
     */
    public boolean isChallengeReset(String challenge, long timestamp) {
        if (resettingChallenges.contains(challenge)) {
            // Check timestamp
            long timeToCheck = resettingChallenges.getLong(challenge + ".resettime");
            long repeat = resettingChallenges.getLong(challenge + ".repeat");
            //plugin.getLogger().info("DEBUG: repeat = " + repeat);
            if (repeat > 0) {
                // TODO: this should be done mathematically, but I'm too tired...
                int numberOfPeriods = (int)((double)(System.currentTimeMillis() - timeToCheck)/repeat);
                //plugin.getLogger().info("DEBUG: time diff = " + ((double)(System.currentTimeMillis() - timeToCheck)));
                //plugin.getLogger().info("DEBUG: number of periods = " + numberOfPeriods);
                timeToCheck = timeToCheck + numberOfPeriods * repeat;
                //plugin.getLogger().info("DEBUG: new time to check = " + timeToCheck);
                /*
                // Advance the reset time to just before the last one
                while (System.currentTimeMillis() > timeToCheck + repeat) {
                    timeToCheck += repeat;
                }
                timeToCheck -= repeat;
                 */
                resettingChallenges.set(challenge + ".resettime", timeToCheck);
                Util.saveYamlFile(resettingChallenges, "resettimers.yml", true);
            }
            if (timeToCheck > timestamp) {
                // Timestamp is older than reset time
                return true;
            }
        }
        return false;
    }

    /**
     * @return formatted list of reset challenges and their duration
     */
    public List<String> getRepeatingChallengeResets() {
        List<String> result = new ArrayList<String>();
        for (String challenge : resettingChallenges.getKeys(false)) {
            long resetTime = resettingChallenges.getLong(challenge + ".resettime");
            Date date = new Date(resetTime);

            String duration = resettingChallenges.getString(challenge + ".duration","");
            if (!duration.isEmpty()) {
                duration = plugin.myLocale().adminResetChallengeForAllRepeating.replace("[duration]", duration);
            }
            result.add(challenge + ": " + plugin.myLocale().adminResetChallengeForAllReset.replace("[date]",date.toString()) + " " + duration);
        }
        if (result.isEmpty()) {
            result.add(plugin.myLocale().banNone);
        }
        return result;
    }

    /**
     * @return set of any challenges in the reset list
     */
    public Set<String> getRepeatingChallengeResetsRaw() {
        return resettingChallenges.getKeys(false);
    }

    /**
     * Clears the challenge from the reset list
     * @param challenge
     */
    public void clearChallengeReset(String challenge) {
        if (resettingChallenges.contains(challenge)) {
            resettingChallenges.set(challenge, null);
            Util.saveYamlFile(resettingChallenges, "resettimers.yml", true);
        }
    }

}
