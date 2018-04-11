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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.wasteofplastic.askyblock.util.HeadGetter.HeadInfo;
import com.wasteofplastic.askyblock.util.Requester;
import com.wasteofplastic.askyblock.util.Util;

/**
 * Handles all Top Ten List functions
 * 
 * @author tastybento
 * 
 */
public class TopTen implements Listener, Requester {
    private  ASkyBlock plugin = ASkyBlock.getPlugin();
    // Top ten list of players
    private Map<UUID, Long> topTenList = new ConcurrentHashMap<>();
    private final int GUISIZE = 27; // Must be a multiple of 9
    private final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
    private final boolean DEBUG = false;
    // Store this as a static because it's the same for everyone and saves memory cleanup
    private Inventory gui;
    private Map<UUID, ItemStack> topTenHeads = new HashMap<>();

    public TopTen(ASkyBlock plugin) {
        this.plugin = plugin;
        topTenLoad();
    }

    /**
     * Adds a player to the top ten, if the level is good enough
     * 
     * @param ownerUUID
     * @param l
     */
    public void topTenAddEntry(UUID ownerUUID, long l) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: adding top ten entry " + ownerUUID + " " + l);
        }
        // Special case for removals. If a level of zero is given the player
        // needs to be removed from the list
        if (l < 1) {
            if (topTenList.containsKey(ownerUUID)) {
                topTenList.remove(ownerUUID);
            }
            return;
        }
        // Try and see if the player is online
        Player player = plugin.getServer().getPlayer(ownerUUID);
        if (player != null) {
            // Online
            if (!player.hasPermission(Settings.PERMPREFIX + "intopten")) {
                topTenList.remove(ownerUUID);
                return;
            }
        }
        topTenList.put(ownerUUID, l);
        topTenList = topTenList.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(10)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        // Add head to cache
        if (topTenList.containsKey(ownerUUID) && !topTenHeads.containsKey(ownerUUID)) {
            String name = plugin.getPlayers().getName(ownerUUID);
            if (name != null && !name.isEmpty()) {
                ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                meta.setDisplayName(name);
                playerSkull.setItemMeta(meta);
                topTenHeads.put(ownerUUID, playerSkull);
                // Get skull async
                plugin.getHeadGetter().getHead(ownerUUID, this);
            }
        } 
    }

    /**
     * Removes ownerUUID from the top ten list
     * 
     * @param ownerUUID
     */
    public void topTenRemoveEntry(UUID ownerUUID) {
        topTenList.remove(ownerUUID);
    }

    /**
     * Generates a sorted map of islands for the Top Ten list from all player
     * files
     */
    public void topTenCreate() {
        topTenCreate(null);
    }

    /**
     * Creates the top ten list from scratch. Does not get the level of each island. Just
     * takes the level from the player's file.
     * Runs asynchronously from the main thread.
     * @param sender
     */
    public void topTenCreate(final CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                plugin.getIslandCmd().setCreatingTopTen(true);
                // This map is a list of owner and island level
                YamlConfiguration player = new YamlConfiguration();
                int index = 0;
                for (final File f : plugin.getPlayersFolder().listFiles()) {
                    // Need to remove the .yml suffix
                    String fileName = f.getName();
                    if (fileName.endsWith(".yml")) {
                        try {
                            String playerUUIDString = fileName.substring(0, fileName.length() - 4);
                            final UUID playerUUID = UUID.fromString(playerUUIDString);
                            if (playerUUID == null) {
                                plugin.getLogger().warning("Player file contains erroneous UUID data.");
                                plugin.getLogger().info("Looking at " + playerUUIDString);
                            }
                            player.load(f);
                            index++;
                            if (index % 1000 == 0) {
                                plugin.getLogger().info("Processed " + index + " players for top ten");
                            }
                            // Players player = new Players(this, playerUUID);
                            int islandLevel = player.getInt("islandLevel", 0);
                            String teamLeaderUUID = player.getString("teamLeader", "");
                            if (islandLevel > 0) {
                                if (!player.getBoolean("hasTeam") || (!teamLeaderUUID.isEmpty() && teamLeaderUUID.equals(playerUUIDString))) {
                                    // Only enter team leaders into the top ten
                                    topTenAddEntry(playerUUID, islandLevel);
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error when reading player file. File is " + fileName);
                            plugin.getLogger().severe("Look at the stack trace and edit the file - it probably has broken YAML in it for some reason.");
                            e.printStackTrace();
                        }
                    }
                }
                plugin.getLogger().info("Processed " + index + " players for top ten");
                // Save the top ten
                topTenSave();

                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (sender != null) {
                            Util.sendMessage(sender, ChatColor.YELLOW + plugin.myLocale().adminTopTenfinished);
                        } else {
                            plugin.getLogger().warning("Completed top ten creation.");
                        }
                        plugin.getIslandCmd().setCreatingTopTen(false);

                    }});
            }});
    }

    public void topTenSave() {
        if (topTenList == null) {
            return;
        }
        //plugin.getLogger().info("Saving top ten list");
        // Make file
        File topTenFile = new File(plugin.getDataFolder(), "topten.yml");
        // Make configuration
        YamlConfiguration config = new YamlConfiguration();
        // Save config
        int rank = 0;
        for (Map.Entry<UUID, Long> m : topTenList.entrySet()) {
            if (rank++ == 10) {
                break;
            }
            config.set("topten." + m.getKey().toString(), m.getValue());
        }
        try {
            config.save(topTenFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save top ten list!");
            e.printStackTrace();
        }
    }

    /**
     * Loads the top ten from the file system topten.yml. If it does not exist
     * then the top ten is created
     */
    public void topTenLoad() {
        plugin.getLogger().info("Loading Top Ten");
        topTenList.clear();
        // Check to see if the top ten list exists
        File topTenFile = new File(plugin.getDataFolder(), "topten.yml");
        if (!topTenFile.exists()) {
            plugin.getLogger().warning("Top ten file does not exist - creating it. This could take some time with a large number of players");
            topTenCreate();
        } else {
            // Load the top ten
            YamlConfiguration topTenConfig = Util.loadYamlFile("topten.yml");
            // Load the values
            if (topTenConfig.isSet("topten")) {
                for (String playerUUID : topTenConfig.getConfigurationSection("topten").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(playerUUID);
                        int level = topTenConfig.getInt("topten." + playerUUID);
                        topTenAddEntry(uuid, level);
                    } catch (Exception e) {
                        e.printStackTrace();
                        plugin.getLogger().severe("Problem loading top ten list - recreating - this may take some time");
                        topTenCreate();
                    }
                }
            }
        }
    }

    /**
     * Displays the Top Ten list if it exists in chat
     * 
     * @param player
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public boolean topTenShow(final Player player) {
        // Old chat display
        if(Settings.displayIslandTopTenInChat){
            Util.sendMessage(player, ChatColor.GOLD + plugin.myLocale(player.getUniqueId()).topTenheader);
            if (topTenList == null) {
                topTenCreate();
                // Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).topTenerrorNotReady);
                // return true;
            }
            topTenList = topTenList.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(10)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            int i = 1;
            // getLogger().info("DEBUG: " + topTenList.toString());
            // getLogger().info("DEBUG: " + topTenList.values());
            Iterator<Entry<UUID, Long>> it = topTenList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> m = it.next();
                UUID playerUUID = m.getKey();
                // Remove from TopTen if the player is online and has the permission
                Player entry = plugin.getServer().getPlayer(playerUUID);
                boolean show = true;
                if (entry != null) {
                    if (!entry.hasPermission(Settings.PERMPREFIX + "intopten")) {
                        it.remove();
                        show = false;
                    }
                }
                if (show) {
                    if (plugin.getPlayers().inTeam(playerUUID)) {
                        // Island name + Island members + Island level
                        final List<UUID> pMembers = plugin.getPlayers().getMembers(playerUUID);
                        String memberList = "";
                        for (UUID members : pMembers) {
                            memberList += plugin.getPlayers().getName(members) + ", ";
                        }
                        if (memberList.length() > 2) {
                            memberList = memberList.substring(0, memberList.length() - 2);
                        }
                        Util.sendMessage(player, ChatColor.AQUA + "#" + i + ": " + plugin.getGrid().getIslandName(playerUUID) + ChatColor.AQUA + " (" + memberList + ") - "
                                + plugin.myLocale(player.getUniqueId()).levelislandLevel + " " + m.getValue());
                    } else {
                        // Island name + Island level
                        Util.sendMessage(player, ChatColor.AQUA + "#" + i + ": " + plugin.getGrid().getIslandName(playerUUID) + ChatColor.AQUA +  " - " + plugin.myLocale(player.getUniqueId()).levelislandLevel + " "
                                + m.getValue());
                    }
                    if (i++ == 10) {
                        break;
                    }
                }
            }
        } else {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: new GUI display");
            // New GUI display (shown by default)
            if (topTenList == null) topTenCreate();
            topTenList = topTenList.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(10)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            // Create the top ten GUI if it does not exist
            if (gui == null) {
                gui = Bukkit.createInventory(null, GUISIZE, plugin.myLocale(player.getUniqueId()).topTenGuiTitle);
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: creating GUI for the first time");
            }
            // Reset
            gui.clear();
            int i = 1;
            Iterator<Entry<UUID, Long>> it = topTenList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> m = it.next();
                UUID playerUUID = m.getKey();
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: " + i + ": " + playerUUID);
                // Remove from TopTen if the player is online and has the permission
                Player entry = plugin.getServer().getPlayer(playerUUID);
                boolean show = true;
                if (entry != null) {
                    if (!entry.hasPermission(Settings.PERMPREFIX + "intopten")) {
                        it.remove();
                        show = false;
                    }
                } else {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: player not online, so no per check");

                }
                if (show) {
                    gui.setItem(SLOTS[i-1], getSkull(i, m.getValue(), playerUUID));
                    if (i++ == 10) break;
                }
            }

            player.openInventory(gui);
        }
        return true;
    }


    ItemStack getSkull(int rank, Long long1, UUID player){
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Getting the skull");
        String playerName = plugin.getPlayers().getName(player);
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: playername = " + playerName);

            plugin.getLogger().info("DEBUG: second chance = " + plugin.getPlayers().getName(player));
        }
        ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (playerName == null) return null;
        SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
        if (topTenHeads.containsKey(player)) {
            playerSkull = topTenHeads.get(player);
            meta = (SkullMeta) playerSkull.getItemMeta();
        }
        meta.setDisplayName((plugin.myLocale(player).topTenGuiHeading.replace("[name]", plugin.getGrid().getIslandName(player))).replace("[rank]", String.valueOf(rank)));
        //meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "<!> " + ChatColor.YELLOW + "Island: " + ChatColor.GOLD + ChatColor.UNDERLINE + plugin.getGrid().getIslandName(player) + ChatColor.GRAY + " (#" + rank + ")");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.YELLOW + plugin.myLocale(player).levelislandLevel + " " + long1);
        if (plugin.getPlayers().inTeam(player)) {
            final List<UUID> pMembers = plugin.getPlayers().getMembers(player);
            // Need to make this a vertical list, because some teams are very large and it'll go off the screen otherwise
            List<String> memberList = new ArrayList<>();
            for (UUID members : pMembers) {
                memberList.add(ChatColor.AQUA + plugin.getPlayers().getName(members));
            }
            lore.addAll(memberList);
        }

        meta.setLore(lore);
        playerSkull.setItemMeta(meta);
        return playerSkull;
    }

    void remove(UUID owner) {
        topTenList.remove(owner);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        if (inventory.getName() == null) {
            return;
        }
        // The player that clicked the item
        Player player = (Player) event.getWhoClicked();
        if (!inventory.getTitle().equals(plugin.myLocale(player.getUniqueId()).topTenGuiTitle)) {
            return;
        }
        event.setCancelled(true);
        player.updateInventory();
        if(event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.SKULL_ITEM) && event.getCurrentItem().hasItemMeta()){
            if (((SkullMeta)event.getCurrentItem().getItemMeta()).hasOwner()) {
                Util.runCommand(player, "is warp " + ((SkullMeta)event.getCurrentItem().getItemMeta()).getOwner());
                player.closeInventory();
            }
            return;
        }
        if (event.getSlotType().equals(SlotType.OUTSIDE)) {
            player.closeInventory();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            return;
        }
    }

    /**
     * Get a sorted descending map of the top players
     * @return the topTenList - may be more or less than ten
     */
    public Map<UUID, Long> getTopTenList() {
        return topTenList;
    }

    @Override
    public void setHead(HeadInfo headInfo) {
        topTenHeads.put(headInfo.getUuid(), headInfo.getHead());

    }
}
