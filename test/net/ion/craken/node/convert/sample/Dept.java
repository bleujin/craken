package net.ion.craken.node.convert.sample;

import java.io.Serializable;
import java.util.List;


public class Dept implements Serializable {
	private static final long serialVersionUID = 8529181055812294059L;
	private int deptno;
	private String name;
	private Employee manager;
	private List<Employee> emps;
	private Address address;

	
	public Dept name(String name){
		this.name = name ;
		return this ;
	}
	
	public Dept deptno(int deptno){
		this.deptno = deptno ;
		return this ;
	}
	
	public Dept manager(Employee manager){
		this.manager = manager ;
		return this ;
	}
	
	public Dept emps(List<Employee> emps){
		this.emps = emps ;
		return this ;
	}
	
	public Dept address(Address address){
		this.address = address ;
		return this ;
	}
	
	
	public String name() {
		return name;
	}

	public int deptNo() {
		return deptno;
	}

	public Employee manager() {
		return manager;
	}

	public List<Employee> emps() {
		return emps;
	}

	public Address address() {
		return address;
	}
}

