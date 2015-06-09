package net.ion.craken.node.problem;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.ListUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;

public class TestLoader extends TestCase {

	private Craken repository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		repository = Craken.create();
		repository.dm().defineConfiguration("test", createFastLocalCacheStore(100)) ;
		repository.start() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (repository != null) repository.shutdown() ;
	}
	
	
	public void testWrite() throws Exception {
		ReadSession session = repository.login("test");
		
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(1000)) {
					wsession.pathBy("/board/" + i).property("reg", "bleujin").property("num", i) ;
				}
				return null;
			}
		}) ;
	}
	
	public void testRead() throws Exception {
		ReadSession session = repository.login("test");
		session.ghostBy("/board").children().debugPrint() ;
	}
	
	
	private Configuration createDefault(){
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).eviction().maxEntries(5000).strategy(EvictionStrategy.LRU).clustering().l1().hash().numOwners(2).build() ;
	}
	
	private Configuration createFastLocalCacheStore(int maxEntry) {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().clustering().hash().numOwners(2).unsafe()
				.eviction().maxEntries(maxEntry)
				.invocationBatching().enable().persistence().passivation(false).addSingleFileStore().preload(true).shared(false).addProperty("location", "./resource/store")
				// ./resource/temp
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
	}
	
}
