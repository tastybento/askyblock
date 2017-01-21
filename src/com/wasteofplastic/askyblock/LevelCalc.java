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

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.events.IslandLevelEvent;

/**
 * A class that calculates the level of an island. Runs through all the island
 * blocks
 * Designed to run repeatedly until the calculation is done.
 * 
 * @author tastybento
 * 
 */
public class LevelCalc extends BukkitRunnable {
    private ASkyBlock plugin;
    private Location l;
    private int counter;
    private int px;
    private int pz;
    private int slice;
    private HashMap<MaterialData, Integer> limitCount = new HashMap<MaterialData, Integer>();
    private int blockCount;
    private int underWaterBlockCount;
    private int oldLevel;
    private UUID targetPlayer;
    private int range = Settings.island_protectionRange;
    private CommandSender asker;
    private int islandLevelHandicap;

    public LevelCalc(ASkyBlock plugin, UUID targetPlayer, CommandSender asker) {
        this(plugin, targetPlayer, asker, false);
    }

    /**
     * Calculates the level of an island
     * @param plugin
     * @param targetPlayer
     * @param sender
     * @param report
     */
    public LevelCalc(ASkyBlock plugin, UUID targetPlayer, final CommandSender sender, boolean report) {
        this.asker = sender;
        this.plugin = plugin;
        //plugin.getLogger().info("DEBUG: running level calc " + report);
        this.targetPlayer = targetPlayer;
        this.counter = 255;
        if (plugin.getPlayers().inTeam(targetPlayer)) {
            this.l = plugin.getPlayers().getTeamIslandLocation(targetPlayer);
        } else {
            this.l = plugin.getPlayers().getIslandLocation(targetPlayer);
        }
        this.px = l.getBlockX();
        this.pz = l.getBlockZ();
        // Get player's island
        Island island = plugin.getGrid().getIsland(targetPlayer);
        if (island != null) {
            range = island.getProtectionSize();
            islandLevelHandicap = island.getLevelHandicap();
        } else {
            range = Settings.island_protectionRange;
        }
        // Calculated based on the size of the protection area
        double ratio = (double) counter * 12100 / (double) (range*range);
        // plugin.getLogger().info("DEBUG: ratio = " + ratio +
        // " protection range = " + range);
        this.slice = (int) ratio;
        if (this.slice < 1) {
            this.slice = 1;
        }
        // Copy the limits hashmap
        limitCount = new HashMap<MaterialData,Integer>(Settings.blockLimits);
        /*
        for (MaterialData m : Settings.blockLimits.keySet()) {
            limitCount.put(m, Settings.blockLimits.get(m));
            //plugin.getLogger().info("DEBUG:" + m.toString() + " x " + Settings.blockLimits.get(m));
        }*/
        this.blockCount = 0;
        this.underWaterBlockCount = 0;
        oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);
    }

    @Override
    public void run() {
        // Only run if the flag is set
        if (!plugin.isCalculatingLevel()) {
            this.cancel();
        }
        //slice = 256;
        //plugin.getLogger().info("DEBUG: slice = " + slice);
        //long lastPoll = System.currentTimeMillis();
        calculateSlice(counter, (counter - slice));
        //plugin.getLogger().info("DEBUG: timer = " + (System.currentTimeMillis()- lastPoll) + "ms");
        counter = counter - slice - 1;
        if (counter <= 0) {
            // Calculations are complete
            // Add in the under water count
            blockCount += (int)((double)underWaterBlockCount * Settings.underWaterMultiplier);
            // Add a multiplier based on the island rating 50 = normal, 100 = max hard, 1 = max easy
            // TODO: Removing this functionality for now as it will be too confusing for players
            // Need to get the rating for the LEADER of the island, not the target player
            // int multiplier = plugin.getPlayers().getStartIslandRating(leader);
            // If not zero then use it.
            //plugin.getLogger().info("DEBUG: block count = " + blockCount);
            // blockCount = (blockCount * multiplier) / 5000;
            // Get the permission multiplier if it is available
            Player player = plugin.getServer().getPlayer(targetPlayer);
            int multiplier = 1;
            if (player != null) {
                // Get permission multiplier                
                for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.multiplier.")) {
                        // Get the max value should there be more than one
                        multiplier = Math.max(multiplier, Integer.valueOf(perms.getPermission().split(Settings.PERMPREFIX + "island.multiplier.")[1]));
                    }
                    // Do some sanity checking
                    if (multiplier < 1) {
                        multiplier = 1;
                    }
                }
            }
            blockCount *= multiplier;
            // Get the death handicap
            int deaths = plugin.getPlayers().getDeaths(targetPlayer);
            if (plugin.getPlayers().inTeam(targetPlayer)) {
                // Get the team leader's deaths
                deaths = plugin.getPlayers().getDeaths(plugin.getPlayers().getTeamLeader(targetPlayer));
                if (Settings.sumTeamDeaths) {
                    deaths = 0;
                    //plugin.getLogger().info("DEBUG: player is in team");
                    for (UUID member : plugin.getPlayers().getMembers(targetPlayer)) {
                        deaths += plugin.getPlayers().getDeaths(member);
                    }
                }
            }
            blockCount -= (deaths * Settings.deathpenalty);
            blockCount /= Settings.levelCost;
            // Adjust using the island level handicap
            blockCount -= islandLevelHandicap;
            // plugin.getLogger().info("DEBUG: updating player");
            // Update player and team mates
            plugin.getPlayers().setIslandLevel(targetPlayer, blockCount);
            // plugin.getLogger().info("DEBUG: set island level, now trying to save player");
            plugin.getPlayers().save(targetPlayer);
            // plugin.getLogger().info("DEBUG: save player, now looking at team members");
            // Update any team members too
            if (plugin.getPlayers().inTeam(targetPlayer)) {
                // plugin.getLogger().info("DEBUG: player is in team");
                for (UUID member : plugin.getPlayers().getMembers(targetPlayer)) {
                    // plugin.getLogger().info("DEBUG: updating team member level too");
                    plugin.getPlayers().setIslandLevel(member, blockCount);
                    plugin.getPlayers().save(member);
                }
            }
            // plugin.getLogger().info("DEBUG: finished team member saving");
            // plugin.updateTopTen();
            if (asker != null) {
                if (!(asker instanceof Player)) {
                    // Console
                    asker.sendMessage(ChatColor.GREEN + plugin.myLocale().islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
                    if (Settings.deathpenalty != 0) {
                        asker.sendMessage(ChatColor.GREEN + "(" + String.valueOf(plugin.getPlayers().getDeaths(targetPlayer)) + " " + plugin.myLocale().deaths + ")");
                    }
                    asker.sendMessage(ChatColor.GREEN + "Level report only available with fast-level calculation option");
                } else {
                    // Tell offline team members the island level increased.
                    if (plugin.getPlayers().getIslandLevel(targetPlayer) > oldLevel) {
                        // plugin.getLogger().info("DEBUG: telling offline players");
                        plugin.getMessages().tellOfflineTeam(targetPlayer, ChatColor.GREEN + plugin.myLocale(targetPlayer).islandislandLevelis + " " + ChatColor.WHITE
                                + plugin.getPlayers().getIslandLevel(targetPlayer));
                    }
                    if (asker instanceof Player && ((Player)asker).isOnline()) {
                        //plugin.getLogger().info("DEBUG: updating player GUI");
                        String message = ChatColor.GREEN + plugin.myLocale(((Player)asker).getUniqueId()).islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer);
                        if (Settings.deathpenalty != 0) {
                            message += " " + plugin.myLocale(((Player)asker).getUniqueId()).levelDeaths.replace("[number]", String.valueOf(plugin.getPlayers().getDeaths(targetPlayer)));
                        }
                        asker.sendMessage(message);
                        //Send player how many points are required to reach next island level
                        int requiredPoints = (Settings.levelCost * (plugin.getPlayers().getIslandLevel(targetPlayer) + 1)) - ((blockCount+islandLevelHandicap)*Settings.levelCost);
                        String toNextLevel = ChatColor.GREEN + plugin.myLocale(((Player)asker).getUniqueId()).islandrequiredPointsToNextLevel.replace("[points]", String.valueOf(requiredPoints));
                        toNextLevel = toNextLevel.replace("[next]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer) + 1));
                        asker.sendMessage(toNextLevel);
                    }
                }

            }
            // plugin.getLogger().info("DEBUG: updating top ten");
            if (plugin.getPlayers().inTeam(targetPlayer)) {
                UUID leader = plugin.getPlayers().getTeamLeader(targetPlayer);
                if (leader != null) {
                    TopTen.topTenAddEntry(leader, blockCount);
                }
            } else {
                TopTen.topTenAddEntry(targetPlayer, blockCount);
            }
            // Fire the level event
            Island island = plugin.getGrid().getIsland(targetPlayer);
            final IslandLevelEvent event = new IslandLevelEvent(targetPlayer, island, blockCount);
            plugin.getServer().getPluginManager().callEvent(event);
            // plugin.getLogger().info("DEBUG: finished updating top ten");

            //plugin.getLogger().info("DEBUG: clearing flag");
            // Clear flag
            plugin.setCalculatingLevel(false);
            // Cancel this task
            //plugin.getLogger().info("DEBUG: canceling task");
            this.cancel();
            //plugin.getLogger().info("DEBUG: cancelled");
        }
    }

    /**
     * This calculates the value of a horizontal slice of island space.
     * It may be 255 to 0, or it may be smaller. The larger the island area, the smaller the slice.
     * This keeps the per-tick time low enough not to cause lag.
     * @param top
     * @param bottom
     */
    private void calculateSlice(int top, int bottom) {
        //plugin.getLogger().info("DEBUG: calculating top = " + top + " bottom = "+ bottom);
        if (bottom < 0) {
            bottom = 0;
        }
        int r = range /2;
        //plugin.getLogger().info("DEBUG: range = " + r);
        for (int y = top; y >= bottom; y--) {
            // plugin.getLogger().info("DEBUG: y = " + y);
            //plugin.getLogger().info("DEBUG: blockcount = " + blockCount);
            //plugin.getLogger().info("DEBUG: underwater blockcount = " + underWaterBlockCount);
            for (int x = px - r; x <= px + r; x++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    Material blockType = l.getWorld().getBlockAt(x, y, z).getType();
                    byte data = l.getWorld().getBlockAt(x, y, z).getData();
                    MaterialData md = new MaterialData(blockType);
                    md.setData(data);
                    MaterialData generic = new MaterialData(blockType);
                    if (blockType != Material.AIR) {
                        // Total up the values
                        if (limitCount.containsKey(md) && Settings.blockValues.containsKey(md)) {
                            int count = limitCount.get(md);
                            //plugin.getLogger().info("DEBUG: Count for non-generic " + md + " is " + count);
                            if (count > 0) {
                                limitCount.put(md, --count);
                                if (y<=Settings.sea_level) {
                                    underWaterBlockCount += Settings.blockValues.get(md);
                                } else {
                                    blockCount += Settings.blockValues.get(md);
                                }
                            }
                        } else if (limitCount.containsKey(generic) && Settings.blockValues.containsKey(generic)) {
                            int count = limitCount.get(generic);
                            //plugin.getLogger().info("DEBUG: Count for generic " + generic + " is " + count);
                            if (count > 0) {  
                                limitCount.put(generic, --count);
                                if (y<=Settings.sea_level) {
                                    underWaterBlockCount += Settings.blockValues.get(generic);
                                } else {
                                    blockCount += Settings.blockValues.get(generic);
                                }
                            }
                        } else if (Settings.blockValues.containsKey(md)) {
                            //plugin.getLogger().info("DEBUG: Adding " + md + " = " + Settings.blockValues.get(md));
                            if (y<=Settings.sea_level) {
                                underWaterBlockCount += Settings.blockValues.get(md);
                            } else {
                                blockCount += Settings.blockValues.get(md);
                            }
                        } else if (Settings.blockValues.containsKey(generic)) {
                            //plugin.getLogger().info("DEBUG: Adding " + generic + " = " + Settings.blockValues.get(generic));
                            if (y<=Settings.sea_level) {
                                underWaterBlockCount += Settings.blockValues.get(generic);
                            } else {
                                blockCount += Settings.blockValues.get(generic);
                            }
                        }

                    }
                }
            }
        }
    }

}