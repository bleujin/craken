package net.ion.craken.node;

import java.util.Iterator;
import java.util.Map;

import javax.swing.tree.TreeNode;

import junit.framework.TestCase;

import org.apache.ecs.xhtml.map;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;

import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;

public class TestWorkspace extends TestCase {

	
	protected RepositoryImpl r ;
	protected ReadSession session;

	public void setUp() throws Exception {
		this.r = RepositoryImpl.create() ;
		this.session = r.testLogin("test") ;
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

		TreeCache cache = workspace.getCache();
		for (Object key : cache.getCache().keySet()) {
			Debug.line(key, key.getClass(), cache.getCache().get(key));
		}
	}

	public void testMapReduce() throws Exception {
		MapReduceTask<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>, String, Integer> task = 
				new MapReduceTask<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>, String, Integer>(session.workspace().getCache().getCache());
		task.mappedWith(new WordCountMapper()).reducedWith(new WordCountReducer()) ;
		
		Map<String, Integer> map = task.execute();
		Debug.line(map) ;

	}

	static class WordCountMapper implements Mapper<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>, String, Integer> {
		private static final long serialVersionUID = -5943370243108735560L;

		@Override
		public void map(TreeNodeKey key, AtomicHashMap<PropertyId, PropertyValue> value, Collector<String, Integer> c) {
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
