package net.ion.craken.node.crud.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.transaction.Transaction;

import net.ion.craken.listener.CDDMListener;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.mr.NodeMapReduceTask;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.NodeWriter;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TranResult;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.OldWriteSession;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.store.ICSFileConfigBuilder;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.TreeCacheFactory;
import net.ion.craken.node.crud.tree.TreeNode;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyId.PType;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.PropertyValue.VType;
import net.ion.craken.node.crud.tree.impl.ProxyHandler;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Action;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectId;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.batch.BatchContainer;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.io.GridFilesystem;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.google.common.cache.CacheBuilder;

@Listener
public class ICSWorkspace extends AutoBatchSupport implements Workspace, ProxyHandler{

	private Repository repository;
	private AdvancedCache<PropertyId, PropertyValue> cache;
	private GridFilesystem gfs;
	private String wsName;
	private BatchContainer batchContainer;
	private Engine parseEngine = Engine.createDefaultEngine();
	private final CDDMListener cddmListener = new CDDMListener();

	private final Log log = LogFactory.getLog(Workspace.class);
	private ExecutorService es = new WithinThreadExecutor();
	private Central central;
	com.google.common.cache.Cache<Transaction, IndexWriteConfig> trans = CacheBuilder.newBuilder().maximumSize(100).build();
	private TreeCache<PropertyId, PropertyValue> tcache;

