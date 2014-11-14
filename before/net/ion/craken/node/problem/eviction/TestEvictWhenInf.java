package net.ion.craken.node.problem.eviction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.radon.util.csv.CsvReader;

import org.infinispan.Cache;
import org.infinispan.batch.BatchContainer;
import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestEvictWhenInf extends TestCase{

	
	private Cache<String, String[]> cache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DefaultCacheManager dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		
		ExpirationConfigurationBuilder builder = new ConfigurationBuilder() //.read(dm.getDefaultCacheConfiguration())
				.invocationBatching().enable()
				.persistence().addSingleFileStore().location("./resource/temp")
				.fetchPersistentState(false).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false)
				.async().enabled(false).flushLockTimeout(20000).shutdownTimeout(1000).modificationQueueSize(1000).threadPoolSize(5) 
				.eviction().maxEntries(20000).eviction().expiration().lifespan(10, TimeUnit.SECONDS) ;
		
		dm.defineConfiguration("evict2", builder.build()) ;
		dm.start(); 
		
		this.cache = dm.getCache("evict2") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		cache.stop(); 
		cache.getCacheManager().stop(); 
		super.tearDown();
	}
	
	
	public void testCount() throws Exception {
		Debug.line(cache.keySet().size()) ;
		Debug.line(cache.get("/bleujin/3755"));
	}
	
	
	public void testAdd() throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 500000 ;
		
		BatchContainer bc = cache.getAdvancedCache().getBatchContainer() ;
		bc.startBatch() ;
		
		while(line != null && line.length > 0 && max-- > 0 ){
//			if (headers.length != line.length ) continue ;
			cache.put("/bleujin/" + max, line) ;
			line = reader.readLine() ;
			if ((max % 5000) == 0) {
				System.out.print('.') ;
				bc.endBatch(true);
				bc.startBatch() ;
			} 
		}
		
		bc.endBatch(true);
		reader.close() ;
		Debug.line("endJob") ;
	}
}
