package com.wasteofplastic.askyblock.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Log-out & SamThePsychoticLeprechaun
 *
 */
public class SimpleConfig {
    private int comments;
    private SimpleConfigManager manager;

    private File file;
    private FileConfiguration config;

    public SimpleConfig(InputStream configStream, File configFile, int comments, JavaPlugin plugin) {
	this.comments = comments;
	this.manager = new SimpleConfigManager(plugin);

	this.file = configFile;
	this.config = YamlConfiguration.loadConfiguration(configStream);
    }

    public Object get(String path) {
	return this.config.get(path);
    }

    public Object get(String path, Object def) {
	return this.config.get(path, def);
    }

    public String getString(String path) {
	return this.config.getString(path);
    }

    public String getString(String path, String def) {
	return this.config.getString(path, def);
    }

    public int getInt(String path) {
	return this.config.getInt(path);
    }

    public int getInt(String path, int def) {
	return this.config.getInt(path, def);
    }

    public boolean getBoolean(String path) {
	return this.config.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
	return this.config.getBoolean(path, def);
    }

    public void createSection(String path) {
	this.config.createSection(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
	return this.config.getConfigurationSection(path);
    }

    public double getDouble(String path) {
	return this.config.getDouble(path);
    }

    public double getDouble(String path, double def) {
	return this.config.getDouble(path, def);
    }

    public List<?> getList(String path) {
	return this.config.getList(path);
    }

    public List<?> getList(String path, List<?> def) {
	return this.config.getList(path, def);
    }

    public List<String> getStringList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<String>(0);

	}

	List<String> result = new ArrayList<String>();

	for (Object object : list) {

	    if ((object instanceof String) || (isPrimitiveWrapper(object))) {

		result.add(String.valueOf(object));
	    }

	}

