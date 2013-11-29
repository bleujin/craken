package net.ion.craken.node;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.Metadata;
import net.ion.craken.io.WritableGridBlob;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.lucene.CentralCacheStore;
import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.mr.NodeMapReduceTask;
import net.ion.craken.node.AbstractWriteSession.LogRow;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.exception.NodeNotExistsException;
import net.ion.craken.node.exception.NotFoundPath;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeStructureSupport;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.ObjectId;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.notifications.Listener;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

@Listener
public abstract class Workspace extends TreeStructureSupport{
	
	private Repository repository;
	private AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache;
	private GridFilesystem gfsBlob ;	
	private GridFilesystem logContent ;
	private Cache<String, Metadata> logMeta ;
	private String wsName;
	private BatchContainer batchContainer;
	private CentralCacheStore cstore;
	private AbstractCacheStoreConfig config;
	private Engine parseEngine = Engine.createDefaultEngine() ;
	
	private static final Log log = LogFactory.getLog(Workspace.class);
	
	protected Workspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, AbstractCacheStoreConfig config) throws CorruptIndexException, IOException {
		this(repository, cache.getAdvancedCache(), wsName, config) ;
	}
	
	private Workspace(Repository repository, AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, AbstractCacheStoreConfig config) throws CorruptIndexException, IOException {
		super(cache, cache.getBatchContainer()) ;
		this.repository = repository;
		this.cache = cache;
		this.gfsBlob = new GridFilesystem(cache.getCacheManager().<String, byte[]>getCache(wsName + ".blob")) ;
		
		this.logContent = new GridFilesystem(cache.getCacheManager().<String, byte[]>getCache(wsName + ".log")) ;
		this.logMeta = cache.getCacheManager().<String, Metadata>getCache(wsName + ".logmeta") ;
		this.cstore = ((CentralCacheStore) cache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore());
		
		this.wsName = wsName;
		this.config = config ;
		this.batchContainer = cache.getBatchContainer();
	}
	
	

	public <T> T getAttribute(String key, Class<T> clz) {
		return repository.getAttribute(key, clz);
	}

	public String wsName() {
		return wsName;
	}

	public void close() {
		final Object[] caches = cache.getListeners().toArray(new Object[0]);
		for (Object listener : caches) {
			this.removeListener(listener);
		}

		// treeCache.getCache().stop() ;
		cache.stop();
	}

//	public void init() {
//		try {
//			beginTran();
//			createNode(IndexWriteConfig.Default, Fqn.ROOT);
//			createNode(IndexWriteConfig.Default, Fqn.TRANSACTIONS);
//		} finally {
//			endTran();
//		}
//	}

	public TreeNode createNode(Fqn fqn) {
		final AtomicHashMap<PropertyId, PropertyValue> props = new AtomicHashMap<PropertyId, PropertyValue>();
		cache.put(fqn.dataKey().createAction(), props) ;
		
		if (log.isTraceEnabled()) log.tracef("Created node %s", fqn);
		return new TreeNode(this, fqn) ;
	}

	public TreeNode resetNode(Fqn fqn) {
		final AtomicHashMap<PropertyId, PropertyValue> props = new AtomicHashMap<PropertyId, PropertyValue>();
		cache.put(fqn.dataKey().resetAction(), props) ;
		
		if (log.isTraceEnabled()) log.tracef("Reset node %s", fqn);
		return new TreeNode(this, fqn) ;
	}

	public TreeNode pathNode(Fqn fqn, boolean createIfNotExist) {
		if (createIfNotExist) {
			mergeAncestor(fqn) ;
			if (log.isTraceEnabled()) log.tracef("Merged node %s", fqn);
			return new TreeNode(this, fqn);
		} else if (exists(fqn)) {
			return new TreeNode(this, fqn) ;
		}
		else throw new NotFoundPath(fqn) ;
	}

	// public TreeNode logNode(IndexWriteConfig iwconfig, Fqn fqn) {
	// return treeCache.logWith(iwconfig, fqn) ;
	// }

