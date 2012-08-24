package net.ion.craken;

import net.ion.craken.TestCacheListener.ServerListener;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

public class TestEntryListener extends TestBase{

	public void xtestFirstServer() throws Exception {
		
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		EntryListener elistener = new EntryListener();
		emps.addListener(elistener) ;

		new InfinityThread().startNJoin() ;
	}
	
	public void xtestSecondServer() throws Exception {
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		EntryListener elistener = new EntryListener();
		emps.addListener(elistener) ;
		
		emps.newInstance(7756).name("bleujin").age(20).save() ;
		emps.findByKey(7756).age(30).save() ;
		
		assertEquals(30, emps.findByKey(7756).age()) ;
		
		
		new InfinityThread().startNJoin() ;
	}
	
	
	@Listener
	public class EntryListener{
		
		@CacheEntryCreated
		public void entryCreated(CacheEntryCreatedEvent<NodeKey, Employee> e){
		} 
		
		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent<NodeKey, Employee> e){
			if (! e.isPre()) Debug.line(e.getValue(), "Modified") ;
		}
		
		@CacheEntryRemoved
		public void entryRemoved(CacheEntryRemovedEvent<NodeKey, Employee> e){
			Debug.line(e.getValue(), "Removed") ;
		}
	}
}
