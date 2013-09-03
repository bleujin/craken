package net.ion.craken.node.search;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;

public class TestSearchChild extends TestBaseSearch {
	
	public void testQuery() throws Exception {
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("name", "bleujin")
					.addChild("hero").property("name", "hero")
						.addChild("jin").property("name", "jin");
				return null;
			}
		}) ;

		assertEquals(3, session.queryRequest("").find().size()) ;
		assertEquals(1, session.pathBy("/bleujin").childQuery("").find().size()) ;
		assertEquals(2, session.pathBy("/bleujin").childQuery("", true).find().size()) ;
		assertEquals(1, session.pathBy("/bleujin/hero").childQuery("", true).find().size()) ;
		assertEquals(1, session.pathBy("/bleujin/hero").childQuery("").find().size()) ;

		assertEquals(1, session.pathBy("/bleujin").childQuery("jin", true).find().size()) ;
		assertEquals(1, session.root().childQuery("jin", true).find().size()) ;
	
	}
	

}
