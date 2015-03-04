package net.ion.craken.node.crud;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.ion.craken.loaders.CrakenStoreConfigurationBuilder;
import net.ion.framework.util.StringUtil;

import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
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
			ClusteringConfigurationBuilder meta_configBuilder = null ;
			ClusteringConfigurationBuilder chunk_configBuilder = null ;
			ClusteringConfigurationBuilder blob_metaBuilder = null ;
			ClusteringConfigurationBuilder blob_chunkBuilder = null ;

			meta_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3).clustering() ;

			chunk_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3).clustering() ; 
//				.eviction().maxEntries(50)
			
			blob_metaBuilder = new ConfigurationBuilder() // .clustering().cacheMode(CacheMode.REPL_SYNC)
				.persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(10).threadPoolSize(3).clustering();
			
			blob_chunkBuilder = new ConfigurationBuilder() // .clustering().cacheMode(CacheMode.DIST_ASYNC)
				.eviction().maxEntries(1000)
				.persistence().passivation(false).addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(10).threadPoolSize(3).clustering();
			
			if (dm.getCacheManagerConfiguration().transport().transport() != null){
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.DIST_SYNC) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.DIST_ASYNC) ;
			}
			
			dm.defineConfiguration(wsName + "-meta", meta_configBuilder.build()) ;
			dm.defineConfiguration(wsName + "-chunk", chunk_configBuilder.build()) ;
			dm.defineConfiguration(BlobMeta(wsName), blob_metaBuilder.build()) ;
			dm.defineConfiguration(BlobChunk(wsName), blob_chunkBuilder.build()) ;
		}
	}
	
	public final static String BlobChunk(String wsName) {
		return wsName +  "-bchunk";
	}

	public final static String BlobMeta(String wsName){
		return wsName + "-bmeta" ;
	}


}
