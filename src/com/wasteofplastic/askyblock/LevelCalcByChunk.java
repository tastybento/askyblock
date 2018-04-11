package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.wasteofplastic.askyblock.events.IslandPostLevelEvent;
import com.wasteofplastic.askyblock.events.IslandPreLevelEvent;
import com.wasteofplastic.askyblock.util.Pair;
import com.wasteofplastic.askyblock.util.Util;


public class LevelCalcByChunk {

    private static final int MAX_CHUNKS = 200;
    private static final long SPEED = 1;
    private boolean checking = true;
    private BukkitTask task;

    private ASkyBlock plugin;

    private Set<Pair<Integer, Integer>> chunksToScan;
    private Island island;
    private World world;
    private CommandSender asker;
    private UUID targetPlayer;
    private Results result;

    // Copy the limits hashmap
    HashMap<MaterialData, Integer> limitCount;
    private boolean report;
    private long oldLevel;


    public LevelCalcByChunk(final ASkyBlock plugin, final Island island, final UUID targetPlayer, final CommandSender asker, final boolean report) {
        this.plugin = plugin;
        this.island = island;
        this.world = island != null ? island.getCenter().getWorld() : null;
        this.asker = asker;
        this.targetPlayer = targetPlayer;
        this.limitCount = new HashMap<>(Settings.blockLimits);
        this.report = report;
        this.oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);

        // Results go here
        result = new Results();

        if (island == null) {
            return;
        }

        // Get chunks to scan
        chunksToScan = getChunksToScan(island);

        // Start checking
        checking = true;

        // Start a recurring task until done or cancelled
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, ()-> {
            if (this.island.getOwner() == null) {
                task.cancel();
                return;
            }
            Set<ChunkSnapshot> chunkSnapshot = new HashSet<>();
            if (checking) {
                Iterator<Pair<Integer, Integer>> it = chunksToScan.iterator();
                if (!it.hasNext()) {
                    // Nothing left
                    tidyUp();
                    return;
                }
                // Add chunk snapshots to the list
                while (it.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
                    Pair<Integer, Integer> pair = it.next();
                    if (!world.isChunkLoaded(pair.x, pair.z)) {
                        world.loadChunk(pair.x, pair.z);
                        chunkSnapshot.add(world.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                        world.unloadChunk(pair.x, pair.z);
                    } else {
                        chunkSnapshot.add(world.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                    }
                    it.remove();
                }
                // Move to next step
                checking = false;
                checkChunksAsync(chunkSnapshot);
            }
        }, 0L, SPEED);
    }

