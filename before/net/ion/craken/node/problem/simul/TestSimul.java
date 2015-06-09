package net.ion.craken.node.problem.simul;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.batch.BatchContainer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Action;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;
import net.ion.radon.util.csv.CsvReader;

public class TestSimul extends TestCase {
	
	private Cache<String, MyEntry> cache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DefaultCacheManager dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		
		EvictionConfigurationBuilder builder = new ConfigurationBuilder() //.read(dm.getDefaultCacheConfiguration())
				.invocationBatching().enable()
				.persistence().addSingleFileStore().location("./resource/temp")
				.fetchPersistentState(false).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false)
				.async().enabled(false).flushLockTimeout(20000).shutdownTimeout(1000).modificationQueueSize(1000).threadPoolSize(5) 
				.eviction().maxEntries(20000) ;
		
		dm.defineConfiguration("evict3", builder.build()) ;
		dm.start(); 
		
		this.cache = dm.getCache("evict3") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		cache.stop(); 
		cache.getCacheManager().stop(); 
		super.tearDown();
	}
	
	
	public void testCount() throws Exception {
		Debug.line(cache.keySet().size()) ;
		while(true){
			assertTrue(cache.containsKey("/bleujin/" + RandomUtil.nextInt(500000))) ;
			Thread.sleep(1000);
		}
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
			cache.put("/bleujin/" + max, new MyEntry().props(headers, line)) ;
			cache.put("/hero/" + max, new MyEntry().props(headers, line)) ;
//			cache.put("/jin/" + max, new MyEntry().props(headers, line)) ;
			line = reader.readLine() ;
			if ((max % 9999) == 0) {
				System.out.print('.') ;
				bc.endBatch(true);
				bc.startBatch() ;
			} 
		}
		
		bc.endBatch(true);
		reader.close() ;
		Debug.line("endJob") ;
		Debug.line(cache.keySet().size()) ;
		Debug.line(cache.get("/bleujin/3755"), cache.get("/bleujin/37755"));
		
		new InfinityThread().startNJoin(); 
	}
	
	

}


