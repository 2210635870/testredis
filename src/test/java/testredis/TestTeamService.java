package testredis;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.oham.testredis.pojo.TbTeam;
import org.oham.testredis.resultMap.UserTeamInfo;
import org.oham.testredis.services.CacheService;
import org.oham.testredis.services.TeamService;
import org.oham.testredis.util.RedisCacheSearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { 
		"/applicationContext.xml",
		"/mybatis.xml"
	})
public class TestTeamService extends AbstractJUnit4SpringContextTests{

	@Autowired
	private TeamService teamService;
	
	@Test
	public void testAdd() {
		TbTeam team = new TbTeam(0L, "teamX", null);
		
		teamService.save(team);
	}
	
	@Test
	public void testUpdate() {
		TbTeam team = new TbTeam(1L, "team1", 62L);
		teamService.update(team);
	}
	
	@Test
	public void testGet() {
		Long id = 1L;
		TbTeam team = teamService.get(id);
		System.out.println(team);
	}
	
	@Test
	public void testDelete() {
		long id = 3L;
		teamService.delete(id);
		
	}
	
	@Test
	public void testDeleteByIds() {
		List<Long> ids = new ArrayList<Long>();
		ids.add(4L);
		ids.add(5L);
		teamService.deleteByIds(ids);
	}
	
	@Test
	public void testSelectByParams() {
		RedisCacheSearchBuilder builder = new RedisCacheSearchBuilder("srch_teamList", 0l);
		
		builder.put("nameLike", "tea");
		
		builder.setPageFromCache(1L, 1L);
		List<TbTeam> list = teamService.selectByParams(builder);
		
		for( TbTeam team: list ) {
			System.out.println(team);
		}
	}
	
	@Test
	public void testSelectTeamMembers() {
		RedisCacheSearchBuilder builder = new RedisCacheSearchBuilder("srch_teammember_info", 0l);
		builder.setPageFromCache(2L, 2L);
		List<UserTeamInfo> list = teamService.selectTeamMember(builder);
		
		for( UserTeamInfo info: list ) {
			System.out.println(info);
		}
	}
	
	
	
}
