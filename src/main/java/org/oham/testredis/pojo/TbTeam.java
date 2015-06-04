package org.oham.testredis.pojo;

import java.io.Serializable;

public class TbTeam implements Serializable {
	private static final long serialVersionUID = -2393771699486311382L;
	
	private Long id;
	private String name;
	private Long userId;

	public TbTeam() {}
	
	public TbTeam(Long id, String name, Long userId) {
		this.id = id;
		this.name = name;
		this.userId = userId;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	@Override
	public String toString() {
		return "id: " + this.id + ", name: " + this.name + ", userId: " + this.userId;
	}
}
