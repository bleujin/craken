package net.ion.craken.node.mr;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.Collector;

import com.google.common.base.Function;

public class TestNodeMapReduce extends TestCase {

	private ReadSession session;
	private Craken r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.local();
		r.start();
	
		this.session = r.login("my");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i : ListUtil.rangeNum(30)) {
					wsession.pathBy("/bleujin/" + i).property("age", RandomUtil.nextInt(30)) ;
				}
				return null;
			}
		});
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testSyncPropertyCount() throws Exception {
		Map<String, Integer> generationMap = session.mapReduceSync(new MyTask()) ;
		Debug.line(generationMap);
	}
	
	public void testAsync() throws Exception {
		Future<Void> future = session.mapReduce(new MyTask(), new Function<Map<String, Integer>, Void>() {
			@Override
			public Void apply(Map<String, Integer> result) {
				Debug.line(result) ;
				return null;
			}
		});
		
		future.get() ;
	}
	
	
	private static class MyTask implements NodeMapReduce<String, Integer> {

		private static final long serialVersionUID = -5943370243108735560L;

		@Override
		public void map(TreeNodeKey key, AtomicMap<PropertyId, PropertyValue> mapValue, Collector<String, Integer> c) {
			if (! key.getFqn().toString().startsWith("/bleujin/")) return ;
			
			PropertyValue value = mapValue.get(PropertyId.normal("age"));
			if (value == null) return ;
			if (value.intValue(0) > 18){
				c.emit("youth", 1);
			} else if (value.intValue(0) > 8){
				c.emit("young", 1);
			} else {
				c.emit("child", 1);
			}
			
		}

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
