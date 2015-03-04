package net.ion.craken.node.ref;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestRefNode extends TestBaseCrud {


	
	public void testUsing() throws Exception {
		session.tranSync(TransactionJobs.dummy("/emp", 10)) ;
		session.tranSync(TransactionJobs.dummy("/dept", 5)) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emp/1").refTos("dept", "/dept/1").refTos("dept", "/dept/2") ;
				return null;
			}
		}).get() ;

		
		ReadNode refNode = session.pathBy("/emp/1").ref("dept") ;
		assertEquals(1, refNode.property("dummy").value()) ;
	}
	
	public void testRefsOrder() throws Exception {
		session.tranSync(TransactionJobs.dummy("/emp", 10)) ;
		session.tranSync(TransactionJobs.dummy("/dept", 5)) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emp/1").refTos("dept", "/dept/1").refTos("dept", "/dept/2") ;
				return null;
			}
		}).get() ;

		List<ReadNode> refs = session.pathBy("/emp/1").refs("dept").toList() ;
		
		assertEquals(2, refs.size()) ;
		assertEquals(1, refs.get(0).property("dummy").value()) ;
		assertEquals(2, refs.get(1).property("dummy").value()) ;
	}

	
	
	
}
