package net.ion.craken.node.crud.property;

import java.util.Set;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.ArrayUtil;

public class TestAppend extends TestBaseCrud {

	
	public void testAppend() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").append("no", 1, 2, 3) ;
				return null;
			}
		}).get() ;
		
		
		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals(1, bleujin.property("no").value()) ;
		
		Set set = bleujin.property("no").asSet() ;

		assertEquals(3, set.size()) ;

		assertEquals(true, set.contains(1)) ;
		assertEquals(true, set.contains(2)) ;
		assertEquals(true, set.contains(3)) ;
	}
	
	
	public void testAppendAfterProperty() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("no", 1).append("no", 2, 3) ;
				return null;
			}
		}).get() ;

		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals(1, bleujin.property("no").value()) ;
		
		Set set = bleujin.property("no").asSet() ;

		assertEquals(3, set.size()) ;

		assertEquals(true, set.contains(1)) ;
		assertEquals(true, set.contains(2)) ;
		assertEquals(true, set.contains(3)) ;
	}
	
	public void testPropertyAfterAppend() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").append("no", 2, 3).property("no", 1) ;
				return null;
			}
		}).get() ;

		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals(1, bleujin.property("no").value()) ;
		
		Set set = bleujin.property("no").asSet() ;

		assertEquals(1, set.size()) ;
	}
	
	
	
	
}
