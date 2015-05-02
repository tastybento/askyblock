/**
 * ****************************************************************************
 * This file is part of ASkyBlock.
 * <p/>
 * ASkyBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * ASkyBlock is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 */
package com.wasteofplastic.askyblock;

import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;
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

import java.util.*;

/**
 * Handles warping in ASkyBlock Players can add one sign
 *
 * @author tastybento
 *
 */
public class WarpSigns implements Listener {
    private final static ASkyBlock plugin = ASkyBlock.getPlugin();
    // Map of all warps stored as player, warp sign Location
    private static HashMap<UUID, Object> warpList = new HashMap<UUID, Object>();
    // Where warps are stored
    private static YamlConfiguration welcomeWarps;

    /*
     * @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
     * public void onSignPopped(BlockPhysicsEvent e) {
     * Block b = e.getBlock();
     * if (!(b.getWorld()).getName().equals(Settings.worldName)) {
     * // Wrong world
     * return;
     * }
     * if (!plugin.checkWarp(b.getLocation())) {
     * return;
     * }
     * // Block b = e.getBlock().getRelative(BlockFace.UP);
     * if (!(b.getState() instanceof Sign)) {
     * return;
     * }
     * if (!plugin.checkWarp(b.getLocation())) {
     * return;
     * }
     * //plugin.plugin.getLogger().info("DEBUG: Known warp location! " +
     * b.getLocation().toString());
     * // This is the sign block - check to see if it is still a sign
     * //if (b.getType().equals(Material.SIGN_POST)) {
     * // Check to see if it is still attached
     * MaterialData m = b.getState().getData();
     * BlockFace face = BlockFace.DOWN; // Most of the time it's going
     * // to be down
     * if (m instanceof Attachable) {
     * face = ((Attachable) m).getAttachedFace();
     * }
     * if (b.getRelative(face).getType().isSolid()) {
     * //plugin.plugin.getLogger().info("Attached to some solid block");
     * } else {
     * plugin.removeWarp(b.getLocation());
     * //plugin.plugin.getLogger().info("Warp removed");
     * }
     * //}
     * }
     */

    /**
     * Saves the warp lists to file
     */
    public static void saveWarpList() {
        if (warpList == null || welcomeWarps == null) {
            return;
        }
        plugin.getLogger().info("Saving warps...");
        final HashMap<String, Object> warps = new HashMap<String, Object>();
        for (UUID p : warpList.keySet()) {
            warps.put(p.toString(), warpList.get(p));
        }
        welcomeWarps.set("warps", warps);
        Util.saveYamlFile(welcomeWarps, "warps.yml");
    }

