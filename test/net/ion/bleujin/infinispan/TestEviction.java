package net.ion.bleujin.infinispan;

import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestEviction extends TestCase {

	public void testMaxEntry() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager();
		dm.defineConfiguration("ev", new ConfigurationBuilder().eviction().maxEntries(10).build()) ;
		
		Cache<String, String> cache = dm.getCache("ev") ;
		
		for (int i = 0; i < 10; i++) {
			int max = RandomUtil.nextInt(10) ;
			for (int k = 0; k < max; k++) {
				cache.put(""+ i * k, ""+ i * k) ;
			}
			
			Debug.line(cache.keySet().size()) ;
		}
		
		Debug.line(cache.keySet());
	}
}
