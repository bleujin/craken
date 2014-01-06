package net.ion.craken.node.convert.to;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.sample.Dept;
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