	return result;

    }

    public List<Integer> getIntegerList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Integer>(0);

	}

	List<Integer> result = new ArrayList<Integer>();

	for (Object object : list) {

	    if (object instanceof Integer) {

		result.add((Integer) object);

	    } else if (object instanceof String) {

		try {

		    result.add(Integer.valueOf((String) object));

		} catch (Exception ex) {

		}

	    } else if (object instanceof Character) {

		result.add((int) ((Character) object).charValue());

	    } else if (object instanceof Number) {

		result.add(((Number) object).intValue());

	    }

	}

	return result;

    }

    public List<Boolean> getBooleanList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Boolean>(0);

	}


	List<Boolean> result = new ArrayList<Boolean>();

	for (Object object : list) {

	    if (object instanceof Boolean) {

		result.add((Boolean) object);

	    } else if (object instanceof String) {

		if (Boolean.TRUE.toString().equals(object)) {

		    result.add(true);

		} else if (Boolean.FALSE.toString().equals(object)) {

		    result.add(false);

		}

	    }

	}

	return result;

    }


    public List<Double> getDoubleList(String path) {

	List<?> list = getList(path);


	if (list == null) {

	    return new ArrayList<Double>(0);

	}

	List<Double> result = new ArrayList<Double>();

	for (Object object : list) {

	    if (object instanceof Double) {

		result.add((Double) object);

	    } else if (object instanceof String) {

		try {

		    result.add(Double.valueOf((String) object));

		} catch (Exception ex) {

		}

	    } else if (object instanceof Character) {

		result.add((double) ((Character) object).charValue());

	    } else if (object instanceof Number) {

		result.add(((Number) object).doubleValue());

	    }

	}

	return result;

    }

    public List<Float> getFloatList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Float>(0);

	}

	List<Float> result = new ArrayList<Float>();

	for (Object object : list) {

	    if (object instanceof Float) {


		result.add((Float) object);

	    } else if (object instanceof String) {

		try {

		    result.add(Float.valueOf((String) object));

		} catch (Exception ex) {

		}

	    } else if (object instanceof Character) {

		result.add((float) ((Character) object).charValue());

	    } else if (object instanceof Number) {

		result.add(((Number) object).floatValue());

	    }

	}

	return result;

    }

    public List<Long> getLongList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Long>(0);

	}

	List<Long> result = new ArrayList<Long>();

	for (Object object : list) {

	    if (object instanceof Long) {

		result.add((Long) object);

	    } else if (object instanceof String) {

		try {

		    result.add(Long.valueOf((String) object));

		} catch (Exception ex) {

		}

	    } else if (object instanceof Character) {

		result.add((long) ((Character) object).charValue());

	    } else if (object instanceof Number) {

		result.add(((Number) object).longValue());

	    }

	}

	return result;

    }

    public List<Byte> getByteList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Byte>(0);

	}

	List<Byte> result = new ArrayList<Byte>();

	for (Object object : list) {

	    if (object instanceof Byte) {

		result.add((Byte) object);

	    } else if (object instanceof String) {

		try {

		    result.add(Byte.valueOf((String) object));

		} catch (Exception ex) {

		}

	    } else if (object instanceof Character) {

		result.add((byte) ((Character) object).charValue());

	    } else if (object instanceof Number) {

		result.add(((Number) object).byteValue());

	    }

	}

	return result;

    }

    public List<Character> getCharacterList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Character>(0);

	}

	List<Character> result = new ArrayList<Character>();

	for (Object object : list) {

	    if (object instanceof Character) {


		result.add((Character) object);

	    } else if (object instanceof String) {

		String str = (String) object;


		if (str.length() == 1) {

		    result.add(str.charAt(0));

		}

	    } else if (object instanceof Number) {

		result.add((char) ((Number) object).intValue());

	    }

	}

	return result;

    }

    public List<Short> getShortList(String path) {

	List<?> list = getList(path);

	if (list == null) {

	    return new ArrayList<Short>(0);

	}

	List<Short> result = new ArrayList<Short>();

	for (Object object : list) {

	    if (object instanceof Short) {

		result.add((Short) object);

	    } else if (object instanceof String) {

		try {

		    result.add(Short.valueOf((String) object));

		} catch (Exception ex) {

		}

	    } else if (object instanceof Character) {

		result.add((short) ((Character) object).charValue());

	    } else if (object instanceof Number) {

		result.add(((Number) object).shortValue());

	    }

	}

	return result;

    }

    public List<Map<?, ?>> getMapList(String path) {

	List<?> list = getList(path);

	List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();

	if (list == null) {

	    return result;

	}

	for (Object object : list) {

	    if (object instanceof Map) {

		result.add((Map<?, ?>) object);

	    }

	}

	return result;

    }



    public boolean contains(String path) {
	return this.config.contains(path);
    }

    public void removeKey(String path) {
	this.config.set(path, null);
    }

    public void set(String path, Object value) {
	this.config.set(path, value);
    }

    public void set(String path, Object value, String comment) {
	if(!this.config.contains(path)) {
	    this.config.set(manager.getPluginName() + "_COMMENT_" + comments, " " + comment);
	    comments++;
	}

	this.config.set(path, value);

    }

    public void set(String path, Object value, String[] comment) {

	for(String comm : comment) {

	    if(!this.config.contains(path)) {
		this.config.set(manager.getPluginName() + "_COMMENT_" + comments, " " + comm);
		comments++;
	    }

	}

	this.config.set(path, value);

    }

    public void setHeader(String[] header) {
	manager.setHeader(this.file, header);
	this.comments = header.length + 2;
	this.reloadConfig();
    }

    public void reloadConfig() {
	this.config = YamlConfiguration.loadConfiguration(manager.getConfigContent(file));
    }

    public void saveConfig() {
	String config = this.config.saveToString();
	manager.saveConfig(config, this.file);

    }

    public Set<String> getKeys() {
	return this.config.getKeys(false);
    }

    protected boolean isPrimitiveWrapper(Object input) {

	return input instanceof Integer || input instanceof Boolean ||
		input instanceof Character || input instanceof Byte ||
		input instanceof Short || input instanceof Double ||
		input instanceof Long || input instanceof Float;

    }

}