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

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveEvents implements Listener {
    private ASkyBlock plugin;
    private PlayerCache players;

    protected JoinLeaveEvents(ASkyBlock aSkyBlock) {
	this.plugin = aSkyBlock;
	this.players = plugin.getPlayers();
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
	final UUID playerUUID = event.getPlayer().getUniqueId();
	if (players == null) {
	    plugin.getLogger().severe("players is NULL");
	}
	if (players.inTeam(playerUUID) && players.getTeamIslandLocation(playerUUID) == null) {
	    final UUID leader = players.getTeamLeader(playerUUID);
	    players.setTeamIslandLocation(playerUUID, players.getIslandLocation(leader));
	}
	players.addPlayer(playerUUID);
	// Set the player's name (it may have changed)
	players.setPlayerName(playerUUID, event.getPlayer().getName());
	players.save(playerUUID);
	if (Settings.logInRemoveMobs) {
	    plugin.removeMobs(event.getPlayer().getLocation());
	}
	//plugin.getLogger().info("Cached " + event.getPlayer().getName());
	// Load any messages for the player
	final List<String> messages = plugin.getMessages(playerUUID);
	if (!messages.isEmpty()) {
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    event.getPlayer().sendMessage(ChatColor.AQUA + Locale.newsHeadline);
		    int i = 1;
		    for (String message : messages) {
			event.getPlayer().sendMessage(i++ + ": " + message);
		    }
		}
	    }, 40L);
	}
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
	// Remove from coop list
	CoopPlay.getInstance().clearMyCoops(event.getPlayer());
	CoopPlay.getInstance().clearMyInvitedCoops(event.getPlayer());
	//CoopPlay.getInstance().returnAllInventories(event.getPlayer());
	//plugin.setMessage(event.getPlayer().getUniqueId(), "Hello! This is a test. You logged out");
	players.removeOnlinePlayer(event.getPlayer().getUniqueId());
    }
}