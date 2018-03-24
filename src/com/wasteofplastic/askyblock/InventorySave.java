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

import org.bukkit.entity.Player;

/**
 * Stashes inventories when required for a player
 * 
 * @author tastybento
 * 
 */
public class InventorySave {
    private static InventorySave instance = new InventorySave(ASkyBlock.getPlugin());
    private HashMap<UUID, InventoryStore> inventories;

    /**
     * Saves the inventory of a player
     * @param plugin - ASkyBlock plugin object - ASkyBlock plugin
     */
    public InventorySave(ASkyBlock plugin) {
        inventories = new HashMap<UUID, InventoryStore>();
    }

    /** Save player's inventory
     * @param player player object
     */
    public void savePlayerInventory(Player player) {
        //plugin.getLogger().info("DEBUG: Saving inventory");
        // Save the player's armor and things
        inventories.put(player.getUniqueId(),new InventoryStore(player.getInventory().getContents(), player.getInventory().getArmorContents()));
    }

    /**
     * Clears any saved inventory
     * @param player player object
     */
    public void clearSavedInventory(Player player) {
        //plugin.getLogger().info("DEBUG: Clearing inventory");
        inventories.remove(player.getUniqueId());
    }
    /**
     * Load the player's inventory
     * 
     * @param player playe object
     */
    public void loadPlayerInventory(Player player) {
        //plugin.getLogger().info("DEBUG: Loading inventory");
        // Get the info for this player
        if (inventories.containsKey(player.getUniqueId())) {
            InventoryStore inv = inventories.get(player.getUniqueId());
            //plugin.getLogger().info("DEBUG: player is known");
            player.getInventory().setContents(inv.getInventory());
            player.getInventory().setArmorContents(inv.getArmor());
            inventories.remove(player.getUniqueId());
            return;
        }
    }

    public static InventorySave getInstance() {
        return instance;
    }

}
