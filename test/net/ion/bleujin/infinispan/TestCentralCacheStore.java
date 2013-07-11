package net.ion.bleujin.infinispan;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestCentralCacheStore extends TestCase {

	public void testCreate() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
		.defaultClusteredBuilder()
			.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.build();
		
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);
		CentralCacheStoreConfig config = CentralCacheStoreConfig.create().location("./resource/ff5") ;
		dcm.defineConfiguration("test", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry())
				.transaction().syncCommitPhase(true).syncRollbackPhase(true)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new CentralCacheStore()).addProperty("location", config.location())
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		Cache<Object, Object> cache = dcm.getCache("test");
		
		for (int i = 0; i < 100; i++) {
			cache.put("/bleujin/" + i, new Integer(i)) ;
		}
		
		dcm.stop() ;
	}
	
	public void testRepository() throws Exception {
		RepositorySearch r = RepositorySearch.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/ff5")) ;
		
		ReadSession session = r.login("test");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 100 ; i++) {
					wsession.pathBy("/bleujin/" + i).property("index", i) ;
				}
				return null;
			}
		}) ;
		
		r.shutdown() ;
	}
	
	
	
}
