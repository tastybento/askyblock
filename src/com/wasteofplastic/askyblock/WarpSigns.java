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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import com.wasteofplastic.askyblock.events.WarpCreateEvent;
import com.wasteofplastic.askyblock.events.WarpListEvent;
import com.wasteofplastic.askyblock.events.WarpRemoveEvent;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;

/**
 * Handles warping in ASkyBlock Players can add one sign
 * 
 * @author tastybento
 * 
 */
public class WarpSigns implements Listener {
    private final ASkyBlock plugin;
    // Map of all warps stored as player, warp sign Location
    private final Map<UUID, Location> warpList = new HashMap<>();
    // Where warps are stored
    private YamlConfiguration welcomeWarps;

    /**
     * @param plugin - ASkyBlock plugin object
     */
    public WarpSigns(ASkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks to see if a sign has been broken
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player player = e.getPlayer();
        if (b.getWorld().equals(ASkyBlock.getIslandWorld()) || b.getWorld().equals(ASkyBlock.getNetherWorld())) {
            if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
                Sign s = (Sign) b.getState();
                if (s != null) {
                    //plugin.getLogger().info("DEBUG: sign found at location " + s.toString());
                    if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
                        // Do a quick check to see if this sign location is in
                        //plugin.getLogger().info("DEBUG: welcome sign");
                        // the list of warp signs
                        if (warpList.containsValue(s.getLocation())) {
                            //plugin.getLogger().info("DEBUG: warp sign is in list");
                            // Welcome sign detected - check to see if it is
                            // this player's sign
                            if ((warpList.containsKey(player.getUniqueId()) && warpList.get(player.getUniqueId()).equals(s.getLocation()))) {
                                // Player removed sign
                                removeWarp(s.getLocation());
                                Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, s.getLocation(), player.getUniqueId()));
                            } else if (player.isOp()  || player.hasPermission(Settings.PERMPREFIX + "mod.removesign")) {
                                // Op or mod removed sign
                                Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpsremoved);
                                removeWarp(s.getLocation());
                                Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, s.getLocation(), player.getUniqueId()));
                            } else {
                                // Someone else's sign - not allowed
                                Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoRemove);
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Event handler for Sign Changes
     * 
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignWarpCreate(SignChangeEvent e) {
        //plugin.getLogger().info("DEBUG: SignChangeEvent called");
        String title = e.getLine(0);
        Player player = e.getPlayer();
        if (player.getWorld().equals(ASkyBlock.getIslandWorld()) || player.getWorld().equals(ASkyBlock.getNetherWorld())) {
            //plugin.getLogger().info("DEBUG: Correct world");
            if (e.getBlock().getType().equals(Material.SIGN_POST) || e.getBlock().getType().equals(Material.WALL_SIGN)) {

                //plugin.getLogger().info("DEBUG: The first line of the sign says " + title);
                // Check if someone is changing their own sign
                // This should never happen !!
                if (title.equalsIgnoreCase(plugin.myLocale().warpswelcomeLine)) {
                    //plugin.getLogger().info("DEBUG: Welcome sign detected");
                    // Welcome sign detected - check permissions
                    if (!(VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp"))) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoPerm);
                        return;
                    }
                    if(Settings.warpLevelsRestriction > 0 && !(ASkyBlockAPI.getInstance().getLongIslandLevel(player.getUniqueId()) > Settings.warpLevelsRestriction)){
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNotEnoughLevel);
                        return;
                    }
                    // Check that the player is on their island
                    if (!(plugin.getGrid().playerIsOnIsland(player, Settings.coopsCanCreateWarps))) {
                        Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoPlace);
                        e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                        return;
                    }
                    // Check if the player already has a sign
                    final Location oldSignLoc = getWarp(player.getUniqueId());
                    if (oldSignLoc == null) {
                        //plugin.getLogger().info("DEBUG: Player does not have a sign already");
                        // First time the sign has been placed or this is a new
                        // sign
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpssuccess);
                            e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
                            for (int i = 1; i<4; i++) {
                                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
                            }
                        } else {
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDuplicate);
                            e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                            for (int i = 1; i<4; i++) {
                                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
                            }
                        }
                    } else {
                        //plugin.getLogger().info("DEBUG: Player already has a Sign");
                        // A sign already exists. Check if it still there and if
                        // so,
                        // deactivate it
                        Block oldSignBlock = oldSignLoc.getBlock();
                        if (oldSignBlock.getType().equals(Material.SIGN_POST) || oldSignBlock.getType().equals(Material.WALL_SIGN)) {
                            // The block is still a sign
                            //plugin.getLogger().info("DEBUG: The block is still a sign");
                            Sign oldSign = (Sign) oldSignBlock.getState();
                            if (oldSign != null) {
                                //plugin.getLogger().info("DEBUG: Sign block is a sign");
                                if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
                                    //plugin.getLogger().info("DEBUG: Old sign had a green welcome");
                                    oldSign.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                                    oldSign.update(true, false);
                                    Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpsdeactivate);
                                    removeWarp(player.getUniqueId());
                                    Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, oldSign.getLocation(), player.getUniqueId()));
                                }
                            }
                        }
                        // Set up the warp
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            Util.sendMessage(player, ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpssuccess);
                            e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
                        } else {
                            Util.sendMessage(player, ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDuplicate);
                            e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                        }
                    }
                }
            }
        }
    }

    /**
     * Saves the warp lists to file
     */
    public void saveWarpList(boolean async) {
        if (welcomeWarps == null) {
            return;
        }
        //plugin.getLogger().info("Saving warps...");
        final HashMap<String, Object> warps = new HashMap<String, Object>();
        for (UUID p : warpList.keySet()) {
            warps.put(p.toString(), Util.getStringLocation(warpList.get(p)));
        }
        welcomeWarps.set("warps", warps);
        Util.saveYamlFile(welcomeWarps, "warps.yml", async);
    }

    /**
     * Creates the warp list if it does not exist
     */
    public void loadWarpList() {
        plugin.getLogger().info("Loading warps...");
        // warpList.clear();
        welcomeWarps = Util.loadYamlFile("warps.yml");
        if (welcomeWarps.getConfigurationSection("warps") == null) {
            welcomeWarps.createSection("warps"); // This is only used to create
            // the warp.yml file so forgive
            // this code
        }
        HashMap<String, Object> temp = (HashMap<String, Object>) welcomeWarps.getConfigurationSection("warps").getValues(true);
        for (String s : temp.keySet()) {
            try {
                final UUID playerUUID = UUID.fromString(s);
                Location l = Util.getLocationString((String) temp.get(s));
                if (l != null) {
                    warpList.put(playerUUID, l);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Problem loading warp at location " + temp.get(s) + " - removing.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores warps in the warp array
     * 
     * @param playerUUID - the player's UUID
     * @param loc
     */
    public boolean addWarp(final UUID playerUUID, final Location loc) {
        if (playerUUID == null) {
            return false;
        }
        // Do not allow warps to be in the same location
        if (warpList.containsValue(loc)) {
            return false;
        }
        // Remove the old warp if it existed
        warpList.remove(playerUUID);
        warpList.put(playerUUID, loc);
        saveWarpList(true);
        // Update warp signs
        // Run one tick later because text gets updated at the end of tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getWarpPanel().addWarp(playerUUID);
            Bukkit.getPluginManager().callEvent(new WarpCreateEvent(plugin, loc, playerUUID));
        });
        return true;
    }

    /**
     * Removes a warp when the welcome sign is destroyed. Called by
     * WarpSigns.java.
     * 
     * @param uuid
     */
    public void removeWarp(UUID uuid) {
        if (warpList.containsKey(uuid)) {
            popSign(warpList.get(uuid));
            warpList.remove(uuid);
            // Update warp signs
            // Run one tick later because text gets updated at the end of tick
            plugin.getWarpPanel().updatePanel();
        }
        saveWarpList(true);
    }

    /**
     * Changes the sign to red if it exists
     * @param loc
     */
    private void popSign(Location loc) {
        Block b = loc.getBlock();
        if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
            Sign s = (Sign) b.getState();
            if (s != null) {
                if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
                    s.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                    s.update(true, false);
                }
            }
        }
    }

    /**
     * Removes a warp at a location. Called by WarpSigns.java.
     * 
     * @param loc
     */
    public void removeWarp(Location loc) {
        //plugin.getLogger().info("Asked to remove warp at " + loc);
        popSign(loc);
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            if (en.getValue().equals(loc)) {
                // Inform player
                Player p = plugin.getServer().getPlayer(en.getKey());
                if (p != null) {
                    // Inform the player
                    Util.sendMessage(p, ChatColor.RED + plugin.myLocale(p.getUniqueId()).warpssignRemoved);
                } else {
                    plugin.getMessages().setMessage(en.getKey(), ChatColor.RED + plugin.myLocale(en.getKey()).warpssignRemoved);
                }
                it.remove();
            }
        }
        saveWarpList(true);
        plugin.getWarpPanel().updatePanel();
    }

    /**
     * Lists all the known warps
     * 
     * @return String set of warps
     */
    public Set<UUID> listWarps() {
        // Check if any of the warp locations are null
        // Check if the location of the warp still exists, if not, delete it
        warpList.entrySet().removeIf(en -> en.getValue() == null);
        return warpList.keySet();
    }

    /**
     * @return Sorted list of warps with most recent players listed first
     */
    public Collection<UUID> listSortedWarps() {
        // Bigger value of time means a more recent login
        TreeMap<Long, UUID> map = new TreeMap<>();
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            // Check if the location of the warp still exists, if not, delete it
            if (en.getValue() == null) {
                it.remove();
            } else {
                UUID uuid = en.getKey();
                // If never played, will be zero
                long lastPlayed = plugin.getServer().getOfflinePlayer(uuid).getLastPlayed();
                // This aims to avoid the chance that players logged off at exactly the same time
                if (!map.isEmpty() && map.containsKey(lastPlayed)) {
                    lastPlayed = map.firstKey() - 1;
                }
                map.put(lastPlayed, uuid);
            }
        }

        Collection<UUID> result = map.descendingMap().values();
        // Fire event
        WarpListEvent event = new WarpListEvent(plugin, result);
        plugin.getServer().getPluginManager().callEvent(event);
        // Get the result of any changes by listeners
        result = event.getWarps();
        return result;
    }
    /**
     * Provides the location of the warp for player or null if one is not found
     * 
     * @param playerUUID - the player's UUID
     *            - the warp requested
     * @return Location of warp
     */
    public Location getWarp(UUID playerUUID) {
        if (playerUUID != null && warpList.containsKey(playerUUID)) {
            if (warpList.get(playerUUID) == null) {
                warpList.remove(playerUUID);
                return null;
            }
            return warpList.get(playerUUID);
        } else {
            return null;
        }
    }

    /**
     * @param location
     * @return Name of warp owner
     */
    public String getWarpOwner(Location location) {
        for (UUID playerUUID : warpList.keySet()) {
            if (location.equals(warpList.get(playerUUID))) {
                return plugin.getPlayers().getName(playerUUID);
            }
        }
        return "";
    }

}
