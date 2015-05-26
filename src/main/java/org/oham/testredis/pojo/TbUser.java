package org.oham.testredis.pojo;

import java.io.Serializable;
import java.util.Date;

public class TbUser implements Serializable{
	
	private static final long serialVersionUID = -3885872424123013404L;

	private Long id;
	
	private String name;
	
	private Date addDate;

	public TbUser() {}
	
	public TbUser(Long id, String name, Date addDate) {
		this.id = id;
		this.name = name;
		this.addDate = addDate;
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

	public Date getAddDate() {
		return addDate;
	}

	public void setAddDate(Date addDate) {
		this.addDate = addDate;
	}
	
	@Override
	public String toString() {
		return "id: " + this.id + ", name: " + this.name + ", addDate: " + this.addDate;
	}
}
