package com.wasteofplastic.askyblock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

public class Util {
    protected static List<String> chop(ChatColor color, String longLine, int length) {
	List<String> result = new ArrayList<String>();
	// int multiples = longLine.length() / length;
	int i = 0;
	for (i = 0; i < longLine.length(); i += length) {
	    // for (int i = 0; i< (multiples*length); i += length) {
	    int endIndex = Math.min(i + length, longLine.length());
	    String line = longLine.substring(i, endIndex);
	    // Do the following only if i+length is not the end of the string
	    if (endIndex < longLine.length()) {
		// Check if last character in this string is not a space
		if (!line.substring(line.length() - 1).equals(" ")) {
		    // If it is not a space, check to see if the next character
		    // in long line is a space.
		    if (!longLine.substring(endIndex, endIndex + 1).equals(" ")) {
			// If it is not, then we are cutting a word in two and
			// need to backtrack to the last space if possible
			int lastSpace = line.lastIndexOf(" ");
			// Only do this if there is a space in the line to backtrack to...
			if (lastSpace != -1 && lastSpace < line.length()) {
			    line = line.substring(0, lastSpace);
			    i -= (length - lastSpace - 1);
			}
		    }
		}
	    }
	    // }
	    result.add(color + line);
	}
	// result.add(color + longLine.substring(i, longLine.length()));
	return result;
    }
}
