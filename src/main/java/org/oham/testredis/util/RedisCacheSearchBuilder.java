package org.oham.testredis.util;

import java.util.HashMap;
import java.util.Map;

import org.oham.testredis.services.CacheService;

public class RedisCacheSearchBuilder {

	private static final String KEY_VALUE_SYMBOL = "->";
	
	private static final String END_SYMBOL = "$|";
	
	private Map<String, Object> paramMap;
	
	private StringBuilder sb;
	
	private Long expiredTime = 60L;
	
	private boolean useCustomizedKey = false;
	
	public RedisCacheSearchBuilder(String businessKey) {
		this.paramMap = new HashMap<String, Object>();
		sb = new StringBuilder(businessKey).append(":");
	}
	
	public RedisCacheSearchBuilder(String businessKey, Long expired) {
		this.paramMap = new HashMap<String, Object>();
		this.sb = new StringBuilder(businessKey).append(":");
		this.expiredTime = expired;
		
	}
	
	public RedisCacheSearchBuilder put(String key, Object val) {
		sb.append(key).append(KEY_VALUE_SYMBOL)
			.append(val).append(END_SYMBOL);
		paramMap.put(key, val);
		
		return this;
	}
	
	public String getCacheKey () {
		if( useCustomizedKey ) {
			return CacheService.CACHE_PREFIX + (this.sb.substring(0, sb.indexOf(":")).hashCode());
		}
		return CacheService.CACHE_PREFIX + (sb.substring(0, sb.length()-END_SYMBOL.length())).hashCode();
	}
	
	public Map<String, Object> getParamsMap() {
		return paramMap;
	}
	
	public Long getExpiredTime() {
		return this.expiredTime;
	}

	public boolean isUseCustomizedKey() {
		return useCustomizedKey;
	}

	public RedisCacheSearchBuilder setUseCustomizedKey(boolean useCustomizedKey) {
		this.useCustomizedKey = useCustomizedKey;
		return this;
	}
	
	
}
