package net.ion.bleujin;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import net.ion.bleujin.EmbedCacheTest.DebugListener;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

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
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.jboss.marshalling.SerializabilityChecker;

public class MonitorTest extends TestCase {
	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().
			transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml")
			.globalJmxStatistics().enable().jmxDomain("myinfinispan")
			
			.build();
		
		Configuration defaultConf = new ConfigurationBuilder().
			clustering().cacheMode(CacheMode.DIST_SYNC).jmxStatistics().enable().
			loaders().addFileCacheStore().
			clustering().l1().enable().lifespan(6000000).invocationBatching().
			clustering().hash().numOwners(2).unsafe().
			jmxStatistics().enable().build() ;
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start();
	}
	
	
	public void testRunForMonitor() throws Exception {
		final Cache<Object, Object> cache = dftManager.getCache();
		cache.put("key0", RandomUtil.nextRandomString(10)) ;
		cache.put("key1", RandomUtil.nextRandomString(10)) ;
		cache.put("key2", RandomUtil.nextRandomString(10)) ;
		cache.put("key3", RandomUtil.nextRandomString(10)) ;
		
		cache.addListener(new DebugListener()) ;
		new InfinityThread().startNJoin();

//		new Thread() {
//			public void run() {
//				try {
//					int i = 1 ;
//					while (true) {
//						Thread.sleep(1000);
//						System.out.print(".") ;
//						cache.remove("key" + RandomUtil.nextInt(10)) ;
//					}
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		}.start();
//
//		new InfinityThread().startNJoin();
	}
	
	public void testRunForOther() throws Exception {
		final Cache<Object, Object> cache = dftManager.getCache();

		new Thread() {
			public void run() {
				try {
					int i = 1 ;
					while (true) {
						Thread.sleep(1000);
						System.out.print(".") ;
						cache.remove("key" + RandomUtil.nextInt(10)) ;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		new InfinityThread().startNJoin();
	}
	
	
	@Listener
	public class DebugListener {
		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent e) throws InterruptedException, ExecutionException {
			if (!e.isPre()) {
				Debug.debug(e.getKey(), e.getValue(), e.getCache().keySet(), e.getCache().get(e.getKey()), e.getCache().getName()) ;
			}
		}
		
		// @CacheEntryVisited
		public void entryEvent(CacheEntryVisitedEvent e){
			Debug.line(e.getKey(), e.getValue(), e) ;
		}
		
		@CacheEntryRemoved
		public void entryRemoved(CacheEntryRemovedEvent e) throws InterruptedException, ExecutionException {
//			if (! e.isPre()){
				Debug.debug(e.getKey(), e.getValue(), e.isPre()) ;
//			}
		}
	}
	
	public void testReconige() throws Exception {
		final Cache<Object, ValueObject> cache = dftManager.getCache();
		cache.put("key0", new ValueObject()) ;
		cache.addListener(new DebugListener()) ;
		
		for (int i = 0; i < 10; i++) {
			ValueObject vo = cache.get("key0");
			vo.add() ;
			Debug.line(vo) ;
			Thread.sleep(1000) ;
		}
	}
	
	public void testRecursiveModify() throws Exception {
		final Cache<Object, ValueObject> cache = dftManager.getCache();
		RecursiveModify l = new RecursiveModify();
		cache.addListener(l) ;
		cache.put("key0", new ValueObject()) ;
		
		
		new InfinityThread().startNJoin() ;
	}
	
	@Listener
	public class RecursiveModify  {
		int i = 0 ;
		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent<String, ValueObject> e) throws InterruptedException, ExecutionException {
			if (!e.isPre()) {
				i++ ;
				Debug.debug(e.getKey(), e.getValue(), e.getCache().keySet(), e.getCache().get(e.getKey()), e.getCache().getName()) ;
				if (e.getValue().i.get() == 3){
					e.getCache().remove(e.getKey()) ; // not notified... --?
					Debug.line(e.getCache().keySet()) ;
				} else if (e.getValue().i.get() < 3){
					e.getValue().add() ;
					e.getCache().put(e.getKey(), e.getValue()) ;
				}
			}
		}
		
		@CacheEntryRemoved
		public void entryRemoved(CacheEntryRemovedEvent<String, ValueObject> e) {
			Debug.line(e) ;
		}
	}
	
	
	public void testRemoveEvent() throws Exception {
		final Cache<Object, ValueObject> cache = dftManager.getCache();
		cache.put("key0", new ValueObject()) ;
		cache.addListener(new RemoveListener()) ;
		cache.remove("key0") ;
		
		new InfinityThread().startNJoin() ;
	}
	
	@Listener
	public class RemoveListener {
		
		@CacheEntryRemoved 
		public void entryRemoved(CacheEntryRemovedEvent e){
			Debug.line(e) ;
		}
		
		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent e){
			Debug.line(e) ;
		}
	}
}

class ValueObject implements Serializable {
	AtomicInteger i = new AtomicInteger(0) ;
	
	public void add(){
		i.incrementAndGet() ;
	}
	
	public String toString(){
		return i.toString() ;
	}
}
