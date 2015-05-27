package net.ion.craken.node.crud.store;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import net.ion.craken.loaders.CrakenStoreConfigurationBuilder;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.GridLoaderConfiguration;
import net.ion.craken.loaders.GridLoaderConfigurationBuilder;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.TreeNodeKey.Action;
import net.ion.craken.node.crud.impl.GridWorkspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.store.Directory;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.io.GridFilesystem;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;

public class GridFileConfigBuilder extends CrakenWorkspaceConfigBuilder {

	private GridFilesystem gfs;
	private String rootPath;
	private Central central;

	public GridFileConfigBuilder(String rootPath) throws IOException {
		this.rootPath = rootPath ;
	}

	@Override
	public CrakenWorkspaceConfigBuilder init(DefaultCacheManager dm, String wsName) throws IOException {
		if (StringUtil.isNotBlank(rootPath)) {

			File rootFile = new File(rootPath);
			String dataIndexPath = new File(rootFile, wsName + "_dataindex").getCanonicalPath() ;
			String dataChunkPath = new File(rootFile, wsName + "_datachunk").getCanonicalPath() ;

			String searchIndexPath = new File(rootFile, wsName + "_searchindex").getCanonicalPath() ;
			String searchChunkPath = new File(rootFile, wsName + "_searchchunk").getCanonicalPath() ;
			
			ClusteringConfigurationBuilder real_configBuilder = null ;
			ClusteringConfigurationBuilder meta_configBuilder = null ;
			ClusteringConfigurationBuilder chunk_configBuilder = null ;
			ClusteringConfigurationBuilder blob_metaBuilder = null ;
			ClusteringConfigurationBuilder blob_chunkBuilder = null ;

			real_configBuilder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration())
				.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable()
				.eviction().maxEntries(maxEntry())
			.clustering() ; // .eviction().expiration().lifespan(10, TimeUnit.SECONDS) ;
			
			
			meta_configBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath)
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.clustering() ;

			chunk_configBuilder = new ConfigurationBuilder()
				.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
				.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(searchIndexPath)
				.dataLocation(searchChunkPath).async().disable()
				.eviction().maxEntries(maxSegment()).clustering();
			
			blob_metaBuilder = new ConfigurationBuilder() 
				.persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath)
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.clustering();
			
			blob_chunkBuilder = new ConfigurationBuilder()
				.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
				.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(dataIndexPath)
				.dataLocation(dataChunkPath).async().disable()
				.eviction().maxEntries(maxEntry()).clustering();
			
			if (cacheMode().isClustered() && cacheMode().isReplicated()){
				real_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering().l1().enable().clustering() ; 
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
			} else if (cacheMode().isClustered()){
				real_configBuilder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering().l1().enable().clustering() ; 
				meta_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				chunk_configBuilder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(100, TimeUnit.SECONDS) ;
			}
			
			dm.defineConfiguration(wsName, real_configBuilder.build()) ;
			dm.defineConfiguration(wsName + "-meta", meta_configBuilder.build()) ;
			dm.defineConfiguration(wsName + "-chunk", chunk_configBuilder.build()) ;
			dm.defineConfiguration(blobMeta(wsName), blob_metaBuilder.build()) ;
			dm.defineConfiguration(blobChunk(wsName), blob_chunkBuilder.build()) ;
			

			this.gfs = makeGridSystem(dm, wsName) ;
			this.central = makeCentral(dm, wsName) ;

		}
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
		Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dm.getCache(wsName);
		String name = cache.getName();
		EmbeddedCacheManager cacheManager = cache.getCacheManager();
		Cache<?, ?> metaCache = cacheManager.getCache(name + "-meta");
		Cache<?, ?> dataCache = cacheManager.getCache(name + "-chunk");

		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, dataCache, metaCache, name);
		bcontext.chunkSize(1024 * 1024);
		Directory directory = bcontext.create();
		return CentralConfig.oldFromDir(directory).build();
	}
	
	
	public Workspace createWorkspace(Craken craken, AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache) throws IOException {
		return  new GridWorkspace(craken, cache, this);
	}

	public void createInterceptor(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans){
		cache.addInterceptor(new GFSInterceptor(gfs(), central, trans), 0);
	}
}

