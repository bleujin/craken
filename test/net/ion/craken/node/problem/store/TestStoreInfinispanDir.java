package net.ion.craken.node.problem.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.TestConfig;
import net.ion.craken.node.search.ReadSearchSession;
import net.ion.craken.node.search.RepositorySearch;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.impl.util.CsvReader;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;

public class TestStoreInfinispanDir extends TestCase {

	private ReadSearchSession session;
	private RepositorySearch rs;



	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RepositoryImpl r = RepositoryImpl.create();
		
//		r.defineConfig("test.meta", TestConfig.createOldSearchCacheStore(1000));
//		r.defineConfig("test.chunks", TestConfig.createOldSearchCacheStore(10));
//		r.defineConfig("test.locks", TestConfig.createOldSearchCacheStore(1000));
		String wsname = "test" ;
		r.defineConfig(wsname + ".meta", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader()
				.cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		r.defineConfig(wsname + ".chunks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(10).invocationBatching().enable().loaders().preload(true).shared(false).passivation(
				false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		r.defineConfig(wsname + ".locks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());

		
		this.rs = r.forSearch();
		this.session = rs.testLogin("test");
	}

	@Override
	protected void tearDown() throws Exception {
		rs.shutdown();
		super.tearDown();
	}

	private void index(Central central) {
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i : ListUtil.rangeNum(10000)) {
					WriteDocument doc = MyDocument.testDocument();
					doc.number("index", i).keyword("name", "bleujin");
					isession.insertDocument(doc);
				}
				return null;
			}
		});
	}
	
	public void testLoadCentral() throws Exception {
		
		Central central = session.central();
//		index(central);
		
		Searcher searcher = central.newSearcher();
//		searcher.createRequest("name:bleujin").find().debugPrint();
		long start = System.currentTimeMillis();
		Debug.line(searcher.createRequest("").find().totalCount(), System.currentTimeMillis() - start) ;
		// new InfinityThread().startNJoin();
		central.close() ;
	}

	public void testIndexSpeed() throws Exception {
//		new File("./resource/search").delete();

		Central central = session.central();
		long start = System.currentTimeMillis();
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv");

				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t');
				String[] headers = reader.readLine();
				String[] line = reader.readLine();
				int max = 600000;
				while (line != null && line.length > 0 && max-- > 0) {
					// if (headers.length != line.length ) continue ;
					WriteDocument wdoc = MyDocument.newDocument("/copy3/" + max);
					for (int ii = 0, last = headers.length; ii < last; ii++) {
						if (line.length > ii)
							wdoc.keyword(headers[ii], line[ii]);
					}

					isession.insertDocument(wdoc);
					line = reader.readLine();

					if ((max % 1000) == 0)
						System.out.print('.');
				}
				return null;
			}
		});
		central.close() ;
		Debug.line(System.currentTimeMillis() - start);
	}

}
