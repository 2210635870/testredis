package org.oham.testredis.mapper;


import java.util.List;
import java.util.Map;

import org.oham.testredis.pojo.TbUser;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface TbUserMapper extends CommonMapper<TbUser>{

	List<UserTeamInfo> selectUserTeamInfo(Map<String, Object> params);
}
