package com.wasteofplastic.askyblock.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.wasteofplastic.askyblock.ASkyBlock;

public class HeadGetter {
    private Map<UUID,HeadInfo> cachedHeads = new HashMap<>();
    private Map<UUID,String> names = new ConcurrentHashMap<>();
    private ASkyBlock plugin;
    private Map<UUID,Set<Requester>> headRequesters = new HashMap<>();

    /**
     * @param plugin
     */
    public HeadGetter(ASkyBlock plugin) {
        super();
        this.plugin = plugin;
        runPlayerHeadGetter();
    }

    @SuppressWarnings("deprecation")
    private void runPlayerHeadGetter() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized(names) {
                Iterator<Entry<UUID,String>> it = names.entrySet().iterator();
                if (it.hasNext()) {
                    Entry<UUID,String> en = it.next();
                    //Bukkit.getLogger().info("DEBUG: getting " + en.getValue());
                    ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                    SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
                    meta.setOwner(en.getValue());
                    meta.setDisplayName(ChatColor.WHITE + en.getValue());
                    playerSkull.setItemMeta(meta);
                    //Bukkit.getLogger().info("DEBUG: Got it!");
                    // Save in cache
                    cachedHeads.put(en.getKey(), new HeadInfo(en.getValue(), en.getKey(), playerSkull));
                    // Tell requesters the head came in
                    if (headRequesters.containsKey(en.getKey())) {
                        for (Requester req : headRequesters.get(en.getKey())) {
                            //Bukkit.getLogger().info("DEBUG: Telling requester");
                            plugin.getServer().getScheduler().runTask(plugin, () -> req.setHead(new HeadInfo(en.getValue(), en.getKey(), playerSkull)));
                        }
                    }
                    it.remove();
                }
            }
        }, 0L, 20L);
    }

    public void getHead(UUID playerUUID, Requester requester) {
        if (playerUUID == null) {
            return;
        }
        String name = plugin.getPlayers().getName(playerUUID);
        if (name == null || name.isEmpty()) {
            return;
        }
        // Check if in cache
        if (cachedHeads.containsKey(playerUUID)) {
            requester.setHead(cachedHeads.get(playerUUID));
        } else {
            // Get the name
            //Bukkit.getLogger().info("DEBUG:Not in cache. Adding");
            headRequesters.putIfAbsent(playerUUID, new HashSet<>());
            Set<Requester> requesters = headRequesters.get(playerUUID);
            requesters.add(requester);
            headRequesters.put(playerUUID, requesters);
            names.put(playerUUID, name);
        }
    }
    
    public class HeadInfo {
        String name = "";
        UUID uuid;
        ItemStack head;
        /**
         * @param name
         * @param uuid
         * @param head
         */
        public HeadInfo(String name, UUID uuid, ItemStack head) {
            this.name = name;
            this.uuid = uuid;
            this.head = head;
        }
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
        /**
         * @return the uuid
         */
        public UUID getUuid() {
            return uuid;
        }
        /**
         * @return the head
         */
        public ItemStack getHead() {
            return head.clone();
        }

    }
}
