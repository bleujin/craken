package net.ion.craken.node.search;


import java.util.List;

import com.google.common.base.Predicate;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.search.util.ReadNodePredicate;
import net.ion.craken.node.search.util.TransactionJobs;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.IKeywordField;

public class TestSearch extends TestBaseSearch {
	
	public void testQuery() throws Exception {
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("age", 20).property("name", "bleujin") ;
				return null;
			}
		}) ;

		// instantly search
		SearchNodeResponse response = session.awaitIndex().createRequest("").find();
		assertEquals(1, response.size()) ;
	}
	
	
	public void testPaging() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(500)) {
					wsession.root().addChild("" +  i).property("index", i).property("name", "group") ;
				}
				return null;
			}
		}) ;
		
		SearchNodeResponse response = session.awaitIndex().createRequest("").skip(10).offset(10).ascending("index").find();
		assertEquals(10, response.size()) ;
		assertEquals(500, response.totalCount()) ;
	}
	
	
	public void testSearchNode() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/emp/bleujin").property("name", "bleujin").property("job", "dev") ;
				wsession.root().addChild("/emp/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		
		SearchNodeResponse response = session.awaitIndex().createRequest("bleujin").find();
		assertEquals(1, response.size()) ;

		ReadNode firstNode = response.first();
		assertEquals("dev", firstNode.property("job").value()) ;
		
	}
	
	
	public void testOnRemove() throws Exception {
		session.tranSync(TransactionJobs.dummyBleujin(10)) ;
		assertEquals(10, session.awaitIndex().createRequest("bleujin").find().totalCount()) ;
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emp").removeChildren() ;
				return null ;
			}
		}) ;
		assertEquals(0, session.awaitIndex().createRequest("bleujin").find().totalCount()) ;
		
	}
	
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
		
		assertEquals(3, session.awaitIndex().createRequest("dev").find().size()) ;
		
		
		List<ReadNode> list = session.createRequest("dev").descending("name").find().predicated(ReadNodePredicate.belowAt("/emp")).toList();
		Debug.line(list) ;
	}
	

	
}
