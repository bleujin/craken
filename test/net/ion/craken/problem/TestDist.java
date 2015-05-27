package net.ion.craken.problem;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestDist extends TestCase {

	public void testFirst() throws Exception {
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder().transport().defaultTransport().clusterName("ics6working").build();
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);
		final RepositoryImpl r = RepositoryImpl.create(dcm, "emanon");

		r.createWorkspace("ics", WorkspaceConfigBuilder.directory("./resource/temp/first").distMode(CacheMode.DIST_SYNC));
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		ReadSession session = r.login("ics");

		int count = 0;
		while (true) {
			Thread.sleep(1000);
			final int index = count++;
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/index", index).property("index", index);
					return null;
				}
			});
			if (index == 1) Debug.line("started");
		}
	}

	public void testSecond() throws Exception {
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder().transport().defaultTransport().clusterName("ics6working").build();
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);
		final RepositoryImpl r = RepositoryImpl.create(dcm, "emanon");

		r.createWorkspace("ics", WorkspaceConfigBuilder.directory("./resource/temp/second").distMode(CacheMode.DIST_SYNC));
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});


		Debug.line(dcm.getCacheConfiguration("ics").clustering().cacheMode()) ;
		
		ReadSession session = r.login("ics");
		while(true){
			Thread.sleep(1000);
			Debug.line(session.ghostBy("/index").children().count()) ;
		}
		
		
	}
}
