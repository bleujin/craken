package net.ion.craken.node.problem;

import junit.framework.TestCase;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.search.ReadSearchSession;
import net.ion.craken.node.search.RepositorySearch;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.framework.util.StringUtil;


public class TestMany extends TestCase {

	
	public void testM() throws Exception{
		Repository r = RepositoryImpl.testSingle();
		r.start() ;
		ReadSession session = r.testLogin("test");
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				WriteNode root = wsession.root();
				for (int i : ListUtil.rangeNum(100000)) {
					root.addChild("" + i).property("property", RandomUtil.nextRandomString(10)) ;
				}
				return null;
			}
		}).get() ;

		int i = 0 ;
		final IteratorList<ReadNode> children = session.root().children();
		while(children.hasNext()) {
			i++ ;
			children.next() ;
		}
		Debug.line(i) ;
		r.shutdown() ;
	}
	
	public void testMWithSearch() throws Exception {

		RepositorySearch r = RepositoryImpl.testSingle().forSearch() ;
		r.start() ;
		ReadSearchSession session = r.testLogin("test");
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				WriteNode root = wsession.root();
				for (int i : ListUtil.rangeNum(100000)) {
					root.addChild("" + i).property("property", RandomUtil.nextRandomString(10)) ;
				}
				return null;
			}
		}).get() ;

		int i = 0 ;
		final IteratorList<ReadNode> children = session.awaitIndex().root().children();
		while(children.hasNext()) {
			i++ ;
			children.next() ;
		}
		Debug.line(i) ;
		
		Debug.line(session.createRequest("").find().totalCount()) ;
		r.shutdown() ;
	}
	
	
	
	
	
	
}
