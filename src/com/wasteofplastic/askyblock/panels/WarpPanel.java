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

package com.wasteofplastic.askyblock.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Util;

public class WarpPanel implements Listener {
    private ASkyBlock plugin;
    private List<Inventory> warpPanel;
    private static final int PANELSIZE = 45; // Must be a multiple of 9
    // The list of all players who have warps and their corresponding inventory icon
    // A stack of zero amount will mean they are not active
    private Map<UUID, ItemStack> cachedWarps;
    private final static boolean DEBUG = false;
    private static final int NOTIFICATION_NUM = 40;
    private Map<UUID,String> names = new HashMap<>();
    private Set<UUID> textQueue = new HashSet<>();

    /**
     * @param plugin - ASkyBlock plugin object
     */
    public WarpPanel(ASkyBlock plugin) {
        this.plugin = plugin;
        warpPanel = new ArrayList<Inventory>();
        cachedWarps = new HashMap<UUID,ItemStack>();
        // Start the Skull getter
        runPlayerHeadGetter();       
        //plugin.getLogger().info("DEBUG: loading the warp panel of size " + plugin.getWarpSignsListener().listSortedWarps().size());
        // Load the cache
        for (UUID playerUUID : plugin.getWarpSignsListener().listSortedWarps()) {
            String playerName = plugin.getPlayers().getName(playerUUID);
            if (playerName == null) {
                if (DEBUG)
                    plugin.getLogger().warning("Warp for Player: UUID " + playerUUID.toString() + " is unknown on this server, skipping...");
            } else {
                // Get the skull
                addName(playerUUID, playerName);
            }
        }
        updatePanel();
    }

