package net.ion.bleujin;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;

@Listener
public class SampleListener {

	@CacheStarted
	public void handleStart(Event event) {
		System.out.println("Cache Started... ");
	}

	@CacheStopped
	public void handleStop(Event event) {
		System.out.println("Cache shudown.... ");
	}

	@CacheEntryModified
	public void cacheEntryModified(CacheEntryModifiedEvent e) {
		System.out.println("Added a entry to cache..." + e.getKey() + " " +  e.toString());
	}
	
	

}