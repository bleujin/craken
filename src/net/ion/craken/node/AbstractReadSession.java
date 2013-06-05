package net.ion.craken.node;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.ion.craken.mr.NodeMapReduceTask;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.mr.NodeReducer;
import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.node.crud.WriteSessionImpl;
import net.ion.craken.node.exception.NotFoundPath;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.StringUtil;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;

import com.google.common.base.Function;

public abstract class AbstractReadSession implements ReadSession {

	private Credential credential ;
	private AbstractWorkspace workspace ;
	protected AbstractReadSession(Credential credential, AbstractWorkspace workspace) {
		this.credential = credential.clearSecretKey() ;
		this.workspace = workspace ;
	}

	public ReadNode pathBy(String fqn0, String... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/'))) ;
	}

	public ReadNode pathBy(Fqn fqn) {
		return pathBy(fqn, false) ;
	}

	public ReadNode pathBy(Fqn fqn, boolean createIf) {
		if (createIf || exists(fqn)) return ReadNodeImpl.load(this, workspace.getNode(fqn));
		else throw new NotFoundPath(fqn) ;
	}

	public ReadNode pathBy(String fqn, boolean createIf) {
		return pathBy(Fqn.fromString(fqn), createIf) ;
	}

	public ReadNode pathBy(String fqn) {
		return pathBy(fqn, false) ;
	}

	
	public boolean exists(String fqn) {
		return workspace.exists(fqn);
	}

	public boolean exists(Fqn fqn) {
		return workspace.exists(fqn);
	}


	public ReadNode root() {
		return pathBy("/");
	}

	public <T> Future<T> tran(TransactionJob<T> tjob) {
		return tran(tjob, TranExceptionHandler.PRINT) ;
	}

	@Override
	public <T> T tranSync(TransactionJob<T> tjob) throws Exception {
		WriteSession tsession = new WriteSessionImpl(this, workspace);
		return workspace.tran(tsession, tjob).get() ;
	}

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSession tsession = new WriteSessionImpl(this, workspace);

		return workspace.tran(tsession, tjob, handler) ;
	}

	@Override
	public AbstractWorkspace workspace() {
		return workspace;
	}

	@Override
	public Credential credential() {
		return credential;
	}

	public <Ri, Rv> Map<Ri, Rv> mapReduceSync(final NodeMapReduce<Ri, Rv> mapper){
		TreeCache<PropertyId, PropertyValue> tcache = workspace().getCache();
		Cache<TreeNodeKey, AtomicMap<?, ?>> cache = tcache.getCache();

		NodeMapReduceTask<Ri, Rv> t = new NodeMapReduceTask<Ri, Rv>(cache);
		return t.mappedWith(new OuterMapper(mapper)).reducedWith(new OuterReducer(mapper)).execute();
	}

	
	public <Ri, Rv, V> Future<V> mapReduce(NodeMapReduce<Ri, Rv> mapper, final Function<Map<Ri, Rv>, V> function){
		TreeCache<PropertyId, PropertyValue> tcache = workspace().getCache();
		Cache<TreeNodeKey, AtomicMap<?, ?>> cache = tcache.getCache();

		NodeMapReduceTask<Ri, Rv> t = new NodeMapReduceTask<Ri, Rv>(cache);
		final Future<Map<Ri, Rv>> future = t.mappedWith(new OuterMapper(mapper)).reducedWith(new OuterReducer(mapper)).executeAsynchronously();
		
		return workspace().executor().submitTask(new Callable<V>() {
			@Override
			public V call() throws Exception {
				return function.apply(future.get()) ;
			}
		}) ;
	}
	

	
	private static class OuterMapper<Ri, Rv> implements Mapper<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, Ri, Rv> {
		private static final long serialVersionUID = -790742017663413150L;
		private NodeMapReduce<Ri, Rv> inner;
		OuterMapper(NodeMapReduce<Ri, Rv> inner){
			this.inner = inner ;
		}
		
		@Override
		public void map(TreeNodeKey key, AtomicMap<PropertyId, PropertyValue> map, Collector<Ri, Rv> iter) {
			if (key.getContents() == TreeNodeKey.Type.STRUCTURE) return ;
//			AbstractReadSession session = AbstractReadSession.this;

			inner.map(key, map, iter) ;
		}
	}

	private static class OuterReducer<Ri, Rv> implements Reducer<Ri, Rv>{
		private static final long serialVersionUID = 6113634132823514149L;
		private NodeMapReduce<Ri, Rv> inner;
		OuterReducer(NodeMapReduce<Ri, Rv> inner){
			this.inner = inner ;
		}
		
		@Override
		public Rv reduce(Ri key, Iterator<Rv> iter) {
			return inner.reduce(key, iter);
		}
	}

}
