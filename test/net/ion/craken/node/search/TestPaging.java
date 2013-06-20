package net.ion.craken.node.search;

import net.ion.craken.node.crud.PredicatedResponse;
import net.ion.craken.node.crud.util.ReadNodePredicate;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.db.Page;

public class TestPaging extends TestBaseSearch {
	
	
	public void testFive() throws Exception {
		session.tranSync(TransactionJobs.dummy("/emp", 20)) ;
		session.tranSync(TransactionJobs.dummy("/dept", 25)) ;
		
		PredicatedResponse result = session.queryRequest("bleujin").ascending("dummy").find().predicated(ReadNodePredicate.belowAt("/emp")).predicated(ReadNodePredicate.page(Page.create(5, 2)));
		assertEquals("bleujin", result.readNode(0).property("name").value()) ;
		assertEquals(5, result.size()) ;
		
		assertEquals(5, result.first().property("dummy").value()) ;
		assertEquals(9, result.last().property("dummy").value()) ;
	}

}
