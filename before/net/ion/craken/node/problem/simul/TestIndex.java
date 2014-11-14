package net.ion.craken.node.problem.simul;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.util.csv.CsvReader;

import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.batch.BatchContainer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestIndex extends TestCase{

	
	private Central central;
	private DefaultCacheManager dm;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		
		Cache<?, ?> metaCache = dm.getCache("search-meta");
		Cache<?, ?> chunkCache = dm.getCache("search-chunk");

		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, chunkCache, metaCache, "search");
		bcontext.chunkSize(16384);
		Directory directory = bcontext.create();
		this.central = CentralConfig.oldFromDir(directory).build();
		dm.start(); 
	}
	
	@Override
	protected void tearDown() throws Exception {
		central.close(); 
		dm.stop();  
		super.tearDown();
	}
	
	
	public void testAdd() throws Exception {
		
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
					if (headers.length != line.length ) continue ;
					
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
		
		new InfinityThread().startNJoin(); 
	}

	
	public void testRead() throws Exception {
		Searcher searcher = central.newSearcher() ;
		searcher.createRequestByKey("/bleujin/88888").find().debugPrint();
		new InfinityThread().startNJoin(); 
	}
}
