package net.ion.craken.node.convert.to.type;

import java.util.List;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.sample.Dept;
import net.ion.craken.node.convert.sample.Employee;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestReference extends TestBaseCrud {
	
	
	public void testWhenHasReference() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").property("deptno", 20).property("name", "dev").refTos("manager", "/emps/bleujin") ;
				
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}).get() ;
		
		Dept dept = session.pathBy("/dept/dev").toBean(Dept.class) ;
		assertEquals(20, dept.deptNo()) ;
		assertEquals("dev", dept.name()) ;
		assertEquals("bleujin", dept.manager().name()) ;
		assertEquals(20, dept.manager().age()) ;
	}
	
	public void testWhenHasReferences() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/depts/dev").property("deptno", 20).property("name", "dev").refTos("manager", "/emps/bleujin") ;
				wsession.pathBy("/depts/design").property("deptno", 30).property("name", "design").refTos("manager", "/emps/bleujin") ;
				
				wsession.pathBy("/ion").property("name", "i-on").property("age", 20)
					.refTos("depts", "/depts/dev")	
					.refTos("depts", "/depts/design")
					.refTos("depts", "/depts/unknown");
				return null;
			}
		}).get() ;
		
		Company ion = session.pathBy("/ion").toBean(Company.class) ;
		
		assertEquals(3, ion.depts().size()) ;
		Dept[] depts = ion.depts().toArray(new Dept[0]);
		
		assertEquals("dev", depts[0].name()) ;
		assertEquals("design", depts[1].name()) ;
		assertEquals(true, depts[2].name() == null) ;
		
	}
	
	public void testRefIsOverwrite() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/depts/dev").property("deptno", 20).property("name", "dev").refTos("manager", "/emps/bleujin") ;
				wsession.pathBy("/depts/design").property("deptno", 30).property("name", "design").refTos("manager", "/emps/bleujin") ;
				
				wsession.pathBy("/ion").property("name", "i-on").property("age", 20)
					.refTo("depts", "/depts/dev")	
					.refTo("depts", "/depts/unknown")
					.refTo("depts", "/depts/design") ;
				return null;
			}
		}).get() ;
		
		Company ion = session.pathBy("/ion").toBean(Company.class) ;
		assertEquals(1, ion.depts().size()) ;
		Dept[] depts = ion.depts().toArray(new Dept[0]);
		
		assertEquals("design", depts[0].name()) ;
	}
	
}

class Company {
	
	private List<Dept> depts ;
	private String name ;
	private Employee owner ; 
	
	public List<Dept> depts(){
		return depts ;
	}
	
	public String name(){
		return name ;
	}
	
	public Employee owner(){
		return owner ;
	}
}
