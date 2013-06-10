package net.ion.bleujin;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.manager.DefaultCacheManager;

public class TestMR extends TestCase {


	public void testFirst() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable().build();

		DefaultCacheManager dm = new DefaultCacheManager(gconfig);
		dm.defineConfiguration("c1", config);
		dm.defineConfiguration("c2", config);

		Cache c1 = dm.getCache("c1");
		// Cache c2 = dm.getCache("c2");

		dm.start();

		c1.put("1", "Hello world here I am");
		c1.put("3", "JUDCon is in Boston");
		c1.put("12", "JBoss Application Server");
		c1.put("14", "Infinispan community");

		c1.put("111", "Infinispan open source");
		c1.put("113", "Toronto is a capital of Ontario");
		c1.put("211", "JBoss World is awesome");
		c1.put("213", "JBoss division of RedHat ");

		// c2.put("2", "Infinispan rules the world");
		// c2.put("4", "JBoss World is in Boston as well");
		// c2.put("15", "Hello world");
		// c2.put("15", "Hello world");
		// c2.put("112", "Boston is close to Toronto");
		// c2.put("114", "JUDCon is cool");
		// c2.put("212", "JBoss rules");
		// c2.put("214", "RedHat community");

		// MapReduceTask<String, String, String, Integer> t = new MapReduceTask<String, String, String, Integer>(c1);
		// t.mappedWith(new WordCountMapper()).reducedWith(new WordCountReducer());
		// Map<String, Integer> wordCountMap = t.execute();

		MapReduceTask<String, String, String, Integer> t = new MapReduceTask<String, String, String, Integer>(c1);
		t.mappedWith(new Mapper<String, String, String, Integer>() {
			private static final long serialVersionUID = -5943370243108735560L;

			@Override
			public void map(String key, String value, Collector<String, Integer> c) {
				StringTokenizer tokens = new StringTokenizer(value);
				while (tokens.hasMoreElements()) {
					String s = (String) tokens.nextElement();
					c.emit(s, 1);
				}
			}

		}).reducedWith(new Reducer<String, Integer>() {
			private static final long serialVersionUID = 1901016598354633256L;

			@Override
			public Integer reduce(String key, Iterator<Integer> iter) {
				int sum = 0;
				while (iter.hasNext()) {
					Integer i = (Integer) iter.next();
					sum += i;
				}
				return sum;
			}
		});
		Map<String, Integer> wordCountMap = t.execute();

		Debug.line(wordCountMap);
	}

	static class WordCountMapper implements Mapper<String, String, String, Integer> {
		/** The serialVersionUID */
		private static final long serialVersionUID = -5943370243108735560L;

		@Override
		public void map(String key, String value, Collector<String, Integer> c) {
			StringTokenizer tokens = new StringTokenizer(value);
			while (tokens.hasMoreElements()) {
				String s = (String) tokens.nextElement();
				c.emit(s, 1);
			}
		}
	}

	static class WordCountReducer implements Reducer<String, Integer> {
		/** The serialVersionUID */
		private static final long serialVersionUID = 1901016598354633256L;

		@Override
		public Integer reduce(String key, Iterator<Integer> iter) {
			int sum = 0;
			while (iter.hasNext()) {
				Integer i = (Integer) iter.next();
				sum += i;
			}
			return sum;
		}
	}
}
