package com.wasteofplastic.askyblock.placeholders;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;

/**
 * Register placeholders
 * 
 * @author Poslovitch
 */
public class Placeholders {
	private static Set<Placeholder> placeholders = new HashSet<Placeholder>();
	
	private ASkyBlock plugin;
	
	public Placeholders(ASkyBlock plugin){
		this.plugin = plugin;
		register();
	}
	
	private void register(){
		
		/*		PLUGIN		*/
		new Placeholder("name") {
			@Override
			public String onRequest(Player player) {
				return plugin.getDescription().getName();
			}
		};
		
		new Placeholder("version") {
			@Override
			public String onRequest(Player player) {
				return plugin.getDescription().getVersion();
			}
		};
		
		/*		ISLAND		*/
		new Placeholder("island_level") {
			
			@Override
			public String onRequest(Player player) {
				return "" + plugin.getPlayers().getIslandLevel(player.getUniqueId());
			}
		};
		
		//TODO add other placeholders
	}
	
	public static Set<Placeholder> getPlaceholders(){
		return placeholders;
	}
	
	public abstract class Placeholder{
		private String identifier;
		
		protected Placeholder(String identifier){
			this.identifier = Settings.PLACEHOLDERPREFIX + "_" + identifier;
			placeholders.add(this);
		}
		
		public abstract String onRequest(Player player);
		
		public String getIdentifier(){
			return this.identifier;
		}
	}
}
