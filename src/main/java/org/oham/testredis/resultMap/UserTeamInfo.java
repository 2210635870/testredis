package org.oham.testredis.resultMap;

import org.oham.testredis.pojo.TbUser;

public class UserTeamInfo extends TbUser{
	private static final long serialVersionUID = -817475419106326528L;
	
	private String teamNm;

	public String getTeamNm() {
		return teamNm;
	}

	public void setTeamNm(String teamNm) {
		this.teamNm = teamNm;
	}
	
	@Override
	public String toString() {
		return super.toString() + ", teamId:" + super.getTeamId() + ", teamNm: " + this.teamNm;
	}
}
