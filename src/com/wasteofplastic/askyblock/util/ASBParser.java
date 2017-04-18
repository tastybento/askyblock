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

import org.bukkit.Sound;

import com.wasteofplastic.askyblock.ASkyBlock;

/**
 * This class parses items and sounds for challenges and panels.
 * 
 * @author Poslovitch
 */
public class ASBParser {
	
	private ASkyBlock plugin;
	private static ASBParser instance;
	
	public ASBParser(ASkyBlock plugin){
		this.plugin = plugin;
		instance = this;
	}
	
	public static ASBParser getInstance(){
		return instance;
	}
	
	public ASBSound parseSound(String text, String key){
		String[] split = text.split(";");
		
		Sound sound;
		float pitch;
		float volume;
		
		if(Sound.valueOf(split[0]) != null) sound = Sound.valueOf(split[0]);
		else {
			plugin.getLogger().warning("Could not parse \"" + text + "\" in \"" + key + "\" : Sound does not exist.");
			return null;
		}
		
		try{
			pitch = Float.valueOf(split[1]);
			if(pitch <= 0){
				plugin.getLogger().warning("Could not parse \"" + text + "\" in \"" + key + "\" : Pitch must be higher than 0. Setting it to 1.");
				pitch = 1.0F;
			}
		} catch(NumberFormatException e) {
			plugin.getLogger().warning("Could not parse \"" + text + "\" in \"" + key + "\" : Pitch invalid.");
			return null;
		}
		
		try{
			volume = Float.valueOf(split[2]);
			if(volume <= 0){
				plugin.getLogger().warning("Could not parse \"" + text + "\" in \"" + key + "\" : Volume must be higher than 0. Setting it to 1.");
				volume = 1.0F;
			}
		} catch(NumberFormatException e) {
			plugin.getLogger().warning("Could not parse \"" + text + "\" in \"" + key + "\" : Volume invalid.");
			return null;
		}
		
		return new ASBSound(sound, pitch, volume);
	}
	
	public class ASBSound{
		private Sound sound;
		private float pitch;
		private float volume;
		
		private ASBSound(Sound sound, float pitch, float volume){
			this.sound = sound;
			this.pitch = pitch;
			this.volume = volume;
		}
		
		public Sound getSound(){
			return this.sound;
		}
		
		public float getPitch(){
			return this.pitch;
		}
		
		public float getVolume(){
			return this.volume;
		}
	}
}