	public ICSWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache, ICSFileConfigBuilder wconfig) throws IOException {
		this.repository = craken;
		this.cache = cache;
		this.gfs = wconfig.gfs();
		this.central = wconfig.central() ;
		
		this.tcache = new TreeCacheFactory().createTreeCache(cache, this) ;
		this.addListener(this);

		this.wsName = cache.getName();
		this.batchContainer = cache.getBatchContainer();

		wconfig.createInterceptor(tcache, central, trans);
	}

	public <T> T getAttribute(String key, Class<T> clz) {
		return repository.getAttribute(key, clz);
	}

	public String wsName() {
		return wsName;
	}

	public Workspace executorService(ExecutorService es) {
		this.es = es;
		return this;
	}

	public Workspace start() {
		this.cache.start();

		// this.cddmListener = new CDDMListener();

		return this;
	}


	public boolean exists(Fqn fqn) {
		if (Fqn.ROOT.equals(fqn)) {
			return true;
		}
		return tcache.exists(fqn) || readNode(fqn) != null ;
	}

	public ICSWorkspace withFlag(Flag... flags) {
		cache = cache.getAdvancedCache().withFlags(flags);
		return this;
	}

	public void close() {
		cddm().clear();
		final Object[] listeners = cache.getListeners().toArray(new Object[0]);
		for (Object wlistener : listeners) {
			if (wlistener instanceof WorkspaceListener)
				this.removeListener((WorkspaceListener) wlistener);
		}

		cache.stop();
	}

	public WriteNode createNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) {
		createAncestor(wsession, ancestorsFqn, fqn.getParent(), fqn);

		fqn.dataKey().createAction();
		final AtomicHashMap<PropertyId, PropertyValue> props = new AtomicHashMap<PropertyId, PropertyValue>();
		readNode(fqn).putAll(props);
		;

		if (log.isTraceEnabled())
			log.tracef("Created node %s", fqn);
		return WriteNodeImpl.loadTo(wsession, fqn);
	}

	public WriteNode resetNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) {
		createAncestor(wsession, ancestorsFqn, fqn.getParent(), fqn);

		fqn.dataKey().resetAction();
		readNode(fqn).clearData();

		if (log.isTraceEnabled())
			log.tracef("Reset node %s", fqn);
		return WriteNodeImpl.loadTo(wsession, fqn);
	}

	private void createAncestor(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn parent, Fqn fqn) {
		if (fqn.isRoot())
			return;

		if (!tcache.exists(fqn)) {
			List<String> names = fqn.peekElements();
			TreeNode<PropertyId, PropertyValue> cnode = tcache.getRoot();
			for (String name : names) {
				if (!cnode.hasChild(name)) {
					Fqn childFqn = Fqn.fromRelativeElements(cnode.getFqn(), name);
					if (readNode(childFqn) == null) {
						cnode.addChild(Fqn.fromString(name));
						WriteNodeImpl.loadTo(wsession, childFqn, Touch.MODIFY);
					}
				}
				cnode = cnode.getChild(name);
			}

		}

		if (parent.isRoot())
			return;
		// createAncestor(wsession, ancestorsFqn, parent.getParent(), parent);
	}

	public WriteNode writeNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) {
		createAncestor(wsession, ancestorsFqn, fqn.getParent(), fqn);

		if (log.isTraceEnabled())
			log.tracef("Merged node %s", fqn);
		return WriteNodeImpl.loadTo(wsession, fqn);
	}

	public TreeNode<PropertyId, PropertyValue> readNode(Fqn fqn) {
		TreeNode<PropertyId, PropertyValue> result = tcache.getNode(fqn);
		if (result == null){
			AtomicHashMap<PropertyId, PropertyValue> created = new AtomicHashMap<PropertyId, PropertyValue>();
			AtomicMap<PropertyId, PropertyValue> found = handleData(fqn.dataKey(), created) ;
			if (found == null) return null ;
			else {
				result = tcache.getRoot().addChild(fqn) ;
				result.putAll(found);
			}
			
		}
		return result;
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob) {
		return tran(wsession, tjob, null);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) {
		return tran(executor(), wsession, tjob, ehandler);
	}

	public <T> Future<T> tran(ExecutorService exec, final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) {

		wsession.attribute(TransactionJob.class, tjob);
		wsession.attribute(TranExceptionHandler.class, ehandler);
		wsession.attribute(CDDMListener.class, cddm());

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
				} catch (Error ex) {
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
		Transaction transaction = batchContainer.getBatchTransaction();
		if (transaction != null) trans.put(transaction, wsession.iwconfig());
		batchContainer.endBatch(true, true);
	}

	public Repository repository() {
		return this.repository;
	}

	public Central central() {
		return central;
	}

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
	public Cache<PropertyId, PropertyValue> cache() {
		return cache;
	}

	public GridFilesystem gfs() {
		return gfs;
	}

	// public WritableGridBlob gridBlob(String fqnPath, Metadata meta) throws IOException {
	// return gfsBlob.getWritableGridBlob(fqnPath, meta);
	// }

	public NodeWriter createLogWriter(WriteSession wsession, ReadSession rsession) throws IOException {
		return NodeWriter.BLANK ;
//		return new ICSFileWriter(this, wsession);
	}

	static class ICSFileWriter implements NodeWriter {
		private WriteSession wsession ;
		private ICSWorkspace wspace;
		public ICSFileWriter(ICSWorkspace wspace, WriteSession wsession){
			this.wspace = wspace ;
			this.wsession = wsession ;
		}
		
		public void writeLog(final Set<TouchedRow> logRows) throws IOException {
			if (wsession.iwconfig().isIgnoreIndex()) return ;
			
			
			IndexJob<Void> indexJob = new IndexJob<Void>() {

				@Override
				public Void handle(IndexSession isession) throws Exception {
					IndexWriteConfig iwconfig = wsession.iwconfig() ;
					long startTime = System.currentTimeMillis() ;
					GridFilesystem gfs = wspace.gfs() ;

					for (TouchedRow trow : logRows) {
						Touch touch = trow.touch() ;
						Action action = trow.target().dataKey().action() ;
						Fqn fqn = trow.target() ;
						String pathKey = fqn.toString() ;
						switch (touch) {
						case TOUCH:
							break;
						case MODIFY:
							Map<PropertyId, PropertyValue> props = trow.sourceMap() ;
							if ("/".equals(pathKey))
								continue;

							String contentFileName = fqn.toString() + "/" + fqn.name()  + ".node";
							File contentFile = gfs.getFile(contentFileName) ;
							if (! contentFile.getParentFile().exists()) {
								contentFile.getParentFile().mkdirs() ;
							}

							WriteDocument wdoc = isession.newDocument(pathKey) ;
							wdoc.keyword(EntryKey.PARENT, fqn.getParent().toString()) ;
							wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());
							
							JsonObject nodeJson = new JsonObject() ;
							for(PropertyId pid : props.keySet()){
								final String propId = pid.idString() ;
								PropertyValue pvalue = props.get(pid) ;
								nodeJson.add(propId, pvalue.json()); // data
								
								JsonArray jarray = pvalue.asJsonArray(); // index
								if (pid.type() == PType.NORMAL) {
									VType vtype = pvalue.type() ;
									for (JsonElement e : jarray.toArray()) {
										if (e == null)
											continue;
										FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
										fieldIndex.index(wdoc, propId, vtype, e.isJsonObject() ? e.toString() : e.getAsString());
									}
								} else { // refer
									for (JsonElement e : jarray.toArray()) {
										if (e == null)
											continue;
										FieldIndex.KEYWORD.index(wdoc, '@' + propId, e.getAsString());
									}
								}
								
							}
							IOUtil.copyNClose(new StringInputStream(nodeJson.toString()), gfs.getOutput(contentFileName)) ;
							if (! iwconfig.isIgnoreIndex()) {
								if (action == Action.CREATE) wdoc.insert() ; 
								else wdoc.update() ;
							}
							break;
						case REMOVE:
							File rfile = gfs.getFile(pathKey) ;
							if (rfile.exists()) rfile.delete() ;
							if (! iwconfig.isIgnoreIndex())  isession.deleteById(pathKey) ;
							isession.deleteQuery(new WildcardQuery(new Term(IKeywordField.DocKey, Fqn.fromString(pathKey).startWith())));
							break;
						case REMOVECHILDREN :
							File cfile = gfs.getFile(pathKey) ;
							if (cfile.exists()) cfile.delete() ;
							isession.deleteQuery(new WildcardQuery(new Term(IKeywordField.DocKey, Fqn.fromString(pathKey).startWith())));
							break;
						default:
							throw new IllegalArgumentException("Unknown modification type " + touch);
						}
					}
					
					wspace.log.debug(wsession.tranId() + " writed") ;
					wsession.readSession().attribute(TranResult.class.getCanonicalName(), TranResult.create(logRows.size(), System.currentTimeMillis() - startTime));
					return null;
				}
			};

			 wspace.central().newIndexer().index(indexJob) ;
		}

	}

	private boolean trace = false;

	public CDDMListener cddm() {
		return cddmListener;
	}

	public void log(String msg) {
		log.info(msg);
	}

	@Override
	public void registered(Workspace workspace) {
	}

	@Override
	public void unRegistered(Workspace workspace) {
	}

	@CacheEntryModified
	public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) {
		cddmListener.modifiedRow(event);
	}

	@CacheEntryRemoved
	public void removed(CacheEntryRemovedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) {
		cddmListener.removedRow(event);
	}

	@Override
	public WriteSession newWriteSession(ReadSession rsession) {
		return new OldWriteSession(rsession, this);
	}

	public void reindex(final WriteNode wnode, Analyzer anal, final boolean includeSub) {
		final IndexWriteConfig iwconfig = wnode.session().iwconfig();

		this.central().newIndexer().index(anal, WorkspaceIndexUtil.makeIndexJob(wnode, includeSub, iwconfig));
	}

	@Override
	public AtomicMap<PropertyId, PropertyValue> handleData(TreeNodeKey dataKey, AtomicMap<PropertyId, PropertyValue> created) {
		
		Fqn fqn = dataKey.getFqn() ;
		String contentFileName = fqn.toString() + "/" + fqn.name() + ".node"; // when write
		File file = gfs.getFile(contentFileName);
		if (!file.exists())
			return created;

		try {
			InputStream input = gfs.getInput(file);
			JsonObject json = JsonObject.fromString(IOUtil.toStringWithClose(input));
			TreeNodeKey tkey = fqn.dataKey();
			for (String key : json.keySet()) {
				PropertyId propId = PropertyId.fromIdString(key);
				PropertyValue pvalue = PropertyValue.loadFrom(tkey, propId, json.asJsonObject(key));
				created.put(propId, pvalue);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return created;
	}

	@Override
	public AtomicMap<Object, Fqn> handleStructure(TreeNodeKey struKey, AtomicMap<Object, Fqn> created) {
		if (gfs == null) {
			return created ;
		}
		
		Fqn fqn = struKey.getFqn() ;
		File file = gfs.getFile(fqn.toString()); // when write
		if (file.exists() && file.isDirectory()) {
			for (File child : file.listFiles()) {
				if (child.isFile())
					continue;
				created.put(child.getName(), Fqn.fromRelativeElements(fqn, child.getName()));
			}
		}

		return created;
	}


}
