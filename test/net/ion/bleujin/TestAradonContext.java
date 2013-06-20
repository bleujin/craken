package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.config.InstanceAttributeValue;

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

public class TestAradonContext extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().
		transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		Configuration defaultConf = new ConfigurationBuilder().
			clustering().cacheMode(CacheMode.DIST_ASYNC).jmxStatistics().enable().
			clustering().l1().enable().lifespan(6000000).invocationBatching().
			clustering().hash().numOwners(2).unsafe().build();
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start();
		
	}
	
	public void testLoad() throws Exception {
		Aradon aradon = Aradon.create() ;
		dftManager.getCache("myjob").addListener(new SampleListener()) ;
		aradon.getConfig().attributes().put("dftmanager", InstanceAttributeValue.create(dftManager)) ;
		
		aradon.startServer(9000) ;
		new InfinityThread().startNJoin() ;
	}
	
	
	public void testWrite() throws Exception {
		Cache<String, Object> myjob = dftManager.getCache("myjob");
		
		for (int i = 0; i < 10; i++) {
			Thread.sleep(1000) ;
			myjob.put("key"+i, "value" + i) ;
			System.out.print('.') ;
		}
	}
	
	
	
	
	@Listener
	public class SampleListener {

		private int created = 0;
		private int modified = 0;

		@CacheEntryCreated
		public void cacheEntryCreated(CacheEntryCreatedEvent e) {
			if (e.isPre()) return ;
			created++;
			Debug.line(e.getKey(), e.getType()) ;
		}

		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent e) {
			if (e.isPre()) return ;
			modified++;
			Debug.line(e.getKey(), e.getValue()) ;
		}

		public int sum() {
			return created + modified;
		}
	}
	
}
