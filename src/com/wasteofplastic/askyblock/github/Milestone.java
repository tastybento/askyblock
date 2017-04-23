package com.wasteofplastic.askyblock.github;

/**
 * This object stores all data about an open milestone
 * @author Poslovitch
 */
public class Milestone {
	
	private String name, description, link;
	private int openIssues, closedIssues;
	private String created, updated, dueDate;
	
	public Milestone(String name, String description, String link, int openIssues, int closedIssues, String created, String updated, String dueDate){
		this.name = name;
		this.description = description;
		this.link = link;
		this.openIssues = openIssues;
		this.closedIssues = closedIssues;
		this.created = created;
		this.updated = updated;
		this.dueDate = dueDate;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public String getLink(){
		return this.link;
	}
	
	public int getOpenIssues(){
		return this.openIssues;
	}
	
	public int getClosedIssues(){
		return this.closedIssues;
	}
	
	public double getProgress(){
		//TODO
		return 0;
	}
	
	public String getCreatedAt(){
		return this.created;
	}
	
	public String getUpdatedAt(){
		return this.updated;
	}
	
	public String getDueDate(){
		return this.dueDate;
	}
}
