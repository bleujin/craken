package net.ion.craken.dir;

import net.ion.framework.db.Page;
import net.ion.framework.util.InfinityThread;
import net.ion.isearcher.common.MyDocument;
import net.ion.isearcher.common.MyField;
import net.ion.isearcher.impl.Central;
import net.ion.isearcher.impl.ISearcher;
import net.ion.isearcher.impl.SingleCentral;
import net.ion.isearcher.indexer.write.IWriter;
import net.ion.isearcher.searcher.MyKoreanAnalyzer;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestInfinispanDirStudy extends TestCase {

	
	public void testIndexFirst() throws Exception {
		Central central = getCentral();

		IWriter indexer = central.newIndexer(new MyKoreanAnalyzer());
		indexer.begin("owner") ;
		indexer.insertDocument(MyDocument.testDocument().add(MyField.keyword("name", "bleujin"))) ;
		indexer.end() ;
	
		ISearcher searcher = central.newSearcher() ;
		assertEquals(1, searcher.searchTest("bleujin").allDocumentSet().list().size()) ;
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testIndexOther() throws Exception {
		Central central = getCentral();

		IWriter indexer = central.newIndexer(new MyKoreanAnalyzer());
		indexer.begin("owner") ;
		indexer.insertDocument(MyDocument.testDocument().add(MyField.keyword("name", "hero"))) ;
		indexer.end() ;
	
		ISearcher searcher = central.newSearcher() ;
		assertEquals(1, searcher.searchTest("hero").allDocumentSet().list().size()) ;
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testSearch() throws Exception {
		Central central = getCentral();

		ISearcher searcher = central.newSearcher() ;
		assertEquals(1, searcher.searchTest("hero").allDocumentSet().list().size()) ;
	}
	
	
	
	
	
	
	
	
	
	

	private Central getCentral() {
		GlobalConfigurationBuilder globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder() ;
		ConfigurationBuilder defaultConf = new ConfigurationBuilder() ;
		defaultConf.clustering().cacheMode(CacheMode.DIST_ASYNC).clustering().l1().enable().lifespan(6000000).invocationBatching().enable().clustering().hash().numOwners(2) ;

		DefaultCacheManager dftManager = new DefaultCacheManager(globalBuilder.build(), defaultConf.build(), true) ;
		Cache cache = dftManager.getCache("employee") ;
		
		InfinispanDirectory dir = new InfinispanDirectory(cache);
		Central central = SingleCentral.createOrGet(dir);
		return central;
	}
	
	
	
	
}
