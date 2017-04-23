package com.wasteofplastic.askyblock.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wasteofplastic.askyblock.ASkyBlock;

public class GithubSetup {
	
	public static void setup(){
		// Lines of code
		new GithubRetriever(ASkyBlock.getPlugin()) {
			
			@Override
			public void onSuccess(JsonElement element) {
				JsonObject object = element.getAsJsonObject();
				GithubData.linesOfCode = object.get("Java").getAsInt();
			}
			
			@Override
			public void onFailure() {}
			
			@Override
			public String getRepository() {
				return "tastybento/askyblock";
			}
			
			@Override
			public String getURLSuffix() {
				return "/languages";
			}
			
			@Override
			public String getFileName() {
				return "lines_of_code";
			}
		};
		
		// Contributors
		new GithubRetriever(ASkyBlock.getPlugin()) {
			
			@Override
			public void onSuccess(JsonElement element) {
				GithubData.contributors.clear();
				JsonArray array = element.getAsJsonArray();
				
				for(int i = 0; i < array.size(); i++){
					JsonObject object = array.get(i).getAsJsonObject();
					
					String name = object.get("login").getAsString();
					String profile = object.get("html_url").getAsString();
					int commits = object.get("contributions").getAsInt();
					
					if(!name.equals("invalid-email-address")){
						GithubData.contributors.add(new Contributor(name, profile, commits));
						GithubData.commitsTotal += commits;
					}
				}
			}
			
			@Override
			public void onFailure() {
				GithubData.contributors.clear();
				GithubData.contributors.add(new Contributor("tastybento", "https://github.com/tastybento", 1));
				GithubData.commitsTotal = 1;
			}
			
			@Override
			public String getRepository() {
				return "tastybento/askyblock";
			}
			
			@Override
			public String getURLSuffix() {
				return "/contributors";
			}
			
			@Override
			public String getFileName() {
				return "contributors";
			}
		};
		
		// Issues
	}
}
