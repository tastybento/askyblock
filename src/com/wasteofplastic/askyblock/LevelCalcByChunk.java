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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.wasteofplastic.askyblock.events.IslandLevelEvent;
import com.wasteofplastic.askyblock.events.IslandPostLevelEvent;
import com.wasteofplastic.askyblock.events.IslandPreLevelEvent;
import com.wasteofplastic.askyblock.util.Util;

/**
 * A class that calculates the level of an island very quickly by copying island
 * chunks to a list and then processing asynchronously.
 * 
 * @author tastybento
 * 
 */
public class LevelCalcByChunk {

    private List<String> reportLines = new ArrayList<String>();

    public LevelCalcByChunk(ASkyBlock plugin, UUID targetPlayer, CommandSender asker) {
        this(plugin, targetPlayer, asker, false);
    }

    /**
     * Calculates the level of an island
     * @param plugin
     * @param targetPlayer - UUID of island owner or team member
     * @param sender - requester of the level calculation, if anyone
     * @param report - provide a report to the asker
     */
    public LevelCalcByChunk(final ASkyBlock plugin, final UUID targetPlayer, final CommandSender sender, final boolean report) {
        //if (report && plugin.getServer().getVersion().contains("(MC: 1.7")) { 
        //    Util.sendMessage(sender, ChatColor.RED + "This option is not available in V1.7 servers, sorry.");
        //    return;
        //}
        //plugin.getLogger().info("DEBUG: running level calc " + silent);
        // Get player's island
        final Island island = plugin.getGrid().getIsland(targetPlayer);
        if (island != null) {
            // Get the permission multiplier if it is available
            Player player = plugin.getServer().getPlayer(targetPlayer);
            int multiplier = 1;
            if (player != null) {
                // Get permission multiplier                
                for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                    if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.multiplier.")) {
                        String spl[] = perms.getPermission().split(Settings.PERMPREFIX + "island.multiplier.");
                        if (spl.length > 1) {
                            if (!NumberUtils.isDigits(spl[1])) {
                                plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                            } else {
                                // Get the max value should there be more than one
                                multiplier = Math.max(multiplier, Integer.valueOf(spl[1]));
                            }
                        }
                    }
                    // Do some sanity checking
                    if (multiplier < 1) {
                        multiplier = 1;
                    }
                }
            }
            final int levelMultiplier = multiplier;
            // Get the handicap
            final int levelHandicap = island.getLevelHandicap();
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
            final int deathHandicap = deaths;
            // Check if player's island world is the nether or overworld and adjust accordingly
            final World world = plugin.getPlayers().getIslandLocation(targetPlayer).getWorld();
            // Get the chunks
            //long nano = System.nanoTime();
            Set<ChunkSnapshot> chunkSnapshot = new HashSet<ChunkSnapshot>();
            for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionSize() + 16); x += 16) {
                for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionSize() + 16); z += 16) {
                    if (!world.getBlockAt(x, 0, z).getChunk().isLoaded()) {
                        world.getBlockAt(x, 0, z).getChunk().load();
                        chunkSnapshot.add(world.getBlockAt(x, 0, z).getChunk().getChunkSnapshot());
                        world.getBlockAt(x, 0, z).getChunk().unload();
                    } else {
                        chunkSnapshot.add(world.getBlockAt(x, 0, z).getChunk().getChunkSnapshot());
                    }                                       
                    //plugin.getLogger().info("DEBUG: getting chunk at " + x + ", " + z);
                }
            }
            //plugin.getLogger().info("DEBUG: time = " + (System.nanoTime() - nano) / 1000000 + " ms");
            //plugin.getLogger().info("DEBUG: size of chunk ss = " + chunkSnapshot.size());
            final Set<ChunkSnapshot> finalChunk = chunkSnapshot;
            final int worldHeight = world.getMaxHeight();
            //plugin.getLogger().info("DEBUG:world height = " +worldHeight);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    // Logging
                    File log = null;
                    PrintWriter out = null;
                    List<MaterialData> mdLog = null;
                    List<MaterialData> uwLog = null;
                    List<MaterialData> noCountLog = null;
                    List<MaterialData> overflowLog = null;
                    if (Settings.levelLogging) {
                        log = new File(plugin.getDataFolder(), "level.log");
                        try {
                            if (log.exists()) {
                                out = new PrintWriter(new FileWriter(log, true));
                            } else {
                                out = new PrintWriter(log);
                            }
                        } catch (FileNotFoundException e) {
                            System.out.println("Level log (level.log) could not be opened...");
                            e.printStackTrace();
                        } catch (IOException e) {
                            System.out.println("Level log (level.log) could not be opened...");
                            e.printStackTrace();
                        }
                    }
                    if (Settings.levelLogging || report) {
                        mdLog = new ArrayList<MaterialData>();
                        uwLog = new ArrayList<MaterialData>();
                        noCountLog = new ArrayList<MaterialData>();
                        overflowLog = new ArrayList<MaterialData>();
                    }
                    // Copy the limits hashmap
                    HashMap<MaterialData, Integer> limitCount = new HashMap<MaterialData, Integer>(Settings.blockLimits);
                    // Calculate the island score
                    int blockCount = 0;
                    int underWaterBlockCount = 0;
                    for (ChunkSnapshot chunk: finalChunk) {
                        for (int x = 0; x< 16; x++) { 
                            // Check if the block coord is inside the protection zone and if not, don't count it
                            if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionSize()) {
                                //plugin.getLogger().info("Block is outside protected area - x = " + (chunk.getX() * 16 + x));
                                continue;
                            }
                            for (int z = 0; z < 16; z++) {
                                // Check if the block coord is inside the protection zone and if not, don't count it
                                if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionSize()) {
                                    //plugin.getLogger().info("Block is outside protected area - z = " + (chunk.getZ() * 16 + z));
                                    continue;
                                }
                                for (int y = 0; y < worldHeight; y++) {
                                    int type = chunk.getBlockTypeId(x, y, z);
                                    int data = chunk.getBlockData(x, y, z);
                                    MaterialData md = new MaterialData(type,(byte) data);                                    
                                    MaterialData generic = new MaterialData(type);
                                    if (type != 0) { // AIR
                                        if (limitCount.containsKey(md) && Settings.blockValues.containsKey(md)) {
                                            int count = limitCount.get(md);
                                            //plugin.getLogger().info("DEBUG: Count for non-generic " + md + " is " + count);
                                            if (count > 0) {
                                                limitCount.put(md, --count);
                                                if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                                    underWaterBlockCount += Settings.blockValues.get(md);                                                    
                                                    if (Settings.levelLogging || report) {
                                                        uwLog.add(md);
                                                    }
                                                } else {
                                                    blockCount += Settings.blockValues.get(md);
                                                    if (Settings.levelLogging || report) {
                                                        mdLog.add(md); 
                                                    }
                                                }
                                            } else if (Settings.levelLogging || report) {
                                                overflowLog.add(md);
                                            }
                                        } else if (limitCount.containsKey(generic) && Settings.blockValues.containsKey(generic)) {
                                            int count = limitCount.get(generic);
                                            //plugin.getLogger().info("DEBUG: Count for generic " + generic + " is " + count);
                                            if (count > 0) {  
                                                limitCount.put(generic, --count);
                                                if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                                    underWaterBlockCount += Settings.blockValues.get(generic);
                                                    if (Settings.levelLogging || report) {
                                                        uwLog.add(md);
                                                    }
                                                } else {
                                                    blockCount += Settings.blockValues.get(generic);
                                                    if (Settings.levelLogging || report) {
                                                        mdLog.add(md); 
                                                    }
                                                }
                                            } else if (Settings.levelLogging || report) {
                                                overflowLog.add(md);
                                            }
                                        } else if (Settings.blockValues.containsKey(md)) {
                                            //plugin.getLogger().info("DEBUG: Adding " + md + " = " + Settings.blockValues.get(md));
                                            if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                                underWaterBlockCount += Settings.blockValues.get(md);
                                                if (Settings.levelLogging || report) {
                                                    uwLog.add(md);
                                                }
                                            } else {
                                                blockCount += Settings.blockValues.get(md);
                                                if (Settings.levelLogging || report) {
                                                    mdLog.add(md); 
                                                }
                                            }
                                        } else if (Settings.blockValues.containsKey(generic)) {
                                            //plugin.getLogger().info("DEBUG: Adding " + generic + " = " + Settings.blockValues.get(generic));
                                            if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                                underWaterBlockCount += Settings.blockValues.get(generic);
                                                if (Settings.levelLogging || report) {
                                                    uwLog.add(md);
                                                }
                                            } else {
                                                blockCount += Settings.blockValues.get(generic);
                                                if (Settings.levelLogging || report) {
                                                    mdLog.add(md); 
                                                }
                                            }
                                        } else if (Settings.levelLogging || report) {
                                            noCountLog.add(md);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    blockCount += (int)((double)underWaterBlockCount * Settings.underWaterMultiplier);
                    //System.out.println("block count = "+blockCount);

                    final int score = (((blockCount * levelMultiplier) - (deathHandicap * Settings.deathpenalty)) / Settings.levelCost) - levelHandicap;
                    // Logging or report
                    if (Settings.levelLogging || report) {
                        // provide counts
                        Multiset<MaterialData> uwCount = HashMultiset.create(uwLog);
                        Multiset<MaterialData> mdCount = HashMultiset.create(mdLog);
                        Multiset<MaterialData> ncCount = HashMultiset.create(noCountLog);
                        Multiset<MaterialData> ofCount = HashMultiset.create(overflowLog);
                        reportLines.add("Level Log for island at " + island.getCenter());
                        if (sender instanceof Player) {
                            reportLines.add("Asker is " + sender.getName() + " (" + ((Player)sender).getUniqueId().toString() + ")");
                        } else {
                            reportLines.add("Asker is console");
                        }
                        reportLines.add("Target player UUID = " + targetPlayer.toString());
                        reportLines.add("Total block value count = " + String.format("%,d",blockCount));
                        reportLines.add("Level cost = " + Settings.levelCost);
                        reportLines.add("Level multiplier = " + levelMultiplier + " (Player must be online to get a permission multiplier)");
                        reportLines.add("Schematic level handicap = " + levelHandicap + " (level is reduced by this amount)");
                        reportLines.add("Deaths handicap = " + (deathHandicap * Settings.deathpenalty) + " (" + deathHandicap + " deaths)");
                        reportLines.add("Level calculated = " + score);
                        reportLines.add("==================================");
                        int total = 0;
                        if (!uwCount.isEmpty()) {
                            reportLines.add("Underwater block count (Multiplier = x" + Settings.underWaterMultiplier + ") value");
                            reportLines.add("Total number of underwater blocks = " + String.format("%,d",uwCount.size()));
                            Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                                    Multisets.copyHighestCountFirst(uwCount).entrySet();
                            Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
                            while (it.hasNext()) {
                                Entry<MaterialData> type = it.next();
                                int value = 0;
                                if (Settings.blockValues.containsKey(type)) {
                                    // Specific
                                    value = Settings.blockValues.get(type);
                                } else if (Settings.blockValues.containsKey(new MaterialData(type.getElement().getItemType()))) {
                                    // Generic
                                    value = Settings.blockValues.get(new MaterialData(type.getElement().getItemType()));
                                }
                                if (value > 0) {
                                    reportLines.add(type.getElement().toString() + ":" 
                                            + String.format("%,d",type.getCount()) + " blocks x " + value + " = " + (value * type.getCount()));
                                    total += (value * type.getCount());
                                }
                            }
                            reportLines.add("Subtotal = " + total);
                            reportLines.add("==================================");
                        }
                        reportLines.add("Regular block count");
                        reportLines.add("Total number of blocks = " + String.format("%,d",mdCount.size()));
                        //Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                        //        Multisets.copyHighestCountFirst(mdCount).entrySet();
                        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                                mdCount.entrySet();
                        Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
                        while (it.hasNext()) {
                            Entry<MaterialData> type = it.next();
                            int value = 0;
                            if (Settings.blockValues.containsKey(type)) {
                                // Specific
                                value = Settings.blockValues.get(type);
                            } else if (Settings.blockValues.containsKey(new MaterialData(type.getElement().getItemType()))) {
                                // Generic
                                value = Settings.blockValues.get(new MaterialData(type.getElement().getItemType()));
                            }
                            if (value > 0) {
                                reportLines.add(type.getElement().toString() + ":" 
                                        + String.format("%,d",type.getCount()) + " blocks x " + value + " = " + (value * type.getCount()));
                                total += (value * type.getCount());
                            }
                        }
                        reportLines.add("Total = " + total);
                        reportLines.add("==================================");
                        reportLines.add("Blocks not counted because they exceeded limits: " + String.format("%,d",ofCount.size()));
                        //entriesSortedByCount = Multisets.copyHighestCountFirst(ofCount).entrySet();
                        entriesSortedByCount = ofCount.entrySet();
                        it = entriesSortedByCount.iterator();
                        while (it.hasNext()) {
                            Entry<MaterialData> type = it.next();
                            Integer limit = Settings.blockLimits.get(type.getElement());
                            String explain = ")";
                            if (limit == null) {
                                MaterialData generic = new MaterialData(type.getElement().getItemType());
                                limit = Settings.blockLimits.get(generic);
                                explain = " - All types)";
                            }
                            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks (max " + limit + explain);
                        }
                        reportLines.add("==================================");
                        reportLines.add("Blocks on island that are not in blockvalues.yml");
                        reportLines.add("Total number = " + String.format("%,d",ncCount.size()));
                        //entriesSortedByCount = Multisets.copyHighestCountFirst(ncCount).entrySet();
                        entriesSortedByCount = ncCount.entrySet();
                        it = entriesSortedByCount.iterator();
                        while (it.hasNext()) {
                            Entry<MaterialData> type = it.next();
                            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks");
                        }                        
                        reportLines.add("=================================");
                    }
                    if (out != null) {
                        // Write to file
                        for (String line : reportLines) {
                            out.println(line);
                        }
                        System.out.println("Finished writing level log.");
                        out.close();
                    }

                    // Calculate how many points are required to get to the next level
                    int calculatePointsToNextLevel = (Settings.levelCost * (score + 1 + levelHandicap)) - ((blockCount * levelMultiplier) - (deathHandicap * Settings.deathpenalty));
                    // Sometimes it will return 0, so calculate again to make sure it will display a good value
                    if(calculatePointsToNextLevel == 0) calculatePointsToNextLevel = (Settings.levelCost * (score + 2 + levelHandicap)) - ((blockCount * levelMultiplier) - (deathHandicap * Settings.deathpenalty));

                    final int pointsToNextLevel = calculatePointsToNextLevel;

                    // Return to main thread
                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

                        @Override
                        public void run() {
                            // Fire the pre-level event
                            Island island = plugin.getGrid().getIsland(targetPlayer);
                            final IslandPreLevelEvent event = new IslandPreLevelEvent(targetPlayer, island, score);
                            event.setPointsToNextLevel(pointsToNextLevel);
                            plugin.getServer().getPluginManager().callEvent(event);
                            int oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);
                            if (!event.isCancelled()) {
                                //plugin.getLogger().info("DEBUG: updating player");

                                if (oldLevel != event.getLevel()) {
                                    // Update player and team mates
                                    plugin.getPlayers().setIslandLevel(targetPlayer, event.getLevel());
                                    //plugin.getLogger().info("DEBUG: set island level, now trying to save player");
                                    plugin.getPlayers().save(targetPlayer);
                                }
                                //plugin.getLogger().info("DEBUG: save player, now looking at team members");
                                // Update any team members too
                                if (plugin.getPlayers().inTeam(targetPlayer)) {
                                    //plugin.getLogger().info("DEBUG: player is in team");
                                    for (UUID member : plugin.getPlayers().getMembers(targetPlayer)) {
                                        //plugin.getLogger().info("DEBUG: updating team member level too");
                                        if (plugin.getPlayers().getIslandLevel(member) != event.getLevel()) {
                                            plugin.getPlayers().setIslandLevel(member, event.getLevel());
                                            plugin.getPlayers().save(member);
                                        }
                                    }
                                }
                                //plugin.getLogger().info("DEBUG: finished team member saving");
                                //plugin.getLogger().info("DEBUG: updating top ten");
                                if (plugin.getPlayers().inTeam(targetPlayer)) {
                                    UUID leader = plugin.getPlayers().getTeamLeader(targetPlayer);
                                    if (leader != null) {
                                        TopTen.topTenAddEntry(leader, event.getLevel());
                                    }
                                } else {
                                    TopTen.topTenAddEntry(targetPlayer, event.getLevel());
                                }
                            }
                            // Fire the island level event
                            final IslandLevelEvent event2 = new IslandLevelEvent(targetPlayer, island, event.getLevel());
                            plugin.getServer().getPluginManager().callEvent(event2);

                            // Fire the island post level calculation event
                            final IslandPostLevelEvent event3 = new IslandPostLevelEvent(targetPlayer, island, event.getLevel(), event.getPointsToNextLevel());
                            plugin.getServer().getPluginManager().callEvent(event3);

                            if(!event3.isCancelled()){
                                // Check that sender still is online
                                if (sender != null) {
                                    // Check if console
                                    if (!(sender instanceof Player)) {
                                        // Console  
                                        if (!report) {
                                            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
                                        } else {
                                            for (String line: reportLines) {
                                                Util.sendMessage(sender, line);
                                            }
                                            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
                                            if (event.getPointsToNextLevel() >= 0) {
                                                String toNextLevel = ChatColor.GREEN + plugin.myLocale().islandrequiredPointsToNextLevel.replace("[points]", String.valueOf(event.getPointsToNextLevel()));
                                                toNextLevel = toNextLevel.replace("[next]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer) + 1));
                                                Util.sendMessage(sender, toNextLevel);
                                            }
                                        }
                                    } else {
                                        // Player
                                        if (!report) {
                                            // Tell offline team members the island level changed
                                            if (plugin.getPlayers().getIslandLevel(targetPlayer) != oldLevel) {
                                                //plugin.getLogger().info("DEBUG: telling offline players");
                                                plugin.getMessages().tellOfflineTeam(targetPlayer, ChatColor.GREEN + plugin.myLocale(targetPlayer).islandislandLevelis + " " + ChatColor.WHITE
                                                        + plugin.getPlayers().getIslandLevel(targetPlayer));
                                            }
                                            if (sender instanceof Player && ((Player)sender).isOnline()) {
                                                String message = ChatColor.GREEN + plugin.myLocale(((Player)sender).getUniqueId()).islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer);
                                                if (Settings.deathpenalty != 0) {
                                                    message += " " + plugin.myLocale(((Player)sender).getUniqueId()).levelDeaths.replace("[number]", String.valueOf(deathHandicap));
                                                }
                                                Util.sendMessage(sender, message);
                                                //Send player how many points are required to reach next island level
                                                if (event.getPointsToNextLevel() >= 0) {
                                                    String toNextLevel = ChatColor.GREEN + plugin.myLocale(((Player)sender).getUniqueId()).islandrequiredPointsToNextLevel.replace("[points]", String.valueOf(event.getPointsToNextLevel()));
                                                    toNextLevel = toNextLevel.replace("[next]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer) + 1));
                                                    Util.sendMessage(sender, toNextLevel);
                                                }
                                            }
                                        } else {
                                            if (((Player)sender).isOnline()) {
                                                for (String line: reportLines) {
                                                    Util.sendMessage(sender, line);
                                                }
                                            }
                                            Util.sendMessage(sender, ChatColor.GREEN + plugin.myLocale().islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
                                            if (event.getPointsToNextLevel() >= 0) {
                                                String toNextLevel = ChatColor.GREEN + plugin.myLocale().islandrequiredPointsToNextLevel.replace("[points]", String.valueOf(event.getPointsToNextLevel()));
                                                toNextLevel = toNextLevel.replace("[next]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer) + 1));
                                                Util.sendMessage(sender, toNextLevel);
                                            }
                                        }
                                    }
                                }
                            }
                        }});
                }});
        }
    }

}