//	public boolean exists(Fqn fqn) {
//		return treeCache.exists(fqn);
//	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob) {
		return tran(wsession, tjob, null);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) {
		final Workspace workspace = this;
		return repository.executor().submitTask(new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					batchContainer.startBatch(true) ;
					wsession.prepareCommit();
					T result = tjob.handle(wsession);
					
					wsession.endCommit();
					batchContainer.endBatch(true, true) ;
					
					index(wsession) ;
					
					return result;
				} catch (Throwable ex) {
//					ex.printStackTrace() ;
					batchContainer.endBatch(true, false) ;
					if (ehandler == null)
						if (ex instanceof Exception)
							throw (Exception) ex;
						else
							throw new Exception(ex);

					ehandler.handle(wsession, ex);
					return null;
				} finally {
				}

			}
		});
	}
	
	
	private void index(WriteSession wsession) throws IOException {
		final Metadata meta = logMeta.get(wsession.tranId());
		InputStream input = logContent.getGridBlob(meta).toInputStream();
		
//		Debug.line(IOUtil.toStringWithClose(input)) ;
		
		final JsonReader reader = new JsonReader(new InputStreamReader(input, "UTF-8"));
		
		reader.beginObject();
		String configName = reader.nextName() ;
		final JsonObject config = reader.nextJsonObject() ;
		String logName = reader.nextName() ;
		
//		Debug.line(config, wsession.tranId(), meta) ;
		
		int count = central().newIndexer().index(new IndexJob<Integer>() {
			public Integer handle(IndexSession isession) throws Exception {

				final long start = System.currentTimeMillis() ;
				isession.setIgnoreBody(config.asBoolean("ignoreBody")) ;
//				isession.indexWriterConfig(new IndexWriterConfig(Version.LUCENE_CURRENT, new MyKoreanAnalyzer(Version.LUCENE_CURRENT)));
				
				reader.beginArray() ;
				final int count = config.asInt("count") ;
				for(int i = 0 ; i < count ; i++){
					JsonObject tlog = reader.nextJsonObject() ;
					String path = tlog.asString("path") ;
					Touch touch = Touch.valueOf(tlog.asString("touch")) ;
					Action action = Action.valueOf(tlog.asString("action")) ;
//					Debug.line(path, touch) ;
					switch (touch) {
					case TOUCH :
						break ;
					case MODIFY:
						JsonObject val = tlog.asJsonObject("val") ;
						WriteDocument propDoc = isession.newDocument(path);
						JsonObject jobj = new JsonObject();
		                jobj.addProperty(DocEntry.ID, path);
		                jobj.addProperty(DocEntry.LASTMODIFIED, System.currentTimeMillis());
		                jobj.add(DocEntry.PROPS, fromMapToJson(path, propDoc, IndexWriteConfig.read(config), val.entrySet()));
		                propDoc.add(MyField.manual(DocEntry.VALUE, jobj.toString(), org.apache.lucene.document.Field.Store.YES, Index.NOT_ANALYZED));
		                
		                if (action == Action.CREATE) isession.insertDocument(propDoc) ;
		                else isession.updateDocument(propDoc);
//						isession.updateDocument(propDoc) ;
						
						break;
					case REMOVE:
						isession.deleteTerm(new Term(IKeywordField.ISKey, path));
						break;
					case REMOVECHILDREN:
						isession.deleteQuery(new WildcardQuery(new Term(DocEntry.PARENT, Fqn.fromString(path).startWith() )));
						break;
					default:
						throw new IllegalArgumentException("Unknown modification type " + touch);
					}
				}
				
//				Debug.line(count, System.currentTimeMillis() - start) ;
				return count;
			}
			
			private JsonObject fromMapToJson(String path, WriteDocument doc, IndexWriteConfig iwconfig, Set<Map.Entry<String, JsonElement>> props) {
				JsonObject jso = new JsonObject();
				String parentPath = Fqn.fromString(path).getParent().toString();
				doc.keyword(DocEntry.PARENT, parentPath);
				doc.number(DocEntry.LASTMODIFIED, System.currentTimeMillis());

				for (Entry<String, JsonElement> entry : props) {
					final PropertyId propertyId = PropertyId.fromIdString(entry.getKey());
					
					
					if (propertyId.type() == PType.NORMAL){
						String propId = propertyId.getString() ;
						JsonArray pvalue = entry.getValue().getAsJsonArray();
						jso.add(propId, entry.getValue().getAsJsonArray());
						for (JsonElement e : pvalue.toArray()) {
							if (e == null)
								continue;
							FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
							fieldIndex.index(doc, propId, e.isJsonObject() ? e.toString() : e.getAsString());
						}
					} else if (propertyId.type() == PType.REFER){
						final String propId = propertyId.getString() ;
						JsonArray pvalue = entry.getValue().getAsJsonArray();
						jso.add(propId, entry.getValue().getAsJsonArray()); // if type == refer, @
						for (JsonElement e : pvalue.toArray()) {
							if (e == null)
								continue;
							FieldIndex.KEYWORD.index(doc, '@' + propId, e.getAsString());
						}
					}
				}
//				for (Entry<String, JsonElement> entry : rels().entrySet()) {
//					final String propId = entry.getKey();
//					JsonArray pvalue = entry.getValue().getAsJsonArray();
//					jso.add(propId, entry.getValue().getAsJsonArray()); // if type == refer, @
//					for (JsonElement e : pvalue.toArray()) {
//						if (e == null)
//							continue;
//						FieldIndex.KEYWORD.index(doc, '@' + propId, e.getAsString());
//					}
//				}

				return jso;
			}
			
			
		}) ;
		reader.endArray() ;
		reader.endObject() ;
		reader.close() ;
		input.close() ;
	}
	

	public void continueUnit(WriteSession wsession) throws IOException {
		wsession.endCommit() ;
		batchContainer.endBatch(true, true) ;
		index(wsession) ;
		
		wsession.tranId(new ObjectId().toString()) ;
		batchContainer.startBatch(true) ;
		wsession.prepareCommit() ;
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
	
	
	
	public Workspace addListener(Object listener) {
		if (listener instanceof WorkspaceListener) {
			((WorkspaceListener) listener).registered(this);
		}

		cache.addListener(listener);
		return this;
	}

	public void removeListener(Object listener) {
		if (listener instanceof WorkspaceListener) {
			((WorkspaceListener) listener).unRegistered(this);
		}

		cache.removeListener(listener);
	}

	public Central central() {
		return cstore.central();
	}

	public IExecutor executor() {
		return repository.executor();
	}

	public AbstractCacheStoreConfig config() {
		return config;
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
	
	
	

	
	public void remove(Fqn fqn){
		cache.remove(fqn.dataKey());
		cache.remove(fqn.struKey());
	}


	
	
	
	
	// TestOnly
	public  Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache() {
		return cache;
	}

	public GridFilesystem gfs() {
		return gfsBlob;
	}

	public WritableGridBlob gridBlob(String fqnPath, Metadata meta) throws IOException {
		return gfsBlob.getWritableGridBlob(fqnPath, meta);
	}

	public InstantLogWriter createLogWriter(WriteSession wsession, ReadSession rsession) throws IOException {
		return new InstantLogWriter(this, wsession, rsession) ;
	}

	private boolean trace = false ;
	public void move(GridFilesystem gfs, Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
		if (trace) log.tracef("Moving node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
		if (nodeToMoveFqn == null || newParentFqn == null)
			throw new NullPointerException("Cannot accept null parameters!");

		if (nodeToMoveFqn.getParent().equals(newParentFqn)) {
			if (trace) log.trace("Not doing anything as this node is equal with its parent");
			// moving onto self! Do nothing!
			return;
		}

		// Depth first. Lets start with getting the node we want.
		boolean success = false;
		try {
			// check that parent's structure map contains the node to be moved. in case of optimistic locking this
			// ensures the write skew is properly detected if some other thread removes the child
			TreeNode parent = pathNode(nodeToMoveFqn.getParent(), true);
			if (!parent.hasChild(nodeToMoveFqn.getLastElement())) {
				 if (trace) log.trace("The parent does not have the child that needs to be moved. Returning...");
				return;
			}
			TreeNode nodeToMove = pathNode(nodeToMoveFqn, true);
			if (nodeToMove == null) {
				if (trace) log.trace("Did not find the node that needs to be moved. Returning...");
				return; // nothing to do here!
			}
			if (!exists(newParentFqn)) {
				// then we need to silently create the new parent
				mergeAncestor(newParentFqn);
				if (trace) log.tracef("The new parent (%s) did not exists, was created", newParentFqn);
			}

			// create an empty node for this new parent
			Fqn newFqn = Fqn.fromRelativeElements(newParentFqn, nodeToMoveFqn.getLastElement());
			mergeAncestor(newFqn);
			TreeNode newNode = pathNode(newFqn, true);
			Map<PropertyId, PropertyValue> oldData = nodeToMove.readMap();
			if (oldData != null && !oldData.isEmpty())
				newNode.putAll(oldData);
			for (Object child : nodeToMove.getChildrenNames()) {
				// move kids
				if (trace) log.tracef("Moving child %s", child);
				Fqn oldChildFqn = Fqn.fromRelativeElements(nodeToMoveFqn, child);
				move(gfs, oldChildFqn, newFqn);
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
	


	static class InstantLogWriter {

		private final Workspace wspace;
		private final WriteSession wsession;
		private final ReadSession rsession ;
		
		private JsonWriter jwriter ;
		private Metadata metadata;
		
		public InstantLogWriter(Workspace wspace, WriteSession wsession, ReadSession rsession) throws IOException {
			this.wspace = wspace ;
			this.wsession = wsession ;
			this.rsession = rsession ;
			this.metadata = Metadata.create(wsession.tranId());
			GridBlob gridBlob = wspace.logContent.getGridBlob(metadata);
			OutputStream output = wspace.logContent.getOutput(gridBlob, false) ;

			Writer swriter = new BufferedWriter(new OutputStreamWriter(output, Charset.forName("UTF-8")));
			this.jwriter = new JsonWriter(swriter) ;
		}

		public InstantLogWriter beginLog(Set<LogRow> logRows) throws IOException {
			final long thisTime = System.currentTimeMillis();
			
			jwriter.beginObject() ;
			
			jwriter.name("config") ;
			jwriter.beginObject() ;
			wsession.iwconfig().writeJson(jwriter, thisTime, logRows.size()) ;
			jwriter.endObject() ;
			
			jwriter.name("logs") ;
			jwriter.beginArray() ;
			
			return this ;
		}
		
		public void writeLog(LogRow row) throws IOException {
			Fqn target = row.target() ;
			Touch touch = row.touch() ;
			
			jwriter.beginObject() ;
			
			jwriter.name("path").value(target.toString()).name("touch").value(touch.name()).name("action").value(target.dataKey().action().toString()) ; //.jsonElement("val", nodeValue) ;
			if (touch == Touch.MODIFY) {
				jwriter.jsonElement("val", rsession.exists(target) ? rsession.pathBy(target).toValueJson() : JsonObject.create()) ;
			} 
			
			jwriter.endObject() ;
		}

		public InstantLogWriter endLog() throws IOException{
			jwriter.endArray() ;
			jwriter.endObject() ;
			jwriter.flush() ;
			jwriter.close() ;
			
			wspace.logMeta.put(wsession.tranId(), metadata) ;
			return this ;
		}
	}


}
