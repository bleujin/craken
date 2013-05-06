package net.ion.craken.node.convert.sample;

import java.io.Serializable;
import java.util.List;
import java.util.Set;


public class Dept implements Serializable {
	private static final long serialVersionUID = 8529181055812294059L;
	private int deptno;
	private String name;
	private Employee manager;
	private List<Employee> emps;
	private Address address;

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

