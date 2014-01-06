package net.ion.craken.node.search;


import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.IndexInfoHandler;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.reader.InfoReader;

public class TestStoreSearch extends TestCase {

	private ReadSession session;
	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest();
		r.start() ;
		
		this.session = r.login("test");
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0 ; i < 20 ; i++) {
					wsession.pathBy("/bleujin/" + i).property("dummy", i).property("dstring", "d" + i).append("darray", "a1", "a2", "a3").property("explain", "this is sample") ;
				}
				return null;
			}
		}) ;
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testMatchAllDoc() throws Exception {
		ChildQueryResponse response = session.root().childQuery("").find();
		assertEquals(1, response.totalCount()) ;
		assertEquals("/bleujin", response.toList().get(0).fqn().toString()) ;
	}

	
	public void testUseCache() throws Exception {
		for (int i = 0; i < 5; i++) {
			long start = System.currentTimeMillis() ;
			ChildQueryResponse response = session.pathBy("/bleujin").childQuery("dummy:3").find();
			response.debugPrint() ;
			Debug.line(System.currentTimeMillis() - start) ;
		}
	}
	
	public void testOperatorLtGtBetween() throws Exception {
		// number
		assertEquals(3, session.pathBy("/bleujin").childQuery("").lt("dummy", 3).find().totalCount()) ;
		assertEquals(4, session.pathBy("/bleujin").childQuery("").lte("dummy", 3).find().totalCount()) ;

		assertEquals(4, session.pathBy("/bleujin").childQuery("").gt("dummy", 15).find().totalCount()) ;
		assertEquals(5, session.pathBy("/bleujin").childQuery("").gte("dummy", 15).find().totalCount()) ;
		
		assertEquals(3, session.pathBy("/bleujin").childQuery("").between("dummy", 5, 7).find().totalCount()) ;

		// string
		assertEquals(13, session.pathBy("/bleujin").childQuery("").lt("dstring", "d3").find().totalCount()) ;
		assertEquals(14, session.pathBy("/bleujin").childQuery("").lte("dstring", "d3").find().totalCount()) ;

		
		assertEquals(2, session.pathBy("/bleujin").childQuery("").gt("dstring", "d7").find().totalCount()) ;
		assertEquals(3, session.pathBy("/bleujin").childQuery("").gte("dstring", "d7").find().totalCount()) ;

		assertEquals(3, session.pathBy("/bleujin").childQuery("").between("dstring", "d7", "d9").find().totalCount()) ;
	}
	
	
	public void testEqInContain() throws Exception {
		// eq
		assertEquals(1, session.pathBy("/bleujin").childQuery("").eq("dummy", 3).find().totalCount()) ;
		assertEquals(1, session.pathBy("/bleujin").childQuery("").eq("dummy", "3").find().totalCount()) ;
		assertEquals(20, session.pathBy("/bleujin").childQuery("").eq("explain", "sample").find().totalCount()) ;
		assertEquals(0, session.pathBy("/bleujin").childQuery("").eq("explain", "sam").find().totalCount()) ; // contain
		
		// in
		assertEquals(20, session.pathBy("/bleujin").childQuery("").eq("darray", "a2").find().totalCount()) ;
		
		// whildcard
		assertEquals(20, session.pathBy("/bleujin").childQuery("").wildcard("explain", "sam*").find().totalCount()) ;
		assertEquals(20, session.pathBy("/bleujin").childQuery("").wildcard("explain", "sam???").find().totalCount()) ;
		assertEquals(0, session.pathBy("/bleujin").childQuery("").wildcard("explain", "sam?").find().totalCount()) ;

	}
	
	public void testWhere() throws Exception {
		ChildQueryResponse response = session.pathBy("/bleujin").childQuery("").query("dummy:3").find() ;
		assertEquals(1, response.totalCount()) ;
	}
	
	
	public void testSkipOffset() throws Exception {
		ChildQueryResponse response = session.pathBy("/bleujin").childQuery("").skip(5).offset(5).find();
		assertEquals(5, response.size()) ;
		
		assertEquals(20, response.totalCount()) ;
	}

	
	public void testSort() throws Exception {
		ChildQueryResponse response = session.pathBy("/bleujin").childQuery("").skip(5).offset(5).descending("dummy _number").find();
		List<ReadNode> list = response.toList();
		assertEquals(5, list.size()) ;
		assertEquals("/bleujin/14", list.get(0).fqn().toString()) ;
		assertEquals("/bleujin/10", list.get(4).fqn().toString()) ;
	}
	
	
	public void testIndexInfo() throws Exception {
		int maxDoc = session.indexInfo(new IndexInfoHandler<Integer>() {
			@Override
			public Integer handle(ReadSession session, InfoReader infoReader) {
				try {
					return infoReader.maxDoc();
				} catch (IOException e) {
					return 0 ;
				}
			}
		}) ;
		Debug.line(maxDoc) ;
	}
	
	
	public void testToRows() throws Exception {
		Rows rows = session.pathBy("/bleujin").childQuery("").find().toRows("dummy, darray");
		rows.debugPrint() ;
	}
	
	
	
	
	
	
	
}





