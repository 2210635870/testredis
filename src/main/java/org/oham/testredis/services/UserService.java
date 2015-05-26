package org.oham.testredis.services;

import org.oham.testredis.mapper.CommonMapper;
import org.oham.testredis.pojo.TbUser;



public interface UserService extends CacheService<TbUser, CommonMapper<TbUser>> {

	void forTest();
	
}
