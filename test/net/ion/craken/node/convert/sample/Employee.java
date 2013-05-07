package net.ion.craken.node.convert.sample;

import java.util.Date;

public class Employee {
	private String name;
	private int age;
	private Date created;
	private Employee pair;

	
	public Employee name(String name){
		this.name = name ;
		return this ;
	}
	
	public Employee age(int age){
		this.age = age ;
		return this ;
	}
	
	public Employee created(Date created){
		this.created = created ;
		return this ;
	}
	
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