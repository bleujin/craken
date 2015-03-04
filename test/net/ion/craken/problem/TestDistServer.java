package net.ion.craken.problem;

import java.util.Map;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationChildBuilder;
import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestDistServer extends TestCase{

	
	public void testFirstRun() throws Exception {
		
//		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().build() ;
		RepositoryImpl r = RepositoryImpl.create(new DefaultCacheManager("./resource/config/craken-dist-config.xml"), "emanon") ;
		r.start() ;
		r.createWorkspace("external", WorkspaceConfigBuilder.directory("./resource/temp/external1")) ;

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
		
		RepositoryImpl r = RepositoryImpl.create(new DefaultCacheManager("./resource/config/craken-dist-config.xml"), "emanon") ;
		r.start() ;
		r.createWorkspace("external", WorkspaceConfigBuilder.directory("./resource/temp/external2")) ;

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
						// TODO Auto-generated method stub
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
