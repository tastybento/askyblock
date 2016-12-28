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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.CoopPlay;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.LevelCalc;
import com.wasteofplastic.askyblock.LevelCalcByChunk;
import com.wasteofplastic.askyblock.PlayerCache;
import com.wasteofplastic.askyblock.Scoreboards;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.TopTen;
import com.wasteofplastic.askyblock.util.VaultHelper;

public class JoinLeaveEvents implements Listener {
    private ASkyBlock plugin;
    private PlayerCache players;
    private final static boolean DEBUG = false;

    public JoinLeaveEvents(ASkyBlock aSkyBlock) {
        this.plugin = aSkyBlock;
        this.players = plugin.getPlayers();
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: on PlayerJoin");
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();
        if (DEBUG)
            plugin.getLogger().info("DEBUG: got player UUID");
        // Check language permission
        if (VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.lang")) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: checking language");
            // Get language
            String language = getLanguage(player);
            //plugin.getLogger().info("DEBUG: language = " + language);
            // Check if we have this language
            if (plugin.getResource("locale/" + language + ".yml") != null) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG:check if lang exists");
                if (plugin.getPlayers().getLocale(playerUUID).isEmpty()) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: setting locale to " + language);
                    plugin.getPlayers().setLocale(playerUUID, language);
                }
            }
        } else {
            // Default locale
            if (DEBUG)
                plugin.getLogger().info("DEBUG: using default locale");
            plugin.getPlayers().setLocale(playerUUID,"");
        }
        // Check updates
        if (player.isOp() && plugin.getUpdateCheck() != null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: checking Updates");
            plugin.checkUpdatesNotify(player);
        }

        if (players == null) {
            plugin.getLogger().severe("players is NULL");
        }
        // Load any messages for the player
        if (DEBUG)
            plugin.getLogger().info("DEBUG: checking messages for " + player.getName());
        final List<String> messages = plugin.getMessages().getMessages(playerUUID);
        if (messages != null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Messages waiting!");
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(ChatColor.AQUA + plugin.myLocale(playerUUID).newsHeadline);
                    int i = 1;
                    for (String message : messages) {
                        player.sendMessage(i++ + ": " + message);
                    }
                    // Clear the messages
                    plugin.getMessages().clearMessages(playerUUID);
                }
            }, 40L);
        } // else {
        // plugin.getLogger().info("no messages");
        // }

        // If this player is not an island player just skip all this
        if (DEBUG)
            plugin.getLogger().info("DEBUG: checking if player has island or is in team");
        if (!players.hasIsland(playerUUID) && !players.inTeam(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: not in team and does not have island");
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: has island");
        UUID leader = null;
        Location loc = null;
        /*
         * This should not be needed
         */
        if (players.inTeam(playerUUID) && players.getTeamIslandLocation(playerUUID) == null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: reseting team island");
            leader = players.getTeamLeader(playerUUID);
            players.setTeamIslandLocation(playerUUID, players.getIslandLocation(leader));
        }
        // Add island to grid if it is not in there already
        // Add owners to the island grid list as they log in
        // Rationalize any out-of-sync issues, which may occur if the server wasn't shutdown properly, etc.
        // Leader or solo
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Getting island info");
        if (players.hasIsland(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: owner");
            loc = players.getIslandLocation(playerUUID);
            leader = playerUUID;
        } else if (players.inTeam(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: team");
            // Team player
            loc = players.getTeamIslandLocation(playerUUID);
            leader = players.getTeamLeader(playerUUID);
            if (leader == null) {
                plugin.getLogger().severe("Player "+ player.getName() + " is in a team but leader's UUID is missing.");
            }
        }
        // If the player has an island location of some kind
        if (loc != null && leader != null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: getting island");
            // Check if the island location is on the grid
            Island island = plugin.getGrid().getIslandAt(loc);
            if (island == null) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: getIslandLoc is null!");
                // Island isn't in the grid, so add it
                // See if this owner is tagged as having an island elsewhere
                Island islandByOwner = plugin.getGrid().getIsland(leader);
                if (islandByOwner == null) {
                    // No previous ownership, so just create the new island in the grid
                    if (plugin.getGrid().onGrid(loc)) {
                        plugin.getGrid().addIsland(loc.getBlockX(), loc.getBlockZ(), leader);
                    } else {
                        plugin.getLogger().severe(player.getName() + " joined and has an island at " + loc + " but those coords are NOT on the grid! Use admin register commands to correct!");
                    }
                } else {
                    // We have a mismatch - correct in favor of the player info
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: getIslandLoc is null but there is a player listing");
                    plugin.getLogger().warning(player.getName() + " login: mismatch - player.yml and islands.yml are out of sync. Fixing...");
                    // Cannot delete by location
                    plugin.getGrid().deleteIslandOwner(playerUUID);
                    if (plugin.getGrid().onGrid(loc)) {
                        plugin.getGrid().addIsland(loc.getBlockX(), loc.getBlockZ(), leader);
                    } else {
                        plugin.getLogger().severe(player.getName() + " joined and has an island at " + loc + " but those coords are NOT on the grid! Use admin register commands to correct!");
                    }
                }
            } else {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: island is not null");
                // Island at this location exists
                //plugin.getLogger().info("DEBUG: getIslandLoc is not null - island exists");
                // See if this owner is tagged as having an island elsewhere
                Island islandByOwner = plugin.getGrid().getIsland(leader);
                if (islandByOwner == null) {
                    plugin.getLogger().warning(player.getName() + " login: has island, but islands.yml says it is unowned, correcting...");
                    // No previous ownership, so just assign ownership
                    plugin.getGrid().setIslandOwner(island, leader);
                } else {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: island by owner found");
                    if (!islandByOwner.equals(island)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: mismatch");
                        plugin.getLogger().warning(player.getName() + " login: mismatch - islands.yml and player.yml are out of sync. Fixing...");
                        // We have a mismatch - correct in favor of the player info
                        plugin.getGrid().deleteIsland(islandByOwner.getCenter());
                        plugin.getGrid().setIslandOwner(island, leader);
                    } else {
                        if (island.getOwner().equals(player.getUniqueId())) {
                            if (DEBUG) {
                                plugin.getLogger().info("DEBUG: This player owns the island and island protection size is " + islandByOwner.getProtectionSize());
                                plugin.getLogger().info("DEBUG: everything looks good");
                            }
                            // Dynamic island range sizes with permissions
                            int range = islandByOwner.getProtectionSize();
                            for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                                if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.range.")) {
                                    if (DEBUG)
                                        plugin.getLogger().info("DEBUG: perm found");
                                    if (perms.getPermission().contains(Settings.PERMPREFIX + "island.range.*")) {
                                        range = islandByOwner.getProtectionSize();
                                        break;
                                    } else {
                                        if (DEBUG)
                                            plugin.getLogger().info("DEBUG: found number perm");
                                        String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "island.range.");
                                        if (spl.length > 1) {
                                            range = Math.max(range, Integer.valueOf(spl[1]));
                                        }
                                    }
                                }
                            }
                            // Do some sanity checking
                            if (range % 2 != 0) {
                                range--;
                                if (DEBUG)
                                    plugin.getLogger().warning("Login range setting: Protection range must be even, using " + range + " for " + player.getName());
                            }
                            if (range > (Settings.islandDistance - 16) && !plugin.getConfig().getBoolean("island.overridelimit", false)) {
                                range = Settings.islandDistance - 16;
                                if (DEBUG)
                                    plugin.getLogger().warning("Login range setting: Island protection range must be " + (Settings.islandDistance - 16) + " or less, (island range -16). Setting to: "+ range);
                            }
                            if (range > islandByOwner.getProtectionSize()) {
                                plugin.getLogger().info(
                                        "Login range setting: Island protection range increased from " + islandByOwner.getProtectionSize() + " to "
                                                + range + " for " + player.getName() + " due to permission.");
                            }
                            islandByOwner.setProtectionSize(range);
                        }
                    }
                }
            }
        }
        // Run the level command if it's free to do so
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Run level calc?");
        if (Settings.loginLevel) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Yes");
            if (Settings.fastLevelCalc) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Fast calc");
                new LevelCalcByChunk(plugin, playerUUID, player, false);
            } else {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: slow calc");
                if (!plugin.isCalculatingLevel()) {
                    // This flag is true if the command can be used
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: calculating");
                    plugin.setCalculatingLevel(true);
                    LevelCalc levelCalc = new LevelCalc(plugin, playerUUID, player, false);
                    levelCalc.runTaskTimer(plugin, 0L, 10L);
                }
            }
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Setting player's name");
        // Set the player's name (it may have changed), but only if it isn't empty
        if (!player.getName().isEmpty()) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Player name is " + player.getName());
            players.setPlayerName(playerUUID, player.getName());
        } else {
            plugin.getLogger().warning("Player that just logged in has no name! " + playerUUID.toString());
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Saving player");
        players.save(playerUUID);
        
        
        if (Settings.logInRemoveMobs) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Removing mobs");
            plugin.getGrid().removeMobs(player.getLocation());
        }

        // Set the TEAMNAME and TEAMSUFFIX variable if required
        if (Settings.setTeamName) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: setTeamName");
            Scoreboards.getInstance().setLevel(playerUUID);
        }

        // Check if they logged in to a locked island and expel them or if they are banned
        Island currentIsland = plugin.getGrid().getIslandAt(player.getLocation());
        if (currentIsland != null && (currentIsland.isLocked() || plugin.getPlayers().isBanned(currentIsland.getOwner(),player.getUniqueId()))) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Current island is locked, or player is banned");
            if (!currentIsland.getMembers().contains(playerUUID) && !player.isOp()
                    && !VaultHelper.checkPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: No bypass - teleporting");
                player.sendMessage(ChatColor.RED + plugin.myLocale(playerUUID).lockIslandLocked);
                plugin.getGrid().homeTeleport(player);
            }
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Setting the player's level in chat listener");
        // Set the player's level
        plugin.getChatListener().setPlayerLevel(playerUUID, plugin.getPlayers().getIslandLevel(player.getUniqueId()));
        
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Remove from top ten if excluded");
        // Remove from TopTen if the player has the permission
        if (!player.hasPermission(Settings.PERMPREFIX + "intopten")) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Removing from top ten");
            TopTen.topTenRemoveEntry(playerUUID);
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Log in completed, passing to other plugins.");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        // Remove from TopTen if the player has the permission
        if (!event.getPlayer().hasPermission(Settings.PERMPREFIX + "intopten")) {
            TopTen.topTenRemoveEntry(event.getPlayer().getUniqueId());
        }
        // Remove from coop list
        if (!Settings.persistantCoops) {
            CoopPlay.getInstance().clearMyCoops(event.getPlayer());
            CoopPlay.getInstance().clearMyInvitedCoops(event.getPlayer());
        }
        plugin.getChatListener().unSetPlayer(event.getPlayer().getUniqueId());
        // CoopPlay.getInstance().returnAllInventories(event.getPlayer());
        // plugin.setMessage(event.getPlayer().getUniqueId(),
        // "Hello! This is a test. You logged out");
        players.removeOnlinePlayer(event.getPlayer().getUniqueId());
    }

    /**
     * Attempts to get the player's language
     * @param p
     * @return language or empty string
     */
    public String getLanguage(Player p){
        Object ep;
        try {
            ep = getMethod("getHandle", p.getClass()).invoke(p, (Object[]) null);
            Field f = ep.getClass().getDeclaredField("locale");
            f.setAccessible(true);
            String language = (String) f.get(ep);
            language.replace('_', '-');
            return language;
        } catch (Exception e) {
            //nothing
        }
        return "en-US";
    }

    private Method getMethod(String name, Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name))
                return m;
        }
        return null;
    }

}