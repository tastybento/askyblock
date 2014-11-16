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
package com.wasteofplastic.askyblock;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author ben Provides a descending order sort
 */
public class MapUtil {
    public static <Key, Value extends Comparable<? super Value>> Map<Key, Value> sortByValue(Map<Key, Value> map) {
	List<Map.Entry<Key, Value>> list = new LinkedList<Map.Entry<Key, Value>>(map.entrySet());
	Collections.sort(list, new Comparator<Map.Entry<Key, Value>>() {
	    public int compare(Map.Entry<Key, Value> o1, Map.Entry<Key, Value> o2) {
		// Switch these two if you want ascending
		return (o2.getValue()).compareTo(o1.getValue());
	    }
	});

	Map<Key, Value> result = new LinkedHashMap<Key, Value>();
	for (Map.Entry<Key, Value> entry : list) {
	    result.put(entry.getKey(), entry.getValue());
	}
	return result;
    }
}
