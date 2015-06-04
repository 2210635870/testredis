package org.oham.testredis.services.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.oham.testredis.mapper.TbUserMapper;
import org.oham.testredis.pojo.TbUser;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.oham.testredis.services.TeamService;
import org.oham.testredis.services.UserService;
import org.oham.testredis.util.DateUtil;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import redis.clients.jedis.Jedis;


@Service
public class UserServiceImpl extends CacheServiceImpl<TbUser, TbUserMapper> implements UserService {
	
	@Resource(name="deleteByIdsLua")
	private DefaultRedisScript<String> deleteByIdsLua;
	
	
	@Override
	protected byte[] saveCache(final TbUser bean, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		byte[] key = serializer.serialize(CACHE_KEY_ENTITY + bean.getId());
		
		BoundHashOperations<Serializable, byte[], byte[]> opts =  redisTemplate.boundHashOps(key);
		
		opts.put(serializer.serialize("id"), serializer.serialize(bean.getId().toString()));
		
		opts.put(serializer.serialize("name"), serializer.serialize(bean.getName()));
		
		opts.put(serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(bean.getAddDate(), DateUtil.datetimeFormat)));
		
		if( bean.getTeamId() != null && bean.getTeamId() != 0L ) {
			opts.put(serializer.serialize("teamId"), serializer.serialize(bean.getTeamId().toString()));
		}
		
		connection.hMSet(key, opts.entries());
		
