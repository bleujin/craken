package net.ion.craken.node.search;

import net.ion.craken.node.search.util.TransactionJobs;
import net.ion.framework.util.Debug;

public class TestFilter extends TestBaseSearch {
	
	public void testNumericRangeQuery() throws Exception {
		session.tranSync(TransactionJobs.dummyBleujin(20)) ;
		// instantly search

		SearchNodeResponse response = session.createRequest("dummy:[+10 TO +20]").awaitIndex().find();
		Debug.line() ;
		
		response.debugPrint() ;
		assertEquals(10, response.size()) ;
	}
	
	
	
	public void testQueryParse() throws Exception {
		assertEquals(true, "3".matches("-?\\d+")) ;
		assertEquals(true, "-3".matches("-?\\d+")) ;
		assertEquals(false, "4d".matches("-?\\d+")) ;
	}
}

