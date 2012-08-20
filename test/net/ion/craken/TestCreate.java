package net.ion.craken;

import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;

import junit.framework.TestCase;

public class TestCreate extends TestCase{

	private Craken craken ;
	@Override
	protected void setUp() throws Exception {
		this.craken = Craken.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.stop() ;
	}
	
	public void testCreate() throws Exception {
		craken.globalConfig().transport().clusterName("my-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml") ;
		craken.defineDefault().clustering().cacheMode(CacheMode.DIST_SYNC).jmxStatistics().enable().clustering().invocationBatching().clustering().hash().numOwners(2) ;
		
		craken.start() ;

		craken.defineLeg("servers",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().invocationBatching().build()) ;
		craken.addListener(new LegListener()) ;
		
		CrakenLeg servers = craken.findLeg("servers") ;
		servers.addListener(new EntryListener()) ;

		while(true){
			servers.putNode(SimpleDataNode.create("bleujin" + RandomUtil.nextInt(10)).put("age", RandomUtil.nextInt(100)).put("server", craken.getManager().getAddress().toString()) ) ;
			servers.keySet() ;
			Thread.sleep(1000) ;
		}
	}
	
	
	
	@Listener
	public class LegListener {
		@CacheStarted
		public void cacheStarted(CacheStartedEvent e){
			Debug.line('c', "") ;
		} 
		
		@CacheStopped
		public void cacheStopped(CacheStoppedEvent e){
		}
		
		@ViewChanged
		public void viewChanged(ViewChangedEvent e){
			Debug.line('x', e.getOldMembers(), e.getNewMembers()) ;
		}
	}
	
	

	@Listener
	public class EntryListener {

		public EntryListener() {
		}

		@CacheEntryCreated
		public void cacheEntryCreated(CacheEntryCreatedEvent e) {
			// if (!e.isPre()) Debug.line(e.getKey()) ;
		}

		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent<NodeKey, DataNode> e) {
			if (!e.isPre()) Debug.line(e.getKey(), e.getValue().getValue("server")) ;
		}
	}
	
	
}
