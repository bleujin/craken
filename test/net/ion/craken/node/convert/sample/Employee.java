package net.ion.craken.node.convert.sample;

import java.util.Date;

public class Employee {
	private String name;
	private int age;
	private Date created;
	private Employee pair;

	public String name() {
		return name;
	}

	public int age() {
		return age;
	}

	public Date created() {
		return created;
	}

	public Employee pair() {
		return pair;
	}
}