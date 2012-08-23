package net.ion.craken;

import net.ion.craken.simple.SimpleKeyFactory;

public class EmpNode extends AbstractNode {

	private static final long serialVersionUID = 5166614064200120569L;
	private final String empNo ;
	
	private int age;
	private String name;
	
	private EmpNode(String empNo){
		this.empNo = empNo ;
	}
	
	@Override
	public NodeKey key() {
		return SimpleKeyFactory.create(empNo);
	}

	public EmpNode name(String name) {
		this.name = name ;
		return this;
	}

	public EmpNode age(int age) {
		this.age = age ;
		return this;
	}

	public String name() {
		return name ;
	}
	
}


