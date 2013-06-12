package net.ion.craken.problem.speed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.radon.impl.util.CsvReader;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.lucene.cachestore.LuceneCacheLoader;

public class TestManyInsert extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testLuceneDirStore() throws Exception {
		r.defineConfig("dir.node", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
				.loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new LuceneCacheLoader()).addProperty("location","c:/temp/resource/store/dir")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		this.session = r.testLogin("dir") ;
		session.tran(new Job()).get() ;
		Debug.line("endGet") ;
	}

	// 454 sec per 100k -> oom 
	public void testFastFileCacheStore() throws Exception {
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
//				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","c:/temp/resource/store/test")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		this.session = r.testLogin("test") ;
		session.tran(new Job()).get() ;
		Debug.line("endGet") ;
	}
	
}

class Job implements TransactionJob<Void> {

	@Override
	public Void handle(WriteSession wsession) throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 100000 ;
		while(line != null && line.length > 0 && max-- > 0 ){
//			if (headers.length != line.length ) continue ;
			WriteNode wnode = wsession.pathBy("/" + max);
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) wnode.property(headers[ii], line[ii]) ;
			}
			line = reader.readLine() ;
			if ((max % 1000) == 0) System.out.print('.') ;
		}
		Debug.line("endFor") ;
		reader.close() ;
		Debug.line("endClose") ;
		return null;
	}
}
