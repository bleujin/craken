package net.ion.craken.tree;

import org.infinispan.Cache;
import org.infinispan.config.ConfigurationException;


public class TreeCacheFactory {

	public <K, V> TreeCache<K, V> createTreeCache(Cache<K, V> cache) {

		// Validation to make sure that the cache is not null.

		if (cache == null) {
			throw new NullPointerException("The cache parameter passed in is null");
		}

		// If invocationBatching is not enabled, throw a new configuration exception.
		if (!cache.getCacheConfiguration().invocationBatching().enabled()) {
			throw new ConfigurationException("invocationBatching is not enabled for cache '" + cache.getName() + "'. Make sure this is enabled by" + " calling configurationBuilder.invocationBatching().enable()");
		}

		return new TreeCache<K, V>(cache);
	}
}
