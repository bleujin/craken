package net.ion.craken.node.crud.store;

import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.framework.util.StringUtil;

public class SingleFileConfigBuilder extends WorkspaceConfigBuilder{

	public SingleFileConfigBuilder(String path){
		super(path) ;
	}

	protected CacheMode init(DefaultCacheManager dm, String wsName) {

		if (StringUtil.isNotBlank(path())) {
			ClusteringConfigurationBuilder meta_configBuilder = null ;
			ClusteringConfigurationBuilder chunk_configBuilder = null ;
			ClusteringConfigurationBuilder blob_metaBuilder = null ;
			ClusteringConfigurationBuilder blob_chunkBuilder = null ;

			meta_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path())
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(100).threadPoolSize(10)
				.clustering() ;

			chunk_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path())
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(100).threadPoolSize(10)
				.eviction().maxEntries(eviMaxSegment())
				.clustering() ;
			
			blob_metaBuilder = new ConfigurationBuilder() // .clustering().cacheMode(CacheMode.REPL_SYNC)
				.persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path())
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(50).threadPoolSize(3).clustering();
			
			blob_chunkBuilder = new ConfigurationBuilder() 
				.eviction()
				.persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(true).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path())
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(50).threadPoolSize(3)
				.eviction().maxEntries(eviMaxSegment()).expiration().maxIdle(100, TimeUnit.SECONDS)
				.clustering();
			
			
			if (cacheMode().isClustered() && cacheMode().isReplicated()){
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
			} else if (cacheMode().isClustered()){
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.DIST_SYNC) ;
			}
			
			dm.defineConfiguration(wsName + "-meta", meta_configBuilder.build()) ;
			dm.defineConfiguration(wsName + "-chunk", chunk_configBuilder.build()) ;
			dm.defineConfiguration(BlobMeta(wsName), blob_metaBuilder.build()) ;
			dm.defineConfiguration(BlobChunk(wsName), blob_chunkBuilder.build()) ;
		}
		return this.cacheMode() ;
	}

}
