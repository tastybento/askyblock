package com.wasteofplastic.askyblock.panels;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.util.Util;

public class SetBiome {
    private static final int SPEED = 5000;
    private int xDone = 0;
    private int zDone = 0;
    private boolean inProgress = false;

    public SetBiome(final ASkyBlock plugin, final Island island, final Biome biomeType, CommandSender sender) {
        final World world = island.getCenter().getWorld();
        final UUID playerUUID = (sender != null && sender instanceof Player) ? ((Player)sender).getUniqueId() : null;
        final String playerName = (sender != null && sender instanceof Player) ? sender.getName() : "";
        if (sender != null) {
            plugin.getLogger().info("Starting biome change for " + playerName + " (" + biomeType.name() + ")");
        }
        // Update the settings so they can be checked later
        island.setBiome(biomeType);
        xDone = island.getMinX();
        zDone = island.getMinZ();
        new BukkitRunnable() {                
            @Override
            public void run() {
                if (inProgress) {
                    return;
                }
                inProgress = true;
                int count = 0;
                //plugin.getLogger().info("DEBUG: Restart xDone = " + xDone + " zDone = " + zDone);
                //plugin.getLogger().info("DEBUG: max x = " + (island.getMinX() + island.getIslandDistance()));
                while (xDone < (island.getMinX() + island.getIslandDistance())) {                    
                    while(zDone < (island.getMinZ() + island.getIslandDistance())) {
                        world.setBiome(xDone, zDone, biomeType);
                        //plugin.getLogger().info("DEBUG: xDone = " + xDone + " zDone = " + zDone);
                        if (count++ > SPEED) {
                            //plugin.getLogger().info("DEBUG: set " + SPEED + " blocks");
                            inProgress = false;
                            return;
                        }
                        zDone++;
                    }
                    zDone = island.getMinZ();
                    xDone++;
                }
                //plugin.getLogger().info("DEBUG: END xDone = " + xDone + " zDone = " + zDone);
                this.cancel(); 
                plugin.getLogger().info("Finished biome change for " + playerName + " (" + biomeType.name() + ")");
                if (playerUUID != null) {
                    Player p = plugin.getServer().getPlayer(playerUUID);
                    if (p != null && p.isOnline()) {
                        Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(playerUUID).biomeSet.replace("[biome]", Util.prettifyText(biomeType.name())));
                        Util.sendMessage(p, ChatColor.GREEN + plugin.myLocale(playerUUID).needRelog);
                    }
                } else {
                    plugin.getMessages().setMessage(playerUUID, ChatColor.GREEN + plugin.myLocale(playerUUID).biomeSet.replace("[biome]", biomeType.name()));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Work every second 

    }

}
