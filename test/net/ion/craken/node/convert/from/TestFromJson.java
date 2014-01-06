package net.ion.craken.node.convert.from;

import java.util.Date;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.sample.Dept;
import net.ion.craken.node.convert.sample.Employee;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;

public class TestFromJson extends TestBaseCrud {

	
	
	public void testFlat() throws Exception {
		Dept dept = new Dept().deptno(20).name("dev");
		final JsonObject json = JsonParser.fromObject(dept).getAsJsonObject();
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").fromJson(json) ;
				return null ;
			}
		}) ;
		
		assertEquals("dev", session.pathBy("/dept/dev").property("name").value()) ; 
		assertEquals(20, session.pathBy("/dept/dev").property("deptno").value()) ; 
		
	}
	
	public void testChild() throws Exception {
		Dept dept = new Dept().deptno(20).name("dev").manager(new Employee().name("bleujin").age(20).created(new Date())) ;
		final JsonObject json = JsonParser.fromObject(dept).getAsJsonObject();
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").fromJson(json) ;
				return null ;
			}
		}) ;
		
		assertEquals("dev", session.pathBy("/dept/dev").property("name").value()) ; 
		assertEquals(20, session.pathBy("/dept/dev").property("deptno").value()) ; 
		
		assertEquals("bleujin", session.pathBy("/dept/dev").child("manager").property("name").value()) ;
	}
	
	
	public void testBasic() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "jin") ;
				return null ;
			}
		}) ;
		
		assertEquals("jin", session.pathBy("/users/bleujin").property("name").stringValue()) ;
	}
	

	public void testAddRelation() throws Exception {
		Dept dept = new Dept().deptno(20).name("dev").manager(new Employee().name("bleujin").age(20).created(new Date())) ;
		final JsonObject json = JsonParser.fromObject(dept).getAsJsonObject();
		json.put("@register", "/users/bleujin") ; // add relation
		
		Debug.line(json) ;
		
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "jin") ;
				wsession.pathBy("/dept/dev").fromJson(json) ;
				return null ;
			}
		}) ;
		
		assertEquals("dev", session.pathBy("/dept/dev").property("name").value()) ; 
		assertEquals(20, session.pathBy("/dept/dev").property("deptno").value()) ; 
		assertEquals("bleujin", session.pathBy("/dept/dev").child("manager").property("name").value()) ;
		
		assertEquals("jin", session.pathBy("/dept/dev").ref("register").property("name").value()) ;
		
	}
	

	
	
}
