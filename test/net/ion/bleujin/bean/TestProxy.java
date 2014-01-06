package net.ion.bleujin.bean;

import java.lang.reflect.Proxy;
import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.bean.ProxyBean;
import net.ion.craken.node.convert.bean.TypeStrategy;
import net.ion.craken.node.convert.sample.Dept;
import net.ion.craken.node.convert.sample.Employee;
import net.ion.craken.node.convert.sample.FlatPerson;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestProxy extends TestBaseCrud {

	
	public void testCall() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}).get() ;
		
		ReadNode node = session.pathBy("/bleujin");
		
		ProxyHandler handler = new ProxyHandler(FlatPerson.class, node) ;
		ProxyIf bean = (ProxyIf) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { ProxyIf.class }, handler);
		
		
		Debug.line(bean, bean.getClass().getDeclaredFields()) ;
		
//		Debug.line(JsonParser.fromObject(bean).getAsJsonObject()) ; 
	}
	
	
	public void testCGLib() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/depts/dev").property("deptno", 20).property("name", "dev")
					.addChild("manager").property("name", "bleujin").property("age", 20).property("created", new Date()) ;
				return null;
			}
		}).get() ;
		
		ReadNode node = session.pathBy("/depts/dev");
		Dept dept = ProxyBean.create(TypeStrategy.DEFAULT, node, Dept.class) ;
		
		assertEquals(20, dept.deptNo()) ;
		assertEquals("dev", dept.name().toString()) ;
		
		Employee manager = dept.manager();
		
		assertEquals("bleujin", manager.name()) ;
		assertEquals(20, manager.age()) ;
		assertEquals(true, manager.created() != null) ;
	}
	

}




