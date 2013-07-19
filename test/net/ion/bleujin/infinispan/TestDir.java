package net.ion.bleujin.infinispan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.OldCacheStoreConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.WriteNode;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.radon.impl.util.CsvReader;

import org.apache.lucene.store.FSDirectory;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestDir extends TestCase {

	private Central central;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
		.defaultClusteredBuilder()
			.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.build();
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);
		String wsName = "test" ;
		
		OldCacheStoreConfig config = OldCacheStoreConfig.create().location("./resource/ff3") ;
		
		dcm.defineConfiguration(wsName + ".node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry())
				.transaction().syncCommitPhase(true).syncRollbackPhase(true)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", config.location())
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

		dcm.defineConfiguration(wsName + ".meta", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable()
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		
		dcm.defineConfiguration(wsName + ".chunks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(config.maxChunkEntries()).invocationBatching().enable()
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		
		final Cache<Object, Object> metaCache = dcm.getCache(wsName + ".meta");
		final Cache<Object, Object> chunkCache = dcm.getCache(wsName + ".chunks");
		final Cache<Object, Object> lockCache = dcm.getCache(wsName + ".locks");
		
		metaCache.start() ;
		chunkCache.start() ;
		lockCache.start() ;

//		Directory dir = new DirectoryBuilderImpl(metaCache, chunkCache, lockCache, wsname).chunkSize(1024 * 64).create(); // .chunkSize()
		InfinispanDirectory dir = new InfinispanDirectory(metaCache, chunkCache, lockCache, wsName, config.chunkSize());
		this.central = CentralConfig.oldFromDir(dir).build();
	}
	
	@Override
	protected void tearDown() throws Exception {
		central.close() ;
		super.tearDown();
	}
	
	public void testWrite() throws Exception {
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i = 0; i < 10; i++) {
					isession.insertDocument(MyDocument.testDocument().add(MyField.keyword("name", "bleujin")).add(MyField.number("index", i))) ;
				}
				return null;
			}
		}) ;
	}
	
	public void testRunning() throws Exception {
		while(true){
			final List<ReadDocument> found = central.newSearcher().search("").getDocument() ;
			if (found.size() > 0){
				Debug.line(found.size(), found) ;
				Debug.line(central.newSearcher().search("").totalCount()) ;
				
			}
			
			Thread.sleep(1000) ;
		}
	}
	
	public void testRead() throws Exception {
		central.newSearcher().search("").debugPrint() ;
	}
	
	
	public void testSpeed() throws Exception {
		
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession indexsession) throws Exception {
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				int max = 60000 ;
				while(line != null && line.length > 0 && max-- > 0 ){
//					if (headers.length != line.length ) continue ;
					WriteDocument wnode = MyDocument.newDocument("" + max) ;
					for (int ii = 0, last = headers.length; ii < last ; ii++) {
						if (line.length > ii) wnode.unknown(headers[ii], line[ii]) ;
					}
					indexsession.updateDocument(wnode) ;
					
					line = reader.readLine() ;
					if ((max % 1000) == 0) {
						System.out.print('.') ;
					}
					if ((max % 2000) == 0) {
						indexsession.continueUnit() ;
					} 
				}
				reader.close() ;
				Debug.line("endJob") ;
				return null;
			}
		}) ;
		
	}
	
	public void testSpeedFs() throws Exception {
		final FSDirectory dir = FSDirectory.open(new File("./resource/ff4"));
		central = CentralConfig.oldFromDir(dir).build() ;
		
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession indexsession) throws Exception {
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				int max = 60000 ;
				while(line != null && line.length > 0 && max-- > 0 ){
//					if (headers.length != line.length ) continue ;
					WriteDocument wnode = MyDocument.newDocument("" + max) ;
					for (int ii = 0, last = headers.length; ii < last ; ii++) {
						if (line.length > ii) wnode.unknown(headers[ii], line[ii]) ;
					}
					indexsession.updateDocument(wnode) ;
					
					line = reader.readLine() ;
					if ((max % 1000) == 0) {
						System.out.print('.') ;
					}
					if ((max % 2000) == 0) {
						indexsession.continueUnit() ;
					} 
				}
				reader.close() ;
				Debug.line("endJob") ;
				return null;
			}
		}) ;
		
	}
	
	
}