    private void checkChunksAsync(final Set<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                for (ChunkSnapshot chunk: chunkSnapshot) {
                    scanChunk(chunk);
                }
                // Nothing happened, change state
                checking = true;
            }

        });  

    }

    @SuppressWarnings("deprecation")
    private void scanChunk(ChunkSnapshot chunk) {
        
        for (int x = 0; x< 16; x++) { 
            // Check if the block coord is inside the protection zone and if not, don't count it
            if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionSize()) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                // Check if the block coord is inside the protection zone and if not, don't count it
                if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionSize()) {
                    continue;
                }

                for (int y = 0; y < island.getCenter().getWorld().getMaxHeight(); y++) {
                    Material blockType = Material.getMaterial(chunk.getBlockTypeId(x, y, z));
                    boolean belowSeaLevel = (Settings.seaHeight > 0 && y<=Settings.seaHeight) ? true : false;
                    // Air is free
                    if (!blockType.equals(Material.AIR)) {
                        checkBlock(blockType, chunk.getBlockData(x, y, z), belowSeaLevel);
                    }
                }
            }
        }
    }

    private void checkBlock(Material type, int blockData, boolean belowSeaLevel) {
        // Currently, there is no alternative to using block data (Feb 2018)
        @SuppressWarnings("deprecation")
        MaterialData md = new MaterialData(type, (byte) blockData);
        int count = limitCount(md);
        if (count > 0) {
            if (belowSeaLevel) {
                result.underWaterBlockCount += count;                                                    
                result.uwCount.add(md);
            } else {
                result.rawBlockCount += count;
                result.mdCount.add(md); 
            } 
        }
    }

    /**
     * Checks if a block has been limited or not and whether a block has any value or not
     * @param md
     * @return value of the block if can be counted
     */
    private int limitCount(MaterialData md) {
        MaterialData generic = new MaterialData(md.getItemType());
        if (limitCount.containsKey(md) && Settings.blockValues.containsKey(md)) {
            int count = limitCount.get(md);
            if (count > 0) {
                limitCount.put(md, --count);
                return Settings.blockValues.get(md);
            } else {
                result.ofCount.add(md);
                return 0;
            }
        } else if (limitCount.containsKey(generic) && Settings.blockValues.containsKey(generic)) {
            int count = limitCount.get(generic);
            if (count > 0) {  
                limitCount.put(generic, --count);
                return Settings.blockValues.get(generic);
            } else {
                result.ofCount.add(md);
                return 0;
            }
        } else if (Settings.blockValues.containsKey(md)) {
            return Settings.blockValues.get(md);
        } else if (Settings.blockValues.containsKey(generic)) {
            return Settings.blockValues.get(generic);
        } else {
            result.ncCount.add(md);
            return 0;
        }
    }

    /**
     * Get a set of all the chunks in island
     * @param island
     * @return
     */
    private Set<Pair<Integer, Integer>> getChunksToScan(Island island) {
        Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionSize() + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionSize() + 16); z += 16) {
                Pair<Integer, Integer> pair = new Pair<>(world.getBlockAt(x, 0, z).getChunk().getX(), world.getBlockAt(x, 0, z).getChunk().getZ());
                chunkSnapshot.add(pair);
            }
        }
        return chunkSnapshot;
    }

    private void tidyUp() {
        // Cancel
        task.cancel();
        // Finalize calculations
        result.rawBlockCount += (long)((double)result.underWaterBlockCount * Settings.underWaterMultiplier);
        // Set the death penalty
        result.deathHandicap = plugin.getPlayers().getDeaths(island.getOwner());
        // Set final score
        result.score = (result.rawBlockCount / Settings.levelCost) - result.deathHandicap - island.getLevelHandicap();
        // Run any modifications
        // Get the permission multiplier if it is available
        int levelMultiplier = 1;
        Player player = plugin.getServer().getPlayer(targetPlayer);
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
                            levelMultiplier = Math.max(levelMultiplier, Integer.valueOf(spl[1]));
                        }
                    }
                }
                // Do some sanity checking
                if (levelMultiplier < 1) {
                    levelMultiplier = 1;
                }
            }
        }
        // Calculate how many points are required to get to the next level
        long pointsToNextLevel = (Settings.levelCost * (result.score + 1 + island.getLevelHandicap())) - ((result.rawBlockCount * levelMultiplier) - (result.deathHandicap * Settings.deathpenalty));
        // Sometimes it will return 0, so calculate again to make sure it will display a good value
        if(pointsToNextLevel == 0) pointsToNextLevel = (Settings.levelCost * (result.score + 2 + island.getLevelHandicap()) - ((result.rawBlockCount * levelMultiplier) - (result.deathHandicap * Settings.deathpenalty)));

        // All done.
        informPlayers(saveLevel(island, targetPlayer, pointsToNextLevel));

    }

    private void informPlayers(IslandPreLevelEvent event) {
        // Fire the island post level calculation event
        final IslandPostLevelEvent event3 = new IslandPostLevelEvent(targetPlayer, island, event.getLongLevel(), event.getLongPointsToNextLevel());
        plugin.getServer().getPluginManager().callEvent(event3);

        if(!event3.isCancelled()){
            // Check that sender still is online
            if (asker != null) {
                // Check if console
                if (!(asker instanceof Player)) {
                    // Console  
                    if (!report) {
                        Util.sendMessage(asker, ChatColor.GREEN + plugin.myLocale().islandislandLevelis.replace("[level]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer))));
                    } else {
                        sendConsoleReport(asker);
                        Util.sendMessage(asker, ChatColor.GREEN + plugin.myLocale().islandislandLevelis.replace("[level]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer))));
                    }
                } else {
                    // Player
                    if (!report) {
                        // Tell offline team members the island level changed
                        if (plugin.getPlayers().getIslandLevel(targetPlayer) != oldLevel) {
                            //plugin.getLogger().info("DEBUG: telling offline players");
                            plugin.getMessages().tellOfflineTeam(targetPlayer, ChatColor.GREEN + plugin.myLocale().islandislandLevelis.replace("[level]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer))));
                        }
                        if (asker instanceof Player && ((Player)asker).isOnline()) {
                            String message = ChatColor.GREEN + plugin.myLocale(((Player)asker).getUniqueId()).islandislandLevelis.replace("[level]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer)));
                            if (Settings.deathpenalty != 0) {
                                message += " " + plugin.myLocale(((Player)asker).getUniqueId()).levelDeaths.replace("[number]", String.valueOf(result.deathHandicap));
                            }
                            Util.sendMessage(asker, message);
                            //Send player how many points are required to reach next island level
                            if (event.getLongPointsToNextLevel() >= 0) {
                                String toNextLevel = ChatColor.GREEN + plugin.myLocale(((Player)asker).getUniqueId()).islandrequiredPointsToNextLevel.replace("[points]", String.valueOf(event.getLongPointsToNextLevel()));
                                toNextLevel = toNextLevel.replace("[next]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer) + 1));
                                Util.sendMessage(asker, toNextLevel);
                            }
                        }
                    } else {
                        if (((Player)asker).isOnline()) {
                            sendConsoleReport(asker);
                        }
                        Util.sendMessage(asker, ChatColor.GREEN + plugin.myLocale().islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
                        if (event.getLongPointsToNextLevel() >= 0) {
                            String toNextLevel = ChatColor.GREEN + plugin.myLocale().islandrequiredPointsToNextLevel.replace("[points]", String.valueOf(event.getLongPointsToNextLevel()));
                            toNextLevel = toNextLevel.replace("[next]", String.valueOf(plugin.getPlayers().getIslandLevel(targetPlayer) + 1));
                            Util.sendMessage(asker, toNextLevel);
                        }
                    }
                }
            }
        }
    }


    private IslandPreLevelEvent saveLevel(Island island, UUID targetPlayer, long pointsToNextLevel) {
        // Fire the pre-level event
        final IslandPreLevelEvent event = new IslandPreLevelEvent(targetPlayer, island, result.score);
        event.setLongPointsToNextLevel(pointsToNextLevel);
        plugin.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            // Save the value
            plugin.getPlayers().setIslandLevel(island.getOwner(), event.getLongLevel());
            if (plugin.getPlayers().inTeam(targetPlayer)) {
                //plugin.getLogger().info("DEBUG: player is in team");
                for (UUID member : plugin.getPlayers().getMembers(targetPlayer)) {
                    //plugin.getLogger().info("DEBUG: updating team member level too");
                    if (plugin.getPlayers().getIslandLevel(member) != event.getLongLevel()) {
                        plugin.getPlayers().setIslandLevel(member, event.getLongLevel());
                        plugin.getPlayers().save(member);
                    }
                }
                UUID leader = plugin.getPlayers().getTeamLeader(targetPlayer);
                if (leader != null) {
                    plugin.getTopTen().topTenAddEntry(leader, event.getLongLevel());
                }
            } else {
                plugin.getTopTen().topTenAddEntry(targetPlayer, event.getLongLevel());
            }

        }
        return event;
    }

    private void sendConsoleReport(CommandSender asker) {
        List<String> reportLines = new ArrayList<>();
        // provide counts
        reportLines.add("Level Log for island at " + island.getCenter());
        reportLines.add("Island owner UUID = " + island.getOwner());
        reportLines.add("Total block value count = " + String.format("%,d",result.rawBlockCount));
        reportLines.add("Level cost = " + Settings.levelCost);
        //reportLines.add("Level multiplier = " + levelMultiplier + " (Player must be online to get a permission multiplier)");
        //reportLines.add("Schematic level handicap = " + levelHandicap + " (level is reduced by this amount)");
        reportLines.add("Deaths handicap = " + result.deathHandicap);
        reportLines.add("Level calculated = " + result.score);
        reportLines.add("==================================");
        int total = 0;
        if (!result.uwCount.isEmpty()) {
            reportLines.add("Underwater block count (Multiplier = x" + Settings.underWaterMultiplier + ") value");
            reportLines.add("Total number of underwater blocks = " + String.format("%,d",result.uwCount.size()));
            reportLines.addAll(sortedReport(total, result.uwCount));
        }
        reportLines.add("Regular block count");
        reportLines.add("Total number of blocks = " + String.format("%,d",result.mdCount.size()));
        reportLines.addAll(sortedReport(total, result.mdCount));

        reportLines.add("Blocks not counted because they exceeded limits: " + String.format("%,d",result.ofCount.size()));
        //entriesSortedByCount = Multisets.copyHighestCountFirst(ofCount).entrySet();
        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = result.ofCount.entrySet();
        Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
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
        reportLines.add("Blocks on island that are not in config.yml");
        reportLines.add("Total number = " + String.format("%,d",result.ncCount.size()));
        //entriesSortedByCount = Multisets.copyHighestCountFirst(ncCount).entrySet();
        entriesSortedByCount = result.ncCount.entrySet();
        it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<MaterialData> type = it.next();
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks");
        }                        
        reportLines.add("=================================");

        for (String line : reportLines) {
            asker.sendMessage(line);
        }
    }

    private Collection<String> sortedReport(int total, Multiset<MaterialData> materialDataCount) {
        Collection<String> result = new ArrayList<>();
        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = Multisets.copyHighestCountFirst(materialDataCount).entrySet();
        Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<MaterialData> en = it.next();
            MaterialData type = en.getElement();

            int value = 0;
            if (Settings.blockValues.containsKey(type)) {
                // Specific
                value = Settings.blockValues.get(type);
            } else if (Settings.blockValues.containsKey(new MaterialData(type.getItemType()))) {
                // Generic
                value = Settings.blockValues.get(new MaterialData(type.getItemType()));
            }
            if (value > 0) {
                result.add(type.toString() + ":" 
                        + String.format("%,d",en.getCount()) + " blocks x " + value + " = " + (value * en.getCount()));
                total += (value * en.getCount());
            }
        }
        result.add("Subtotal = " + total);
        result.add("==================================");
        return result;
    }

    /**
     * Results class
     *
     */
    public class Results {
        Multiset<MaterialData> mdCount = HashMultiset.create();
        Multiset<MaterialData> uwCount = HashMultiset.create();
        Multiset<MaterialData> ncCount = HashMultiset.create();
        Multiset<MaterialData> ofCount = HashMultiset.create();
        long rawBlockCount = 0;
        Island island;
        long underWaterBlockCount = 0;
        long score = 0;
        int deathHandicap = 0;
    }
}
