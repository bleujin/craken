package net.ion.craken.loaders;

import java.lang.reflect.Field;

import junit.framework.TestCase;

import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.neo.Credential;
import net.ion.neo.NeoRepository;
import net.ion.neo.ReadSession;
import net.ion.neo.TransactionJob;
import net.ion.neo.WriteNode;
import net.ion.neo.WriteRelationship;
import net.ion.neo.WriteSession;
import net.ion.neo.NeoWorkspace.RelType;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.AbstractLoaderConfiguration;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.util.InfinispanCollections;
import org.neo4j.graphdb.Direction;

import scala.reflect.generic.Trees.Star;

public class TestNeoNodeCacheStore extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	private Configuration defaultConf ;
	private NeoNodeCacheStore neoNodeCacheStore;

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		neoNodeCacheStore = new NeoNodeCacheStore();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		this.defaultConf = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching()
			.clustering().hash().numOwners(2).unsafe()
			.eviction().maxEntries(100)
			.loaders().preload(true).shared(true).addCacheLoader().cacheLoader(neoNodeCacheStore).addProperty("wsName", "test").addProperty("analyzerClzName", StandardAnalyzer.class.getCanonicalName())
			.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(true)
			.build();
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start();
	}

	
	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}

	public void testNeoWith() throws Exception {
		NeoRepository rep = new NeoRepository();
		ReadSession session = rep.login(Credential.EMANON, "test", StandardAnalyzer.class);
		session.workspace().clear() ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession tsession) {
				WriteNode root = tsession.rootNode();
				
				WriteNode hello = root.mergeRelationNode(RelType.CHILD, "hello") ;
				hello.property("pkey", "Hello") ;
				
				WriteNode world = root.mergeRelationNode(RelType.CHILD, "world") ;
				world.property("pkey", "World") ;
				
				WriteRelationship relation = hello.createRelationshipTo(world, RelType.create("KNOW"));
				relation.property("msg", " ") ;
				return null;
			}
		}).get() ;
		
		session.createQuery().find().debugPrint(Page.ALL) ;
		session.rootNode().relationShips(Direction.OUTGOING, RelType.CHILD).debugPrint(Page.ALL) ;
		rep.shutdown() ;
	}
	
	public void testClear() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.clear() ;
		cache.stop() ;
	}
	
	
	public void testFirst() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.start() ;
		
		
		for(String key : cache.keySet()){
			Debug.line(key, cache.get(key)) ;
		}
		
		//		cache.clear() ;
//		Thread.sleep(1000) ;
		cache.put("jin", Employee.createEmp(20, "jin", 7789));
		cache.put("hero", Employee.createEmp(22, "hero", 7790));
		cache.put("bleujin", Employee.createEmp(24, "bleujin", 7791));

		Employee hero = cache.get("hero");
		assertEquals(7790, hero.getEmpno());
		
		CacheLoaderConfig c = cache.getConfiguration().getCacheLoaders().get(0);
		final ReadSession session = ((NeoNodeCacheStoreConfig)c).login();
		
		session.createQuery().find().debugPrint(Page.ALL) ;
		
//		neoNodeCacheStore.getConfigurationClass()

//		cache.clear() ;
		cache.stop() ;
		
	}
	
	
	
	
}
