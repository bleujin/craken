package net.ion.craken.node.crud.store;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.impl.SessionWorkspace;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
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

public class SessionWorkspaceBuilder extends WorkspaceConfigBuilder {

	private GridFilesystem gfs;
	private Central central;

	public SessionWorkspaceBuilder() {
	}

	@Override
	public WorkspaceConfigBuilder build(DefaultCacheManager dm, String wsName) throws IOException {
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

		this.central = makeCentral(dm, wsName);
		this.gfs = makeGridSystem(dm, wsName);
		
		return this ;
	}

	public GridFilesystem gfs(){
		return gfs ;
	}
	
	public Central central(){
		return this.central ;
	}
	
	private GridFilesystem makeGridSystem(DefaultCacheManager dm, String wsName){
		Cache<String, byte[]> blobChunk = dm.getCache(blobChunk(wsName));
		Cache<String, Metadata> blobMeta = dm.getCache(blobMeta(wsName));

		return new GridFilesystem(blobChunk, blobMeta, 8192);
	}
	
	private Central makeCentral(DefaultCacheManager dm, String wsName) throws IOException {
		return CentralConfig.newRam().indexConfigBuilder().executorService(new WithinThreadExecutor()).build();
	}
	
	
	public Workspace createWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache) throws IOException {
		return  new SessionWorkspace(craken, cache, this);
	}

	public void createInterceptor(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans){
	}

}
