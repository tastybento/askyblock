package com.wasteofplastic.askyblock.listeners;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

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

    private ASkyBlock plugin;
    private ConcurrentHashMap<UUID,Boolean> teamChatUsers;
    private ConcurrentHashMap<UUID,Integer> playerLevels;

    /**
     * @param plugin
     * @param teamChatOn
     */
    public ChatListener(ASkyBlock plugin) {
	this.teamChatUsers = new ConcurrentHashMap<UUID,Boolean>();
	this.playerLevels = new ConcurrentHashMap<UUID,Integer>();
	this.plugin = plugin;
	// Add all online player Levels
	for (Player player : plugin.getServer().getOnlinePlayers()) {
	    playerLevels.put(player.getUniqueId(), plugin.getPlayers().getIslandLevel(player.getUniqueId()));
	}
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
	final String oldMessage = event.getMessage();
	//Bukkit.getLogger().info("DEBUG: pre: " + oldMessage);
	// Substitute variable - thread safe
	String level = "";
	if (playerLevels.containsKey(event.getPlayer().getUniqueId())) {
	    level = String.valueOf(playerLevels.get(event.getPlayer().getUniqueId()));
	}
	final String message = oldMessage.replace("{ISLAND_LEVEL}", level);
	event.setMessage(message);
	// Team chat
	if (Settings.teamChat && teamChatUsers.containsKey(event.getPlayer().getUniqueId())) {
	    // Cancel the event
	    event.setCancelled(true);
	    // Queue the sync task because you cannot use HashMaps asynchronously. Delaying to the next tick
	    // won't be a major issue for synch events either.
	    Bukkit.getScheduler().runTask(plugin, new Runnable() {
		@Override
		public void run() {
		    teamChat(event,message);
		}});
	}
    }

    private void teamChat(final AsyncPlayerChatEvent event, final String message) {
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
	    for (UUID teamMember : teamMembers) {
		Player teamPlayer = plugin.getServer().getPlayer(teamMember);
		if (teamPlayer != null) {
		    teamPlayer.sendMessage(plugin.myLocale(playerUUID).teamChatPrefix.replace("{ISLAND_PLAYER}",player.getDisplayName()) + message);
		    if (!teamMember.equals(playerUUID)) {
			onLine = true;
		    }
		}
	    }
	    if (!onLine) {
		player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).teamChatNoTeamAround);
		player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).teamChatStatusOff);
		teamChatUsers.remove(playerUUID);
	    }
	} else {
	    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).teamChatNoTeamAround);
	    player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).teamChatStatusOff);
	    // Not in a team any more so delete
	    teamChatUsers.remove(playerUUID);
	}
    }

    /**
     * @param Adds player to team chat
     */
    public void setPlayer(UUID playerUUID) {
	this.teamChatUsers.put(playerUUID,true);
    }

    /**
     * Removes player from team chat
     * @param playerUUID
     */
    public void unSetPlayer(UUID playerUUID) {
	this.teamChatUsers.remove(playerUUID);
    }

    /**
     * Whether the player has team chat on or not
     * @param playerUUID
     * @return true if team chat is on
     */
    public boolean isTeamChat(UUID playerUUID) {
	return this.teamChatUsers.containsKey(playerUUID);
    }

    /**
     * Store the player's level for use in their chat tag
     * @param playerUUID
     * @param level
     */
    public void setPlayerLevel(UUID playerUUID, int level) {
	//plugin.getLogger().info("DEBUG: putting " + playerUUID.toString() + " Level " + level);
	playerLevels.put(playerUUID, level);
    }
}
