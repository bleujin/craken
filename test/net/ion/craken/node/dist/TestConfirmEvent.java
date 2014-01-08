package net.ion.craken.node.dist;

import java.io.File;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryLoadedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.xa.GlobalTransaction;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

public class TestConfirmEvent extends TestCase {

	
	public void testStart() throws Exception {
		// run xtestReader
		// run xtestWriter
	}
	
	
	public void testInit() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		DefaultCacheManager dcm = r.dm();
		dcm.defineConfiguration("tran", FastFileCacheStore.fileStoreConfig(CacheMode.DIST_SYNC, "./resource/temp/s2", 50)) ;
		Cache<String, String> cache = dcm.getCache("tran");
		cache.start() ;
		
		for (int i = 0; i < 10; i++) {
			cache.put("name" + i, "bleujin" + i) ;
		}
		cache.stop() ;
		dcm.stop() ;
	}
	
	
	public void testCacheReader() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		DefaultCacheManager dcm = r.dm();
		dcm.defineConfiguration("tran", FastFileCacheStore.fileStoreConfig(CacheMode.DIST_SYNC, "./resource/temp/s1", 2)) ;
		dcm.addListener(new CacheListener()) ;
		Cache<String, String> cache = dcm.getCache("tran");
		cache.addListener(new CacheListener()) ;
		while(true){
			Thread.sleep(1000) ;
			System.out.print('.') ;
		}
	}

	
	@Listener
	public static class CacheListener {
		@ViewChanged
		public void viewChanged(final ViewChangedEvent event){
			final EmbeddedCacheManager cm = event.getCacheManager();
			
			new Thread(){
				public void run(){
					String value = cm.<String,String>getCache("tran").get("name1") ;
					Debug.line(value, event.getNewMembers()) ;
				}
			}.start() ;
		}
		
		@CacheStarted
		public void cacheStarted(CacheStartedEvent event) throws InterruptedException{
			Address self = event.getCacheManager().getAddress();
			Debug.debug(self, event.getCacheManager().getMembers()) ;
			
			
			if (event.getCacheManager().getMembers().size() > 1)
				Thread.sleep(10000) ;
//				new InfinityThread().startNJoin() ;
		}
		
		@CacheEntryLoaded
		public void entryViewed(CacheEntryLoadedEvent<String, String> entry){
			Debug.line(entry.getKey(), entry.getValue()) ;
		}
		
		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent<String, String> event){
			Debug.line(event) ;
		}
	}
	

	public void testCacheRead() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		DefaultCacheManager dcm = r.dm();
		dcm.defineConfiguration("tran", FastFileCacheStore.fileStoreConfig(CacheMode.DIST_SYNC, "./resource/temp/s2", 100)) ;
		dcm.addListener(new CacheListener()) ;
		Cache<String, String> cache = dcm.getCache("tran");
		cache.start() ;
		
		while(true){
			Thread.sleep(1000) ;
			System.out.print('!') ;
			cache.put("name" + RandomUtil.nextRandomInt(10), "bleujin" + RandomUtil.nextRandomInt(10)) ;
		}
	}


}