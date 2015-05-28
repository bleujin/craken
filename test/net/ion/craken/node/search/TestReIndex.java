package net.ion.craken.node.search;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestReIndex extends TestBaseCrud{

	
	public void testNoIndex() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().ignoreIndex() ;
				wsession.pathBy("/emp/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/emp/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		
		assertEquals(0, session.root().childQuery("").find().totalCount()) ;
		
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp").reindex(true) ;
				return null;
			}
		}).get() ;
		
		assertEquals(3, session.root().childQuery("", true).find().totalCount()) ;
	}
}
