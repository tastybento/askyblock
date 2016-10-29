package com.wasteofplastic.askyblock.challenge;

public enum ChallengeType {
	PLAYER,
	ISLAND,
	ISLAND_LEVEL,
	MEGA_PLAYER,
	MEGA_ISLAND;
	
	static ChallengeType getFromString(String s){
		if(s == null || s.trim().isEmpty()) return PLAYER;
		switch(s.trim()){
		case "player":
			return PLAYER;
		case "island":
			return ISLAND;
		case "level":
			return ISLAND_LEVEL;
		case "megaplayer":
			return MEGA_PLAYER;
		case "megaisland":
			return MEGA_ISLAND;
		default:
			return PLAYER;
		}
	}
}
