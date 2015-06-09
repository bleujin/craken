package net.ion.bleujin.crawl;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import com.sun.corba.se.impl.activation.RepositoryImpl;

public class Test2 extends TestCase {
	private ReadSession session;

	protected void setUp() throws Exception {
		super.setUp();
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder()
		.transport().defaultTransport()
			.clusterName("storetest")
			.nodeName("gigabyte")
			.addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.transport().addProperty("maxThreads", "100").addProperty("threadNamePrefix", "mytransport-thread")
		.build();
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig) ;
		
		Repository r = Craken.create(dcm, "bleujin").createWorkspace("enha", OldFileConfigBuilder.directory("./resource/enha2").distMode(CacheMode.REPL_SYNC));
		this.session = r.login("enha");
	}

	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown();
		super.tearDown();
	}
	
	
	public void testCount() throws Exception {
		Thread.sleep(2000);
		Debug.line("loaded");
		int count = session.root().childQuery("").find().totalCount() ;
		Debug.line(count);
		new InfinityThread().startNJoin();
	}
	
	
}
