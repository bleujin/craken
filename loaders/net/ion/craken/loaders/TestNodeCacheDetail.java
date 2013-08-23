package net.ion.craken.loaders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;
import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.neo.ReadSession;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.distexec.mapreduce.Collator;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.manager.DefaultCacheManager;

public class TestNodeCacheDetail extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	private Configuration defaultConf;
	private NeoNodeCacheStore neoNodeCacheStore;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		neoNodeCacheStore = new NeoNodeCacheStore();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		this.defaultConf = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().clustering().hash().numOwners(2).unsafe()
		.eviction().maxEntries(2).loaders().preload(true).shared(true).addCacheLoader().cacheLoader(neoNodeCacheStore)
				.addProperty("wsName", "test").addProperty("analyzerClzName", StandardAnalyzer.class.getCanonicalName()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(true).build();
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start();
	}

	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}

	public void testEviction() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.start();
		
		for (String key : cache.keySet()) {
			Debug.line(key, cache.get(key));
		}

		CacheLoaderConfig c = cache.getConfiguration().getCacheLoaders().get(0);
		final ReadSession session = ((NeoNodeCacheStoreConfig) c).login();

		session.createQuery().find().debugPrint(Page.ALL);
		
		session.createQuery().find() ;
	}
	
	public void testMapReduce() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.start();

		MapReduceTask<String, Employee, String, Long> task = new MapReduceTask(cache);

		List<Entry<String, Long>> result = task.mappedWith(new Mapper<String, Employee, String, Long>() {
			@Override
			public void map(String key, Employee value, Collector<String, Long> c) {
				c.emit(key, Long.valueOf(value.getEmpno())) ;
			}
		}).reducedWith(new Reducer<String, Long>() {
			@Override
			public Long reduce(String key, Iterator<Long> iter) {
				int sum = 0 ;
				while(iter.hasNext()){
					sum++ ;
					iter.next() ;
				}
				return Long.valueOf(sum);
			}
		}).execute(new Collator<String, Long, List<Entry<String, Long>>>() {
			public List<Entry<String, Long>> collate(Map<String, Long> reduceResult) {
				
				List<Entry<String, Long>> result = ListUtil.newList() ;
				for (Entry<String, Long> entry : reduceResult.entrySet()) {
					result.add(entry) ;
				}
				return result;
			}
		});
		
		
		Debug.line(result) ;
		

		cache.stop();
	}

}
