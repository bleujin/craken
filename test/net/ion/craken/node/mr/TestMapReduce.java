package net.ion.craken.node.mr;

import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.config.CentralConfig;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;

public class TestMapReduce extends TestCase {

	
	protected RepositoryImpl r ;
	protected ReadSession session;

	public void setUp() throws Exception {
		this.r = RepositoryImpl.create() ;
		this.r.defineWorkspace("test", CentralCacheStoreConfig.create()) ;
		this.session = r.login("test") ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "bleujin").property("age", 20);
				wsession.pathBy("/users/hero").property("name", "hero").property("age", 20);
				return null;
			}
		});
	}
	
	public void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testInterface() throws Exception {
		Workspace workspace = session.workspace();

		TreeCache tcache = workspace.getCache();
		for (Object key : tcache.cache().keySet()) {
			Debug.line(key, key.getClass(), tcache.cache().get(key));
		}
	}

	public void testMapReduce() throws Exception {
		MapReduceTask<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, String, Integer> task = 
				new MapReduceTask<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, String, Integer>(session.workspace().getCache().cache());
		task.mappedWith(new WordCountMapper()).reducedWith(new WordCountReducer()) ;
		
		Map<String, Integer> map = task.execute();
		Debug.line(map) ;

	}

	static class WordCountMapper implements Mapper<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, String, Integer> {
		private static final long serialVersionUID = -5943370243108735560L;

		@Override
		public void map(TreeNodeKey key, AtomicMap<PropertyId, PropertyValue> value, Collector<String, Integer> c) {
			PropertyValue pvalue = value.get(PropertyId.normal("age"));
			if (pvalue == null) return ;
			for (Object ivalue : pvalue.asSet()) {
				// String s = (String) tokens.nextElement();
				c.emit(key.toString(), 1);
			}
		}
	}

	static class WordCountReducer implements Reducer<String, Integer> {
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
