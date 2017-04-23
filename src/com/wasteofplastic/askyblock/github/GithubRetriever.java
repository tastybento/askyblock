package com.wasteofplastic.askyblock.github;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.wasteofplastic.askyblock.ASkyBlock;

public abstract class GithubRetriever {

	private ASkyBlock plugin;
	
	private static Set<GithubRetriever> retrievers = new HashSet<GithubRetriever>();

	private File file;

	public GithubRetriever(ASkyBlock plugin){
		this.plugin = plugin;
		this.file = new File(plugin.getDataFolder() + "/github/" + this.getFileName() + ".json");
		retrievers.add(this);
	}

	public boolean hasData() {
		return this.getFile().exists();
	}

	public File getFile() {
		return this.file;
	}

	public abstract String getFileName();
	public abstract String getRepository();
	public abstract String getURLSuffix();
	public abstract void onSuccess(JsonElement element);
	public abstract void onFailure();

	public void parseData(){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(this.getFile()));

			String full = "";

			String line;
			while ((line = reader.readLine()) != null) {
				full += line;
			}

			reader.close();

			JsonElement element = new JsonParser().parse(full);

			this.onSuccess(element);
		} 
		catch (IOException e) {
			e.printStackTrace();
			this.onFailure();
		}
	}

	public void updateFile() {
		plugin.getLogger().info("Downloading \"" + this.getFileName() + ".json\" from GitHub");
		try {
			URL website = new URL("https://api.github.com/repos/" + this.getRepository() + this.getURLSuffix());

			URLConnection connection = website.openConnection();
			connection.setConnectTimeout(3000);
			connection.addRequestProperty("User-Agent", "Tastybento - ASkyBlock/AcidIsland");
			connection.setDoOutput(true);

			ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
			FileOutputStream fos = new FileOutputStream(file);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			plugin.getLogger().info("Finished download: \"" + this.getFileName() + ".json\"");
			this.parseData();
		} catch (IOException e) {
			plugin.getLogger().warning("Could not connect to GitHub to download files");

			if (hasData()) {
				this.parseData();
			}
			else {
				this.onFailure();
			}
		}
	}
}
