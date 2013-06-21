package net.ion.craken.node.search;

import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.search.filter.TermFilter;

public class TestFilter extends TestBaseSearch {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(TransactionJobs.dummyEmp(20)) ;
	}
	
	public void testNumericRangeQuery() throws Exception {
		ChildQueryResponse response = session.queryRequest("dummy:[+10 TO +20]").find();
		Debug.line() ;
		
		response.debugPrint() ;
		assertEquals(10, response.size()) ;
	}
	
	public void testAndFilter() throws Exception {
		session.queryRequest("").filter(new TermFilter("name", "bleujin")).lt("dummy", 10).find().debugPrint() ;
	}
	
	
	
	public void testQueryParse() throws Exception {
		assertEquals(true, "3".matches("-?\\d+")) ;
		assertEquals(true, "-3".matches("-?\\d+")) ;
		assertEquals(false, "4d".matches("-?\\d+")) ;
	}
}

