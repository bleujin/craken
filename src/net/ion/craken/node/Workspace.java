package net.ion.craken.node;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.Metadata;
import net.ion.craken.io.WritableGridBlob;
import net.ion.craken.listener.CDDMListener;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.mr.NodeMapReduceTask;
import net.ion.craken.node.crud.ReadSessionImpl;
import net.ion.craken.node.crud.TreeNode;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.TreeNodeKey.Action;
import net.ion.craken.node.crud.TreeStructureSupport;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.exception.NodeNotExistsException;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.config.Central;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.notifications.Listener;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

@Listener
public abstract class Workspace extends TreeStructureSupport implements Closeable {

	private Repository repository;
	private AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache;
	private GridFilesystem gfsBlob;
	private GridFilesystem logContent;
	private TranLogManager logManager;
	private String wsName;
	private BatchContainer batchContainer;
	private Engine parseEngine = Engine.createDefaultEngine();
	private CDDMListener cddmListener;

	private final Log log = LogFactory.getLog(Workspace.class);
	private ExecutorService es = new WithinThreadExecutor();

	public Workspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, WorkspaceConfig config) {
		this(repository, cache.getAdvancedCache(), wsName, config);
	}

	private Workspace(Repository repository, AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, WorkspaceConfig config) {
		super(cache, cache.getBatchContainer());
		this.repository = repository;
		this.cache = cache;

		this.gfsBlob = new GridFilesystem(cache.getCacheManager().<String, byte[]> getCache(wsName + ".blob"));
		this.logContent = new GridFilesystem(cache.getCacheManager().<String, byte[]> getCache(wsName + ".log"));
		this.logManager = TranLogManager.create(this, cache.getCacheManager().<String, Metadata> getCache(wsName + ".logmeta"), repository().memberId());

		this.wsName = wsName;
		this.batchContainer = cache.getBatchContainer();
	}

	public <T> T getAttribute(String key, Class<T> clz) {
		return repository.getAttribute(key, clz);
	}

	public String wsName() {
		return wsName;
	}

	
	public Workspace executorService(ExecutorService es){
		this.es = es ;
		return this ;
	}
	
	
	public Workspace start() {
		this.cache.start();
		this.logManager.start();

		this.cddmListener = new CDDMListener();
//		this.addListener(cddmListener) ;
		
		return this;
	}
	


	public void close() {
		final Object[] listeners = cache.getListeners().toArray(new Object[0]);
		for (Object wlistener : listeners) {
			if (wlistener instanceof WorkspaceListener) this.removeListener((WorkspaceListener)wlistener);
		}

		cache.stop();
	}

	protected WriteNode createNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) {
		createAncestor(wsession, ancestorsFqn, fqn.getParent(), fqn);

		final AtomicHashMap<PropertyId, PropertyValue> props = new AtomicHashMap<PropertyId, PropertyValue>();
		cache.put(fqn.dataKey().createAction(), props);

		if (log.isTraceEnabled())
			log.tracef("Created node %s", fqn);
		return WriteNodeImpl.loadTo(wsession, TreeNode.create(this, fqn));
	}

	protected WriteNode resetNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) {
		createAncestor(wsession, ancestorsFqn, fqn.getParent(), fqn);

		final AtomicHashMap<PropertyId, PropertyValue> props = new AtomicHashMap<PropertyId, PropertyValue>();
		cache.put(fqn.dataKey().resetAction(), props);

		if (log.isTraceEnabled())
			log.tracef("Reset node %s", fqn);
		return WriteNodeImpl.loadTo(wsession, TreeNode.create(this, fqn));
	}

	private void createAncestor(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn parent, Fqn fqn) {
		if (fqn.isRoot())
			return;

		AtomicMap<String, Fqn> parentStru = super.strus(parent);
		if (parentStru.containsKey(fqn.getLastElement())) {

		} else {
			parentStru.put(fqn.getLastElementAsString(), fqn);
			// super.props(fqn) ;
			cache.put(fqn.dataKey().createKey(Action.CREATE), new AtomicHashMap<PropertyId, PropertyValue>());
			// cache.put(fqn.struKey().createKey(Action.CREATE), new AtomicHashMap()) ;
			WriteNodeImpl.loadTo(wsession, TreeNode.create(this, fqn), Touch.MODIFY);
		}

		if (parent.isRoot())
			return;
		createAncestor(wsession, ancestorsFqn, parent.getParent(), parent);
	}

	protected WriteNode writeNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) {
		createAncestor(wsession, ancestorsFqn, fqn.getParent(), fqn);

		if (log.isTraceEnabled())
			log.tracef("Merged node %s", fqn);
		return WriteNodeImpl.loadTo(wsession, TreeNode.create(this, fqn), exists(fqn) ? Touch.TOUCH : Touch.MODIFY);
		// return exists(fqn) ? WriteNodeImpl.loadTo(this, workspace().pathNode(fqn, false)) : WriteNodeImpl.loadTo(this, workspace().pathNode(fqn, true), Touch.MODIFY);

		// mergeAncestor(fqn) ;
		// if (log.isTraceEnabled()) log.tracef("Merged node %s", fqn);
		// return new TreeNode(this, fqn);
	}

	protected TreeNode readNode(Fqn fqn) {
		return TreeNode.create(this, fqn);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob) {
		return tran(wsession, tjob, null);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) {
		return tran(executor(), wsession, tjob, ehandler) ;
	}
	
	public <T> Future<T> tran(ExecutorService exec, final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) {
		
		wsession.attribute(TransactionJob.class, tjob) ;
		wsession.attribute(TranExceptionHandler.class, ehandler) ;
		wsession.attribute(CDDMListener.class, cddm()) ;
		
		return exec.submit(new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					batchContainer.startBatch(true);
					wsession.prepareCommit();
					T result = tjob.handle(wsession);

					endTran(wsession);

					return result;
				} catch (Exception ex) {
					batchContainer.endBatch(true, false);
					if (ehandler == null)
						throw (Exception) ex;

					ehandler.handle(wsession, tjob, ex);
					return null;
				} catch(Error ex) {
					batchContainer.endBatch(true, false);
					ehandler.handle(wsession, tjob, ex);
					return null;
					
				} finally {
				}

			}
		});
	}

	private void endTran(WriteSession wsession) throws IOException {
		wsession.endCommit();
		batchContainer.endBatch(true, true);
		
		if (wsession.iwconfig().isInmemory()) return ;
		
		
		Metadata metadata = logContent.newGridBlob(wsession.tranId()).getMetadata();
		int count = index(wsession.readSession(), wsession.tranId(), metadata);

		logManager.endTran(wsession, metadata) ;
	}

	GridFilesystem logContent() {
		return logContent;
	}
	
	public Repository repository() {
		return this.repository;
	}
	
	public TranLogManager tranLogManager(){
		return this.logManager ;
	}

	public boolean exists(Fqn f) {
		if (Fqn.ROOT.equals(f)) {
			return true;
		}
		final boolean result = cache.containsKey(f.dataKey()); // && cache.containsKey(f.struKey());
		return result;
	}

	public boolean existsData(Fqn f) {
		if (Fqn.ROOT.equals(f)) {
			return true;
		}
		final boolean result = cache.containsKey(f.dataKey());
		return result;
	}

	private int index(ReadSession rsession, String logPath, Metadata meta) throws IOException {
		long startTime = System.currentTimeMillis();
		InputStream input = null;
		int count;
		try {
			input = logContent.gridBlob(logPath, meta).toInputStream();
			count = storeData(input);
			log.info(logPath + " writed") ;
		} catch(Exception ex){
			log.warn(logPath + " failed");
			throw new IOException(ex) ;
		} finally {
			input.close() ;
//			IOUtil.closeQuietly(input);
		}

		rsession.attribute(TranResult.class.getCanonicalName(), TranResult.create(count, System.currentTimeMillis() - startTime));
		return count;
	}

	protected abstract int storeData(InputStream input) throws IOException;

	public void continueUnit(WriteSession wsession) throws IOException {
		endTran(wsession);

		wsession.tranId(new ObjectId().toString());
		batchContainer.startBatch(true);
		wsession.prepareCommit();
	}

	public void begin() {
		super.startAtomic();
	}

	public void failEnd() {
		super.failAtomic();
	}

	public void end() {
		super.endAtomic();
	}

	public Workspace addListener(WorkspaceListener listener) {
		listener.registered(this);
		cache.addListener(listener);
		return this;
	}

	public void removeListener(WorkspaceListener listener) {
		listener.unRegistered(this);
		cache.removeListener(listener);
	}
	
	public abstract Central central();

	public abstract WorkspaceConfig config();

	public ExecutorService executor() {
		return es;
	}

	public Engine parseEngine() {
		return parseEngine;
	}

	public <Ri, Rv> Future<Map<Ri, Rv>> mapReduce(NodeMapReduce<Ri, Rv> mapper) {

		// CacheMode cmode = cache.getCacheConfiguration().clustering().cacheMode();
		// if (CacheMode.DIST_ASYNC != cmode || CacheMode.DIST_SYNC != cmode){
		// }

		NodeMapReduceTask<Ri, Rv> t = new NodeMapReduceTask<Ri, Rv>(cache);
		final Future<Map<Ri, Rv>> future = t.mappedWith(new OuterMapper(mapper)).reducedWith(new OuterReducer(mapper)).executeAsynchronously();
		return future;

	}

	private static class OuterMapper<Ri, Rv> implements Mapper<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, Ri, Rv> {
		private static final long serialVersionUID = -790742017663413150L;
		private NodeMapReduce<Ri, Rv> inner;

		OuterMapper(NodeMapReduce<Ri, Rv> inner) {
			this.inner = inner;
		}

		@Override
		public void map(TreeNodeKey key, AtomicMap<PropertyId, PropertyValue> map, Collector<Ri, Rv> iter) {
			if (key.getType() == TreeNodeKey.Type.STRUCTURE)
				return;

			inner.map(key, map, iter);
		}
	}

	private static class OuterReducer<Ri, Rv> implements Reducer<Ri, Rv> {
		private static final long serialVersionUID = 6113634132823514149L;
		private NodeMapReduce<Ri, Rv> inner;

		OuterReducer(NodeMapReduce<Ri, Rv> inner) {
			this.inner = inner;
		}

		@Override
		public Rv reduce(Ri key, Iterator<Rv> iter) {
			return inner.reduce(key, iter);
		}
	}

	public void remove(Fqn fqn) {
		cache.remove(fqn.dataKey());
		cache.remove(fqn.struKey());
	}

	@Deprecated
	public Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache() {
		return cache;
	}


	public GridFilesystem gfs() {
		return gfsBlob;
	}

	public WritableGridBlob gridBlob(String fqnPath, Metadata meta) throws IOException {
		return gfsBlob.getWritableGridBlob(fqnPath, meta);
	}

	public InstantLogWriter createLogWriter(WriteSession wsession, ReadSession rsession) throws IOException {
		return new InstantLogWriter(this, wsession, rsession);
	}

	static class InstantLogWriter {

		private final Workspace wspace;
		private final WriteSession wsession;
		private final ReadSession rsession;

		private JsonWriter jwriter;

		public InstantLogWriter(Workspace wspace, WriteSession wsession, ReadSession rsession) throws IOException {
			this.wspace = wspace;
			this.wsession = wsession;
			this.rsession = rsession;
			GridBlob gridBlob = wspace.logContent.newGridBlob(wsession.tranId());
			OutputStream output = wspace.logContent.getOutput(gridBlob, false);

			Writer swriter = new BufferedWriter(new OutputStreamWriter(output, Charset.forName("UTF-8")));
			this.jwriter = new JsonWriter(swriter);
		}

		public InstantLogWriter beginLog(Set<TouchedRow> logRows) throws IOException {
			final long thisTime = System.currentTimeMillis();

			jwriter.beginObject();

			jwriter.name("config");
			jwriter.beginObject();
			wsession.iwconfig().writeJson(jwriter, thisTime, logRows.size());
			jwriter.endObject();

			jwriter.name("logs");
			jwriter.beginArray();

			return this;
		}

		public void writeLog(TouchedRow row) throws IOException {
			Fqn target = row.target();
			Touch touch = row.touch();

			jwriter.beginObject();

			jwriter.name("path").value(target.toString()).name("touch").value(touch.name()).name("action").value(target.dataKey().action().toString()); // .jsonElement("val", nodeValue) ;
			if (touch == Touch.MODIFY) {
				jwriter.jsonElement("val", rsession.workspace().existsData(target) ? rsession.workspace().readNode(target).toValueJson() : JsonObject.create());
			}

			jwriter.endObject();
		}

		public void endLog() throws IOException {
			jwriter.endArray();
			jwriter.endObject();
			jwriter.flush();
			jwriter.close();

			// Metadata metadata = wspace.logContent.gridBlob(wsession.tranId()).getMetadata() ;
			// wspace.logMeta.put(wsession.tranId(), metadata);
			// return new EndCommit(wsession.tranId(), metadata);
		}

	}

	

	private boolean trace = false;

	void move(WriteSession wsession, Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
		if (trace)
			log.tracef("Moving node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
		if (nodeToMoveFqn == null || newParentFqn == null)
			throw new NullPointerException("Cannot accept null parameters!");

		if (nodeToMoveFqn.getParent().equals(newParentFqn)) {
			if (trace)
				log.trace("Not doing anything as this node is equal with its parent");
			// moving onto self! Do nothing!
			return;
		}

		// Depth first. Lets start with getting the node we want.
		boolean success = false;
		try {
			// check that parent's structure map contains the node to be moved. in case of optimistic locking this
			// ensures the write skew is properly detected if some other thread removes the child
			TreeNode parent = readNode(nodeToMoveFqn.getParent());
			if (!parent.hasChild(nodeToMoveFqn.getLastElement())) {
				if (trace)
					log.trace("The parent does not have the child that needs to be moved. Returning...");
				return;
			}
			TreeNode nodeToMove = readNode(nodeToMoveFqn);
			if (nodeToMove == null) {
				if (trace)
					log.trace("Did not find the node that needs to be moved. Returning...");
				return; // nothing to do here!
			}
			if (!exists(newParentFqn)) {
				// then we need to silently create the new parent
				writeNode(wsession, SetUtil.EMPTY, newParentFqn);
				if (trace)
					log.tracef("The new parent (%s) did not exists, was created", newParentFqn);
			}

			// create an empty node for this new parent
			Fqn newFqn = Fqn.fromRelativeElements(newParentFqn, nodeToMoveFqn.getLastElement());
			writeNode(wsession, SetUtil.EMPTY, newFqn);
			TreeNode newNode = readNode(newFqn);
			Map<PropertyId, PropertyValue> oldData = nodeToMove.readMap();
			if (oldData != null && !oldData.isEmpty())
				newNode.putAll(oldData);
			for (Object child : nodeToMove.getChildrenNames()) {
				// move kids
				if (trace)
					log.tracef("Moving child %s", child);
				Fqn oldChildFqn = Fqn.fromRelativeElements(nodeToMoveFqn, child);
				move(wsession, oldChildFqn, newFqn);
			}
			remove(nodeToMoveFqn);
			success = true;
		} finally {
			if (!success) {
				failAtomic();
			}
		}
		log.tracef("Successfully moved node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
	}

	public CDDMListener cddm() {
		return cddmListener;
	}

	public void log(String msg){
		log.info(msg);
	}




}
