package net.ion.craken.loaders.lucene;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.SearchResponse;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.TermQuery;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.loaders.modifications.Remove;
import org.infinispan.loaders.modifications.Store;
import org.infinispan.marshall.StreamingMarshaller;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

@CacheLoaderMetadata(configurationClass = CentralCacheStoreConfig.class)
public class CentralCacheStore extends AbstractCacheStore implements SearcherCacheStore {

	private CentralCacheStoreConfig config;
	private Central central;
	ScheduledExecutorService sche = Executors.newSingleThreadScheduledExecutor();

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return CentralCacheStoreConfig.class;
	}

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (CentralCacheStoreConfig) config;
	}

	@Override
	public void start() throws CacheLoaderException {
		try {
			// open the data file
			super.start();
			this.central = config.buildCentral();
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public void stop() throws CacheLoaderException {
		try {
			sche.shutdown() ;
			sche.awaitTermination(1, TimeUnit.SECONDS) ;
		} catch (InterruptedException ignore) {
			ignore.printStackTrace();
		}
		IOUtil.closeQuietly(central);
		super.stop();
	}

	public void fromStream(ObjectInput inputStream) throws CacheLoaderException {
		throw new UnsupportedOperationException();
	}

	public void toStream(ObjectOutput outputStream) throws CacheLoaderException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void purgeInternal() throws CacheLoaderException {
		// TODO Auto-generated method stub

	}

	@Override
	public void clear() throws CacheLoaderException {
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.deleteAll();
				return null;
			}
		});
	}

	@Override
	public boolean remove(Object _key) throws CacheLoaderException {
		final TreeNodeKey key = (TreeNodeKey) _key;
		if (key.getType().isStructure())
			return false;

		return central.newIndexer().index(new IndexJob<Boolean>() {
			@Override
			public Boolean handle(IndexSession isession) throws Exception {
				isession.deleteTerm(new Term(IKeywordField.ISKey, key.idString()));

				return Boolean.TRUE;
			}
		});
		// return true ;
	}

	private ArrayBlockingQueue<Modification> queue = new ArrayBlockingQueue<Modification>(100000);
	volatile boolean runningIndex = false ;
	protected void applyModifications(final List<? extends Modification> mods) throws CacheLoaderException {
		
		
		try {
			if (mods.size() <= 1) {
				for (Modification modification : mods) {
					queue.put(modification);
				}
				
				if (runningIndex) return ;
				sche.submit(new IndexCallable(this, this.queue)) ;
				
			} else {
				LinkedBlockingQueue<Modification> groupQueue = new LinkedBlockingQueue<Modification>();
				groupQueue.addAll(extractModiEvent(mods)) ;
				sche.submit(new IndexCallable(this, groupQueue)).get() ;
			}
		} catch (ExecutionException e) {
			throw new CacheLoaderException(e.getCause()) ;
		} catch (InterruptedException e) {
			throw new CacheLoaderException(e);
		}

	}

	private List<? extends Modification> extractModiEvent(List<? extends Modification> ori) {
		if (ori.size() == 1)
			return ori;

		final ListOrderedSet result = new ListOrderedSet();
		result.addAll(Lists.reverse(ori));
		return Lists.reverse(Lists.newArrayList(result));
	}

	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {
		final WriteDocument doc = toWriteDocument(entry);
		if (doc == null)
			return;
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.updateDocument(doc);
				return null;
			}
		});

	}

	WriteDocument toWriteDocument(InternalCacheEntry entry) {
		TreeNodeKey key = (TreeNodeKey) entry.getKey();
		if (key.getType() == Type.STRUCTURE)
			return null;

		AtomicMap value = (AtomicMap) entry.getValue();

		final WriteDocument doc = MyDocument.newDocument(key.idString());

		JsonObject jobj = new JsonObject();
		jobj.addProperty(DocEntry.ID, key.idString());
		jobj.addProperty(DocEntry.LASTMODIFIED, System.currentTimeMillis());
		jobj.add(DocEntry.PROPS, fromMapToJson(doc, key, value));

		doc.add(MyField.manual(DocEntry.VALUE, jobj.toString(), org.apache.lucene.document.Field.Store.YES, Index.NOT_ANALYZED));
		return doc;
	}

	private final static JsonObject fromMapToJson(WriteDocument doc, TreeNodeKey key, Map valueMap) {
		if (key.getType().isStructure()) {
			JsonObject jso = new JsonObject();
			AtomicMap<String, Fqn> childPaths = (AtomicMap<String, Fqn>) valueMap;
			for (Entry<String, Fqn> entry : childPaths.entrySet()) {
				jso.put(entry.getKey(), entry.getKey());
			}
			return jso;
		} else {
			JsonObject jso = new JsonObject();
			AtomicMap<PropertyId, PropertyValue> propertyMap = (AtomicMap<PropertyId, PropertyValue>) valueMap;
			String parentPath = key.getFqn().isRoot() ? "" : key.getFqn().getParent().toString();
			doc.keyword(DocEntry.PARENT, parentPath);

			for (Entry<PropertyId, PropertyValue> entry : propertyMap.entrySet()) {
				final PropertyId pid = entry.getKey();
				final String pstring = pid.idString(); // if type == refer, @
				final PropertyValue pvalue = entry.getValue();
				jso.add(pstring, pvalue.asJsonArray());

				if (pid.isIgnoreIndex()){
					continue ;
				}
				
				for (Object e : pvalue.asSet()) {
					if (pstring.startsWith("@")) {
						doc.keyword(pstring, ObjectUtil.toString(e));
					} else {
						doc.unknown(pstring, e);
					}
				}
			}
			return jso;
		}
	}

	@Override
	public InternalCacheEntry load(Object _key) throws CacheLoaderException {
		try {

			TreeNodeKey key = (TreeNodeKey) _key;
			if (key.getType().isStructure()) {
				List<ReadDocument> docs = central.newSearcher().createRequest(new TermQuery(new Term(DocEntry.PARENT, key.idString().substring(1)))).offset(1000000).find().getDocument();
				return DocEntry.create(key, docs);
			}
			// central.newSearcher().createRequest("").find().getDocument()
			ReadDocument read = central.newSearcher().createRequest(new TermQuery(new Term(IKeywordField.ISKey, key.idString()))).findOne();
			// Debug.line(key, "", read, Thread.currentThread().getStackTrace()) ;
			if (read == null) {
				return null;
			}
			InternalCacheEntry readObject = DocEntry.create(read);
			if (readObject != null && readObject.isExpired(System.currentTimeMillis())) {
				return null;
			}
			return readObject;
		} catch (IOException e) {
			throw new CacheLoaderException(e);
		} catch (ParseException e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException {
		try {
			SearchResponse response = central.newSearcher().createRequest("").selections(DocEntry.VALUE).offset(numEntries).find();
			List<ReadDocument> docs = response.getDocument();
			Set<InternalCacheEntry> result = new HashSet<InternalCacheEntry>();
			for (ReadDocument readDocument : docs) {
				InternalCacheEntry ice = readDocument.transformer(new Function<ReadDocument, InternalCacheEntry>() {
					@Override
					public InternalCacheEntry apply(ReadDocument doc) {
						return DocEntry.create(doc);
					}
				});
				if (ice == null)
					continue;
				result.add(ice);
			}

			return result;
		} catch (IOException e) {
			throw new CacheLoaderException(e);
		} catch (ParseException e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		return load(Integer.MAX_VALUE / 100);
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException {
		try {
			SearchResponse response = central.newSearcher().createRequest("").selections(DocEntry.VALUE).find();
			List<ReadDocument> docs = response.getDocument();

			Set<Object> result = new HashSet<Object>();
			for (ReadDocument readDocument : docs) {
				TreeNodeKey key = readDocument.transformer(new Function<ReadDocument, TreeNodeKey>() {
					public TreeNodeKey apply(ReadDocument readDoc) {
						String idString = JsonObject.fromString(readDoc.get(DocEntry.VALUE)).asString(DocEntry.ID);
						return TreeNodeKey.fromString(idString);
					}
				});
				if (key != null)
					result.add(key);
			}
			if (keysToExclude != null)
				result.removeAll(keysToExclude);
			return result;
		} catch (IOException ex) {
			throw new CacheLoaderException(ex);
		} catch (ParseException ex) {
			throw new CacheLoaderException(ex);
		}
	}

	public Central central() {
		return this.central;
	}

}


class IndexCallable implements Callable<Integer>{

	private CentralCacheStore parent ;
	private BlockingQueue<? extends Modification> queue ;
	
	IndexCallable(CentralCacheStore parent, BlockingQueue<? extends Modification> queue){
		this.parent = parent ;
		this.queue = queue ;
	}
	
	@Override
	public Integer call() throws Exception {
		parent.runningIndex = true ;
		boolean nextTry = queue.size() > 0 ;
		Integer indexedCount = parent.central().newIndexer().index(new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				int count = 0;
				while(queue.size() > 0) {
					count++;
					Modification m = queue.take() ;
					switch (m.getType()) {
					case STORE:
						Store s = (Store) m;
						final WriteDocument doc = parent.toWriteDocument(s.getStoredEntry());
						if (doc == null)
							break;
						isession.updateDocument(doc);
						break;
					case CLEAR:
						isession.deleteAll();
						break;
					case REMOVE:

						Remove r = (Remove) m;
						final TreeNodeKey key = (TreeNodeKey) r.getKey();
						isession.deleteTerm(new Term(IKeywordField.ISKey, key.idString()));
						break;
					default:
						throw new IllegalArgumentException("Unknown modification type " + m.getType());
					}
				}
				return count;
			}
		}); // end index
		if (nextTry) parent.sche.schedule(this, 100, TimeUnit.MILLISECONDS) ;
		parent.runningIndex = false ;
		return indexedCount ;
	}
	
}