    /**
     * Creates the warp list if it does not exist
     */
    public static void loadWarpList() {
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
                UUID playerUUID = UUID.fromString(s);
                Location l = Util.getLocationString((String) temp.get(s));
                //plugin.getLogger().info("DEBUG: Loading warp at " + l);
                Block b = l.getBlock();
                // Check that a warp sign is still there
                if (b.getType().equals(Material.SIGN_POST)) {
                    warpList.put(playerUUID, temp.get(s));
                } else {
                    plugin.getLogger().warning("Warp at location " + (String) temp.get(s) + " has no sign - removing.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Problem loading warp at location " + (String) temp.get(s) + " - removing.");
            }
        }
    }

    /**
     * Stores warps in the warp array
     *
     * @param player
     * @param loc
     */
    public static boolean addWarp(UUID player, Location loc) {
        final String locS = Util.getStringLocation(loc);
        // Do not allow warps to be in the same location
        if (warpList.containsValue(locS)) {
            return false;
        }
        // Remove the old warp if it existed
        if (warpList.containsKey(player)) {
            warpList.remove(player);
        }
        warpList.put(player, locS);
        saveWarpList();
        return true;
    }

    /**
     * Removes a warp when the welcome sign is destroyed. Called by
     * WarpSigns.java.
     *
     * @param uuid
     */
    public static void removeWarp(UUID uuid) {
        if (warpList.containsKey(uuid)) {
            popSign(Util.getLocationString((String) warpList.get(uuid)));
            warpList.remove(uuid);
        }
        saveWarpList();
    }

    private static void popSign(Location loc) {
        Block b = loc.getBlock();
        if (b.getType().equals(Material.SIGN_POST)) {
            Sign s = (Sign) b.getState();
            if (s != null) {
                if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
                    s.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                    s.update();
                }
            }
        }
    }

    /**
     * Removes a warp at a location. Called by WarpSigns.java.
     *
     * @param loc
     */
    public static void removeWarp(Location loc) {
        final String locS = Util.getStringLocation(loc);
        plugin.getLogger().info("Asked to remove warp at " + locS);
        popSign(loc);
        if (warpList.containsValue(locS)) {
            // Step through every key (sigh)
            List<UUID> playerList = new ArrayList<UUID>();
            for (UUID player : warpList.keySet()) {
                if (locS.equals(warpList.get(player))) {
                    playerList.add(player);
                }
            }
            for (UUID rp : playerList) {
                warpList.remove(rp);
                final Player p = plugin.getServer().getPlayer(rp);
                if (p != null) {
                    // Inform the player
                    p.sendMessage(ChatColor.RED + plugin.myLocale(p.getUniqueId()).warpssignRemoved);
                }
                plugin.getLogger().warning(rp.toString() + "'s welcome sign at " + loc.toString() + " was removed by something.");
            }
        } else {
            plugin.getLogger().info("Not in the list which is:");
            for (UUID player : warpList.keySet()) {
                plugin.getLogger().info(player.toString() + "," + warpList.get(player));
            }

        }
        saveWarpList();
    }

    /**
     * Lists all the known warps
     *
     * @return String set of warps
     */
    public static Set<UUID> listWarps() {
        // plugin.getLogger().info("DEBUG Warp list count = " +
        // warpList.size());
        return warpList.keySet();
    }

    /**
     * Provides the location of the warp for player
     *
     * @param player
     *            - the warp requested
     * @return Location of warp
     */
    public static Location getWarp(UUID player) {
        if (warpList.containsKey(player)) {
            return Util.getLocationString((String) warpList.get(player));
        } else {
            return null;
        }
    }

    /**
     * @param location
     * @return Name of warp owner
     */
    public static String getWarpOwner(Location location) {
        for (UUID playerUUID : warpList.keySet()) {
            Location l = Util.getLocationString((String) warpList.get(playerUUID));
            if (l.equals(location)) {
                return plugin.getPlayers().getName(playerUUID);
            }
        }
        return "";
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player player = e.getPlayer();
        if (b.getWorld().equals(ASkyBlock.getIslandWorld())) {
            if (b.getType().equals(Material.SIGN_POST)) {
                Sign s = (Sign) b.getState();
                if (s != null) {
                    if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
                        // Do a quick check to see if this sign location is in
                        // the list of warp signs
                        if (checkWarp(s.getLocation())) {
                            // Welcome sign detected - check to see if it is
                            // this player's sign
                            final Location playerSignLoc = getWarp(player.getUniqueId());
                            if (playerSignLoc != null) {
                                if (playerSignLoc.equals(s.getLocation())) {
                                    // This is the player's sign, so allow it to
                                    // be destroyed
                                    player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpssignRemoved);
                                    removeWarp(player.getUniqueId());
                                } else {
                                    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoRemove);
                                    e.setCancelled(true);
                                }
                            } else {
                                // Someone else's sign because this player has
                                // none registered
                                player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoRemove);
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
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignWarpCreate(SignChangeEvent e) {
        //plugin.getLogger().info("DEBUG: SignChangeEvent called");
        String title = e.getLine(0);
        Player player = e.getPlayer();
        if (player.getWorld().equals(ASkyBlock.getIslandWorld())) {
            //plugin.getLogger().info("DEBUG: Correct world");
            if (e.getBlock().getType().equals(Material.SIGN_POST)) {

                //plugin.getLogger().info("DEBUG: The first line of the sign says " + title);
                // Check if someone is changing their own sign
                // This should never happen !!
                if (title.equalsIgnoreCase(plugin.myLocale().warpswelcomeLine)) {
                    //plugin.getLogger().info("DEBUG: Welcome sign detected");
                    // Welcome sign detected - check permissions
                    if (!(VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp"))) {
                        player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoPerm);
                        return;
                    }
                    // Check that the player is on their island
                    if (!(plugin.getGrid().playerIsOnIsland(player))) {
                        player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorNoPlace);
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
                            player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpssuccess);
                            e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
                        } else {
                            player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDuplicate);
                            e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                        }
                    } else {
                        //plugin.getLogger().info("DEBUG: Player already has a Sign");
                        // A sign already exists. Check if it still there and if
                        // so,
                        // deactivate it
                        Block oldSignBlock = oldSignLoc.getBlock();
                        if (oldSignBlock.getType().equals(Material.SIGN_POST)) {
                            // The block is still a sign
                            //plugin.getLogger().info("DEBUG: The block is still a sign");
                            Sign oldSign = (Sign) oldSignBlock.getState();
                            if (oldSign != null) {
                                //plugin.getLogger().info("DEBUG: Sign block is a sign");
                                if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
                                    //plugin.getLogger().info("DEBUG: Old sign had a green welcome");
                                    oldSign.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                                    oldSign.update();
                                    player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpsdeactivate);
                                    removeWarp(player.getUniqueId());
                                }
                            }
                        }
                        // Set up the warp
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            player.sendMessage(ChatColor.GREEN + plugin.myLocale(player.getUniqueId()).warpssuccess);
                            e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
                        } else {
                            player.sendMessage(ChatColor.RED + plugin.myLocale(player.getUniqueId()).warpserrorDuplicate);
                            e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns true if the location supplied is a warp location
     *
     * @param loc
     * @return true if this location has a warp sign, false if not
     */
    public boolean checkWarp(Location loc) {
        final String locS = Util.getStringLocation(loc);
        if (warpList.containsValue(locS)) {
            return true;
        }
        return false;
    }

}