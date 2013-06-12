package net.ion.craken.node.problem.store;

import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.lucene.impl.DirectoryBuilderImpl;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.TestConcurrent;
import net.ion.craken.node.problem.TestConfig;
import net.ion.craken.node.search.ReadSearchSession;
import net.ion.craken.node.search.RepositorySearch;
import net.ion.craken.node.search.util.TransactionJobs;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.Searcher;
import junit.framework.TestCase;

public class TestStoreBehavior extends TestCase {

	
	public void testLoadInCache() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineConfig("test.node", TestConfig.createFastLocalCacheStore(5)) ;
		
		ReadSession session = r.testLogin("test");
		session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
		
		
		session.pathBy("/bleujin").children().debugPrint() ;
		
		new InfinityThread().startNJoin() ;
		r.shutdown() ;
	}
	
	public void testLoadCentral() throws Exception {
		RepositoryImpl r = RepositoryImpl.create() ;
		r.defineConfig("test.meta", TestConfig.createFastSearchCacheStore()) ;
		r.defineConfig("test.chunks", TestConfig.createFastSearchCacheStore()) ;
		r.defineConfig("test.locks", TestConfig.createFastSearchCacheStore()) ;
		
		RepositorySearch rs = r.forSearch();
		ReadSearchSession session = rs.testLogin("test");
		
		Central central = session.central() ;
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i : ListUtil.rangeNum(10)) {
					WriteDocument doc = MyDocument.testDocument();
					doc.number("index", i).keyword("name", "bleujin") ;
					isession.insertDocument(doc) ;
				}
				return null;
			}
		}) ;
		

		Searcher searcher = central.newSearcher();
		
		searcher.createRequest("").find().debugPrint() ;
		

		
		rs.shutdown() ;
	}
}
