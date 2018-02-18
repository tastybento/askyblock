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

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

/**
 * This class puts a player into a "team" and sets the island level as the suffix.
 * The team suffix variable can then be used by other plugins, such as Essentials Chat
 * {TEAMSUFFIX}
 * @author tastybento
 *
 */
public class Scoreboards {
    private static ASkyBlock plugin = ASkyBlock.getPlugin();
    private static Scoreboards instance = new Scoreboards();
    private static ScoreboardManager manager;
    private static Scoreboard board;

    /**
     * 
     */
    private Scoreboards() {
        manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
    }

    /**
     * @return the instance
     */
    public static Scoreboards getInstance() {
        return instance;
    }

    /**
     * Puts a player into a team of their own and sets the team suffix to be the level
     * @param playerUUID - the player's UUID
     */
    public void setLevel(UUID playerUUID) {
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player == null) {
            // Player is offline...
            return;
        }
        // The default team name is their own name
        String teamName = player.getName();
        String level = String.valueOf(plugin.getPlayers().getIslandLevel(playerUUID));
        Team team = board.getTeam(teamName);
        if (team == null) {
            //Team does not exist
            team = board.registerNewTeam(teamName);
        }
        // Add the suffix
        team.setSuffix(Settings.teamSuffix.replace("[level]",String.valueOf(level)));
        //Adding player to team
        team.addPlayer(player);
        // Assign scoreboard to player
        player.setScoreboard(board);
    } 

    /**
     * Sets the player's level explicitly
     * @param playerUUID - the player's UUID
     * @param l
     */
    public void setLevel(UUID playerUUID, long l) {
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player == null) {
            // Player is offline...
            return;
        }
        // The default team name is their own name - must be 16 chars or less
        String teamName = player.getName();
        Team team = board.getTeam(teamName);
        if (team == null) {
            //Team does not exist
            team = board.registerNewTeam(teamName);
        }
        // Add the suffix
        team.setSuffix(Settings.teamSuffix.replace("[level]",String.valueOf(l)));
        //Adding player to team
        team.addPlayer(player);
        // Assign scoreboard to player
        player.setScoreboard(board);
    }
}
