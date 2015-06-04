package org.oham.testredis.util;

import java.io.IOException;
import java.util.Properties;

public class PropertiesUtil {

	public static String getValue(String path, String key) {
		Properties prop = new Properties();
		try {
			prop.load(PropertiesUtil.class.getClassLoader().getResourceAsStream(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop.getProperty(key);
	}
}