		return key;
	}

	@Override
	protected void updateCache(final TbUser bean, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] key = serializer.serialize(CACHE_KEY_ENTITY + bean.getId());
		
		RedisConnection cnn = redisTemplate.getConnectionFactory().getConnection(); 
		Boolean hasKey = cnn.exists(key);
		
		if(hasKey) {
			if( !StringUtils.isEmpty(bean.getName()) ) {
				connection.hSet(key, serializer.serialize("name"), serializer.serialize(bean.getName()));
			}
			
			if( bean.getAddDate() != null ) {
				connection.hSet(key, serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(bean.getAddDate(), DateUtil.datetimeFormat)));
			}
			
			if( bean.getTeamId() != null && bean.getTeamId() != 0L ) {
				connection.hSet(key, serializer.serialize("teamId"), serializer.serialize(bean.getTeamId().toString()));
			} else {
				connection.hSet(key, serializer.serialize("teamId"), null);
			}
		} else {
			BoundHashOperations<Serializable, byte[], byte[]> opts =  redisTemplate.boundHashOps(key);
			
			opts.put(serializer.serialize("id"), serializer.serialize(bean.getId().toString()));
			opts.put(serializer.serialize("name"), serializer.serialize(bean.getName()));
			
			opts.put(serializer.serialize("addDate"), serializer.serialize(DateUtil.date2String(bean.getAddDate(), DateUtil.datetimeFormat)));
			
			if( bean.getTeamId() != null && bean.getTeamId() != 0L ) {
				opts.put(serializer.serialize("teamId"), serializer.serialize(bean.getTeamId().toString()));
			} else {
				opts.put(serializer.serialize("teamId"), null);
			}
			connection.hMSet(key, opts.entries());
		}
	}

	@Override
	public void forTest() {
		List<Serializable> list = new ArrayList<Serializable>();
		list.add(redisTemplate.getStringSerializer().serialize("key"));
		
		Jedis jedis = (Jedis)redisTemplate.getConnectionFactory().getConnection().getNativeConnection();

		System.out.println(deleteByIdsLua.getScriptAsString());
		Object result = jedis.eval(deleteByIdsLua.getScriptAsString());
		System.out.println(result);
	}

	@Override
	protected TbUser constructEntityFromCache(RedisConnection connection, RedisSerializer<String> serializer, byte[] entityKey) {
		List<byte[]> data = connection.hMGet(entityKey, serializer.serialize("id"),
				serializer.serialize("name"),
				serializer.serialize("addDate"),
				serializer.serialize("teamId"));
			
		if( data.get(0) == null ) {
			return null;
		}
		Long teamId = data.get(3) == null ? null : Long.parseLong(serializer.deserialize(data.get(3)));
		return new TbUser(Long.parseLong(serializer.deserialize(data.get(0))), 
				serializer.deserialize(data.get(1)), 
				DateUtil.string2Date(serializer.deserialize(data.get(2)), DateUtil.datetimeFormat), teamId);
	}

	@Override
	protected void cacheSearchResultIndex(RedisConnection connection, RedisSerializer<String> serializer, byte[] srchKey, List<TbUser> result) {
		for(TbUser user : result) {
			connection.rPush(srchKey, serializer.serialize(CACHE_KEY_ENTITY + user.getId()));
		}
	}
	
	
	// 连接查询的逻辑与单实体查询的差不多，少了selectAll的逻辑
	// 多了的是根据连接的实体表的情况，需要处理多个结果集列表
	@Override
	public List<UserTeamInfo> selectUserTeamInfo(RedisCacheSearchBuilder builder) {
		List<UserTeamInfo> result = new ArrayList<UserTeamInfo>();
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		byte[] srchUserInfoKey = serializer.serialize(builder.getCacheKey()+"_user");
		byte[] srchTeamInfoKey = serializer.serialize(builder.getCacheKey()+"_team");
		RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
		if( !conn.exists(srchUserInfoKey) || !conn.exists(srchTeamInfoKey)) {
			List<UserTeamInfo> listFromDB = this.mainDao.selectUserTeamInfo(builder.getParamsMap());
			if( listFromDB.size() > 0 ){
				byte[] srchKeySetKey = serializer.serialize(UserService.SEARCH_KEY_SET);
				byte[] teamSrchKeySetKey = serializer.serialize(TeamService.SEARCH_KEY_SET);
				conn.sAdd(srchKeySetKey, srchUserInfoKey);
				conn.sAdd(teamSrchKeySetKey, srchUserInfoKey);
				conn.sAdd(srchKeySetKey, srchTeamInfoKey);
				conn.sAdd(teamSrchKeySetKey, srchTeamInfoKey);
				
				for( UserTeamInfo item : listFromDB ) {
					conn.rPush(srchUserInfoKey, serializeEntityKey(serializer, item.getId()));
					Long teamId = item.getTeamId() != null && item.getTeamId() != 0L ? item.getTeamId() : -1;
					conn.rPush(srchTeamInfoKey, serializer.serialize(TeamService.CACHE_KEY_ENTITY + teamId));
				}
				if( builder.getExpiredTime() > 0 ) {
					conn.expire(srchUserInfoKey, builder.getExpiredTime());
					conn.expire(srchTeamInfoKey, builder.getExpiredTime());
				}
			}
		} 
		
		List<byte[]> userEntityKeys;
		List<byte[]> teamEntityKeys;
		
		if( builder.isPageFromCache() ) {
			long start = builder.getStart();
			long end = builder.getEnd();
			userEntityKeys = conn.lRange(srchUserInfoKey, start, end);
			teamEntityKeys = conn.lRange(srchTeamInfoKey, start, end);
		} else {
			userEntityKeys = conn.lRange(srchUserInfoKey, 0L, -1L);
			teamEntityKeys = conn.lRange(srchTeamInfoKey, 0L, -1L);
		}
		
		for(int i=0; i<userEntityKeys.size(); i++ ) {
			UserTeamInfo info = new UserTeamInfo();
			List<byte[]> userData = conn.hMGet(userEntityKeys.get(i), 
					serializer.serialize("id"),
					serializer.serialize("name"),
					serializer.serialize("addDate"));
			
			List<byte[]> teamData = conn.hMGet(teamEntityKeys.get(i), 
					serializer.serialize("id"),
					serializer.serialize("name"));
			
			info.setId(Long.parseLong(serializer.deserialize(userData.get(0))));
			info.setName(serializer.deserialize(userData.get(1)));
			info.setAddDate(DateUtil.string2Date(serializer.deserialize(userData.get(2)), DateUtil.datetimeFormat));
			Long teamId = teamData.get(0) == null ? null : Long.parseLong(serializer.deserialize(teamData.get(0)));
			String teamNm  = teamId == null ? null : serializer.deserialize(teamData.get(1));
			info.setTeamId(teamId);
			info.setTeamNm(teamNm);
			result.add(info);
		}
		
		return result;
	}

	@Override
	protected String getKeyPrefix() {
		return CACHE_KEY_ENTITY;
	}

	@Override
	public String getSearchKeySet() {
		return SEARCH_KEY_SET;
	}
}
