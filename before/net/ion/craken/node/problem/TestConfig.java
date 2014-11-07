package net.ion.craken.node.problem;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestConfig {
	
	public static Configuration createFastLocalCacheStore(int maxEntry) {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().clustering().hash().numOwners(1).unsafe()
				.eviction().maxEntries(maxEntry)
				.invocationBatching().enable().persistence().passivation(false).addSingleFileStore().preload(true).shared(false).addProperty("location", "./resource/store/temp")
				// ./resource/temp
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
	}
	
	
	public static Configuration createFastSearchCacheStore() {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().clustering().hash().numOwners(2).unsafe()
				.invocationBatching().enable().persistence().passivation(false).addSingleFileStore().preload(true).shared(false).addProperty("location", "./resource/store/temp")
				// ./resource/temp
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
	}

	
	public static Configuration createOldSearchCacheStore(int maxEntries) {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
				.eviction().maxEntries(maxEntries)
				.clustering().l1().enable().invocationBatching().clustering().hash().numOwners(2).unsafe()
				.invocationBatching().enable().persistence().passivation(false).addSingleFileStore().preload(true).shared(false).addProperty("location", "./resource/store/temp")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
	}

}
