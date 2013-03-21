package net.ion.craken;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

public class TestCacheListener extends TestBase {
	
	public void xtestFirstServer() throws Exception {
		ServerListener servers = new ServerListener();
		craken.addListener(servers) ;
		
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);

		new InfinityThread().startNJoin() ;
	}
	
	public void xtestSecondServer() throws Exception {
		ServerListener servers = new ServerListener();
		craken.addListener(servers) ;

		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testOri1() throws Exception {
		GlobalConfiguration globalConfig = GlobalConfigurationBuilder.defaultClusteredBuilder()
		.transport().clusterName("mysearch").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build() ;
		DefaultCacheManager dftManager = new DefaultCacheManager(globalConfig, true);
		dftManager.addListener(new ServerListener()) ;
		dftManager.start() ;
		
		dftManager.getCache("home") ;

		new InfinityThread().startNJoin() ;
	}

	public void testOri2() throws Exception {
		GlobalConfiguration globalConfig = GlobalConfigurationBuilder.defaultClusteredBuilder()
		.transport().clusterName("mysearch").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build() ;
		DefaultCacheManager dftManager = new DefaultCacheManager(globalConfig, true);
		dftManager.addListener(new ServerListener()) ;
		dftManager.start() ;
		
		dftManager.getCache("home") ;

		new InfinityThread().startNJoin() ;
	}
	

	
	@Listener
	public class ServerListener{
		
		@CacheStarted
		public void serverStarted(CacheStartedEvent e){
			Debug.line(e.getCacheManager().getAddress() + " started", e.getCacheManager().getMembers()) ;
		} 
		
		@CacheStopped
		public void serverStopped(CacheStoppedEvent e){
			Debug.line(e.getCacheManager().getAddress() + " stopped") ;
		}
		
		@ViewChanged
		public void viewChanged(ViewChangedEvent e){
			Debug.line("Changed", e.getOldMembers(), e.getNewMembers()) ;
		}
	}
}
