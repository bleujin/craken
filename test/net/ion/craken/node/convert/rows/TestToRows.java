package net.ion.craken.node.convert.rows;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.search.TestBaseSearch;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Row;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public class TestToRows extends TestBaseSearch {
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(50)) {
					wsession.root().child("/board1/" +  i).property("index", i).property("name", "board1").property("writer",  "hijin").child("address").property("city", "seoul")
					.parent().refTos("register", "/users/bleujin").root().child("/board1").property("name", "free") ;
				}
				for (int i : ListUtil.rangeNum(50)) {
					wsession.root().child("/board2/" +  i).property("index", i).property("name", "board2").property("writer", "hero") ;
				}
				return null;
			}
		}) ;
		
		
	}

	public void testFirst() throws Exception {
		long start = System.currentTimeMillis() ;
		session.pathBy("/board1").childQuery("").descending("index").skip(10).offset(10).find().debugPrint() ;
		Debug.line(System.currentTimeMillis() - start) ;
	}
	
	public void testFrom() throws Exception{
		Rows rows = session.pathBy("/board1").children().toAdRows("name, writer");

		rows.debugPrint() ;
	}


	
}
