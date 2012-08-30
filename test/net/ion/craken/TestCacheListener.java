package net.ion.craken;

import java.util.List;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

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
