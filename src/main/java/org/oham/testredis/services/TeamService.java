package org.oham.testredis.services;

import java.util.List;

import org.oham.testredis.mapper.CommonMapper;
import org.oham.testredis.pojo.TbTeam;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.oham.testredis.util.RedisCacheSearchBuilder;

public interface TeamService extends CacheService<TbTeam, CommonMapper<TbTeam>> {
	public static final String CACHE_KEY_ENTITY = CACHE_PREFIX + "tb_team_";
	public static final String SEARCH_KEY_SET = CACHE_KEY_ENTITY + "srchMap";
	
	List<UserTeamInfo> selectTeamMember(RedisCacheSearchBuilder builder);
}
