package net.ion.craken.node.problem;

import java.util.Iterator;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;


public class TestMany extends TestCase {

	
	public void testM() throws Exception{
		Repository r = Craken.inmemoryCreateWithTest();
		r.start() ;
		ReadSession session = r.login("test");
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				WriteNode root = wsession.root();
				for (int i : ListUtil.rangeNum(100000)) {
					root.child("" + i).property("property", RandomUtil.nextRandomString(10)) ;
				}
				return null;
			}
		}).get() ;

		int i = 0 ;
		final Iterator<ReadNode> children = session.root().children().iterator();
		while(children.hasNext()) {
			i++ ;
			children.next() ;
		}
		Debug.line(i) ;
		r.shutdown() ;
	}
	
	public void testMWithSearch() throws Exception {

		Craken r = Craken.inmemoryCreateWithTest()  ;
		r.start() ;
		ReadSession session = r.login("test");
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				WriteNode root = wsession.root();
				for (int i : ListUtil.rangeNum(100000)) {
					root.child("" + i).property("property", RandomUtil.nextRandomString(10)) ;
				}
				return null;
			}
		}).get() ;

		int i = 0 ;
		final Iterator<ReadNode> children = session.root().children().iterator();
		while(children.hasNext()) {
			i++ ;
			children.next() ;
		}
		Debug.line(i) ;
		
		Debug.line(session.queryRequest("").find().totalCount()) ;
		r.shutdown() ;
	}
	
	
	
	
	
	
}
