package net.ion.craken.node.convert.to;

import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.sample.Dept;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestToRefBean extends TestBaseCrud {

	public void testIncludeRef() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/dev").property("name", "dev").property("deptno", 20)
					.child("manager").property("name", "bleujin").property("created", new Date()).parent()
					.refTos("emps", "/emps/jin")
					.refTos("emps", "/emps/hero") ;
				
				wsession.root().child("/emps/jin").property("name", "jin") ;
				wsession.root().child("/emps/hero").property("name", "hero") ;
				
				return null ;
			}
		}).get() ;
		
		ReadNode dev = session.pathBy("/dev");

		final Dept devBean = dev.toBean(Dept.class);
		assertEquals("dev", devBean.name().toString()) ;
		assertEquals(20, devBean.deptNo()) ;
		
		assertEquals("bleujin", devBean.manager().name()) ;
		Debug.line(devBean.emps()) ;
		assertEquals(2, devBean.emps().size()) ;
	}
	
}


