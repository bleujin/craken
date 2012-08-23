package net.ion.craken;

import org.apache.commons.validator.EmailValidator;

import net.ion.craken.simple.SimpleKeyFactory;
import junit.framework.TestCase;

public class TestFirst extends TestCase{

	private Craken craken ;
	
	protected void setUp() throws Exception {
		this.craken = Craken.create();
		craken.globalConfig().transport().clusterName("my-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml") ;
		craken.start() ;
	}
	
	protected void tearDown() throws Exception {
		craken.stop() ;
	}
	
	public void testStart() throws Exception {
		craken.defineLeg(Employee.class, confOverride)
	}

}


class Employee extends AbstractNode {

	private NodeKey empNo ;
	private int age;
	private String name;
	
	private Employee(Integer empNo){
		this.empNo = SimpleKeyFactory.create(empNo) ;
	}
	
	@Override
	public NodeKey key() {
		return empNo;
	}
	
	public String name(){
		return this.name ;
	}
	
	public int age(){
		return this.age ;
	}

	public Employee name(String name){
		this.name = name ;
		return this;
	}
	
	public Employee age(int age){
		this.age = age ;
		return this;
	}
}