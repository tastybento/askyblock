package com.wasteofplastic.askyblock;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import jdbm.PrimaryHashMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;

/**
 * Tiny database for a hashmap that is not used very often, but could be very big so I
 * don't want it in memory. 
 * @author tastybento
 *
 */
public class TinyDB {
    private ASkyBlock plugin;
    private RecordManager recMan;
    private PrimaryHashMap<String,UUID> treeMap;
    private String fileName = "nameUUIDdb";
    private File dbFolder;
    private File name2UUID;
    private boolean dbReady;
    /**
     * Opens the database
     * @param plugin
     */
    public TinyDB(ASkyBlock plugin) {
	this.plugin = plugin;
	this.dbReady = false;
	// Open database
	dbFolder = new File(plugin.getDataFolder(),"database");
	name2UUID = new File(dbFolder,fileName);
	if (!dbFolder.exists()) {
	    plugin.getLogger().info("Creating a tiny playerName-UUID database");
	    // Create folder
	    dbFolder.mkdir();
	    // Run async task to do conversion
	    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

		@Override
		public void run() {
		    convertFiles();
		}});

	} else {
	    try {
		recMan = RecordManagerFactory.createRecordManager(name2UUID.getAbsolutePath());
		String recordName = "nameToUUID";
		treeMap = recMan.hashMap(recordName);
		dbReady = true;
	    } catch (IOException e) {
		dbReady = false;
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    /**
     * Commits the database records and closes the database
     */
    public void closeDB() {
	/** Map changes are not persisted yet, commit them (save to disk) */
	try {
	    recMan.commit();
	    /** close record manager */
	    recMan.close();
	    plugin.getLogger().info("Saved name database");
	} catch (IOException e) {
	    plugin.getLogger().severe("Problem saving name database!");
	    e.printStackTrace();
	}
    }

    private void convertFiles() {
	/** create database */
	try {
	    recMan = RecordManagerFactory.createRecordManager(name2UUID.getAbsolutePath());
	    String recordName = "nameToUUID";
	    treeMap = recMan.hashMap(recordName);
	    // Load all the files from the player folder
	    FilenameFilter ymlFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
		    String lowercaseName = name.toLowerCase();
		    if (lowercaseName.endsWith(".yml")) {
			return true;
		    } else {
			return false;
		    }
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
		    Scanner scanner = new Scanner(file);
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
		    scanner.close();
		} catch (Exception ex) {
		    System.err.println("[ASkyBlock/AcidIsland]: Problem reading " + file.getName() + " skipping...");
		    //ex.printStackTrace();
		}
	    }
	    /** Map changes are not persisted yet, commit them (save to disk) */
	    recMan.commit();
	    System.out.println("[ASkyBlock]: Complete. Processed " + count + " names to database");
	    // Set flag
	    dbReady = true;
	} catch (Exception e) {
	    System.err.println("[ASkyBlock/AcidIsland] : Problem creating database");
	}
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
     * @param playerUUID
     */
    public void savePlayerName(String playerName, UUID playerUUID) {
	treeMap.put(playerName.toLowerCase(), playerUUID);
    }

    /**
     * Gets the UUID for this player name or null if not known. Case insensitive!
     * @param playerName
     */
    public UUID getPlayerUUID(String playerName) {
	return treeMap.get(playerName.toLowerCase());
    }
}
