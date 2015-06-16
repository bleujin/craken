package net.ion.craken.node.crud.store;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.impl.OldWorkspace;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.framework.util.StringUtil;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.store.Directory;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.io.GridFilesystem;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;

public class OldFileConfigBuilder extends WorkspaceConfigBuilder {

	private CacheMode cacheMode = CacheMode.LOCAL ;
	
	private GridFilesystem gfs;
	private String rootPath;
	private Central central;
	
	OldFileConfigBuilder(String rootPath){
		this.rootPath = rootPath ;
	}
	
	public static OldFileConfigBuilder directory(String rootPath) {
		return new OldFileConfigBuilder(rootPath);
	}

	@Override
	public Workspace createWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache) throws IOException {
		return new OldWorkspace(craken, cache, this);
	}
	
	public WorkspaceConfigBuilder build(DefaultCacheManager dm, String wsName) throws IOException {

		if (StringUtil.isBlank(rootPath)) {
			ClusteringConfigurationBuilder real_configBuilder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration())
					.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().clustering();

			ClusteringConfigurationBuilder idx_meta_builder = new ConfigurationBuilder().persistence().passivation(false)
					.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering();
			ClusteringConfigurationBuilder idx_chunk_builder = new ConfigurationBuilder().persistence().passivation(false)
					.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering();;
			ClusteringConfigurationBuilder idx_lock_builder = new ConfigurationBuilder().persistence().passivation(true)
					.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering();;

			if (cacheMode().isClustered()){
				real_configBuilder.cacheMode(CacheMode.REPL_ASYNC) ;
				idx_meta_builder.cacheMode(CacheMode.REPL_SYNC) ;
				idx_chunk_builder.cacheMode(CacheMode.DIST_SYNC) ;
				idx_lock_builder.cacheMode(CacheMode.REPL_SYNC) ;
			}
					
			dm.defineConfiguration(wsName, real_configBuilder.build());
			dm.defineConfiguration(wsName + "-meta", idx_meta_builder.build());
			dm.defineConfiguration(wsName + "-chunk", idx_chunk_builder.build());
			dm.defineConfiguration(wsName + "-lock", idx_lock_builder.build());

			this.central = makeCentral(dm, wsName);
			this.gfs = makeGridSystem(dm, wsName);

		} else if (StringUtil.isNotBlank(rootPath)) {
			
			ClusteringConfigurationBuilder real_configBuilder = null;
			ClusteringConfigurationBuilder meta_configBuilder = null ;
			ClusteringConfigurationBuilder chunk_configBuilder = null ;
			ClusteringConfigurationBuilder blob_metaBuilder = null ;
			ClusteringConfigurationBuilder blob_chunkBuilder = null ;

			real_configBuilder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).eviction().maxEntries(maxEntry()) // .eviction().expiration().lifespan(30, TimeUnit.SECONDS)
					.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().clustering();

			meta_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path())
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(100).threadPoolSize(10)
				.clustering() ;

			chunk_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path())
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(100).threadPoolSize(10)
				.eviction().maxEntries(maxSegment())
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
				.eviction().maxEntries(maxSegment()).expiration().maxIdle(100, TimeUnit.SECONDS)
				.clustering();
			
			
			if (cacheMode().isClustered() && cacheMode().isReplicated()){
				real_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering() ; //.l1().enable().clustering() ; 
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
			} else if (cacheMode().isClustered()){
				real_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering() ; //.l1().enable().clustering() ; 
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.DIST_SYNC) ;
			}
			
			dm.defineConfiguration(wsName, real_configBuilder.build());
			
			dm.defineConfiguration(wsName + "-meta", meta_configBuilder.build()) ;
			dm.defineConfiguration(wsName + "-chunk", chunk_configBuilder.build()) ;
			this.central = makeCentral(dm, wsName);
			
			dm.defineConfiguration(blobMeta(wsName), blob_metaBuilder.build()) ;
			dm.defineConfiguration(blobChunk(wsName), blob_chunkBuilder.build()) ;
			this.gfs = makeGridSystem(dm, wsName);
		}
		return this ;
	}
	
	public String path(){
		return rootPath ;
	}
	
	public OldFileConfigBuilder distMode(CacheMode cmode){
		this.cacheMode = cmode ;
		return this ;
	}
	
	public CacheMode cacheMode(){
		return cacheMode ;
	}

	public GridFilesystem gfs() {
		return gfs;
	}

	public Central central() {
		return this.central;
	}


	private GridFilesystem makeGridSystem(DefaultCacheManager dm, String wsName) {
		Cache<String, byte[]> blobChunk = dm.getCache(blobChunk(wsName));
		Cache<String, Metadata> blobMeta = dm.getCache(blobMeta(wsName));

		return new GridFilesystem(blobChunk, blobMeta, 8192);
	}

	private Central makeCentral(DefaultCacheManager dm, String wsName) throws IOException {
		Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dm.getCache(wsName);
		String name = cache.getName();
		EmbeddedCacheManager cacheManager = cache.getCacheManager();
		Cache<?, ?> metaCache = cacheManager.getCache(name + "-meta");
		Cache<?, ?> dataCache = cacheManager.getCache(name + "-chunk");
		Cache<?, ?> lockCache = cacheManager.getCache(name + "-lock");

		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, dataCache, lockCache, name);
//		bcontext.chunkSize(1024 * 1024);
		Directory directory = bcontext.create();
		
		
		return CentralConfig.oldFromDir(directory).indexConfigBuilder().executorService(new WithinThreadExecutor()).build();
	}
}
