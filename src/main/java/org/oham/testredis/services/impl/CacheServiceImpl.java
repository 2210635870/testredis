package org.oham.testredis.services.impl;

import java.io.Serializable;
import java.util.ArrayList;
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
				
				// 把这实体的key存在一个列表里是为了selectAll用的，缓存分页也用到
				// 可以看看selectListCacheByParam的实现， 当没有任何参数的时候就相当于selectAll
				if(!keyStr.equals(lastKey)) {
					connection.lPush(listKey, key);
				}
				
				// 实体表的任何数据变动都会影响查询的结果，所以这里要清除查询缓存结果
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
				// 实体表的任何数据变动都会影响查询的结果，所以这里要清除查询缓存结果
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
				// 实体表的任何数据变动都会影响查询的结果，所以这里要清除查询缓存结果
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
				// 实体表的任何数据变动都会影响查询的结果，所以这里要清除查询缓存结果
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
		
		//  这里，若缓存无命中，则尝试从数据库获取，若数据库有数据，则将数据进行缓存，
		// 这样的目的是方便项目部署时，对实体数据建立缓存
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
	
	
	/**
	 * 清空 查询缓存方法
	 */
	@Override
	public void clearSrchKey(String srchKeyMap, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		byte[] srchkeyMapKey = serializer.serialize(srchKeyMap);
		Set<byte[]> srchSet = redisTemplate.getConnectionFactory().getConnection().sMembers(srchkeyMapKey);
		
		List<byte[]> temp = new ArrayList<byte[]>();
		for( byte[] item : srchSet ) {
			connection.del(item);
			temp.add(item);
		}
		
		for(byte[] item : temp) {
			connection.sRem(srchkeyMapKey, item);
		}
	}
	
	protected byte[] serializeEntityKey(RedisSerializer<String> serializer, Object key) {
		return serializer.serialize(getKeyPrefix() + key);
	}
	
	protected byte[] deleteCache(Long id, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(getKeyPrefix() + id);
		connection.del(key);
		return key;
	}
	
	protected List<byte[]> deleteByKeysCache(List<Long> ids, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		List<byte[]> keys = new ArrayList<byte[]>();
		
		RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
		
		for(Long id : ids) {
			byte[] key = serializer.serialize(getKeyPrefix() + id);
			long res = conn.del(key);
			if(res != 0L) {
				keys.add(key);
			}
		}
		return keys;
	}
	
	protected List<T> selectListCacheByParam(RedisCacheSearchBuilder builder, RedisConnection connection) {
		List<T> result = new ArrayList<T>();
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		if( builder.getParamsMap().size() == 0) {  // 这里若无参数，相当于selectAll
			byte[] entityList = serializer.serialize(getKeyPrefix()+CACHE_LIST);
			
			List<byte[]> entityKeys;
			if( builder.isPageFromCache() ) { // 使用 缓存分页
				long start = builder.getStart();
				long end = builder.getEnd();
				entityKeys = connection.lRange(entityList, start, end);
			} else {
				entityKeys = connection.lRange(entityList, 0L, -1L);
			}
			if( entityKeys.size() == 0 ) { //缓存未命中，则从数据库查询
				if(builder.isPageFromCache()) {
					builder.put("start", builder.getStart());
					builder.put("size", builder.getEnd() + 1 - builder.getStart()); //需要进行逆运算
				}
				return this.mainDao.selectByParams(builder.getParamsMap());
			} else {
				for( byte[] entityKey: entityKeys ) {
					T entity = constructEntityFromCache(connection, serializer, entityKey);
					if( entity != null ) {
						result.add(entity);
					}
				}
			}
		} else {
			byte[] srchKey = serializer.serialize(builder.getCacheKey());//通过RedisCacheSearchBuilder获取本次查询的key
			
			if( !connection.exists(srchKey) ) {//缓存未命中，则从db 查询， 然后再对结果进行缓存，这样就保证缓存数据的同步性
				List<T> listFromDB = this.mainDao.selectByParams(builder.getParamsMap());
				if(listFromDB.size() > 0) {
					connection.sAdd(serializer.serialize(this.getSearchKeySet()), srchKey);
					cacheSearchResultIndex(connection, serializer, srchKey, listFromDB);
					if( builder.getExpiredTime() > 0 ) {
						connection.expire(srchKey, builder.getExpiredTime());
					}
				}
			} 
			
			//  从缓存中获取数据
			List<byte[]> entityKeys;
			if( builder.isPageFromCache() ) {
				long start = builder.getStart();
				long end = builder.getEnd();
				entityKeys = connection.lRange(srchKey, start, end);
			} else {
				entityKeys = connection.lRange(srchKey, 0L, -1L);
			}
			
			for( byte[] entityKey: entityKeys ) {
				T entity = constructEntityFromCache(connection, serializer, entityKey);
				if( entity != null ) {
					result.add(entity);
				}
			}
		}
		
		return result;
	}
	
	protected T getCache(Long id, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(getKeyPrefix() + id);
		return constructEntityFromCache(connection, serializer, key);
	}
	
	protected abstract String getKeyPrefix();
	
	protected abstract T constructEntityFromCache (RedisConnection connection, RedisSerializer<String> serializer, byte[] entityKey);
	
	protected abstract void cacheSearchResultIndex (RedisConnection connection, RedisSerializer<String> serializer, byte[] srchKey, List<T> result);
	
	protected abstract byte[] saveCache(T bean, RedisConnection connection);
	
	protected abstract void updateCache(T bean, RedisConnection connection);
	
	
	/**
	 *  自己写了个callback，目的是使得数据库与缓存有一定事务机制，尽可能保持同步
	 *  之前想过写个拦截器，但redis缓存的事务必须是对链接的，不能对redisTemplate，
	 *  一时想不出怎么组织，无奈之下唯有此下策
	 */
	protected abstract class TxRedisCallback<X> implements RedisCallback<X> {

		protected abstract X doRedisAction (RedisConnection connection) throws DataAccessException;
		
		protected abstract X doTxDBAction () throws DataAccessException;
		
		@Override
		public X doInRedis(RedisConnection connection) throws DataAccessException {
			connection.multi(); // 缓存事务控制
			X result = null;
			try {
				doTxDBAction();
				result = this.doRedisAction(connection);
			} catch (Exception e) {
				connection.discard(); // 缓存事务撤销
				throw e;
			}
			connection.exec(); // 缓存事务提交
			
			return result;
		}
	}
}
