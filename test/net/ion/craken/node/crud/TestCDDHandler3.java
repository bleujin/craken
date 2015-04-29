package net.ion.craken.node.crud;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

public class TestCDDHandler3 extends TestCase {

	
	public void testRunServerA() throws Exception {
		new ServerA().start(); 
		
	}
	
	
	public void testCallB() throws Exception {
		new ServerB().start(); 
		
	}
	
}



class ServerA {
	public void start() throws IOException, InterruptedException{

		GlobalConfiguration gconfig = new GlobalConfigurationBuilder()
			.transport().defaultTransport()
				.clusterName("cddtest")
				.nodeName("servera")
				.addProperty("configurationFile", new File("./resource/config/jgroups-udp.xml").getCanonicalPath())
			.transport().asyncTransportExecutor().addProperty("maxThreads", "100").addProperty("threadNamePrefix", "mytransport-thread")
			.globalJmxStatistics().enabled(false)
			.build() ;

		DefaultCacheManager dcm = new DefaultCacheManager(gconfig) ;
		final RepositoryImpl r = RepositoryImpl.create(dcm, "ics");
		r.createWorkspace("ics", WorkspaceConfigBuilder.directory("./resource/temp/servera").distMode(CacheMode.REPL_SYNC)) ;
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});
		r.start() ;

		final ReadSession session = r.login("ics") ;
		session.workspace().cddm().add(new CDDHandler() {
			public String pathPattern() {
				return "/emps/{userid}";
			}
			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				final String userId = rmap.get("userid") ;
				session.tran(new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/output/" + userId).property("result", "success") ;
						int waitTime = RandomUtil.nextInt(2000) + 1000 ;
//						Thread.sleep(waitTime);
						Debug.line(waitTime + " waited");
						return null;
					}
					
				}) ;
				return null;
			}
			@Override
			public TransactionJob<Void> deleted(Map<String, String> arg0, CDDRemovedEvent arg1) {
				return null;
			}
		}) ;
		System.out.println("serverA started");
		new InfinityThread().startNJoin(); 
	}
}

class ServerB {
	public void start() throws IOException, InterruptedException {
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder()
		.transport().defaultTransport()
			.clusterName("cddtest")
			.nodeName("serverb")
			.addProperty("configurationFile", new File("./resource/config/jgroups-udp.xml").getCanonicalPath())
		.transport().asyncTransportExecutor().addProperty("maxThreads", "100").addProperty("threadNamePrefix", "mytransport-thread")
		.globalJmxStatistics().enabled(false)
		.build() ;
		
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig) ;
		final RepositoryImpl r = RepositoryImpl.create(dcm, "ics");
		r.createWorkspace("ics", WorkspaceConfigBuilder.directory("./resource/temp/serverb").distMode(CacheMode.REPL_SYNC)) ;
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});
		r.start() ;
		
		final ReadSession session = r.login("ics") ;
		while(true){
			final String userId = "" + RandomUtil.nextRandomInt(100) ;
			final CountDownLatch cdown = new CountDownLatch(1) ;
			session.workspace().cddm().add(new CDDHandler() {
				
				@Override
				public String pathPattern() {
					return "/output/{userid}";
				}
				
				@Override
				public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
					cdown.countDown(); 
					Debug.line("called success", resolveMap.get("userid"));
					session.workspace().cddm().remove(this);
					return null;
				}
				
				@Override
				public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
					// TODO Auto-generated method stub
					return null;
				}
			}) ;
			
			session.tran(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/emps", userId).property("name", userId) ;
					return null;
				}
			}) ;
			
			boolean result = cdown.await(2, TimeUnit.SECONDS);
			if (! result){
				Debug.line("called fail", userId);
			}
			
			
			Thread.sleep(1000);
		}
		//System.out.println("serverB runned");
		
	}
	
}


