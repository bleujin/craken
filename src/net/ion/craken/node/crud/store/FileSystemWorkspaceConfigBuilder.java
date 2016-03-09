package net.ion.craken.node.crud.store;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.impl.FileSystemWorkspace;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.io.GridFilesystem;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.TransactionMode;

public class FileSystemWorkspaceConfigBuilder extends WorkspaceConfigBuilder {

	
	private File rootDir;
	private GridFilesystem gfs;
	private Central central;
	private File dataDir;
	private File indexDir;
	
	public FileSystemWorkspaceConfigBuilder(String rootPath) throws CorruptIndexException, IOException {
		this.rootDir = new File(rootPath) ;
		this.indexDir = new File(rootDir, "index") ;
		this.dataDir = new File(rootDir, "data");
		
		if (! indexDir.exists()){
			indexDir.mkdirs() ;
		}
	}

	@Override
	public Workspace createWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache) throws IOException {
		return new FileSystemWorkspace(craken, cache, this);
	}
	
	@Override
	public WorkspaceConfigBuilder build(DefaultCacheManager dm, String wsName) throws IOException {
		ClusteringConfigurationBuilder real_configBuilder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).eviction().maxEntries(maxEntry()) // .eviction().expiration().lifespan(30, TimeUnit.SECONDS)
				.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().clustering();
		
		dm.defineConfiguration(wsName, real_configBuilder.build());
		
		dm.defineConfiguration(blobChunk(wsName), new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).eviction().expiration().lifespan(30, TimeUnit.SECONDS)
				.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().clustering().build());
		dm.defineConfiguration(blobMeta(wsName), new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).eviction().expiration().lifespan(30, TimeUnit.SECONDS)
				.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().clustering().build());

		this.central = CentralConfig.newLocalFile().dirFile(indexDir).build() ;
		this.gfs = makeGridSystem(dm, wsName);
		return this;
	}

	public static WorkspaceConfigBuilder test() throws CorruptIndexException, IOException {
		return new FileSystemWorkspaceConfigBuilder("./resource/fstemp");
	}
	
	private GridFilesystem makeGridSystem(DefaultCacheManager dm, String wsName) {
		Cache<String, byte[]> blobChunk = dm.getCache(blobChunk(wsName));
		Cache<String, Metadata> blobMeta = dm.getCache(blobMeta(wsName));

		return new GridFilesystem(blobChunk, blobMeta, 8192);
	}

	
	public GridFilesystem gfs() {
		return gfs;
	}

	public Central central() {
		return this.central;
	}
	
	public void createInterceptor(TreeCache<PropertyId, PropertyValue> tcache, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans) {
//		tcache.getCache().getAdvancedCache().addInterceptor(new DebugInterceptor(gfs(), central, trans), 0);
	}

	public File dataDir() {
		return dataDir ;
	}

	
}
