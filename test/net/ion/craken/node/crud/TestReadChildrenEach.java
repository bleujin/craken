package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;

public class TestReadChildrenEach extends TestBaseCrud{

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.pathBy("/bleujin/" + i).property("index", i).property("odd", i % 2) ;
				}
				return null;
			}
		}) ;
	}
	
	public void testSkip() throws Exception {
		assertEquals(10, session.pathBy("/bleujin").children().count()) ;
		assertEquals(5, session.pathBy("/bleujin").children().ascending("index").skip(5).count()) ;
	}

	public void testOffset() throws Exception {
		assertEquals(2, session.pathBy("/bleujin").children().ascending("index").skip(5).offset(2).count()) ;
		assertEquals(2, session.pathBy("/bleujin").children().ascending("index").offset(2).count()) ;
	}
	
	public void testOrder() throws Exception {
		assertEquals(2, session.pathBy("/bleujin").children().ascending("index").offset(2).count()) ;
		assertEquals(0, session.pathBy("/bleujin").children().ascending("index").offset(2).toList().get(0).property("index").intValue(0)) ;
		assertEquals(1, session.pathBy("/bleujin").children().ascending("index").offset(2).toList().get(1).property("index").intValue(0)) ;

		assertEquals(9, session.pathBy("/bleujin").children().descending("index").offset(2).toList().get(0).property("index").intValue(0)) ;
		assertEquals(8, session.pathBy("/bleujin").children().descending("index").offset(2).toList().get(1).property("index").intValue(0)) ;
	}
	
	public void testFilter() throws Exception {
		assertEquals(5, session.pathBy("/bleujin").children().gte("index", 5).count()) ;
		assertEquals(6, session.pathBy("/bleujin").children().lte("index", 5).count()) ;
	}

	
	public void testFilterNSort() throws Exception {
		assertEquals(5, session.pathBy("/bleujin").children().gte("index", 5).ascending("index").firstNode().property("index").asInt()) ;
		assertEquals(7, session.pathBy("/bleujin").children().gte("index", 5).skip(2).ascending("index").firstNode().property("index").asInt()) ;
	}
	
	public void testEach() throws Exception {
		long start = System.currentTimeMillis(); 
		ReadNode index0 = session.pathBy("/bleujin").children().ascending("index").eachNode(new ReadChildrenEach<ReadNode>() {
			@Override
			public ReadNode handle(ReadChildrenIterator citer) {
				return citer.next();
			}
		}) ;
		
		ReadNode found = session.pathBy("/bleujin").children().ascending("index").firstNode() ;
		assertEquals(true, index0.equals(found));
		Debug.line(System.currentTimeMillis() - start);
	}
	
}
