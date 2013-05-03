package net.ion.craken.node.bean;

import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestToRefBean extends TestBaseCrud {

	public void testIncludeRef() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/dev").property("name", "dev").property("deptno", 20)
					.addChild("manager").property("name", "bleujin").property("created", new Date()).parent()
					.refTo("emps", "/emps/jin")
					.refTo("emps", "/emps/hero") ;
				
				wsession.root().addChild("/emps/jin").property("name", "jin") ;
				wsession.root().addChild("/emps/hero").property("name", "hero") ;
				
				return null ;
			}
		}).get() ;
		
		ReadNode dev = session.pathBy("/dev");

		final Dept devBean = dev.toBean(Dept.class);
		assertEquals("dev", devBean.name().toString()) ;
		assertEquals(20, devBean.deptNo()) ;
		
		assertEquals("bleujin", devBean.manager().name()) ;
		assertEquals(2, devBean.emps().size()) ;
	}
	
}


