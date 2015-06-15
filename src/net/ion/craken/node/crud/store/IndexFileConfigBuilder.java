package net.ion.craken.node.crud.store;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.impl.IndexWorkspace;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.store.Directory;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.io.GridFilesystem;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;

public class IndexFileConfigBuilder extends WorkspaceConfigBuilder {

	private GridFilesystem gfs;
	private String rootPath;
	private Central central;

	public IndexFileConfigBuilder(String rootPath) throws IOException {
		this.rootPath = rootPath;
	}

	@Override
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

			File rootFile = new File(rootPath);
			String dataIndexPath = new File(rootFile, wsName + "_dataindex").getCanonicalPath();
			String dataChunkPath = new File(rootFile, wsName + "_datachunk").getCanonicalPath();

			String searchIndexPath = new File(rootFile, wsName + "_searchindex").getCanonicalPath();
			String searchChunkPath = new File(rootFile, wsName + "_searchchunk").getCanonicalPath();

			ClusteringConfigurationBuilder real_configBuilder = null;
			ClusteringConfigurationBuilder idx_meta_builder = null;
			ClusteringConfigurationBuilder idx_chunk_builder = null;
			ClusteringConfigurationBuilder data_meta_builder = null;
			ClusteringConfigurationBuilder data_chunk_builder = null;

			real_configBuilder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).eviction().maxEntries(maxEntry()) // .eviction().expiration().lifespan(30, TimeUnit.SECONDS)
					.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().clustering();

			idx_meta_builder = new ConfigurationBuilder().persistence().passivation(false).addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath).async().disable().flushLockTimeout(300000)
					.shutdownTimeout(2000).modificationQueueSize(1000).threadPoolSize(3).clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering();

			idx_chunk_builder = new ConfigurationBuilder().persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(searchIndexPath)
					.dataLocation(searchChunkPath).async().disable().modificationQueueSize(1000).threadPoolSize(3).eviction().maxEntries(maxSegment()).clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering();

			data_meta_builder = new ConfigurationBuilder().persistence().passivation(false).addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath).async().disable().flushLockTimeout(300000)
					.shutdownTimeout(2000).modificationQueueSize(1000).threadPoolSize(3).clustering();

			data_chunk_builder = new ConfigurationBuilder().persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(dataIndexPath)
					.dataLocation(dataChunkPath).async().disable().modificationQueueSize(1000).threadPoolSize(3).eviction().maxEntries(maxEntry()).clustering();

			if (cacheMode().isClustered() && cacheMode().isReplicated()) {
				real_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering() ; //.l1().enable().clustering();
				idx_meta_builder.clustering().cacheMode(CacheMode.REPL_SYNC);
				idx_chunk_builder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS);

				data_meta_builder.clustering().cacheMode(CacheMode.REPL_SYNC);
				data_chunk_builder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS);
			} else if (cacheMode().isClustered()) {
				real_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering() ; //.l1().enable().clustering();
				idx_meta_builder.clustering().cacheMode(CacheMode.REPL_SYNC);
				idx_chunk_builder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS);

				data_meta_builder.clustering().cacheMode(CacheMode.REPL_SYNC);
				data_chunk_builder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS);
			}

			dm.defineConfiguration(wsName, real_configBuilder.build());

			// GlobalJmxStatisticsConfigurationBuilder gbuilder = new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains(true) ;
			// DefaultCacheManager other = new DefaultCacheManager(gbuilder.build()) ;
			dm.defineConfiguration(wsName + "-meta", idx_meta_builder.build());
			dm.defineConfiguration(wsName + "-chunk", idx_chunk_builder.build());
			dm.defineConfiguration(wsName + "-lock", idx_meta_builder.build());
			this.central = makeCentral(dm, wsName);

			dm.defineConfiguration(blobMeta(wsName), data_meta_builder.build());
			dm.defineConfiguration(blobChunk(wsName), data_chunk_builder.build());
			this.gfs = makeGridSystem(dm, wsName);

		}
		return this;
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

	public Workspace createWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache) throws IOException {
		return new IndexWorkspace(craken, cache, this);
	}

	public void createInterceptor(TreeCache<PropertyId, PropertyValue> tcache, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans) {
//		tcache.getCache().getAdvancedCache().addInterceptor(new IndexInterceptor(gfs(), central, trans), 0);
	}

}

class DebugInterceptor extends BaseCustomInterceptor {

	private Central central;
	private com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans;

	public DebugInterceptor(GridFilesystem gfs, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans) {
		this.central = central;
		this.trans = trans;
	}

	@Override
	public Object visitCommitCommand(final TxInvocationContext ctx, CommitCommand command) throws Throwable {
		List list = ctx.getModifications();
		for (Object cmd : list) {
			Debug.line(cmd);
		}
		return invokeNextInterceptor(ctx, command);
	}

	protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable{
		Debug.line(ctx, command);
		return super.handleDefault(ctx, command) ;
	}

}