class GFSInterceptor extends BaseCustomInterceptor {

	private Central central;
	private com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans;
	private GridFilesystem gfs;
	
	public GFSInterceptor(GridFilesystem gfs, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans) {
		this.gfs = gfs ;
		this.central = central ;
		this.trans = trans ;
	}

	@Override
	 public Object visitCommitCommand(final TxInvocationContext ctx, CommitCommand command) throws Throwable {
		if (ctx.getTransaction() == null) return  invokeNextInterceptor(ctx, command) ;
		
		final IndexWriteConfig iwconfig = trans.getIfPresent(ctx.getTransaction()) ;
		if (iwconfig == null) return  invokeNextInterceptor(ctx, command) ;
		

		IndexJob<Void> indexJob = new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				List<DataWriteCommand> list = extractCommand(ctx.getModifications()) ;
				
				for (DataWriteCommand wcom : list) {
					TreeNodeKey tkey = (TreeNodeKey) wcom.getKey()  ;
					
					if (tkey.getFqn().isRoot()) continue ;
					String pathKey = tkey.fqnString();
					switch (wcom.getCommandId()) {
					case PutKeyValueCommand.COMMAND_ID :
						
						File dirFile = gfs.getFile(tkey.fqnString()) ;
						if (! dirFile.exists()) dirFile.mkdirs() ;
						if (tkey.getType().isStructure()) break ;
						
						
						
						String contentFileName = tkey.fqnString() + "/" + tkey.getFqn().name()  + ".node";
						File contentFile = gfs.getFile(contentFileName) ;
						if (! contentFile.getParentFile().exists()) {
							contentFile.getParentFile().mkdirs() ;
						}
		
						
						PutKeyValueCommand pcommand = (PutKeyValueCommand) wcom ;
						Map<PropertyId, PropertyValue> valueMap = (Map) pcommand.getValue() ;

						WriteDocument wdoc = isession.newDocument(pathKey) ;
						wdoc.keyword(EntryKey.PARENT, tkey.getFqn().getParent().toString()) ;
						wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());

						JsonObject nodeJson = new JsonObject() ;
						for(PropertyId pid : valueMap.keySet()){
							final String propId = pid.idString() ;
							PropertyValue pvalue = valueMap.get(pid) ;
							nodeJson.add(propId, pvalue.json()); // data
							
							JsonArray jarray = pvalue.asJsonArray(); // index
							if (pid.type() == PType.NORMAL) {
								VType vtype = pvalue.type() ;
								for (JsonElement e : jarray.toArray()) {
									if (e == null)
										continue;
									FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
									fieldIndex.index(wdoc, propId, vtype, e.isJsonObject() ? e.toString() : e.getAsString());
								}
							} else { // refer
								for (JsonElement e : jarray.toArray()) {
									if (e == null)
										continue;
									FieldIndex.KEYWORD.index(wdoc, '@' + propId, e.getAsString());
								}
							}
							
						}
						IOUtil.copyNClose(new StringInputStream(nodeJson.toString()), gfs.getOutput(contentFileName)) ;
						
						
						if (! iwconfig.isIgnoreIndex()) {
							if (tkey.action() == Action.CREATE) wdoc.insert() ; 
							else wdoc.update() ;
						}
						break;
					case RemoveCommand.COMMAND_ID :
						gfs.getFile(pathKey).delete() ;
						if (! iwconfig.isIgnoreIndex())  isession.deleteById(pathKey) ;
					default:
						break;
					}
				}
				return null ;
			}
		} ;
		central.newIndexer().index(indexJob) ;
		
		trans.invalidate(ctx.getTransaction());
		return invokeNextInterceptor(ctx, command) ;
	}
	
	private List<DataWriteCommand> extractCommand(List list){
		List<DataWriteCommand> result = ListUtil.newList() ;
		for (Object obj : list) {
			if (obj instanceof DataWriteCommand){
				DataWriteCommand wcom = (DataWriteCommand) obj ;
				TreeNodeKey tkey = (TreeNodeKey) wcom.getKey() ;
//				if (tkey.getType().isStructure()) continue ;
				
				result.add(wcom) ;
			}
		}
		
		return result ;
	}
}
