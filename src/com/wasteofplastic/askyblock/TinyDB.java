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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tiny database for a hashmap that is not used very often, but could be very big so I
 * don't want it in memory. 
 * @author tastybento
 *
 */
public class TinyDB {
    private final ASkyBlock plugin;
    private final Map<String,UUID> treeMap;
    private boolean dbReady;
    private boolean savingFlag;
    /**
     * Opens the database
     * @param plugin - ASkyBlock plugin object
     */
    public TinyDB(ASkyBlock plugin) {       
        this.plugin = plugin;
        this.treeMap = new ConcurrentHashMap<>();
        File database = new File(plugin.getDataFolder(), "name-uuid.txt");
        if (!database.exists()) {
            // Import from player files. Done async so may take a while
            convertFiles();
        } else {
            // Ready to go...
            this.dbReady = true;
        }
    }

    /**
     * Async Saving of the DB
     */
    public void asyncSaveDB() {
        if (!savingFlag) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    saveDB();                
                }}.runTaskAsynchronously(plugin);
        }
    }

    /**
     * Saves the DB
     */
    public void saveDB() {
        savingFlag = true;
        try {
            File oldDB = new File(plugin.getDataFolder(), "name-uuid.txt");
            File newDB = new File(plugin.getDataFolder(), "name-uuid-new.txt");
            //File backup = new File(plugin.getDataFolder(), "name-uuid.bak");
            try(PrintWriter out = new PrintWriter(newDB)) {
                // Write the newest entries at the top
                for (Entry<String, UUID> entry: treeMap.entrySet()) {
                    out.println(entry.getKey());
                    out.println(entry.getValue().toString());
                }
                if (oldDB.exists()) {
                    // Go through the old file and remove any                     
                    try(BufferedReader br = new BufferedReader(new FileReader(oldDB))){
                        // Go through the old file and read it line by line and write to the new file
                        String line = br.readLine();
                        String uuid = br.readLine();
                        while (line != null) {
                            if (!treeMap.containsKey(line)) {                  
                                out.println(line);
                                out.println(uuid);
                            }                    
                            // Read next lines
                            line = br.readLine();
                            uuid = br.readLine();
                        }
                    }

                }
            }

            // Move files around
            try {
                Files.move(newDB.toPath(), oldDB.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().severe("Problem saving name database! Could not rename files!");                
            }                         
        } catch (IOException e) {
            plugin.getLogger().severe("Problem saving name database!");
            e.printStackTrace();
        }
        savingFlag = false;
    }

    private void convertFiles() {
        /** create database */
        new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    // Load all the files from the player folder
                    FilenameFilter ymlFilter = (dir, name) -> {
                        String lowercaseName = name.toLowerCase();
                        if (lowercaseName.endsWith(".yml")) {
                            return true;
                        } else {
                            return false;
                        }
                    };
                    int count = 0;
                    for (final File file : plugin.getPlayersFolder().listFiles(ymlFilter)) {
                        if (count % 1000 == 0) {
                            System.out.println("[ASkyBlock]: Processed " + count + " names to database");
                        }
                        count++;
                        try {
                            // Get UUID
                            String uuid = file.getName().substring(0, file.getName().length() - 4);
                            //System.out.println("DEBUG: UUID is " + uuid);
                            final UUID playerUUID = UUID.fromString(uuid);
                            // Get the player's name
                            try (Scanner scanner = new Scanner(file)) {
                                while (scanner.hasNextLine()) {
                                    final String lineFromFile = scanner.nextLine();
                                    if (lineFromFile.contains("playerName:")) { 
                                        // Check against potentialUnowned list
                                        String playerName = lineFromFile.substring(lineFromFile.indexOf(' ')).trim();
                                        //System.out.println("DEBUG: Player name is " + playerName);
                                        treeMap.put(playerName.toLowerCase(), playerUUID);
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("[ASkyBlock/AcidIsland]: Problem reading " + file.getName() + " skipping...");
                            //ex.printStackTrace();
                        }
                    }
                    // Save files
                    saveDB();
                    // Save memory
                    treeMap.clear();
                    System.out.println("Complete. Processed " + count + " names to database");
                    // Set flag
                    dbReady = true;
                } catch (Exception e) {
                    System.err.println("[ASkyBlock/AcidIsland] : Problem creating database");
                }
            }}.runTaskAsynchronously(plugin);
    }

    /**
     * @return the dbReady
     */
    public boolean isDbReady() {
        return dbReady;
    }

    /**
     * Saves the player name to the database. Case insensitive!
     * @param playerName
     * @param playerUUID - the player's UUID
     */
    public void savePlayerName(String playerName, UUID playerUUID) {
        if (playerName == null) {
            return;
        }
        treeMap.put(playerName.toLowerCase(), playerUUID);
        // This will be saved when everything shuts down
    }

    /**
     * Gets the UUID for this player name or null if not known. Case insensitive!
     * @param playerName
     * @return UUID of player, or null if unknown
     */
    public UUID getPlayerUUID(String playerName) {
        if (playerName == null) {
            return null;
        }
        // Try cache
        if (treeMap.containsKey(playerName.toLowerCase())) {
            //plugin.getLogger().info("DEBUG: found in UUID cache");
            return treeMap.get(playerName.toLowerCase());
        }
        // Names and UUID's are stored in line pairs.
        try(BufferedReader br = new BufferedReader(new FileReader(new File(plugin.getDataFolder(), "name-uuid.txt")))) {
            String line = br.readLine();
            String uuid = br.readLine();
            while (line != null && !line.equalsIgnoreCase(playerName)) {                
                line = br.readLine();
                uuid = br.readLine();
            }
            if (line == null) {
                return null;
            }
            UUID result = UUID.fromString(uuid);
            // Add to cache
            treeMap.put(playerName.toLowerCase(), result);
            //plugin.getLogger().info("DEBUG: found in UUID database");
            return result;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return null;
    }

    /**
     * Gets players name from tiny database
     * @param playerUuid - the player's UUID
     * @return Name or empty string if unknown
     */
    public String getPlayerName(UUID playerUuid) {
        if (playerUuid == null) {
            return "";
        }
        // Names and UUID's are stored in line pairs.
        try(BufferedReader br = new BufferedReader(new FileReader(new File(plugin.getDataFolder(), "name-uuid.txt")))) {
            String line = br.readLine();
            String uuid = br.readLine();
            while (uuid != null && !uuid.equals(playerUuid.toString())) {                
                line = br.readLine();
                uuid = br.readLine();
            }
            if (line == null) {
                return "";
            }
            // Add to cache
            treeMap.put(line.toLowerCase(), playerUuid);
            plugin.getLogger().info("DEBUG: found in UUID database - name is " + line);
            return line;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return "";
    }
}
