package net.ion.craken.node.problem.speed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.radon.util.csv.CsvReader;

import org.apache.lucene.store.MMapDirectory;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestBasicSpeed extends TestCase {

	
	// 7sec 20K
	public void testISearcherIndex() throws Exception {
		File path = new File("./resource/isearcher");
		if (! path.exists()) path.mkdirs() ;
		Central central = CentralConfig.oldFromDir(MMapDirectory.open(path)).build();
		Indexer indexer = central.newIndexer();

		final String prefix = "/bleujin/" ;
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				int max = 20000 ;
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				
				while(line != null && line.length > 0 && max-- > 0 ){
					WriteDocument doc = isession.newDocument(prefix + max);
					for (int ii = 0, last = headers.length; ii < last ; ii++) {
						if (line.length > ii) doc.unknown(headers[ii], line[ii]) ;
					}
					line = reader.readLine() ;
					if ((max % 5000) == 0) {
						System.out.print('.') ;
						isession.continueUnit() ;
					}
					isession.insertDocument(doc) ;
				}
				reader.close() ;
				return null;
			}
		}) ;
	}
	
	
	public void testInfinispan() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
		.defaultClusteredBuilder()
			.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.build();
		DefaultCacheManager dm = new DefaultCacheManager(gconfig);
		dm.defineConfiguration("infinispan", FastFileCacheStore.fastStoreConfig(CacheMode.LOCAL, "./resource/infinispan", 1000)) ;
		
		dm.start() ;
		Cache<String, Map> cache = dm.getCache("infinispan");
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 20000 ;
		
		while(line != null && line.length > 0 && max-- > 0 ){
			Map<String, String> doc = MapUtil.newMap() ;
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) doc.put(headers[ii], line[ii]) ;
			}
			line = reader.readLine() ;
			if ((max % 5000) == 0) {
				System.out.print('.') ;
			}
			cache.put("", doc) ;
		}
		reader.close() ;
		dm.stop() ;
	}
}



class LuceneInsertJob implements TransactionJob<Void> {

	private String prefix;
	private int max = 0 ;
	
	public LuceneInsertJob(String prefix, int max){
		this.prefix = prefix ;
		this.max = max ;
	}
	
	public LuceneInsertJob(int max){
		this("/", max) ;
	}
	
	@Override
	public Void handle(WriteSession wsession) throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		
		while(line != null && line.length > 0 && max-- > 0 ){
//			if (headers.length != line.length ) continue ;
			WriteNode wnode = wsession.resetBy(prefix + max) ;
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) wnode.property(headers[ii], line[ii]) ;
			}
			line = reader.readLine() ;
			if ((max % 5000) == 0) {
				System.out.print('.') ;
				wsession.continueUnit() ;
			} 
		}
		reader.close() ;
		Debug.line("endJob") ;
		return null;
	}
}