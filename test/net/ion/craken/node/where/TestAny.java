package net.ion.craken.node.where;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestAny extends TestBaseCrud{

	
	public void testAny() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").append("nick", "bleu", "jin", "hero") ;
				return null;
			}
		}) ;
		
		
		assertEquals(0, session.root().childQuery("").in("nick", "air").find().totalCount()) ;
		assertEquals(1, session.root().childQuery("").in("nick", "air", "jin").find().totalCount()) ;
		assertEquals(1, session.root().childQuery("").in("nick", "air", "kkk", "hero").find().totalCount()) ;
		
	}
}
