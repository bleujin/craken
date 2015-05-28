package net.ion.bleujin;

import java.io.File;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestBigSize extends TestCase {

	public void testLoad() throws Exception {
		File udpFile = new File("./resource/config/bleujin-udp-config.xml") ;
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder()
		.transport().defaultTransport()
			.clusterName("bworking")
			.nodeName("ics")
			.addProperty("configurationFile", udpFile.getCanonicalPath())
		.globalJmxStatistics().enabled(false)
		.build() ;
		DefaultCacheManager dm = new DefaultCacheManager(gconfig) ;
		
		Cache<Object, Object> notdefine = dm.getCache("notdefined") ;
		Debug.line(notdefine.getCacheConfiguration().clustering()) ;
		
		dm.stop(); 
	}

}
