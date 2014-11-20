package net.ion.craken.node.problem.inf.v7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.radon.util.csv.CsvReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestIndex extends TestCase {

	private DefaultCacheManager dm;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dm = new DefaultCacheManager() ;
		String path = "./resource/temp/chunk";
		EvictionConfigurationBuilder builder = new ConfigurationBuilder() //.read(dm.getDefaultCacheConfiguration())
			.invocationBatching().enable()
			.persistence().addSingleFileStore().location("./resource/temp/chunk")
			.fetchPersistentState(true).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false)
			.async().enabled(false).flushLockTimeout(20000).shutdownTimeout(1000).modificationQueueSize(1000).threadPoolSize(5)
			.eviction().maxEntries(20000).strategy(EvictionStrategy.LIRS) ; 
		
		Configuration meta_config = new ConfigurationBuilder().persistence().passivation(false)
				.addSingleFileStore().fetchPersistentState(false).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path)
				.async().enable().flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3) 
				.build() ;
		
		dm.defineConfiguration("chunk", builder.build()) ;
		dm.defineConfiguration("meta", meta_config) ;
		
		dm.start();
	}

	@Override
	protected void tearDown() throws Exception {
		this.dm.stop(); 
		super.tearDown();
	}
	
	
	public void testRead() throws Exception {
		Cache<Object, Object> chunk = dm.getCache("chunk") ;
		Cache<?, ?> meta = dm.getCache("meta") ;
		
		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(meta, chunk, meta, "search");
		bcontext.chunkSize(1024 * 1024);
		Directory directory = bcontext.create();

		
		IndexSearcher searcher = new IndexSearcher(IndexReader.open(directory)) ;
		TopDocs topdocs = searcher.search(new MatchAllDocsQuery(), 100);
		Debug.line(topdocs.totalHits);
		
		DirectoryReader reader = IndexReader.open(directory) ;
		for(ScoreDoc sd : topdocs.scoreDocs){
			Document doc = reader.document(sd.doc);
			Debug.line(doc);
		}
		
		directory.close();
	}
	
	
	public void testCentralIndex() throws Exception {
		Cache<Object, Object> chunk = dm.getCache("chunk") ;
		Cache<?, ?> meta = dm.getCache("meta") ;
		
		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(meta, chunk, meta, "search");
		bcontext.chunkSize(1024 * 1024);
		
		Directory directory = bcontext.create();

		Central central = CentralConfig.oldFromDir(directory).indexConfigBuilder().indexAnalyzer(new StandardAnalyzer(SearchConstant.LuceneVersion)).build() ;
		Indexer indexer = central.newIndexer() ;

		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				int max = 500000 ;
				
				while(line != null && line.length > 0 && max-- > 0 ){
					createDoc("/bleujin/" + max, isession, headers, line).updateVoid() ;
					line = reader.readLine() ;
					if ((max % 4999) == 0) {
						System.out.print('.') ;
						isession.continueUnit() ;
					} 
				}

				return null;
			}

			private WriteDocument createDoc(String id, IndexSession isession, String[] headers, String[] line) {
				WriteDocument doc = isession.newDocument(id) ;
				for(int i = 0 ; i <headers.length ; i++){
					String header = headers[i] ;
					String value = line[i] ;
					doc.unknown(header, value) ;
				}
				return doc;
			}
		}) ;
		Debug.line("endJob") ;
//		new InfinityThread().startNJoin(); 
	}
	
	
	
	public void testIndex() throws Exception { // 59 sec
		Cache<Object, Object> chunk = dm.getCache("chunk") ;
		Cache<?, ?> meta = dm.getCache("meta") ;
		
		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(meta, chunk, meta, "search");
		bcontext.chunkSize(1024 * 1024);
		
		Directory directory = bcontext.create();
		
//		directory = FSDirectory.open(new File("./resource/temp/index")) ;
		
		IndexWriterConfig iwconfig = new IndexWriterConfig(Version.LUCENE_44, new StandardAnalyzer(SearchConstant.LuceneVersion));
		IndexWriter iw = new IndexWriter(directory, iwconfig) ;
		
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 500000 ;
		
		int skipCount = 0 ;
		while(line != null && line.length > 0 && max-- > 0 ){
			String path = "/bleujin/" + max;
			Document doc = createDoc(path, iw, headers, line) ;
			iw.updateDocument(new Term("id", path), doc);
			
			line = reader.readLine() ;
			if ((max % 4999) == 0) {
				System.out.print('.') ;
				iw.commit(); 
			} 
		}
		iw.commit();
		iw.close(); 
		
		directory.close(); 

		Debug.line("endJob", skipCount) ;
		
//		new InfinityThread().startNJoin(); 
	}
	
	

	private Document createDoc(String id, IndexWriter iw, String[] headers, String[] values) {
		Document doc = new Document() ;
		doc.add(new StringField("id", id, Store.YES));
		
		for(int i = 0 ; i <headers.length ; i++){
			String name = headers[i] ;
			String value = (values.length > i) ? values[i] : "" ;
			if (value.contains(" "))
				doc.add(new TextField(name, value, Store.YES));
			else 
				doc.add(new StringField(name, value, Store.YES)) ;
		}

		return doc;
	}
}
