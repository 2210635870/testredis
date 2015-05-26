package testredis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.oham.testredis.pojo.TbUser;
import org.oham.testredis.services.CacheService;
import org.oham.testredis.services.UserService;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { 
		"/applicationContext.xml",
		"/mybatis.xml"
	})
public class TestUserService extends AbstractJUnit4SpringContextTests {

	@Autowired
	private UserService userService;
	
	@Test
	public void testAdd() {
		
		TbUser user = new TbUser(0L, "gouzi", new Date());
		userService.save(user);
	}
	
	@Test
	public void testUpdate() {
		TbUser user = new TbUser(53L, "oham", new Date());
		
		userService.update(user);
	}
	
	
	@Test
	public void testGet() {
		Long id = 55L;
		TbUser user = userService.get(id);
		System.out.println(user);
	}
	
	
	@Test
	public void testDelete() {
		Long id = 55L;
		userService.delete(id);
	}
	
	@Test
	public void testDeleteByIds() {
		List<Long> ids = new ArrayList<Long>();
		ids.add(53L);
		ids.add(54L);
		userService.deleteByIds(ids);
	}
	
	@Test
	public void testSelectByParams() {
		RedisCacheSearchBuilder builder = new RedisCacheSearchBuilder(CacheService.CACHE_PREFIX+"srch_userList", 0L);
		Calendar calendar = Calendar.getInstance();
		calendar.set(2015, 4, 26, 00, 00, 0);
		builder.put("addDateSt", calendar.getTime());
		builder.put("orderBy", "name asc, add_date desc");
		
		
		List<TbUser> list =  userService.selectByParams(builder);
		for(TbUser user : list) {
			System.out.println(user);
		}
		
	}
	
	
	@Test
	public void forTest() {
		userService.forTest();
	}
	
	
}
