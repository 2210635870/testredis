package org.oham.testredis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;


public interface CommonMapper<T> {

	int add(T bean);
	
	int update(T bean);
	
	int delete(Long id);
	
	int deleteByIds(@Param("ids")String ids);
	
	T get(Long id);
	
	List<T> selectByParams(Map<String, Object> params);
}
