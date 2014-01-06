package net.ion.craken.node.convert.sample;

import java.util.Date;

import net.ion.bleujin.bean.ProxyIf;

public class FlatPerson implements ProxyIf {
	private static final long serialVersionUID = 3779755512963230596L;
	private String name;
	private int age;
	private Date created;

	public String name() {
		return name;
	}

	public int age() {
		return age;
	}

	public Date created() {
		return created;
	}

}