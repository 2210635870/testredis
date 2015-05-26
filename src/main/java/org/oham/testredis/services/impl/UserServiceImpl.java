package org.oham.testredis.services.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.oham.testredis.mapper.TbUserMapper;
import org.oham.testredis.pojo.TbUser;
import org.oham.testredis.services.CacheService;
import org.oham.testredis.services.UserService;
import org.oham.testredis.util.DateUtil;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class UserServiceImpl extends CacheServiceImpl<TbUser, TbUserMapper> implements UserService {
	
	private static final String CACHE_KEY_ENTITY = "tb_user_";
	
	@Override
	protected byte[] saveCache(final TbUser bean, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY + bean.getId());
		BoundHashOperations<Serializable, byte[], byte[]> opts =  redisTemplate.boundHashOps(key);
		
		opts.put(serializer.serialize("id"), serializer.serialize(bean.getId().toString()));
		opts.put(serializer.serialize("name"), serializer.serialize(bean.getName()));
		
		opts.put(serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(bean.getAddDate(), DateUtil.datetimeFormat)));
		connection.hMSet(key, opts.entries());
		
		return key;
	}

	@Override
	protected void updateCache(final TbUser bean, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY + bean.getId());
		
		RedisConnection cnn = redisTemplate.getConnectionFactory().getConnection(); 
		Boolean hasKey = cnn.exists(key);
		
		if(hasKey) {
			
			if( !StringUtils.isEmpty(bean.getName()) ) {
				connection.hSet(key, serializer.serialize("name"), serializer.serialize(bean.getName()));
			}
			
			if( bean.getAddDate() != null ) {
				connection.hSet(key, serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(bean.getAddDate(), DateUtil.datetimeFormat)));
			}
		} else {
			BoundHashOperations<Serializable, byte[], byte[]> opts =  redisTemplate.boundHashOps(key);
			
			opts.put(serializer.serialize("id"), serializer.serialize(bean.getId().toString()));
			opts.put(serializer.serialize("name"), serializer.serialize(bean.getName()));
			
			opts.put(serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(bean.getAddDate(), DateUtil.datetimeFormat)));
			connection.hMSet(key, opts.entries());
		}
	}

	@Override
	protected byte[] deleteCache(final Long id, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY + id);
		connection.del(key);
		
		return key;
	}
	
	@Override
	protected TbUser getCache(final Long id, RedisConnection connection) {
		
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY + id);
		
		List<byte[]> data = connection.hMGet(key, 
				serializer.serialize("id"),
				serializer.serialize("name"),
				serializer.serialize("addDate"));
		
		if( data.get(0) == null ) {
			return null;
		} else {
			return new TbUser(id, serializer.deserialize(data.get(1)), 
					DateUtil.string2Date(serializer.deserialize(data.get(2)), DateUtil.datetimeFormat));
		}
		
	}


	@Override
	public void forTest() {
		redisTemplate.execute(new RedisCallback<Boolean>() {

			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				redisTemplate.multi();
				RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
				byte[] key = serializer.serialize(CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY + 100L);
				BoundHashOperations<Serializable, byte[], byte[]> opts =  redisTemplate.boundHashOps(key);
				
				opts.put(serializer.serialize("id"), serializer.serialize("100"));
				opts.put(serializer.serialize("name"), serializer.serialize("test"));
				
				
				opts.put(serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(new Date(), DateUtil.datetimeFormat)));
				connection.hMSet(key, opts.entries());
				redisTemplate.exec();
				return true;
			}
		});
	}

	@Override
	protected List<byte[]> deleteByKeysCache(List<Long> ids, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		List<byte[]> keys = new ArrayList<byte[]>();
		
		RedisConnection cnn = redisTemplate.getConnectionFactory().getConnection();
		for(Long id : ids) {
			byte[] key = serializer.serialize(CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY + id);
			long res = cnn.del(key);
			if(res != 0L) 
				keys.add(key);
		}
		return keys;
	}

	@Override
	protected List<TbUser> selectListCacheByParam(RedisCacheSearchBuilder builder, RedisConnection connection) {
		List<TbUser> result = new ArrayList<TbUser>();
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] srchKey = serializer.serialize(builder.getCacheKey());

		if( !connection.exists(srchKey) ) {
			connection.sAdd(serializer.serialize(this.getSearchKeySet()), srchKey);
			result = this.mainDao.selectByParams(builder.getParamsMap());
			
			for(TbUser user : result) {
				connection.rPush(srchKey, serializer.serialize(CACHE_PREFIX+CACHE_KEY_ENTITY+user.getId()));
			}
			
			if(builder.getExpiredTime() > 0) {
				connection.expire(srchKey, builder.getExpiredTime());
			}
		} else {
			List<byte[]> entityKeys = connection.lRange(srchKey, 0L, -1L);
			
			for( byte[] entityKey : entityKeys ) {
				List<byte[]> data = connection.hMGet(entityKey, serializer.serialize("id"),
					serializer.serialize("name"),
					serializer.serialize("addDate"));
				
				result.add(new TbUser(Long.parseLong(serializer.deserialize(data.get(0))), 
						serializer.deserialize(data.get(1)), 
						DateUtil.string2Date(serializer.deserialize(data.get(2)), DateUtil.datetimeFormat)));
			}
		}
		
		return result;
	}

	@Override
	protected String getKeyPrefix() {
		return CacheService.CACHE_PREFIX + CACHE_KEY_ENTITY;
	}

	@Override
	public String getSearchKeySet() {
		return CACHE_PREFIX+CACHE_KEY_ENTITY+"srchMap";
	}
	

}
