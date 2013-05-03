package net.ion.craken.node.bean.type;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.base.Objects.ToStringHelper;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.bean.ProxyIf;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestChild extends TestBaseCrud {

	public void testWhenHasChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").property("deptno", 20).property("name", "dev")
					.addChild("manager").property("name", "bleujin").property("age", 20).parent() 
					.addChild("address").property("city", "seoul").property("bun", 0) ;
				return null;
			}
		}).get() ;
		
		
		Dept dept = session.pathBy("/dept/dev").toBean(Dept.class) ;
		assertEquals(20, dept.deptNo()) ;
		assertEquals("dev", dept.name()) ;
		assertEquals("bleujin", dept.manager().name()) ;
		assertEquals(20, dept.manager().age()) ;
		
		assertEquals("seoul", dept.address().city()) ;
		assertEquals(0, dept.address().bun()) ;
	}
	
	public void testWhenHasGrandChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").property("deptno", 20).property("name", "dev")
					.addChild("address").property("city", "seoul").property("bun", 0).parent()
					.addChild("manager").property("name", "bleujin").property("age", 20)
						.addChild("pair").property("name", "hero").property("age", 30) ;
				return null;
			}
		}).get() ;
		
		Dept dept = session.pathBy("/dept/dev").toBean(Dept.class) ;
		assertEquals("hero", dept.manager().pair().name()) ;
		assertEquals(30, dept.manager().pair().age()) ;
		assertEquals(30, dept.manager().pair().age()) ;
	}
}


class Dept implements Serializable {
	private int deptno ;
	private String name ;
	private Employee manager ;
	private Address address ;
	
	public String name(){
		return name ;
	}
	
	public Address address() {
		return address;
	}

	public int deptNo(){
		return deptno ;
	}
	
	public Employee manager(){
		return manager ;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
}


class Address {
	private String city ;
	private int bun ;
	
	public String city(){
		return city ;
	}
	public int bun(){
		return bun ;
	}
	
}

class Employee {
	private String name ;
	private int age ;
	private Date created ;
	private Employee pair ;
	
	public String name() {
		return name;
	}

	public int age() {
		return age;
	}
	
	public Date created(){
		return created ;
	}
	
	public Employee pair(){
		return pair ;
	}
}