package net.ion.craken.io;

import java.util.Map.Entry;
import java.util.Set;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;

public class TestLRU extends TestCase{

	private DefaultCacheManager dm;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		dm.stop(); 
		super.tearDown();
	}
	
	public void testPutObject() throws Exception {
		Cache<String, StringBuilder> cache = dm.getCache("notdefined") ;
		
		cache.put("bleujin", new StringBuilder("Hello")) ;

		StringBuilder builder = cache.get("bleujin") ;
		builder.append(" World") ;
		
		StringBuilder find = cache.get("bleujin") ;
		
		builder.append(" !!") ;
		
		Debug.line(find);
	}
	
	
	public void testWrite() throws Exception {
		final Cache<String, StringBuilder> cache = definedCache(dm) ;
		
		for (int i = 0; i < 100; i++) {
			int next = RandomUtil.nextInt(1000);
			cache.put("index_" + next, new StringBuilder("value " + next)) ;
		}
		Set<String> keys = cache.keySet() ;
		Debug.line(keys);
		cache.stop(); 
	}
	
	public void testWriteData() throws Exception {
		final Cache<String, StringBuilder> cache = definedCache(dm) ;
		
		for (int i = 0; i < 100; i++) {
//			cache.put("index_" + i, new StringBuilder("value " + i)) ;
		}
		Set<String> keys = cache.keySet() ;
		Debug.line(keys);
		
		new Thread(){
			public void run(){
				while (true) {
					int next = RandomUtil.nextInt(1000);
					cache.put("index_" + next, new StringBuilder("value " + next)) ;
					try {
						Thread.sleep(RandomUtil.nextInt(100));
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}
				}
			}
		} ; //.start(); 
		

		for (int i = 0; i < 1000; i++) {
			Set<Entry<String, StringBuilder>> entry = cache.entrySet() ;
			Debug.line(entry.size());
			Thread.sleep(1000);
		}
	}
	
	private Cache<String, StringBuilder> definedCache(DefaultCacheManager dm){
		Configuration config = new ConfigurationBuilder()
			.clustering().cacheMode(CacheMode.LOCAL)
			.persistence().addSingleFileStore().location("./resource/store")
			.eviction().strategy(EvictionStrategy.FIFO).maxEntries(50)
			.expiration().wakeUpInterval(3000).lifespan(10000).maxIdle(3000)
			.build();
		dm.defineConfiguration("lru", config) ;
		Cache<String, StringBuilder> cache = dm.getCache("lru") ;
		return cache ;
	}
	
}
