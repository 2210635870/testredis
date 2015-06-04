package testredis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.oham.testredis.pojo.TbTeam;
import org.oham.testredis.pojo.TbUser;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.oham.testredis.services.CacheService;
import org.oham.testredis.services.TeamService;
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
		TbUser user = new TbUser(0L, "xxx", new Date(), null);
		userService.save(user);
	}
	
	@Test
	public void testUpdate() {
		TbUser user = new TbUser(56L, "oham", new Date(), 1L);
		
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
		ids.add(63L);
		ids.add(64L);
		userService.deleteByIds(ids);
	}
	
	@Test
	public void testSelectByParams() {
		RedisCacheSearchBuilder builder = new RedisCacheSearchBuilder("srch_userList", 0L);
		/*Calendar calendar = Calendar.getInstance();
		calendar.set(2015, 4, 26, 00, 00, 0);
		builder.put("addDateSt", calendar.getTime());
		builder.put("orderBy", "name asc, add_date desc");*/
		
		
		List<TbUser> list =  userService.selectByParams(builder);
		for(TbUser user : list) {
			System.out.println(user);
		}
	}

	@Test
	public void testSelectUserTeamInfo() {
		RedisCacheSearchBuilder builder = new RedisCacheSearchBuilder("srch_userteam_info", 0L);
		
		builder.setPageFromCache(1L, 2L);
		List<UserTeamInfo> list =  userService.selectUserTeamInfo(builder);
		for(UserTeamInfo item : list) {
			System.out.println(item);
		}
	}
	
	@Test
	public void forTest() {
		userService.forTest();
	}
}
