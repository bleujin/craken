package net.ion.craken.node.crud;

import java.util.concurrent.TimeUnit;

import net.ion.craken.node.crud.store.SifsFileConfigBuilder;
import net.ion.craken.node.crud.store.SingleFileConfigBuilder;
import net.ion.framework.util.StringUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class WorkspaceConfigBuilder {

	private CacheMode cacheMode = CacheMode.LOCAL ;
	
	private int maxEntry = 1000 ;
	private int eviMaxSegment = 10 ;

	private String path;
	private WorkspaceConfigBuilder(String path){
		this.path = path ;
	}
	
	public static WorkspaceConfigBuilder directory(String path) {
		return new WorkspaceConfigBuilder(path);
	}

	public WorkspaceConfigBuilder maxEntry(int maxEntry){
		this.maxEntry = maxEntry ;
		return this ;
	}
	
	public WorkspaceConfigBuilder eviMaxSegment(int eviMaxSegment){
		this.eviMaxSegment = eviMaxSegment ;
		return this ;
	}
	
	protected WorkspaceConfigBuilder init(DefaultCacheManager dm, String wsName) {

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
			
			blob_metaBuilder = new ConfigurationBuilder() 
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
		return this ;
	}
	
	public String path(){
		return path ;
	}
	
	public WorkspaceConfigBuilder distMode(CacheMode cmode){
		this.cacheMode = cmode ;
		return this ;
	}
	
	public CacheMode cacheMode(){
		return cacheMode ;
	}

	public int maxEntry() {
		return maxEntry;
	}

	public int eviMaxSegment(){
		return eviMaxSegment ;
	}

	public final static String BlobChunk(String wsName) {
		return wsName +  "-bchunk";
	}

	public final static String BlobMeta(String wsName){
		return wsName + "-bmeta" ;
	}

}
