package net.ion.craken.node.crud.store;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.TreeNodeKey.Action;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.infinispan.AdvancedCache;
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
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;

import com.google.common.cache.Cache;

public class SifsFileConfigBuilder extends CrakenWorkspaceConfigBuilder {

	private String rootPath;

	public SifsFileConfigBuilder(String rootPath) {
		this.rootPath = rootPath ;
	}

	@Override
	public CrakenWorkspaceConfigBuilder init(DefaultCacheManager dm, String wsName) throws IOException {
		if (StringUtil.isBlank(rootPath)) {
			ClusteringConfigurationBuilder real_configBuilder = new ConfigurationBuilder()
			.persistence().passivation(false)
			.transaction().invocationBatching().enable()
			.clustering() ; 
			
			dm.defineConfiguration(wsName, real_configBuilder.build()) ;
			
		} else if (StringUtil.isNotBlank(rootPath)) {
			File rootFile = new File(rootPath);
			String dataIndexPath = new File(rootFile, wsName + "_dataindex").getCanonicalPath() ;
			String dataChunkPath = new File(rootFile, wsName + "_datachunk").getCanonicalPath() ;

			String searchIndexPath = new File(rootFile, wsName + "_searchindex").getCanonicalPath() ;
			String searchChunkPath = new File(rootFile, wsName + "_searchchunk").getCanonicalPath() ;

			String blobIndexPath = new File(rootFile, wsName + "_blobindex").getCanonicalPath() ;
			String blobChunkPath = new File(rootFile, wsName + "_blobchunk").getCanonicalPath() ;

			
			ClusteringConfigurationBuilder real_configBuilder = null ;
			ClusteringConfigurationBuilder index_metaBuilder = null ;
			ClusteringConfigurationBuilder index_chunkBuilder = null ;
			ClusteringConfigurationBuilder blob_metaBuilder = null ;
			ClusteringConfigurationBuilder blob_chunkBuilder = null ;

			real_configBuilder = new ConfigurationBuilder()
				.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(true)
				.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(dataIndexPath)
				.dataLocation(dataChunkPath).async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.eviction().maxEntries(this.maxEntry()) // alert : no expire
				.transaction().invocationBatching().enable()
				.clustering() ; 
				
			index_metaBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath)
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.clustering() ;

			index_chunkBuilder = new ConfigurationBuilder().persistence().passivation(false)
				.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
				.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(searchIndexPath).dataLocation(searchChunkPath).async().disable()
				.eviction().maxEntries(maxSegment())
				.clustering();
			
			blob_metaBuilder = new ConfigurationBuilder() // .clustering().cacheMode(CacheMode.REPL_SYNC)
				.persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath)
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.clustering();
			
			blob_chunkBuilder = new ConfigurationBuilder() 
				.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
				.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(blobIndexPath).dataLocation(blobChunkPath).async().disable()
				.eviction().maxEntries(maxEntry())
				.clustering();
			
			
			if (cacheMode().isClustered() && cacheMode().isReplicated()){
				real_configBuilder.clustering().cacheMode(CacheMode.REPL_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering().l1().enable().clustering() ; 
				index_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				index_chunkBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
			} else if (cacheMode().isClustered()){
				real_configBuilder.clustering().cacheMode(CacheMode.DIST_SYNC).persistence().addClusterLoader().remoteCallTimeout(10, TimeUnit.SECONDS).clustering().l1().enable().clustering() ;
				index_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				index_chunkBuilder.clustering().cacheMode(CacheMode.DIST_SYNC) ;
				
				blob_metaBuilder.clustering().cacheMode(CacheMode.REPL_SYNC) ;
				blob_chunkBuilder.clustering().cacheMode(CacheMode.DIST_SYNC) ;
			}
			
			dm.defineConfiguration(wsName, real_configBuilder.build()) ;
			dm.defineConfiguration(wsName + "-meta", index_metaBuilder.build()) ;
			dm.defineConfiguration(wsName + "-chunk", index_chunkBuilder.build()) ;
			dm.defineConfiguration(blobMeta(wsName), blob_metaBuilder.build()) ;
			dm.defineConfiguration(blobChunk(wsName), blob_chunkBuilder.build()) ;
		}
		
		return this;
	}

	@Override
	public void createInterceptor(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans){
//		cache.addInterceptor(new IndexInterceptor(central, trans), 0);
	}


}


class IndexInterceptor extends BaseCustomInterceptor {

	private Central central;
	private com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans;
	public IndexInterceptor(Central central, com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans) {
		this.central = central ;
		this.trans = trans ;
	}

	@Override
	 public Object visitCommitCommand(final TxInvocationContext ctx, CommitCommand command) throws Throwable {
		if (ctx.getTransaction() == null) return  invokeNextInterceptor(ctx, command) ;
		
		final IndexWriteConfig iwconfig = trans.getIfPresent(ctx.getTransaction()) ;
		if (iwconfig == null) return  invokeNextInterceptor(ctx, command) ;
		
		if (iwconfig.isIgnoreIndex()) {
			trans.invalidate(ctx.getTransaction());
			return invokeNextInterceptor(ctx, command) ;
		}
		
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
						WriteDocument wdoc = isession.newDocument(pathKey) ;
						wdoc.keyword(EntryKey.PARENT, tkey.getFqn().getParent().toString()) ;
						wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());
						
						PutKeyValueCommand pcommand = (PutKeyValueCommand) wcom ;
						Map<PropertyId, PropertyValue> valueMap = (Map) pcommand.getValue() ;
						
						for(PropertyId pid : valueMap.keySet()){
							PropertyValue pvalue = valueMap.get(pid) ;
							JsonArray jarray = pvalue.asJsonArray();
							final String propId = pid.getString();
							
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
						
						if (tkey.action() == Action.CREATE) wdoc.insert() ; 
						else wdoc.update() ;
						break;
					case RemoveCommand.COMMAND_ID :
						isession.deleteById(pathKey) ;
					default:
						break;
					}
				}
				return null;
			}
		} ;
		
		if (iwconfig.isAsync()) central.newIndexer().asyncIndex(indexJob) ;
		else central.newIndexer().index(indexJob) ;
		
		trans.invalidate(ctx.getTransaction());
		return invokeNextInterceptor(ctx, command) ;
	}
	
	private List<DataWriteCommand> extractCommand(List list){
		List<DataWriteCommand> result = ListUtil.newList() ;
		for (Object obj : list) {
			if (obj instanceof DataWriteCommand){
				DataWriteCommand wcom = (DataWriteCommand) obj ;
				TreeNodeKey tkey = (TreeNodeKey) wcom.getKey() ;
				if (tkey.getType().isStructure()) continue ;
				
				result.add(wcom) ;
			}
		}
		
		return result ;
	}
	
}