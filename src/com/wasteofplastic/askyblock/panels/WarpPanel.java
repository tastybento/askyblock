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
import java.util.List;
import java.util.Map;
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
import com.wasteofplastic.askyblock.util.HeadGetter.HeadInfo;
import com.wasteofplastic.askyblock.util.Requester;
import com.wasteofplastic.askyblock.util.Util;

public class WarpPanel implements Listener, Requester {
    private ASkyBlock plugin;
    private List<Inventory> warpPanel;
    private static final int PANELSIZE = 45; // Must be a multiple of 9
    // The list of all players who have warps and their corresponding inventory icon
    // A stack of zero amount will mean they are not active
    private Map<UUID, ItemStack> cachedWarps;
    private final static boolean DEBUG = false;
    /**
     * @param plugin - ASkyBlock plugin object
     */
    public WarpPanel(ASkyBlock plugin) {
        this.plugin = plugin;
        warpPanel = new ArrayList<Inventory>();
        cachedWarps = new HashMap<UUID,ItemStack>();
        // Load the cache
        for (UUID playerUUID : plugin.getWarpSignsListener().listSortedWarps()) {
            String playerName = plugin.getPlayers().getName(playerUUID);
            if (playerName == null) {
                if (DEBUG)
                    plugin.getLogger().warning("Warp for Player: UUID " + playerUUID.toString() + " is unknown on this server, skipping...");
            } else {
                if (Settings.warpHeads) {
                    ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                    SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                    meta.setDisplayName(playerName);
                    playerSkull.setItemMeta(meta);
                    cachedWarps.put(playerUUID, playerSkull);
                    // Get the player head async
                    addName(playerUUID, playerName);
                } else {
                    ItemStack playerSign = new ItemStack(Material.SIGN);
                    ItemMeta meta = playerSign.getItemMeta();
                    meta.setDisplayName(playerName);
                    playerSign.setItemMeta(meta);
                    updateText(playerSign, playerUUID);
                    cachedWarps.put(playerUUID, playerSign);
                }
            }
        }
        updatePanel();
    }

    /**
     * Adds a name to the list of player head requests
     * @param playerUUID
     * @param name
     */
    public void addName(UUID playerUUID, String playerName) {
        plugin.getHeadGetter().getHead(playerUUID, this);
    }


    /**
     * Only change the text of the warp
     * @param playerUUID - the player's UUID
     */
    public void updateWarp(UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: update Warp");
        plugin.getHeadGetter().getHead(playerUUID, this);
    }

    /**
     * Update the text on all the warp icons.
     */
    public void updateAllWarpText() {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: update all Warps");
        for (UUID playerUUID : cachedWarps.keySet()) {
            plugin.getHeadGetter().getHead(playerUUID, this);
        }
        updatePanel();
    }

    /**
     * Adds a new warp
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
            updatePanel();
            return;
        }
        //plugin.getLogger().info("DEBUG: New skull");
        String playerName = plugin.getPlayers().getName(playerUUID);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: name of warp = " + playerName);
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        if (Settings.warpHeads) {
            ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
            meta.setDisplayName(playerName);
            playerSkull.setItemMeta(meta);
            cachedWarps.put(playerUUID, playerSkull);
            updatePanel();
            // Get the player head async
            addName(playerUUID, playerName);
        } else {
            ItemStack playerSign = new ItemStack(Material.SIGN);
            ItemMeta meta = playerSign.getItemMeta();
            meta.setDisplayName(playerName);
            playerSign.setItemMeta(meta);
            updateText(playerSign,playerUUID);
            cachedWarps.put(playerUUID, playerSign);
            updatePanel();
        }
    }

    /**
     * Updates the meta text on the warp sign icon by looking at the warp sign
     * This MUST be run 1 TICK AFTER the sign has been created otherwise the sign is blank
     * @param playerSign
     * @param playerUUID - the player's UUID
     * @return updated skull item stack
     */
    private ItemStack updateText(ItemStack playerSign, final UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Updating text on item");
        ItemMeta meta = playerSign.getItemMeta();
        //get the sign info
        Location signLocation = plugin.getWarpSignsListener().getWarp(playerUUID);
        if (signLocation == null) {
            plugin.getWarpSignsListener().removeWarp(playerUUID);
            return playerSign;
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
        playerSign.setItemMeta(meta);
        return playerSign;
    }

    /**
     * Creates the inventory panels from the warp list and adds nav buttons
     */
    public void updatePanel() {
        List<Inventory> updated = new ArrayList<>();
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
            updated.add(Bukkit.createInventory(null, PANELSIZE, plugin.myLocale().warpsTitle + " #" + (i+1)));
        }
        // Make the last panel
        if (DEBUG)
            plugin.getLogger().info("DEBUG: created panel " + (i+1));
        updated.add(Bukkit.createInventory(null, remainder, plugin.myLocale().warpsTitle + " #" + (i+1)));
        warpPanel = new ArrayList<>(updated);
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
                        warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.PAPER,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
                    }
                    warpPanel.get(panelNumber).setItem(slot, new CPItem(Material.PAPER,plugin.myLocale().warpsNext,"warps " + (panelNumber+1),"").getItem());
                    // Move onto the next panel
                    panelNumber++;
                    slot = 0;
                }
            }
        }
        if (remainder != 0 && panelNumber > 0) {
            warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.PAPER,plugin.myLocale().warpsPrevious,"warps " + (panelNumber-1),"").getItem());
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

    @Override
    public void setHead(HeadInfo headInfo) {
        ItemStack head = headInfo.getHead();
        head = updateText(head, headInfo.getUuid());
        cachedWarps.put(headInfo.getUuid(), headInfo.getHead());
        for (Inventory panel: warpPanel) {
            for (int i = 0; i < panel.getSize(); i++) {
                ItemStack it = panel.getItem(i);
                if (it != null && it.getType().equals(Material.SKULL_ITEM)) {
                    ItemMeta meta = it.getItemMeta();
                    if (headInfo.getName().equals(meta.getDisplayName())) {
                        panel.setItem(i, head);
                        return;
                    }
                }
            }
        }   
    }
}
