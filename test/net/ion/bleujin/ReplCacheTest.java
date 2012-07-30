package net.ion.bleujin;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryLoadedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import junit.framework.TestCase;

@Listener
public class ReplCacheTest extends TestCase {

	public void testStart() throws Exception {
		DefaultCacheManager cm = new DefaultCacheManager(
				GlobalConfigurationBuilder.defaultClusteredBuilder().transport().addProperty("configurationFile", "resource/config/jgroups-tcp.xml").build(), 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build());

		cm.start();

		Cache<Object, Object> cache = cm.getCache("workspace");
		while (true) {
			cache.put("hero", "hero " + new Date());
			Thread.sleep(800);
		}
	}

	public void testGetter() throws Exception {
		DefaultCacheManager cacheManager = new DefaultCacheManager(
					GlobalConfigurationBuilder.defaultClusteredBuilder().transport().addProperty("configurationFile", "resource/config/jgroups-tcp.xml").build(), 
					new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build());
		cacheManager.start();

		Cache<Object, Object> cache = cacheManager.getCache("workspace");
		cache.addListener(new SampleListener());

		while (true) {

			System.out.println(cache.get("hero"));
			Thread.sleep(900);
		}

	}

	public void testListenerSpeed() throws Exception {
		final DefaultCacheManager cm = new DefaultCacheManager(
					GlobalConfigurationBuilder.defaultClusteredBuilder().transport().addProperty("configurationFile", "resource/config/jgroups-tcp.xml").build(), 
					new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build());

		cm.start();
		new Thread(){
			public void run(){
				Cache<Object, Object> cache = cm.getCache("workspace");
				cache.addListener(new ReplCacheTest()) ;
				int i = 0 ;
				while (true) {
					cache.put("key" + i++, "hero " + new Date());
					try {
						Thread.sleep(10) ;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start() ;

		new InfinityThread().startNJoin() ;
	}

	private AtomicInteger count = new AtomicInteger() ;
	@CacheEntryCreated
	public void cacheEntryModified(CacheEntryCreatedEvent e) {
//		System.out.println("Added a entry to cache..." + e.getKey() + " " +  e.toString());
		System.out.println(count.incrementAndGet()) ;
	}
	
	
	
}
