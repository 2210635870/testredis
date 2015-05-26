package org.oham.testredis.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

public class JsonUtil {

	private static ObjectMapper objectMapper = new ObjectMapper();
	
	public static String toJsonString(Object entity) throws JsonGenerationException, JsonMappingException, IOException {
		return objectMapper.writeValueAsString(entity);
	}
	
	public static <T> T json2Entity(String json, Class<T> cls) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json, cls);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> json2List(String json, Class<T> cls) throws JsonParseException, JsonMappingException, IOException {
		JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, cls);
		return (List<T>)objectMapper.readValue(json, javaType);
	}
	
}
