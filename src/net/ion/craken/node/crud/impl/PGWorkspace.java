package net.ion.craken.node.crud.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transaction;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.io.GridFilesystem;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.google.common.cache.CacheBuilder;

import net.ion.craken.listener.CDDMListener;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.mr.NodeMapReduceTask;
import net.ion.craken.node.IndexWriteConfig;
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
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.OldWriteSession;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.store.FileSystemWorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.PGWorkspaceConfigBuilder;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.TreeCacheFactory;
import net.ion.craken.node.crud.tree.TreeNode;
import net.ion.craken.node.crud.tree.impl.GridBlob;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.ProxyHandler;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.craken.node.crud.tree.impl.PropertyId.PType;
import net.ion.craken.node.crud.tree.impl.PropertyValue.VType;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Action;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectId;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

@Listener(clustered = true)
public class PGWorkspace extends AbWorkspace implements Workspace, ProxyHandler {

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

	private PGNodeStore pstore;

	public PGWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache, PGWorkspaceConfigBuilder wconfig) throws IOException {
		this.repository = craken;
		this.cache = cache;
		this.addListener(this);

		this.wsName = cache.getName();
		this.batchContainer = cache.getBatchContainer();

		this.gfs = wconfig.gfs();
		this.central = wconfig.central();
		this.pstore = new PGNodeStore(wconfig.dc());
		this.tcache = new TreeCacheFactory().createTreeCache(cache, this);
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
		return tcache.exists(fqn) || readNode(fqn) != null;
	}

	public PGWorkspace withFlag(Flag... flags) {
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
		central.indexConfig().indexExecutor().shutdown();
		try {
			central.indexConfig().indexExecutor().awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		central.destroySelf();
		cache.stop();
		pstore.close() ;
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
				if (!cnode.hasChild(name))
					cnode.addChild(Fqn.fromString(name));
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
		if (result == null) {
			AtomicHashMap<PropertyId, PropertyValue> created = new AtomicHashMap<PropertyId, PropertyValue>();
			AtomicMap<PropertyId, PropertyValue> found = handleData(fqn.dataKey(), created);
			if (found == null) {
				// result = tcache.getRoot().addChild(fqn) ;
				return null;
			} else {
				result = tcache.createTreeNode(cache, fqn);
				result.putAll(found);
			}

		}
		return result;
	}

	public TreeNode<PropertyId, PropertyValue> writeNode(Fqn fqn) {
		if (!tcache.exists(fqn)) {
			handleStructure(fqn.struKey(), new AtomicHashMap<Object, Fqn>());
			// tcache.getNode(fqn.getParent()).addChild(fqn) ;
		}
		return readNode(fqn);
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
					ex.printStackTrace();
					batchContainer.endBatch(true, false);
					if (ehandler == null)
						throw (Exception) ex;

					ehandler.handle(wsession, tjob, ex);
					return null;
				} catch (Error ex) {
					ex.printStackTrace();
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
		if (transaction != null)
			trans.put(transaction, wsession.iwconfig());
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
	public Cache<?, ?> cache() {
		return cache;
	}

	@Deprecated
	public TreeCache<PropertyId, PropertyValue> treeCache() {
		return tcache;
	}

	public GridFilesystem gfs() {
		return gfs;
	}

	// public WritableGridBlob gridBlob(String fqnPath, Metadata meta) throws IOException {
	// return gfsBlob.getWritableGridBlob(fqnPath, meta);
	// }

	public NodeWriter createNodeWriter(WriteSession wsession, ReadSession rsession) throws IOException {
		return new PGWriter(this, wsession);
	}

	static class PGWriter implements NodeWriter {
		private WriteSession wsession;
		private PGWorkspace wspace;
		private PGNodeStore pstore;

		public PGWriter(PGWorkspace wspace, WriteSession wsession) {
			this.wspace = wspace;
			this.wsession = wsession;
			this.pstore = wspace.pstore();
		}

		public void writeLog(final Set<TouchedRow> logRows) throws IOException {

			Callable<Void> job = new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					long startTime = System.currentTimeMillis();

					for (TouchedRow trow : logRows) {
						Touch touch = trow.touch();
						Fqn fqn = trow.target();
						String pathKey = fqn.toString();

						PGNodeEntry pentry = pstore.find(pathKey);
						switch (touch) {
						case TOUCH:
							break;
						case MODIFY:
							Map<PropertyId, PropertyValue> props = trow.sourceMap();
							if ("/".equals(pathKey))
								continue;

							pentry.write(EntryKey.PARENT, fqn.getParent().toString()).write(EntryKey.LASTMODIFIED, "" + System.currentTimeMillis());

							JsonObject nodeJson = new JsonObject();
							for (PropertyId pid : props.keySet()) {
								final String propId = pid.idString();
								PropertyValue pvalue = props.get(pid);
								nodeJson.add(propId, pvalue.json()); // data
							}

							pentry.write(EntryKey.VALUE, nodeJson.toString());
							pentry.update();
							break;
						case REMOVE:
							wspace.treeCache().removeNode(fqn); // @Todo -_- ?
							pentry.delete();
							break;
						case REMOVECHILDREN:
							pentry.deleteChildren();
							break;
						default:
							break;
						}
					}

					wspace.log.debug(wsession.tranId() + " writed");
					wsession.readSession().attribute(TranResult.class.getCanonicalName(), TranResult.create(logRows.size(), System.currentTimeMillis() - startTime));
					return null;
				}
			};
			wspace.es.submit(job);

			IndexJob<Void> indexJob = new IndexJob<Void>() {
				@Override
				public Void handle(IndexSession isession) throws Exception {
					IndexWriteConfig iwconfig = wsession.iwconfig();
					long startTime = System.currentTimeMillis();

					for (TouchedRow trow : logRows) {
						Touch touch = trow.touch();
						Action action = trow.target().dataKey().action();
						Fqn fqn = trow.target();
						String pathKey = fqn.toString();

						switch (touch) {
						case TOUCH:
							break;
						case MODIFY:
							Map<PropertyId, PropertyValue> props = trow.sourceMap();
							if ("/".equals(pathKey))
								continue;

							WriteDocument wdoc = isession.newDocument(pathKey);
							wdoc.keyword(EntryKey.PARENT, fqn.getParent().toString());
							wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());

							for (PropertyId pid : props.keySet()) {
								final String propId = pid.idString();
								PropertyValue pvalue = props.get(pid);

								JsonArray jarray = pvalue.asJsonArray(); // index
								if (pid.type() == PType.NORMAL) {
									VType vtype = pvalue.type();
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
										FieldIndex.KEYWORD.index(wdoc, propId, e.getAsString());
									}
								}

							}

							if (!iwconfig.isIgnoreIndex()) {
								if (action == Action.CREATE)
									wdoc.insert();
								else
									wdoc.update();
							}

							break;
						case REMOVE:
							wspace.treeCache().removeNode(fqn); // @Todo -_- ?
							if (!iwconfig.isIgnoreIndex())
								isession.deleteById(pathKey);
							isession.deleteQuery(new WildcardQuery(new Term(IKeywordField.DocKey, Fqn.fromString(pathKey).startWith())));
							break;
						case REMOVECHILDREN:
							isession.deleteQuery(new WildcardQuery(new Term(IKeywordField.DocKey, Fqn.fromString(pathKey).startWith())));
							break;
						default:
							break;
						}
					}

					wspace.log.debug(wsession.tranId() + " indexed");

					return null;
				}
			};

			wspace.central().newIndexer().asyncIndex(indexJob);
		}

	}

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
	@CacheEntryCreated
	public void modified(CacheEntryEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) {
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
	public AtomicMap<PropertyId, PropertyValue> handleData(TreeNodeKey dataKey, final AtomicMap<PropertyId, PropertyValue> created) {
		try {
			String fqnString = dataKey.fqnString();
			PGNodeEntry pentry = pstore().find(fqnString);
			if (!pentry.exists())
				return null;
			
			JsonObject json = pentry.toJson() ;

			for (String key : json.keySet()) {
				PropertyId propId = PropertyId.fromIdString(key);
				JsonElement val = json.asJsonObject(key);
				if (propId.type() == PType.REFER && val.isJsonObject() && ((JsonObject) val).has("vals")) {
					val = ((JsonObject) val).get("vals").getAsJsonArray();
					// val = val.asJsonArray("vals").toObjectArray()) ;
				}

				PropertyValue pvalue = PropertyValue.loadFrom(TreeNodeKey.fromString(fqnString), propId, val);
				created.put(propId, pvalue);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return created;
	}

	@Override
	public AtomicMap<Object, Fqn> handleStructure(TreeNodeKey struKey, final AtomicMap<Object, Fqn> created) {
		PGNodeEntry pentry = pstore().find(struKey.fqnString());
		if (pentry.exists()) {
			pentry.loadChild(created);
		}
		return created;
	}

	private PGNodeStore pstore() {
		return this.pstore;
	}

	public InputStream toInputStream(GridBlob gblob) throws FileNotFoundException {
		return pstore.find(gblob.path()).asInputStream();
	}

	public File toFile(GridBlob gblob) {
		throw new UnsupportedOperationException() ;
	}

	public GridBlob saveAt(GridBlob gblob, InputStream input) throws IOException {
		throw new UnsupportedOperationException() ;
	}

	public OutputStream toOutputStream(GridBlob gblob) throws IOException {
		return pstore.find(gblob.path()).asOutputStream();
	}

}

class PGNodeStore {

	private IDBController dc;

	public PGNodeStore(IDBController dc) {
		this.dc = dc;
	}

	public void close() {
		try {
			dc.destroySelf();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	public PGNodeEntry find(String pathKey) {
		try {
			Rows rows = dc.createUserProcedure("node@findBy(?)").addParam(pathKey).execQuery();
			return new PGNodeEntry(dc, pathKey, rows);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

}

class PGNodeEntry {

	private IDBController dc;
	private String pathKey;
	private Rows rows;
	private JsonObject props = new JsonObject();

	public PGNodeEntry(IDBController dc, String pathKey, Rows rows) {
		this.dc = dc;
		this.pathKey = pathKey;
		this.rows = rows;
	}

	public JsonObject toJson() throws SQLException {
		if (rows.first()){
			return JsonObject.fromString(rows.getString("property")).asJsonObject("__value") ;
		} else throw new SQLException("node not found : " + pathKey);
	}

	public OutputStream asOutputStream() throws FileNotFoundException {
		throw new UnsupportedOperationException();
	}

	public InputStream asInputStream() throws FileNotFoundException {
		throw new UnsupportedOperationException();
	}

	public void write(InputStream input) throws FileNotFoundException, IOException {
		throw new UnsupportedOperationException();
	}

	public String asString(String key) throws IOException {
		try {
			return toJson().asString(key) ;
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	public void loadChild(AtomicMap<Object, Fqn> created) {

		try {
			Rows crows = dc.createUserProcedure("node@childrenBy(?)").addParam(pathKey).execQuery();
			while (crows.next()) {
				created.put(crows.getString("name"), Fqn.fromRelativeElements(Fqn.fromString(pathKey), crows.getString("name")));
			}
		} catch (SQLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public boolean exists() {
		try {
			return rows.first();
		} catch (SQLException e) {
			return false;
		}
	}

	public PGNodeEntry update() throws IOException {
		try {
			dc.createUserProcedure("node$mergeWith(?,?,?)").addParam(pathKey).addParam(Fqn.fromString(pathKey).name()).addParam(props.toString()).execUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new IllegalStateException(e) ;
		}

		return this;
	}

	public PGNodeEntry write(String key, String value) throws IOException {
		if ("__value".equals(key)) props.put(key, JsonObject.fromString(value)) ;
		else props.put(key, value);
		return this;
	}

	public void delete() throws IOException {
		try {
			dc.createUserProcedure("node@deleteWith(?)").addParam(pathKey).execUpdate();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	public void deleteChildren() throws IOException {
		try {
			dc.createUserProcedure("Node@deleteChildWith(?)").addParam(pathKey).execUpdate();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
}
