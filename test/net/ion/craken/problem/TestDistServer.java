package net.ion.craken.problem;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.manager.DefaultCacheManager;

public class TestDistServer extends TestCase{

	
	public void testFirstRun() throws Exception {
		
//		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().build() ;
		Craken r = Craken.create(new DefaultCacheManager("./resource/config/craken-dist-config.xml"), "emanon") ;
		r.start() ;
		r.createWorkspace("external", OldFileConfigBuilder.directory("./resource/temp/external1")) ;

		ReadSession session = r.login("external") ;
		
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/emp/{userid}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				
				Debug.debug("external1",resolveMap);
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				Debug.debug(resolveMap);
				return null;
			}
		}) ;
		
		
		
		
		// session.tran(TransactionJobs.HelloBleujin) ;

		
		new InfinityThread().startNJoin(); 
		
		r.shutdown() ;
	}
	

	
	public void testSecondRun() throws Exception {
		
		Craken r = Craken.create(new DefaultCacheManager("./resource/config/craken-dist-config.xml"), "emanon") ;
		r.start() ;
		r.createWorkspace("external", OldFileConfigBuilder.directory("./resource/temp/external2")) ;

		ReadSession session = r.login("external") ;
		
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/emp/{userid}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				Debug.debug("external2", resolveMap);
				return new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						return null;
					}
				};
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				Debug.debug(resolveMap);
				return null;
			}
		}) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleuin").property("name", "bleujin") ;
				return null;
			}
		}) ;

		
		new InfinityThread().startNJoin(); 
		
		r.shutdown() ;
	}
	
	
}
