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
package com.wasteofplastic.askyblock.util;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.wasteofplastic.askyblock.ASkyBlock;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

/**
 * Helper class for Vault Economy and Permissions
 */
public class VaultHelper {
    public static Economy econ = null;
    public static Permission permission = null;

    /**
     * Sets up the economy instance
     * 
     * @return true if successful
     */
    public static boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = ASkyBlock.getPlugin().getServer().getServicesManager()
                .getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        }
        return econ != null;
    }

    /**
     * Sets up the permissions instance
     * 
     * @return true if successful
     */
    public static boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = ASkyBlock.getPlugin().getServer().getServicesManager()
                .getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }

    /**
     * Checks permission of player in the world the player is in now
     * 
     * @param player
     * @param perm
     * @return true if the player has the perm
     */
    public static boolean checkPerm(final Player player, final String perm) {
        return permission.has(player, perm);
    }

    /**
     * Checks permission of player in world 
     * 
     * @param player
     * @param perm
     * @param world
     * @return
     */
    public static boolean checkPerm(final Player player, final String perm, final World world) {
        return permission.playerHas(world.getName(), player, perm);
    }

    /**
     * Adds permission to player
     * 
     * @param player
     * @param perm
     */
    public static void addPerm(final Player player, final String perm) {
        permission.playerAdd(player, perm);
    }
    
    /**
     * Add permission to player in world
     * @param player
     * @param perm
     * @param world
     */
    public static void addPerm(final Player player, final String perm, final World world) {
        permission.playerAdd(world.getName(), player, perm);
    }

    /**
     * Removes a player's permission
     * 
     * @param player
     * @param perm
     */
    public static void removePerm(final Player player, final String perm) {
        permission.playerRemove(player, perm);
    }

    /**
     * Removes a player's permission in world
     * 
     * @param player
     * @param perm
     * @param world
     */
    public static void removePerm(final Player player, final String perm, World world) {
        permission.playerRemove(world.getName(), player, perm);
    }
}