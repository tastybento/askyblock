package com.wasteofplastic.askyblock.placeholders;

import java.text.DecimalFormat;
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
				String level = plugin.getChatListener().getPlayerLevel(player.getUniqueId());
				if(Settings.fancyIslandLevelDisplay) {
	                if (Integer.valueOf(level) > 1000){
	                    // 1052 -> 1.0k
	                    level = new DecimalFormat("#.#").format(Double.valueOf(level)/1000.0) + "k";
	                }
	            }
				return level;
			}
		};
		
		new Placeholder("island_level_raw") {
			@Override
			public String onRequest(Player player) {
				return "" + plugin.getPlayers().getIslandLevel(player.getUniqueId());
			}
		};
		
		/*		PLAYER		*/
		new Placeholder("player_name") {
			@Override
			public String onRequest(Player player) {
				return player.getName();
			}
		};
		
		new Placeholder("challenge_level") {
			@Override
			public String onRequest(Player player) {
				return "" + plugin.getChatListener().getPlayerChallengeLevel(player.getUniqueId());
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
			this.identifier = identifier;
			placeholders.add(this);
		}
		
		public abstract String onRequest(Player player);
		
		public String getIdentifier(){
			return this.identifier;
		}
	}
}
