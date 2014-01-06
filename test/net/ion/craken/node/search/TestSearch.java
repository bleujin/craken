package net.ion.craken.node.search;


import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

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
		ChildQueryResponse response = session.queryRequest("").find();
		assertEquals(1, response.size()) ;
	}
	
	
	public void testDopple() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emps/bleujin").property("age", 20).property("name", "bleujin") ;
				wsession.pathBy("/emps/hero").property("age", 20).property("name", "hero") ;
				return null;
			}
		}) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emps/bleujin").property("age", 20).property("name", "bleujin") ;
				wsession.pathBy("/emps/hero").property("age", 20).property("name", "hero") ;
				return null;
			}
		}) ;
		
		assertEquals(1, session.pathBy("/emps").childQuery("bleujin").find().size()) ;
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
		
		ChildQueryResponse response = session.queryRequest("").skip(10).offset(10).ascending("index").find();
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
		
		ChildQueryResponse response = session.queryRequest("bleujin").find();
		assertEquals(1, response.size()) ;

		ReadNode firstNode = response.first();
		assertEquals("dev", firstNode.property("job").value()) ;
		
	}
	
	
	public void testOnRemove() throws Exception {
		session.tranSync(TransactionJobs.dummyEmp(10)) ;
		assertEquals(10, session.queryRequest("bleujin").find().totalCount()) ;
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emp").removeChildren() ;
				return null ;
			}
		}) ;
		assertEquals(0, session.queryRequest("bleujin").find().totalCount()) ;
		
	}
	
	
	
	public void testIgnorePropertyIndex() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().ignore("pwd") ;
				wsession.pathBy("/people").property("pwd", "qwer").property("id", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals(0, session.queryRequest("pwd:qwer").find().size()) ;
		assertEquals(0, session.queryRequest("qwer").find().size()) ;
	}
	
	
	public void testIgnoreAll() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().ignoreIndex() ;
				wsession.pathBy("/people").property("pwd", "bleujin") ;
				Debug.line(wsession.iwconfig()) ;
				return null;
			}
		}) ;
		
		assertEquals(0, session.queryRequest("pwd:bleujin").find().size()) ;
		assertEquals(0, session.queryRequest("bleujin").find().size()) ;
		assertEquals("bleujin", session.pathBy("/people").property("pwd").stringValue()) ;
	}
	
	
	

	
}
