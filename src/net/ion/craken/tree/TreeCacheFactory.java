package net.ion.craken.tree;

import net.ion.craken.io.GridFile;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.Repository;
import net.ion.craken.node.crud.RepositoryListener;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.config.ConfigurationException;
import org.infinispan.manager.DefaultCacheManager;

import com.sun.corba.se.spi.orbutil.threadpool.Work;


public class TreeCacheFactory {

	public static TreeCache createTreeCache(Repository repository, DefaultCacheManager dftManager, String cacheName) {

		// Validation to make sure that the cache is not null.
		Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dftManager.getCache(cacheName + ".node");
		if (cache == null) {
			throw new NullPointerException("The cache parameter passed in is null");
		}

		// If invocationBatching is not enabled, throw a new configuration exception.
		if (!cache.getCacheConfiguration().invocationBatching().enabled()) {
			throw new ConfigurationException("invocationBatching is not enabled for cache '" + cache.getName() + "'. Make sure this is enabled by" + " calling configurationBuilder.invocationBatching().enable()");
		}
		
		cache.addListener(repository.listener()) ;
		Cache<String, byte[]> blobdata = cache.getCacheManager().getCache(cacheName + ".blobdata") ;
		Cache<String, GridFile.Metadata> blobmeta = cache.getCacheManager().getCache(cacheName + ".blobmeta") ;


		return new TreeCache(cache, new GridFilesystem(blobdata, blobmeta));
	}
}
