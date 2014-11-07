package net.ion.bleujin;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

public class EmbedCacheTest extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().
			transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		Configuration defaultConf = new ConfigurationBuilder().
			clustering().cacheMode(CacheMode.DIST_SYNC).jmxStatistics().enable().
			clustering().l1().enable().lifespan(6000000).invocationBatching().
			clustering().hash().numOwners(2).unsafe().build();
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start();
	}

	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}

	public void testCreate() throws Exception {
		Cache<String, Object> cache = dftManager.getCache("workspace");
		cache.put("key", "bleujin");
		assertEquals("bleujin", cache.get("key"));
		while(true){
			Thread.sleep(1000);
		}
	}
	
	public void xtestConfirm() throws Exception {
		Cache<String, Object> cache = dftManager.getCache("workspace");
		Debug.line(cache.keySet()) ;
	}
	
	

	public void testFileCache() throws Exception {
		Configuration fileCache = new ConfigurationBuilder().persistence().passivation(false).addSingleFileStore()
			.shared(false).preload(true).fetchPersistentState(true).ignoreModifications(false).purgeOnStartup(false).location("/tmp").async().enable().threadPoolSize(10).build();
		dftManager.defineConfiguration("fileCache", fileCache);
		dftManager.start();

		dftManager.getCache("fileCache").put("key", "bleujin");
		dftManager.stop();

		final DefaultCacheManager newDc = new DefaultCacheManager(globalConf);
		newDc.defineConfiguration("fileCache", fileCache);
		assertEquals("bleujin", newDc.getCache("fileCache").get("key"));
	}

	public void testListener() throws Exception {
		SampleListener listener = new SampleListener();
		Cache<Object, Object> cache = dftManager.getCache();
		cache.addListener(listener);

		cache.put("key", "bleujin");
		cache.put("key", "hero");

		assertEquals(3, listener.sum()); // 1+2
	}

	public void xtestStateShare() throws Exception {
		final Cache<Object, Object> cache = dftManager.getCache();
		cache.put("key2", "key2");
		cache.addListener(new DebugListener()) ;

		new Thread() {
			public void run() {
				try {
					while (true) {
						cache.put("key2", "key2") ;
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		new InfinityThread().startNJoin();
	}
	
	
	public void testConfirm() throws Exception {
		DefaultCacheManager rcm = new DefaultCacheManager("resource/config/client-simple.xml");
		Cache<Object, Object> defaultCache = rcm.getCache();
		Cache<Object, Object> fileCache = rcm.getCache("fileCache");
		Cache<Object, Object> otherCache = rcm.getCache("otherCache");
		
//		defaultCache.addListener(new CacheListener()) ;
//		fileCache.addListener(new CacheListener()) ;

		// Debug.line(defaultCache.get("name"));
		// Debug.line(fileCache.get("date"));

		BufferedReader bs = new BufferedReader(new InputStreamReader(System.in));
		while (!(bs.readLine().equalsIgnoreCase("X"))) {
			Debug.line(defaultCache.get("name"));
			Debug.line(fileCache.get("date"));
			Debug.line(otherCache.get("other"));
		}
		rcm.stop();
	}
	
	@Listener
	public class DebugListener {
		@CacheEntryModified
		public void cacheEntryCreated(CacheEntryModifiedEvent e) {
			if (!e.isPre()) System.out.println(e.getKey() + " " + e.getValue()) ;
		}
	}
	
	@Listener
	public class SampleListener {

		private int created = 0;
		private int modified = 0;

		@CacheEntryCreated
		public void cacheEntryCreated(CacheEntryCreatedEvent e) {
			if (!e.isPre())
				created++;
		}

		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent e) {
			if (!e.isPre())
				modified++;
		}

		public int sum() {
			return created + modified;
		}
	}

}
