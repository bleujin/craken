package net.ion.craken.node.problem.store;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestTransactionLog extends TestBaseCrud {

	
	public void testWhenPathBy() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.pathBy("/bleujin/" + i).property("index", i).property("name", "bleujin") ;
				}
				return null;
			}
		}) ;
		
	}
}
