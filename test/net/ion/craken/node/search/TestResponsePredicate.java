package net.ion.craken.node.search;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.PredicatedResponse;
import net.ion.craken.node.crud.util.ResponsePredicates;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;

public class TestResponsePredicate extends TestBaseSearch {

	public void testBelow() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/emp/bleujin").property("name", "bleujin").property("job", "dev") ;
				wsession.root().addChild("/emp/hero").property("name", "hero") ;
				wsession.root().addChild("/dept/dev").property("name", "dev") ;
				wsession.root().addChild("/emp/jin").property("name", "jin").property("job", "dev") ;
				return null;
			}
		}) ;
		
		assertEquals(3, session.queryRequest("dev").find().size()) ;
		
		assertEquals(0, session.root().childQuery("dev", false).find().size()) ;
		assertEquals(1, session.pathBy("/dept").childQuery("dev", false).find().size()) ;
		
		List<ReadNode> list = session.queryRequest("dev").descending("name").find().predicated(ResponsePredicates.belowAt("/emp")).toList();
		Debug.line(list) ;
	}
	
	
	public void testWhere() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/emp/bleujin").property("name", "bleujin").property("job", "dev").property("age", 20) ;
				wsession.root().addChild("/emp/hero").property("name", "hero").property("age", 21) ;
				wsession.root().addChild("/dept/dev").property("name", "dev").property("age", 22) ;
				wsession.root().addChild("/emp/jin").property("name", "jin").property("job", "dev").property("age", 23) ;
				return null;
			}
		}) ;
		final PredicatedResponse response = session.queryRequest("").descending("name").find().predicated(ResponsePredicates.where("age between 20 and 22 and this.name in ('hero', 'dev')"));
		response.debugPrint() ;
		assertEquals(2, response.totoalCount()) ;
	}
	
	
	public void testFive() throws Exception {
		session.tranSync(TransactionJobs.dummy("/emp", 20)) ;
		session.tranSync(TransactionJobs.dummy("/dept", 25)) ;
		
		PredicatedResponse result = session.queryRequest("bleujin").ascending("dummy").find()
			.predicated(ResponsePredicates.belowAt("/emp"))
			.predicated(ResponsePredicates.page(Page.create(5, 2)));
		assertEquals("bleujin", result.readNode(0).property("name").value()) ;
		assertEquals(5, result.size()) ;
		
		assertEquals(5, result.first().property("dummy").value()) ;
		assertEquals(9, result.last().property("dummy").value()) ;
	}
	
	
	
	
}
