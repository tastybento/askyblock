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

package com.wasteofplastic.askyblock.listeners;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;

/**
 * This class is to catch chats and do two things: (1) substitute in the island level to the chat string
 * and (2) implement team chat. As it can be called asynchronously (and usually it is when a player chats)
 * it cannot access any HashMaps or Bukkit APIs without running the risk of a clash with another thread
 * or the main thread. As such two things are done:
 * 1. To handle the level substitution, a thread-safe hashmap of players and levels is stored and updated
 * in this class.
 * 2. To handle team chat, a thread-safe hashmap is used to store whether team chat is on for a player or not
 * and if it is, the team chat itself is queued to run on the next server tick, i.e., in the main thread
 * This all ensures it's thread-safe.
 * @author tastybento
 *
 */
public class ChatListener implements Listener {

    private final ASkyBlock plugin;
    private final Map<UUID,Boolean> teamChatUsers;
    private final Map<UUID,String> playerLevels;
    private final Map<UUID, String> playerChallengeLevels;
    // List of which admins are spying or not on team chat
    private final Set<UUID> spies;
    private static final boolean DEBUG = false;

    /**
     * @param plugin - ASkyBlock plugin object
     */
    public ChatListener(ASkyBlock plugin) {
        this.teamChatUsers = new ConcurrentHashMap<>();
        this.playerLevels = new ConcurrentHashMap<>();
        this.playerChallengeLevels = new ConcurrentHashMap<>();
        this.plugin = plugin;
        // Add all online player Levels
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playerLevels.put(player.getUniqueId(), String.valueOf(plugin.getPlayers().getIslandLevel(player.getUniqueId())));
            playerChallengeLevels.put(player.getUniqueId(), plugin.getChallenges().getChallengeLevel(player));
        }
        // Initialize spies
        spies = new HashSet<>();
    }

    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    /**
     * Provides an easy way to "fancy" the island level in chat
     * @since 3.0.8.3
     */
    private static final TreeMap<BigInteger, String> LEVELS;
    static {
        LEVELS = new TreeMap<>();

        LEVELS.put(THOUSAND, "k");
        LEVELS.put(THOUSAND.pow(2), "M");
        LEVELS.put(THOUSAND.pow(3), "G");
        LEVELS.put(THOUSAND.pow(4), "T");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + event.getEventName());
        // Substitute variable - thread safe
        String level = "";
        if (playerLevels.containsKey(event.getPlayer().getUniqueId())) {
            level = playerLevels.get(event.getPlayer().getUniqueId());
            if(Settings.fancyIslandLevelDisplay) {
                BigInteger levelValue = BigInteger.valueOf(Long.valueOf(level));

                Map.Entry<BigInteger, String> stage = LEVELS.floorEntry(levelValue);

                if (stage != null) { // level > 1000
                    // 1 052 -> 1.0k
                    // 1 527 314 -> 1.5M
                    // 3 874 130 021 -> 3.8G
                    // 4 002 317 889 -> 4.0T
                    level = new DecimalFormat("#.#").format(levelValue.divide(stage.getKey().divide(THOUSAND)).doubleValue()/1000.0) + stage.getValue();
                }
            }
        }
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: player level = " + level);
            plugin.getLogger().info("DEBUG: getFormat = " + event.getFormat());
            plugin.getLogger().info("DEBUG: getMessage = " + event.getMessage());
        }
        String format = event.getFormat();
        if (!Settings.chatLevelPrefix.isEmpty()) {
            format = format.replace(Settings.chatLevelPrefix, level);
            if (DEBUG)
                plugin.getLogger().info("DEBUG: format (island level substitute) = " + format);
        }
        if (!Settings.chatChallengeLevelPrefix.isEmpty()) {
            level = "";
            if (playerChallengeLevels.containsKey(event.getPlayer().getUniqueId())) {
                level = playerChallengeLevels.get(event.getPlayer().getUniqueId());
            }
            format = format.replace(Settings.chatChallengeLevelPrefix, level);
            if (DEBUG)
                plugin.getLogger().info("DEBUG: format (challenge level sub) = " + format);           
        }
        event.setFormat(format);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: format set");
        // Team chat
        if (Settings.teamChat && teamChatUsers.containsKey(event.getPlayer().getUniqueId())) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: team chat");
            // Cancel the event
            event.setCancelled(true);
            // Queue the sync task because you cannot use HashMaps asynchronously. Delaying to the next tick
            // won't be a major issue for synch events either.
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    teamChat(event,event.getMessage());
                }});
        }
    }

    private void teamChat(final AsyncPlayerChatEvent event, String message) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        //Bukkit.getLogger().info("DEBUG: post: " + message);
        // Is team chat on for this player
        // Find out if this player is in a team (should be if team chat is on)
        // TODO: remove when player resets or leaves team
        if (plugin.getPlayers().inTeam(playerUUID)) {
            List<UUID> teamMembers = plugin.getPlayers().getMembers(player.getUniqueId());
            // Tell only the team members if they are online
            boolean onLine = false;
            if (Settings.chatIslandPlayer.isEmpty()) {
                message = plugin.myLocale(playerUUID).teamChatPrefix + message;
            } else {
                message = plugin.myLocale(playerUUID).teamChatPrefix.replace(Settings.chatIslandPlayer,player.getDisplayName()) + message;
            }
            for (UUID teamMember : teamMembers) {
                Player teamPlayer = plugin.getServer().getPlayer(teamMember);
                if (teamPlayer != null) {
                    Util.sendMessage(teamPlayer, message);
                    if (!teamMember.equals(playerUUID)) {
                        onLine = true;
                    }
                }
            }
            // Spy function
            if (onLine) {
                for (Player onlinePlayer: plugin.getServer().getOnlinePlayers()) {
                    if (spies.contains(onlinePlayer.getUniqueId()) && onlinePlayer.hasPermission(Settings.PERMPREFIX + "mod.spy")) {
                        Util.sendMessage(onlinePlayer, ChatColor.RED + "[TCSpy] " + ChatColor.WHITE + message);
                    }
                }
                //Log teamchat
                if(Settings.logTeamChat) plugin.getLogger().info(ChatColor.stripColor(message));
            }
            if (!onLine) {
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).teamChatNoTeamAround);
                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).teamChatStatusOff);
                teamChatUsers.remove(playerUUID);
            }
        } else {
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).teamChatNoTeamAround);
            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(playerUUID).teamChatStatusOff);
            // Not in a team any more so delete
            teamChatUsers.remove(playerUUID);
        }
    }

    /**
     * Adds player to team chat
     * @param playerUUID - the player's UUID
     */
    public void setPlayer(UUID playerUUID) {
        this.teamChatUsers.put(playerUUID,true);
    }

    /**
     * Removes player from team chat
     * @param playerUUID - the player's UUID
     */
    public void unSetPlayer(UUID playerUUID) {
        this.teamChatUsers.remove(playerUUID);
    }

    /**
     * Whether the player has team chat on or not
     * @param playerUUID - the player's UUID
     * @return true if team chat is on
     */
    public boolean isTeamChat(UUID playerUUID) {
        return this.teamChatUsers.containsKey(playerUUID);
    }

    /**
     * Store the player's level for use in their chat tag
     * @param playerUUID - the player's UUID
     * @param l
     */
    public void setPlayerLevel(UUID playerUUID, long l) {
        //plugin.getLogger().info("DEBUG: putting " + playerUUID.toString() + " Level " + level);
        playerLevels.put(playerUUID, String.valueOf(l));
    }

    /**
     * Store the player's challenge level for use in their chat tag
     * @param player
     */
    public void setPlayerChallengeLevel(Player player) {
        //plugin.getLogger().info("DEBUG: setting player's challenge level to " + plugin.getChallenges().getChallengeLevel(player));
        playerChallengeLevels.put(player.getUniqueId(), plugin.getChallenges().getChallengeLevel(player));
    }

    /**
     * Return the player's level for use in chat - async safe
     * @param playerUUID - the player's UUID
     * @return Player's level as string
     */
    public String getPlayerLevel(UUID playerUUID) {
        return playerLevels.get(playerUUID);
    }

    /**
     * Return the player's challenge level for use in chat - async safe
     * @param playerUUID - the player's UUID
     * @return challenge level as string or empty string none
     */
    public String getPlayerChallengeLevel(UUID playerUUID) {
        if (playerChallengeLevels.containsKey(playerUUID))
            return playerChallengeLevels.get(playerUUID);
        return "";
    }

    /**
     * Toggles team chat spy. Spy must also have the spy permission to see chats
     * @param playerUUID - the player's UUID
     * @return true if toggled on, false if toggled off
     */
    public boolean toggleSpy(UUID playerUUID) {
        if (spies.contains(playerUUID)) {
            spies.remove(playerUUID);
            return false;
        } else {
            spies.add(playerUUID);
            return true;
        }
    }
}
