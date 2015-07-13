package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.bleujin.craken.TestCraken;
import net.ion.craken.io.FileVisitor;
import net.ion.craken.io.Files;
import net.ion.craken.loaders.EntryKey;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

import org.apache.lucene.store.Directory;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.batch.BatchContainer;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;

public class TestCashDouble extends TestCase{

	
	public void testRun() throws Exception {
		DefaultCacheManager dcm = new DefaultCacheManager() ;
		
		dcm.defineConfiguration("front", new ConfigurationBuilder()
			.eviction().maxEntries(500)
	//		.eviction().expiration().lifespan(30, TimeUnit.SECONDS)
			.transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable()
			.build()) ;
		
		String rootPath = "./resource/double" ;
		String searchIndexPath = "./resource/double/index" ;
		String searchChunkPath = "./resource/double/chunk" ;
		
		FileUtil.deleteDirectory(new File(rootPath));
		
		ClusteringConfigurationBuilder idx_meta_builder = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(rootPath)
				.async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
				.modificationQueueSize(1000).threadPoolSize(3)
				.clustering() ;
		
		ClusteringConfigurationBuilder idx_chunk_builder = new ConfigurationBuilder()
			.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
			.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(searchIndexPath)
			.dataLocation(searchChunkPath).async().disable()
			.modificationQueueSize(1000).threadPoolSize(3)
			.eviction().maxEntries(20).clustering() ;
		
		dcm.defineConfiguration("sindex", idx_meta_builder.build()) ;
		dcm.defineConfiguration("schunk", idx_chunk_builder.build()) ;
		
		Cache<?, ?> metaCache = dcm.getCache("sindex");
		Cache<?, ?> dataCache = dcm.getCache("schunk");

		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, dataCache, metaCache, "search");
		bcontext.chunkSize(4 * 1024 * 1024);
		Directory directory = bcontext.create();
		
		Cache<String,String> cache = dcm.getCache("front") ;
		final AdvancedCache<String, String> acache = cache.getAdvancedCache();
		acache.addInterceptor(new IndexInterceptor(directory), 0);
		
		final BatchContainer bcon = acache.getBatchContainer() ;
		bcon.startBatch(true) ;
		
		Files.walkFileTree(new File("C:/crawl/enha/wiki"), new FileVisitor() {
			private long start = System.currentTimeMillis();
			private AtomicInteger count = new AtomicInteger() ;
			private int maxcount = 200000 ;

			public FileVisitResult visitFile(File file) throws IOException {
				try {
					if (file.isDirectory())
						return FileVisitResult.CONTINUE;
					int icount = count.incrementAndGet();
					
					if (icount >= maxcount) return FileVisitResult.TERMINATE ;
					
					if ((icount % 200) == 0) {
						System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
						this.start = System.currentTimeMillis();
						bcon.endBatch(true, true);
						bcon.startBatch() ;
					}

					String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
					String wpath = makePath(file) ;
					acache.put(wpath, content);

					return FileVisitResult.CONTINUE;
				} catch (Throwable e) {
					System.err.println(file);
					throw new IOException(e);
				}
			}
			
			private String makePath(File path) throws IOException{
//				return "/" + new ObjectId().toString() ;
				return TestCraken.makePathString(path) ;
			}
			
		});
		
		bcon.endBatch(true, false);
	}
}

class IndexInterceptor extends BaseCustomInterceptor {

	private Central central;
	public IndexInterceptor(Directory dir) throws IOException {
		this.central = CentralConfig.oldFromDir(dir).build() ;
	}

	@Override
	public Object visitCommitCommand(final TxInvocationContext ctx, CommitCommand command) throws Throwable {
		Indexer indexer = central.newIndexer() ;
		
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				List<DataWriteCommand> list = ctx.getModifications() ;
				for (DataWriteCommand wcom : list) {
					PutKeyValueCommand pcommand = (PutKeyValueCommand) wcom ;
					String pathKey = (String) wcom.getKey()  ;
					String content = (String)pcommand.getValue() ;
					
					switch (wcom.getCommandId()) {
					case PutKeyValueCommand.COMMAND_ID :
						
						WriteDocument wdoc = isession.newDocument(pathKey) ;
						wdoc.keyword(EntryKey.PARENT, pathKey) ;
						wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());
						wdoc.text("content", content) ;
						
						wdoc.add(MyField.noIndex(EntryKey.VALUE, content).ignoreBody(true));
						wdoc.update() ;
						break ;
					default:
						break;
					}
				}
				return null;
			}
		}) ;

		return invokeNextInterceptor(ctx, command) ;	
	}
}