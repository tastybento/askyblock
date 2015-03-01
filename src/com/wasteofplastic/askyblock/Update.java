package com.wasteofplastic.askyblock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Bukkit.org update checker
 * Source:
 * https://github.com/gravitylow/ServerModsAPI-Example/blob/master/Update.java
 * License: free
 */
public class Update {

    // The project's unique ID
    private final int projectID;

    // An optional API key to use, will be null if not submitted
    private final String apiKey;

    // Keys for extracting file information from JSON response
    private static final String API_NAME_VALUE = "name";
    private static final String API_LINK_VALUE = "downloadUrl";
    private static final String API_RELEASE_TYPE_VALUE = "releaseType";
    private static final String API_FILE_NAME_VALUE = "fileName";
    private static final String API_GAME_VERSION_VALUE = "gameVersion";

    // Static information for querying the API
    private static final String API_QUERY = "/servermods/files?projectIds=";
    private static final String API_HOST = "https://api.curseforge.com";

    // Results of query
    // Version's title
    String versionName = "";

    // Version's link
    String versionLink = "";

    // Version's release type
    String versionType = "";

    // Version's file name
    String versionFileName = "";

    // Version's game version
    String versionGameVersion = "";

    private boolean success = false;

    /**
     * Check for updates anonymously (keyless)
     * 
     * @param projectID
     *            The BukkitDev Project ID, found in the "Facts" panel on the
     *            right-side of your project page.
     */
    public Update(int projectID) {
	this(projectID, null);
    }

    /**
     * Check for updates using your Curse account (with key)
     * 
     * @param projectID
     *            The BukkitDev Project ID, found in the "Facts" panel on the
     *            right-side of your project page.
     * @param apiKey
     *            Your ServerMods API key, found at
     *            https://dev.bukkit.org/home/servermods-apikey/
     */
    public Update(int projectID, String apiKey) {
	this.projectID = projectID;
	this.apiKey = apiKey;

	success = query();
    }

    /**
     * Query the API to find the latest approved file's details.
     * 
     * @return true if successful
     */
    public boolean query() {
	URL url = null;

	try {
	    // Create the URL to query using the project's ID
	    url = new URL(API_HOST + API_QUERY + projectID);
	} catch (MalformedURLException e) {
	    // There was an error creating the URL

	    e.printStackTrace();
	    return false;
	}

	try {
	    // Open a connection and query the project
	    URLConnection conn = url.openConnection();

	    if (apiKey != null) {
		// Add the API key to the request if present
		conn.addRequestProperty("X-API-Key", apiKey);
	    }

	    // Add the user-agent to identify the program
	    conn.addRequestProperty("User-Agent", "ASkyBlockAcidIsland Update Checker");

	    // Read the response of the query
	    // The response will be in a JSON format, so only reading one line
	    // is necessary.
	    final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    String response = reader.readLine();

	    // Parse the array of files from the query's response
	    JSONArray array = (JSONArray) JSONValue.parse(response);

	    if (array.size() > 0) {
		// Get the newest file's details
		JSONObject latest = (JSONObject) array.get(array.size() - 1);

		// Get the version's title
		versionName = (String) latest.get(API_NAME_VALUE);

		// Get the version's link
		versionLink = (String) latest.get(API_LINK_VALUE);

		// Get the version's release type
		versionType = (String) latest.get(API_RELEASE_TYPE_VALUE);

		// Get the version's file name
		versionFileName = (String) latest.get(API_FILE_NAME_VALUE);

		// Get the version's game version
		versionGameVersion = (String) latest.get(API_GAME_VERSION_VALUE);

		return true;
	    } else {
		System.out.println("There are no files for this project");
		return false;
	    }
	} catch (IOException e) {
	    // There was an error reading the query

	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * @return the versionName
     */
    public String getVersionName() {
	return versionName;
    }

    /**
     * @return the versionLink
     */
    public String getVersionLink() {
	return versionLink;
    }

    /**
     * @return the versionType
     */
    public String getVersionType() {
	return versionType;
    }

    /**
     * @return the versionFileName
     */
    public String getVersionFileName() {
	return versionFileName;
    }

    /**
     * @return the versionGameVersion
     */
    public String getVersionGameVersion() {
	return versionGameVersion;
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
	return success;
    }
}