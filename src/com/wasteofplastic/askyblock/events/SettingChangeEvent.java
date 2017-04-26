/*******************************************************************************
 * This file is part of ASkyBlock.
 * <p>
 * ASkyBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * ASkyBlock is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.askyblock.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;

import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Island.SettingsFlag;

/**
 * This event is fired when a player changes a setting on his island
 * <p>
 * Cancelling this event will result in cancelling the change.
 * 
 * @author Poslovitch
 * @since 4.0
 */
public class SettingChangeEvent extends ASkyBlockEvent implements Cancellable{

	private boolean cancelled;
	private SettingsFlag editedSetting;
	private boolean setTo;
	
	/**
	 * @param player
	 * @param island
	 * @param editedSetting
	 * @param setTo
	 */
	public SettingChangeEvent(UUID player, Island island, SettingsFlag editedSetting, boolean setTo) {
		super(player, island);
		this.editedSetting = editedSetting;
		this.setTo = setTo;
	}
	
	/**
	 * @return the edited setting
	 */
	public SettingsFlag getSetting(){
		return this.editedSetting;
	}
	
	/**
	 * @return enabled/disabled
	 */
	public boolean getSetTo(){
		return this.setTo;
	}
	
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
