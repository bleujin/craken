package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

public class TestFieldIndexConfig extends TestBaseCrud {

	
	public void testNormalIsUnknown() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/index/2").property("num", 2000).property("string", "2000") ;
				return null;
			}
		}).get() ;
		
		session.workspace().central().newSearcher().search("num:2000").debugPrint() ;
		
		assertEquals(1, session.pathBy("/index").childQuery("num:2000").find().toList().size()) ;
	}
	
	public void testIgnore() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.fieldIndexConfig().ignore("num", "string") ;
				wsession.pathBy("/index/2").property("num", 2000).property("string", "2000") ;
				return null;
			}
		}).get() ;
		
		assertEquals(0, session.pathBy("/index").childQuery("num:2000").find().toList().size()) ;
	}

	
	public void testKeyword() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.fieldIndexConfig().keyword("keyword").text("text") ;
				wsession.pathBy("/index/2").property("keyword", "abcd efg").property("text", "abcd efg") ;
				return null;
			}
		}).get() ;
		assertEquals(1, session.pathBy("/index").childQuery("keyword:abcd efg").find().toList().size()) ;
		assertEquals(0, session.pathBy("/index").childQuery("keyword:abcd").find().toList().size()) ;

		assertEquals(1, session.pathBy("/index").childQuery("text:abcd efg").find().toList().size()) ;
		assertEquals(1, session.pathBy("/index").childQuery("text:abcd").find().toList().size()) ;
	}
	
}