    @SuppressWarnings("deprecation")
    private void runPlayerHeadGetter() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized(names) {
                if (names.size() > 0 && names.size() % NOTIFICATION_NUM == 0) {
                    plugin.getLogger().info("Loading player heads (Rate limit is 1 head/s): " + names.size() + " to go...");
                    // Run a panel update every NOTIFICATION_NUM heads
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        synchronized(warpPanel) {
                            updatePanel();
                        }
                    }, 10L);
                }
                Iterator<Entry<UUID,String>> it = names.entrySet().iterator();
                if (it.hasNext()) {
                    Entry<UUID,String> en = it.next();

                    ItemStack playerSkull = cachedWarps.get(en.getKey());
                    SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                    meta.setOwner(en.getValue());
                    meta.setDisplayName(ChatColor.WHITE + en.getValue());
                    playerSkull.setItemMeta(meta);
                    // Update
                    cachedWarps.put(en.getKey(), playerSkull);
                    it.remove();
                    // If the names queue drops to zero, then run an panel update
                    if (names.size() == 0) {
                        plugin.getLogger().info("Player heads loaded.");
                        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                            synchronized(warpPanel) {
                                updatePanel();
                            }
                        }, 10L);
                    }
                }
            }
        }, 0L, 20L);

        // Also run the text updateding task
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {

            Iterator<UUID> it = textQueue.iterator();
            if (it.hasNext()) {
                UUID next = it.next();
                if (cachedWarps.containsKey(next)) {
                    updateText(cachedWarps.get(next), next);
                }
                it.remove();
                if (textQueue.size() == 0) {
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        synchronized(warpPanel) {
                            updatePanel();
                        }
                    }, 10L);
                }
            }
        }, 3L, 5L);

    }

    /**
     * Adds a name to the list of player head requests
     * @param playerUUID
     * @param name
     */
    public ItemStack addName(UUID playerUUID, String playerName) {
        if (!cachedWarps.containsKey(playerUUID)) {
            ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + playerName);
            playerSkull.setItemMeta(meta);
            cachedWarps.put(playerUUID, playerSkull);
            names.put(playerUUID, playerName);
            textQueue.add(playerUUID);
            return playerSkull;
        } else {
            return cachedWarps.get(playerUUID);
        }
    }


    /**
     * Only change the text of the warp
     * @param playerUUID - the player's UUID
     */
    public void updateWarp(UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: update Warp");

        if (cachedWarps.containsKey(playerUUID)) {
            // Get the item
            ItemStack playerSkull = cachedWarps.get(playerUUID);
            playerSkull = updateText(playerSkull, playerUUID);
            updatePanel();
        } else {
            plugin.getLogger().warning("Warps: update requested, but player unknown " + playerUUID.toString()); 
        }
    }

    /**
     * Update the text on all the warp icons.
     */
    public void updateAllWarpText() {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: update all Warps");
        for (UUID playerUUID : cachedWarps.keySet()) {
            // Get the item
            ItemStack playerSkull = cachedWarps.get(playerUUID);
            playerSkull = updateText(playerSkull, playerUUID);
        }
        updatePanel();
    }
    /**
     * Adds a new warp to the cache. Does NOT update the panels
     * @param playerUUID - the player's UUID
     */
    public void addWarp(UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Adding warp");
        // Check cached warps
        if (cachedWarps.containsKey(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Found in cache");
            // Get the item
            ItemStack playerSkull = cachedWarps.get(playerUUID);
            playerSkull = updateText(playerSkull, playerUUID);
            plugin.getWarpPanel().updatePanel();
            return;
        }
        //plugin.getLogger().info("DEBUG: New skull");
        String playerName = plugin.getPlayers().getName(playerUUID);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: name of warp = " + playerName);
        if (playerName == null) {
            if (DEBUG)
                plugin.getLogger().warning("Warp for Player: UUID " + playerUUID.toString() + " is unknown on this server, skipping...");
            return;
            //playerName = playerUUID.toString().substring(0, 10);
        }
        // Get the skull
        addName(playerUUID, playerName);
        updatePanel();
    }

    /**
     * Updates the meta text on the skull by looking at the warp sign
     * This MUST be run 1 TICK AFTER the sign has been created otherwise the sign is blank
     * @param playerSkull
     * @param playerUUID - the player's UUID
     * @return updated skull item stack
     */
    private ItemStack updateText(ItemStack playerSkull, final UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Updating text on item");
        ItemMeta meta = playerSkull.getItemMeta();
        //get the sign info
        Location signLocation = plugin.getWarpSignsListener().getWarp(playerUUID);
        if (signLocation == null) {
            plugin.getWarpSignsListener().removeWarp(playerUUID);
            return playerSkull;
        }            
        //plugin.getLogger().info("DEBUG: block type = " + signLocation.getBlock().getType());
        // Get the sign info if it exists
        if (signLocation.getBlock().getType().equals(Material.SIGN_POST) || signLocation.getBlock().getType().equals(Material.WALL_SIGN)) {
            Sign sign = (Sign)signLocation.getBlock().getState();
            List<String> lines = new ArrayList<String>(Arrays.asList(sign.getLines()));
            // Check for PVP and add warning
            Island island = plugin.getGrid().getIsland(playerUUID);
            if (island != null) {
                if ((signLocation.getWorld().equals(ASkyBlock.getIslandWorld()) && island.getIgsFlag(SettingsFlag.PVP))
                        || (signLocation.getWorld().equals(ASkyBlock.getNetherWorld()) && island.getIgsFlag(SettingsFlag.NETHER_PVP))) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: pvp warning added");
                    lines.add(ChatColor.RED + plugin.myLocale().igs.get(SettingsFlag.PVP));
                }
            }
            meta.setLore(lines);
            if (DEBUG)
                plugin.getLogger().info("DEBUG: lines = " + lines);
        } else {
            // No sign there - remove the warp
            plugin.getWarpSignsListener().removeWarp(playerUUID);
        }
        playerSkull.setItemMeta(meta);
        return playerSkull;
    }

    /**
     * Creates the inventory panels from the warp list and adds nav buttons
     */
    public void updatePanel() {
        // Clear the inventory panels
        warpPanel.clear();
        Collection<UUID> activeWarps = plugin.getWarpSignsListener().listSortedWarps();
        // Create the warp panels
        if (DEBUG)
            plugin.getLogger().info("DEBUG: warps size = " + activeWarps.size());
        int size = activeWarps.size();
        int panelNumber = size / (PANELSIZE-2);
        int remainder = (size % (PANELSIZE-2)) + 8 + 2;
        remainder -= (remainder % 9);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: panel number = " + panelNumber + " remainder = " + remainder);
        int i = 0;
        // TODO: Make panel title a string
        for (i = 0; i < panelNumber; i++) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: created panel " + (i+1));
            warpPanel.add(Bukkit.createInventory(null, PANELSIZE, plugin.myLocale().warpsTitle + " #" + (i+1)));
        }
        // Make the last panel
        if (DEBUG)
            plugin.getLogger().info("DEBUG: created panel " + (i+1));
        warpPanel.add(Bukkit.createInventory(null, remainder, plugin.myLocale().warpsTitle + " #" + (i+1)));
        panelNumber = 0;
        int slot = 0;
        // Run through all the warps and add them to the inventories with anv buttons
        for (UUID playerUUID: activeWarps) {
            ItemStack icon = cachedWarps.get(playerUUID);
            if (icon != null) {
                warpPanel.get(panelNumber).setItem(slot++, icon);

                // Check if the panel is full
                if (slot == PANELSIZE-2) {
                    // Add navigation buttons
                    if (panelNumber > 0) {
                        warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.SIGN,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
                    }
                    warpPanel.get(panelNumber).setItem(slot, new CPItem(Material.SIGN,plugin.myLocale().warpsNext,"warps " + (panelNumber+1),"").getItem());
                    // Move onto the next panel
                    panelNumber++;
                    slot = 0;
                }
            }
        }
        if (remainder != 0 && panelNumber > 0) {
            warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.SIGN,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
        }
    }

    public Inventory getWarpPanel(int panelNumber) {
        //makePanel();
        if (panelNumber < 0) {
            panelNumber = 0;
        } else if (panelNumber > warpPanel.size()-1) {
            panelNumber = warpPanel.size()-1;
        }
        return warpPanel.get(panelNumber);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        if (inventory.getName() == null) {
            return;
        }
        // The player that clicked the item
        final Player player = (Player) event.getWhoClicked();
        String title = inventory.getTitle();
        if (!inventory.getTitle().startsWith(plugin.myLocale().warpsTitle + " #")) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlotType().equals(SlotType.OUTSIDE)) {
            player.closeInventory();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            player.updateInventory();
            return;
        }
        ItemStack clicked = event.getCurrentItem(); // The item that was clicked
        if (DEBUG)
            plugin.getLogger().info("DEBUG: inventory size = " + inventory.getSize());
        if (DEBUG)
            plugin.getLogger().info("DEBUG: clicked = " + clicked);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: rawslot = " + event.getRawSlot());
        if (event.getRawSlot() >= event.getInventory().getSize() || clicked.getType() == Material.AIR) {
            return;
        }
        int panelNumber = 0;
        try {
            panelNumber = Integer.valueOf(title.substring(title.indexOf('#')+ 1));
        } catch (Exception e) {
            panelNumber = 0;
        }
        if (clicked.getItemMeta().hasDisplayName()) {
            String command = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (DEBUG)
                plugin.getLogger().info("DEBUG: command = " + command);
            if (command != null) {
                if (command.equalsIgnoreCase(ChatColor.stripColor(plugin.myLocale().warpsNext))) {
                    player.closeInventory();
                    Util.runCommand(player, Settings.ISLANDCOMMAND + " warps " + (panelNumber+1));
                } else if (command.equalsIgnoreCase(ChatColor.stripColor(plugin.myLocale().warpsPrevious))) {
                    player.closeInventory();
                    Util.runCommand(player, Settings.ISLANDCOMMAND + " warps " + (panelNumber-1));
                } else {
                    player.closeInventory();
                    Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpswarpToPlayersSign.replace("<player>", command));
                    Util.runCommand(player, Settings.ISLANDCOMMAND + " warp " + command);
                }
            }
        }
    }
}
