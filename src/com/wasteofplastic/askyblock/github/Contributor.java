package com.wasteofplastic.askyblock.github;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This object stores all data about a contributor
 * @author Poslovitch
 */
public class Contributor {

	private String name, profile;
	private int commits;
	
	public Contributor(String name, String profile, int commits){
		this.name = name;
		this.profile = profile;
		this.commits = commits;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getProfile(){
		return this.profile;
	}
	
	public int getCommits(){
		return this.commits;
	}
	
	public double getContribution(){
		return new BigDecimal( getCommits() / GithubData.commitsTotal * 100 ).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
