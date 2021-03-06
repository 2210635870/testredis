package org.oham.testredis.services;

import java.util.List;

import org.oham.testredis.mapper.CommonMapper;
import org.oham.testredis.pojo.TbUser;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.oham.testredis.util.RedisCacheSearchBuilder;



public interface UserService extends CacheService<TbUser, CommonMapper<TbUser>> {

	public static final String CACHE_KEY_ENTITY = CACHE_PREFIX + "tb_user_";
	public static final String SEARCH_KEY_SET = CACHE_KEY_ENTITY + "srchMap";
	
	void forTest();
	
	List<UserTeamInfo> selectUserTeamInfo(RedisCacheSearchBuilder builder);
	
}
