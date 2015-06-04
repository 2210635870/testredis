package org.oham.testredis.services.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.oham.testredis.mapper.TbTeamMapper;
import org.oham.testredis.pojo.TbTeam;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.oham.testredis.services.TeamService;
import org.oham.testredis.services.UserService;
import org.oham.testredis.util.DateUtil;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class TeamServiceImpl extends CacheServiceImpl<TbTeam, TbTeamMapper> implements TeamService{

	@Override
	public String getSearchKeySet() {
		return SEARCH_KEY_SET;
	}

	@Override
	protected byte[] saveCache(final TbTeam bean, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		byte[] key = serializeEntityKey(serializer, bean.getId());
		
		BoundHashOperations<Serializable, byte[], byte[]> opts = redisTemplate.boundHashOps(key);
		
		opts.put(serializer.serialize("id"), serializer.serialize(bean.getId().toString()));
		
		opts.put(serializer.serialize("name"), serializer.serialize(bean.getName().toString()));
		
		if( bean.getUserId() != null && bean.getUserId() != 0L) {
			opts.put(serializer.serialize("userId"), serializer.serialize(bean.getUserId().toString()));
		}
		
		connection.hMSet(key, opts.entries());
		
		return key;
	}

	@Override
	protected void updateCache(final TbTeam bean, RedisConnection connection) {
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		byte[] key = serializeEntityKey(serializer, bean.getId());
		
		RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
		
		Boolean hasKey =  conn.exists(key);
		
		if(hasKey) {
			if( !StringUtils.isEmpty(bean.getName()) ) {
				connection.hSet(key, serializer.serialize("name"), serializer.serialize(bean.getName()));
			}
			if( bean.getUserId() != null && bean.getUserId() != 0L ) {
				connection.hSet(key, serializer.serialize("userId"), serializer.serialize(bean.getUserId().toString()));
			} else {
				connection.hSet(key, serializer.serialize("userId"), null);
			}
		} else {
			BoundHashOperations<Serializable, byte[], byte[]> opts = redisTemplate.boundHashOps(key);
			
			opts.put(serializer.serialize("id"), serializer.serialize(bean.getId().toString()));
			opts.put(serializer.serialize("name"), serializer.serialize(bean.getName()));

			if( bean.getUserId() != null && bean.getId() != 0L ) {
				opts.put(serializer.serialize("userId"), serializer.serialize(bean.getUserId().toString()));
			} else {
				opts.put(serializer.serialize("userId"), null);
			}
			
			connection.hMSet(key, opts.entries());
		}
	}

	@Override
	protected void cacheSearchResultIndex(RedisConnection connection, RedisSerializer<String> serializer, byte[] srchKey, List<TbTeam> result) {
		for( TbTeam team : result ) {
			connection.rPush(srchKey, serializer.serialize(CACHE_KEY_ENTITY + team.getId()));
		}
	}
	
	@Override
	protected TbTeam constructEntityFromCache (RedisConnection connection, RedisSerializer<String> serializer, byte[] entityKey) {
		List<byte[]> data = connection.hMGet(entityKey, serializer.serialize("id"),
				serializer.serialize("name"),
				serializer.serialize("userId"));
		
		if(data.get(0) == null) {
			return null;
		}
		
		Long userId = data.get(2) == null ? null : Long.parseLong(serializer.deserialize(data.get(2)));
		
		return new TbTeam(Long.parseLong(serializer.deserialize(data.get(0))), 
			serializer.deserialize(data.get(1)), userId);
	}

	@Override
	protected String getKeyPrefix() {
		return CACHE_KEY_ENTITY;
	}

	// 连接查询的逻辑与单实体查询的差不多，少了selectAll的逻辑
	// 多了的是根据连接的实体表的情况，需要处理多个结果集列表
	@Override
	public List<UserTeamInfo> selectTeamMember(RedisCacheSearchBuilder builder) {
		List<UserTeamInfo> result = new ArrayList<UserTeamInfo>();
		RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
		
		byte[] srchUserInfoKey = serializer.serialize(builder.getCacheKey()+"_user");
		byte[] srchTeamInfoKey = serializer.serialize(builder.getCacheKey()+"_team");
		
		RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
		
		if( !conn.exists(srchUserInfoKey) || !conn.exists(srchTeamInfoKey)) {
			byte[] srchKeySetKey = serializer.serialize(UserService.SEARCH_KEY_SET);
			byte[] teamSrchKeySetKey = serializer.serialize(TeamService.SEARCH_KEY_SET);
			
			List<UserTeamInfo> listFromDB = mainDao.selectTeamMember(builder.getParamsMap());
			
			if( listFromDB.size() > 0 ) {
				conn.sAdd(srchKeySetKey, srchUserInfoKey);
				conn.sAdd(teamSrchKeySetKey, srchUserInfoKey);
				conn.sAdd(srchKeySetKey, srchTeamInfoKey);
				conn.sAdd(teamSrchKeySetKey, srchTeamInfoKey);
				
				for( UserTeamInfo item : listFromDB ) {
					conn.rPush(srchUserInfoKey, serializer.serialize(UserService.CACHE_KEY_ENTITY + item.getId()));
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
			
			Long userId = userData.get(0) == null ? null : Long.parseLong(serializer.deserialize(userData.get(0)));
			String userNm  = userId == null ? null : serializer.deserialize(userData.get(1));
			Date addDate = userId == null ? null : DateUtil.string2Date(serializer.deserialize(userData.get(2)), DateUtil.datetimeFormat);
			info.setId(userId);
			info.setName(userNm);
			info.setAddDate(addDate);
			info.setTeamId(Long.parseLong(serializer.deserialize(teamData.get(0))));
			info.setTeamNm(serializer.deserialize(teamData.get(1)));
			result.add(info);
		}
		
		return result;
	}
}
