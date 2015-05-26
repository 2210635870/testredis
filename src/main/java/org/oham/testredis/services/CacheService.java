package org.oham.testredis.services;

import java.util.List;

import org.oham.testredis.mapper.CommonMapper;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.data.redis.connection.RedisConnection;

public interface CacheService<T, D extends CommonMapper<T>> {
	
	public static final String CACHE_PREFIX = "testredis:";
	public static final String CACHE_LIST = "list";
	
	public void save(final T bean);
	
	public void update(final T bean);
	
	public void delete(final Long id);
	
	public int deleteByIds (final List<Long> ids);
	
	public T get(final Long id) ;
	
	public List<T> selectByParams(RedisCacheSearchBuilder builder);
	
	public String getSearchKeySet();
	
	public void clearSrchKey(String srchKeyMap, RedisConnection connection);
	
}
