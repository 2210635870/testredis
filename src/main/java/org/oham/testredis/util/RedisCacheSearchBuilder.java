package org.oham.testredis.util;

import java.util.HashMap;
import java.util.Map;

import org.oham.testredis.services.CacheService;


/**
 *线程不安全 
 */
public class RedisCacheSearchBuilder {

	private static final String KEY_VALUE_SYMBOL = "->";
	
	private static final String END_SYMBOL = "$|";
	
	private Map<String, Object> paramMap;
	
	private StringBuilder sb;
	
	private Long expiredTime = 60L;
	
	private boolean useCustomizedKey = false; // 当为true时，使用自己指定的key
	
	private boolean pageFromCache = false;
	
	private Long start = null;
	
	private Long end = null;
	
	
	public RedisCacheSearchBuilder(String businessKey) {
		if( businessKey.indexOf(CacheService.CACHE_PREFIX) == 0) {
			businessKey = businessKey.substring(CacheService.CACHE_PREFIX.length(), businessKey.length());
		}
		
		this.paramMap = new HashMap<String, Object>();
		sb = new StringBuilder(businessKey).append(":");
	}
	
	public RedisCacheSearchBuilder(String businessKey, Long expired) {
		if( businessKey.indexOf(CacheService.CACHE_PREFIX) == 0) {
			businessKey = businessKey.substring(CacheService.CACHE_PREFIX.length(), businessKey.length());
		}
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
		if(paramMap.size() == 0) {
			return CacheService.CACHE_PREFIX + (this.sb.substring(0, sb.indexOf(":")).hashCode()) + "_all";
		}
		
		if( useCustomizedKey ) {
			return CacheService.CACHE_PREFIX + (this.sb.substring(0, sb.indexOf(":")));
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
	
	// 用于判断是否使用缓存分页
	public boolean isPageFromCache() {
		return ( pageFromCache && start != null && end != null );
	}
	
	// 用于设置使用缓存分页， 逻辑跟mysql分页的一样
	public void setPageFromCache(Long start, Long size) {
		this.pageFromCache = true;
		this.start = start;
		this.end = start + size -1;
	}
	
	public Long getStart() {
		return start;
	}

	public Long getEnd() {
		return end;
	}
}
