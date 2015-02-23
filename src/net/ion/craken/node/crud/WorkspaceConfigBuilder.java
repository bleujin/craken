package net.ion.craken.node.crud;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.ion.craken.loaders.CrakenStoreConfigurationBuilder;
import net.ion.framework.util.StringUtil;

import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfiguration;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.file.SingleFileStore;

public class WorkspaceConfigBuilder {

	private String path;
	private WorkspaceConfigBuilder(String path) {
		this.path = path ;
	}

	public static WorkspaceConfigBuilder directory(String path) {
		return new WorkspaceConfigBuilder(path);
	}

	void init(DefaultCacheManager dm, String wsName) {
		if (StringUtil.isNotBlank(path)) {
			Configuration meta_config = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3)
				.build() ;
			dm.defineConfiguration(wsName + "-meta", meta_config) ;

			Configuration chunk_config = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3) 
//				.eviction().maxEntries(50)
				.build() ;
			dm.defineConfiguration(wsName + "-chunk", chunk_config) ;
		}
	}


}
