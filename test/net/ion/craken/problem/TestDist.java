package net.ion.craken.problem;

import java.io.File;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;
import net.ion.framework.util.WithinThreadExecutor;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;

import com.sun.corba.se.impl.activation.RepositoryImpl;

public class TestDist extends TestCase {

	public void testFirst() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/first"));
		final Craken r = Craken.create();

		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("./resource/temp/first").distMode(CacheMode.DIST_SYNC));
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		ReadSession session = r.login("ics");

		int count = 0;
		while (true) {
			Thread.sleep(1000);
			final int index = count++;
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/index", index).property("index", index);
					return null;
				}
			});
			if (index == 1) Debug.line("started");
		}
	}


	public void testSecond() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/second"));
		
		final Craken r = Craken.create();
		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("./resource/temp/second").distMode(CacheMode.DIST_SYNC));

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});


		ReadSession session = r.login("ics");
		while(true){
			Thread.sleep(1000);
			Debug.line(session.ghostBy("/index").children().count()) ;
		}
	}
	
	
	public void testCofirmFirst() throws Exception {
		final Craken r = Craken.create();

		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("./resource/temp/first").distMode(CacheMode.DIST_SYNC));
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		ReadSession session = r.login("ics");
		Debug.line(session.workspace().cache().keySet());
		Debug.line(session.workspace().central().newSearcher().createRequest("").find().totalCount()); 
	}
	
	
	public void testLuceneIndex() throws Exception {
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder().transport().defaultTransport()
				.addProperty("configurationFile", "./resource/config/craken-tcp.xml")
					.globalJmxStatistics().enabled(false).build() ;
		DefaultCacheManager dm = new DefaultCacheManager(gconfig) ; // new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		
		
		ClusteringConfigurationBuilder idx_meta_builder = new ConfigurationBuilder().persistence().passivation(false)
				.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering().cacheMode(CacheMode.REPL_SYNC);

		ClusteringConfigurationBuilder idx_chunk_builder = new ConfigurationBuilder().persistence().passivation(false)
				.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering().cacheMode(CacheMode.DIST_SYNC);
		
		dm.defineConfiguration("meta", idx_meta_builder.build()) ;
		dm.defineConfiguration("chunk", idx_chunk_builder.build()) ;
//		dm.defineConfiguration("lock", idx_meta_builder.build()) ;
		
		Cache<?, ?> meta = dm.getCache("meta") ;
		Cache<?, ?> chunk = dm.getCache("chunk") ;
		
		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(meta, chunk, dm.getCache("lock"), "test") ;
		Directory dir = bcontext.create() ;
		
		Central central = CentralConfig.oldFromDir(dir).indexConfigBuilder().executorService(new WithinThreadExecutor()).build() ;
		
		int count = 0;
		while(true){
			central.newIndexer().index(new IndexJob<Void>() {
				@Override
				public Void handle(IndexSession isession) throws Exception {
					isession.newDocument().keyword("name", "bleujin").update() ;
					return null;
				}
			})  ;
			Thread.sleep(1000);
			if (count++ == 1) Debug.line("started");
			
//			IndexSearcher isearcher = new IndexSearcher(DirectoryReader.open(dir)) ;
//			Debug.line(isearcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE).totalHits);
		}
	}
	
	
	public void testLuceneSearch() throws Exception {
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder().transport().defaultTransport()
				.addProperty("configurationFile", "./resource/config/craken-tcp.xml")
					.globalJmxStatistics().enabled(false).build() ;
		DefaultCacheManager dm = new DefaultCacheManager(gconfig) ; // new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		
		ClusteringConfigurationBuilder idx_meta_builder = new ConfigurationBuilder().persistence().passivation(false)
				.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering().cacheMode(CacheMode.REPL_SYNC);

		ClusteringConfigurationBuilder idx_chunk_builder = new ConfigurationBuilder().persistence().passivation(false)
				.clustering().stateTransfer().timeout(300, TimeUnit.SECONDS).clustering().cacheMode(CacheMode.DIST_SYNC) ;
		
		dm.defineConfiguration("meta", idx_meta_builder.build()) ;
		dm.defineConfiguration("chunk", idx_chunk_builder.build()) ;
//		dm.defineConfiguration("lock", idx_meta_builder.build()) ;
		
		Cache<?, ?> meta = dm.getCache("meta") ;
		Cache<?, ?> chunk = dm.getCache("chunk") ;
		
		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(meta, chunk, dm.getCache("lock") , "test") ;
		Directory dir = bcontext.create() ;
		Central central = CentralConfig.oldFromDir(dir).indexConfigBuilder().executorService(new WithinThreadExecutor()).build() ;
		
		while(true){
			Debug.line(central.newSearcher().createRequest("").find().totalCount()) ;
			Thread.sleep(2000);
		}
	}

	
	
}
