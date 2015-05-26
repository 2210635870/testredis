package org.oham.testredis.services.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.oham.testredis.mapper.CommonMapper;
import org.oham.testredis.services.CacheService;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public abstract class CacheServiceImpl<T, D extends CommonMapper<T>> implements CacheService<T, CommonMapper<T>>{
	
	@Autowired
	protected RedisTemplate<Serializable, Serializable> redisTemplate;
	
	@Autowired
	protected D mainDao;
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void save(final T bean) {
		redisTemplate.execute(new TxRedisCallback<Boolean>() {
			@Override
			protected Boolean doRedisAction(RedisConnection connection) throws DataAccessException {
				byte[] key = saveCache(bean, connection);
				
				RedisSerializer<String> serializer = redisTemplate.getStringSerializer(); 
				byte[] listKey = serializer.serialize(getKeyPrefix()+CACHE_LIST);
				
				String lastKey = serializer.deserialize(redisTemplate.getConnectionFactory().getConnection().lIndex(listKey, 0));
				String keyStr = serializer.deserialize(key);
				if(!keyStr.equals(lastKey)) {
					connection.lPush(listKey, key);
				}
				
				clearSrchKey(getSearchKeySet(), connection);
				
				return true;
			}
			@Override
			protected Boolean doTxDBAction() throws DataAccessException {
				mainDao.add(bean);
				return true;
			}
		});
	}
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void update(final T bean) {
		redisTemplate.execute(new TxRedisCallback<Boolean>() {
			@Override
			protected Boolean doTxDBAction() throws DataAccessException {
				mainDao.update(bean);
				return true;
			}
			
			@Override
			protected Boolean doRedisAction(RedisConnection connection) throws DataAccessException {
				updateCache(bean, connection);
				clearSrchKey(getSearchKeySet(), connection);
				return true;
			}
		});
	}
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void delete(final Long id) {
		redisTemplate.execute(new TxRedisCallback<Boolean>() {
			@Override
			protected Boolean doTxDBAction() throws DataAccessException {
				mainDao.delete(id);
				return true;
			}
			
			@Override
			protected Boolean doRedisAction(RedisConnection connection) throws DataAccessException {
				RedisSerializer<String> serializer = redisTemplate.getStringSerializer(); 
				byte[] listKey = serializer.serialize(getKeyPrefix()+CACHE_LIST);
				byte[] key =  deleteCache(id, connection);
				connection.lRem(listKey, 1, key);
				clearSrchKey(getSearchKeySet(), connection);
				return true;
			}
		});
	}
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public int deleteByIds (final List<Long> ids){
		return redisTemplate.execute(new TxRedisCallback<Integer>() {
			@Override
			protected Integer doTxDBAction() throws DataAccessException {
				StringBuilder sb = new StringBuilder();
				for( Long id: ids ) {
					sb.append(id).append(",");
				}
				
				int len = sb.length();
				if( len == 0 ) {
					return 0;
				}
				sb.delete(len-1, len);
				
				return mainDao.deleteByIds(sb.toString());
			}
			
			@Override
			protected Integer doRedisAction(RedisConnection connection) throws DataAccessException {
				RedisSerializer<String> serializer = redisTemplate.getStringSerializer(); 
				byte[] listKey = serializer.serialize(getKeyPrefix()+CACHE_LIST);
				List<byte[]> keys = deleteByKeysCache(ids, connection); 
				for( byte[] key : keys ) {
					connection.lRem(listKey, 1, key);
				}
				clearSrchKey(getSearchKeySet(), connection);
				return keys.size();
			}
		});
	}
	
	@Override
	public T get(final Long id) {
		T result = redisTemplate.execute(new RedisCallback<T>() {
			@Override
			public T doInRedis(RedisConnection connection) throws DataAccessException {
				
				return getCache(id, connection);
			}
		});
		
		if( result == null) {
			result = mainDao.get(id);
			if( result != null ) {
				final T bean = result;
				redisTemplate.execute(new TxRedisCallback<Boolean>() {
					@Override
					protected Boolean doTxDBAction() throws DataAccessException {
						return true;
					}
					
					@Override
					protected Boolean doRedisAction(RedisConnection connection) throws DataAccessException {
						saveCache(bean, connection);
						return true;
					}
				});
			}
		}
		return result;
	}
	
	@Override
	public List<T> selectByParams(final RedisCacheSearchBuilder builder) {
		List<T> result = redisTemplate.execute(new RedisCallback<List<T>>() {
			@Override
			public List<T> doInRedis(RedisConnection connection) throws DataAccessException {
				return selectListCacheByParam(builder, connection);
			}
		});
		
		return result;
	}
	
	
	@Override
	public void clearSrchKey(String srchKeyMap, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		Set<byte[]> srchSet = redisTemplate.getConnectionFactory().getConnection().sMembers(serializer.serialize(srchKeyMap));
		
		for( byte[] item : srchSet ) {
			connection.del(item);
		}
	}

	protected abstract byte[] saveCache(T bean, RedisConnection connection);
	
	protected abstract void updateCache(T bean, RedisConnection connection);
	
	protected abstract byte[] deleteCache(Long id, RedisConnection connection);
	
	protected abstract List<byte[]> deleteByKeysCache(List<Long> ids, RedisConnection connection);
	
	protected abstract List<T> selectListCacheByParam(RedisCacheSearchBuilder builder, RedisConnection connection);
	
	protected abstract T getCache(Long id, RedisConnection connection);
	
	protected abstract String getKeyPrefix();
	
	protected abstract class TxRedisCallback<X> implements RedisCallback<X> {

	protected abstract X doRedisAction (RedisConnection connection) throws DataAccessException;
		
	protected abstract X doTxDBAction () throws DataAccessException;
		
		@Override
		public X doInRedis(RedisConnection connection) throws DataAccessException {
			connection.multi();
			X result = null;
			try {
				doTxDBAction();
				result = this.doRedisAction(connection);
			} catch (Exception e) {
				connection.discard();
				throw e;
			}
			connection.exec();
			
			return result;
		}
	}
	
}
