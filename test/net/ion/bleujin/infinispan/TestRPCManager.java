package net.ion.bleujin.infinispan;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestRPCManager extends TestCase {

	public void testConnect() throws Exception {

		GlobalConfiguration gconfig = new GlobalConfigurationBuilder()
			.transport().defaultTransport()
				.clusterName("storetest")
				.nodeName("main")
				.addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
				.addProperty("maxThreads", "100").addProperty("threadNamePrefix", "mytransport-thread")

			.build();
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);

//		Configuration oconfig = new ConfigurationBuilder()
//			.persistence().addStore(RemoteStoreConfigurationBuilder.class)
//			.fetchPersistentState(false)
//			.ignoreModifications(false)
//			.purgeOnStartup(false)
//			.remoteCacheName("sifs")
//			.rawValues(true)
//			.addServer()
//			.host("bleujin-GIGA").port(12111)
//			.connectionPool()
//			.maxActive(10)
//			.exhaustedAction(ExhaustedAction.CREATE_NEW)
//			.async().enable().build();

		Configuration oconfig = new ConfigurationBuilder().persistence()
		    .addClusterLoader().remoteCallTimeout(3000)
		    .build();
		
		dcm.defineConfiguration("sifs", oconfig) ;

		Cache<String, String> cache = dcm.getCache("sifs") ;
		
		
		Debug.line(cache.getAdvancedCache().getEvictionManager(), cache.getAdvancedCache().getRpcManager(), dcm.getTransport().getMembers());
		
		String key = "//C:/crawl/enha/wiki/(영도 차고)종점" ;

//		Debug.line(cache.containsKey(key));
		dcm.stop();
	}
}
