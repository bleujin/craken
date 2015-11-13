package net.ion.craken.node.convert.rows;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestIterators extends TestBaseCrud {

	
	public void testUnionAll() throws Exception {
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/user/bleujin").property("userid", "bleujin") ;
				wsession.pathBy("/user/hero").property("userid", "hero") ;
				wsession.pathBy("/group/dev").property("groupid", "dev") ;
				return null;
			}
		}) ;

		AdNodeRows rows = AdNodeRowsBuilder.create(session, session.ghostBy("/user1").children().iterator(), "userid")
			.unionAll(session.pathBy("/group").children().iterator(), "groupid userid")
			.unionAll(session.pathBy("/user").children().iterator(), "userid").build() ;
		
		rows.debugPrint();
	}
}
