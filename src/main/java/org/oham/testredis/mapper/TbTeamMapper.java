package org.oham.testredis.mapper;

import java.util.List;
import java.util.Map;

import org.oham.testredis.pojo.TbTeam;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface TbTeamMapper extends CommonMapper<TbTeam>{

	List<UserTeamInfo> selectTeamMember(Map<String, Object> params);
}
