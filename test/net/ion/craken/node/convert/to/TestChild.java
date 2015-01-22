package net.ion.craken.node.convert.to;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.sample.Dept;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestChild extends TestBaseCrud {

	public void testWhenHasChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").property("deptno", 20).property("name", "dev")
					.child("manager").property("name", "bleujin").property("age", 20).parent() 
					.child("address").property("city", "seoul").property("bun", 0) ;
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
		
		
		Debug.line(session.pathBy("/dept/dev").childrenNames()) ;
		
	}
	
	public void testWhenHasGrandChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/dev").property("deptno", 20).property("name", "dev")
					.child("address").property("city", "seoul").property("bun", 0).parent()
					.child("manager").property("name", "bleujin").property("age", 20)
						.child("pair").property("name", "hero").property("age", 30) ;
				return null;
			}
		}).get() ;
		
		Dept dept = session.pathBy("/dept/dev").toBean(Dept.class) ;
		assertEquals("hero", dept.manager().pair().name()) ;
		assertEquals(30, dept.manager().pair().age()) ;
		assertEquals(30, dept.manager().pair().age()) ;
	}
}

