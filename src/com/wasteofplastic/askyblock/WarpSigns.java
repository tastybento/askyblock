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

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Handles warping in ASkyBlock Players can add one sign
 * 
 * @author ben
 * 
 */
public class WarpSigns implements Listener {
    private final ASkyBlock plugin;

    public WarpSigns(ASkyBlock aSkyBlock) {
	plugin = aSkyBlock;
    }
    /*
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignPopped(BlockPhysicsEvent e) {
	Block b = e.getBlock();
	if (!(b.getWorld()).getName().equals(Settings.worldName)) {
	    // Wrong world
	    return;
	}
	if (!plugin.checkWarp(b.getLocation())) {
	    return;
	}
	// Block b = e.getBlock().getRelative(BlockFace.UP);	
	if (!(b.getState() instanceof Sign)) {
	    return;
	}
	if (!plugin.checkWarp(b.getLocation())) {
	    return;
	}
	//plugin.getLogger().info("DEBUG: Known warp location! " + b.getLocation().toString());
	// This is the sign block - check to see if it is still a sign
	//if (b.getType().equals(Material.SIGN_POST)) {
	// Check to see if it is still attached
	MaterialData m = b.getState().getData();
	BlockFace face = BlockFace.DOWN; // Most of the time it's going
	// to be down
	if (m instanceof Attachable) {
	    face = ((Attachable) m).getAttachedFace();
	}
	if (b.getRelative(face).getType().isSolid()) {
	    //plugin.getLogger().info("Attached to some solid block");
	} else {
	    plugin.removeWarp(b.getLocation());
	    //plugin.getLogger().info("Warp removed");
	}
	//}

    }*/

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignBreak(BlockBreakEvent e) {
	Block b = e.getBlock();
	Player player = e.getPlayer();
	if (b.getWorld().getName().equals(Settings.worldName)) {
	    if (b.getType().equals(Material.SIGN_POST)) {
		Sign s = (Sign) b.getState();
		if (s != null) {
		    if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + Locale.warpswelcomeLine)) {
			// Do a quick check to see if this sign location is in
			// the list of warp signs
			if (plugin.checkWarp(s.getLocation())) {
			    // Welcome sign detected - check to see if it is
			    // this player's sign
			    final Location playerSignLoc = plugin.getWarp(player.getUniqueId());
			    if (playerSignLoc != null) {
				if (playerSignLoc.equals(s.getLocation())) {
				    // This is the player's sign, so allow it to
				    // be destroyed
				    player.sendMessage(ChatColor.GREEN + Locale.warpssignRemoved);
				    plugin.removeWarp(player.getUniqueId());
				} else {
				    player.sendMessage(ChatColor.RED + Locale.warpserrorNoRemove);
				    e.setCancelled(true);
				}
			    } else {
				// Someone else's sign because this player has
				// none registered
				player.sendMessage(ChatColor.RED + Locale.warpserrorNoRemove);
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
	//plugin.getLogger().info("SignChangeEvent called");
	String title = e.getLine(0);
	Player player = e.getPlayer();
	if (player.getWorld().getName().equals(Settings.worldName)) {
	    if (e.getBlock().getType().equals(Material.SIGN_POST)) {
		//plugin.getLogger().info("Correct world");
		//plugin.getLogger().info("The first line of the sign says " + title);
		// Check if someone is changing their own sign
		// This should never happen !!
		if (title.equalsIgnoreCase(Locale.warpswelcomeLine)) {
		    //plugin.getLogger().info("Welcome sign detected");
		    // Welcome sign detected - check permissions
		    if (!(VaultHelper.checkPerm(player, Settings.PERMPREFIX + "island.addwarp"))) {
			player.sendMessage(ChatColor.RED + Locale.warpserrorNoPerm);
			return;
		    }
		    // Check that the player is on their island
		    if (!(plugin.playerIsOnIsland(player))) {
			player.sendMessage(ChatColor.RED + Locale.warpserrorNoPlace);
			e.setLine(0, ChatColor.RED + Locale.warpswelcomeLine);
			return;
		    }
		    // Check if the player already has a sign
		    final Location oldSignLoc = plugin.getWarp(player.getUniqueId());
		    if (oldSignLoc == null) {
			//plugin.getLogger().info("Player does not have a sign already");
			// First time the sign has been placed or this is a new sign
			if (plugin.addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
			    player.sendMessage(ChatColor.GREEN + Locale.warpssuccess);
			    e.setLine(0, ChatColor.GREEN + Locale.warpswelcomeLine);
			} else {
			    player.sendMessage(ChatColor.RED + Locale.warpserrorDuplicate);
			    e.setLine(0, ChatColor.RED + Locale.warpswelcomeLine);
			}
		    } else {
			//plugin.getLogger().info("Player already has a Sign");
			// A sign already exists. Check if it still there and if so,
			// deactivate it
			Block oldSignBlock = oldSignLoc.getBlock();
			if (oldSignBlock.getType().equals(Material.SIGN_POST)) {
			    // The block is still a sign
			    //plugin.getLogger().info("The block is still a sign");
			    Sign oldSign = (Sign) oldSignBlock.getState();
			    if (oldSign != null) {
				//plugin.getLogger().info("Sign block is a sign");
				if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + Locale.warpswelcomeLine)) {
				    //plugin.getLogger().info("Old sign had a green welcome");
				    oldSign.setLine(0, ChatColor.RED + Locale.warpswelcomeLine);
				    oldSign.update();
				    player.sendMessage(ChatColor.RED + Locale.warpsdeactivate);
				    plugin.removeWarp(player.getUniqueId());
				}
			    }
			}
			// Set up the warp
			if (plugin.addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
			    player.sendMessage(ChatColor.GREEN + Locale.warpssuccess);
			    e.setLine(0, ChatColor.GREEN + Locale.warpswelcomeLine);
			} else {
			    player.sendMessage(ChatColor.RED + Locale.warpserrorDuplicate);
			    e.setLine(0, ChatColor.RED + Locale.warpswelcomeLine);
			}
		    }
		}
	    }
	}
    }

}
