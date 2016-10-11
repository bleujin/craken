package net.ion.ics6.dfile;

import net.ion.framework.util.Debug;
import net.ion.nradon.restlet.Metadata;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

@Listener
public class DataListener {

	@CacheEntryCreated
	public void created(CacheEntryCreatedEvent<String, Metadata> cevent){
		Debug.line("created", cevent.getKey(), cevent.getMetadata());
	}
	
	@CacheEntryInvalidated
	public void invalidated(CacheEntryInvalidatedEvent<String, Metadata> cevent){
		Debug.line("invalidated", cevent.getKey(), cevent.getMetadata());
	}
	
	@CacheEntryModified
	public void modified(CacheEntryModifiedEvent<String, Metadata> cevent){
		Debug.line("modified", cevent.getKey(), cevent.getMetadata());
	}
	
	@CacheEntryRemoved
	public void removed(CacheEntryRemovedEvent<String, Metadata> cevent){
		Debug.line("removed", cevent.getKey(), cevent.getMetadata());
	}
	
	@CacheEntryPassivated
	public void passivated(CacheEntryPassivatedEvent<String, Metadata> cevent){
		Debug.line("passivated", cevent.getKey(), cevent.getMetadata());
	}
}
