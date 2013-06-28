package net.ion.craken.loaders.lucene;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.SearchResponse;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.loaders.modifications.Remove;
import org.infinispan.loaders.modifications.Store;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.StreamingMarshaller;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

@CacheLoaderMetadata(configurationClass = ISearcherCacheStoreConfig.class)
public class ISearcherCacheStore extends AbCacheStore {

	private ISearcherCacheStoreConfig config;
	private Central central;
	private String wsname;

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return ISearcherCacheStoreConfig.class;
	}

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (ISearcherCacheStoreConfig) config;
	}

	@Override
	public void start() throws CacheLoaderException {
		try {
			// open the data file
			this.wsname = StringUtil.substringBefore(cache().getName(), ".node");
			this.central = CentralConfig.oldFromDir(createDir()).build();

			super.start();

		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}

	private Directory createDir() {
		EmbeddedCacheManager dftManager = cache().getCacheManager();
		
		final Cache<Object, Object> metaCache = dftManager.getCache(wsname + ".meta");
		final Cache<Object, Object> chunkCache = dftManager.getCache(wsname + ".chunks");
		final Cache<Object, Object> lockCache = dftManager.getCache(wsname + ".locks");
		
		metaCache.start() ;
		chunkCache.start() ;
		lockCache.start() ;
		
//		Directory dir = new DirectoryBuilderImpl(metaCache, chunkCache, lockCache, wsname).chunkSize(1024 * 64).create(); // .chunkSize()
		InfinispanDirectory dir = new InfinispanDirectory(metaCache, chunkCache, lockCache, wsname, config.chunkSize());
		
//		String location = config.getLocation();
//		if (location == null || location.trim().length() == 0)
//			location = "Infinispan-FileCacheStore";
//		File dir = new File(location);
//		if (!dir.exists() && !dir.mkdirs())
//			throw new ConfigurationException("Directory " + dir.getAbsolutePath() + " does not exist and cannot be created!");
		return dir;
	}

	@Override
	public void stop() throws CacheLoaderException {
		try {
			IOUtil.closeQuietly(central);
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
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
		return central.newIndexer().index(new IndexJob<Boolean>() {
			@Override
			public Boolean handle(IndexSession isession) throws Exception {
				isession.deleteTerm(new Term(IKeywordField.ISKey, key.idString()));

				Debug.line(key.idString()) ;
				return Boolean.TRUE;
			}
		});
//		return true ;
	}

	protected void applyModifications(final List<? extends Modification> mods) throws CacheLoaderException {
//		if (true) return ;
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {

				List<Modification> extMods = extractModiEvent(mods) ;
				for (Modification m : extMods) {
					switch (m.getType()) {
					case STORE:
						Store s = (Store) m;
						isession.updateDocument(toWriteDocument(s.getStoredEntry()));
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
				
				return null;
			}

		});
	}


	private List<Modification> extractModiEvent(List<? extends Modification> ori) {
		final ListOrderedSet result = new ListOrderedSet();
		result.addAll(Lists.reverse(ori)) ;
		return Lists.reverse(Lists.newArrayList(result)) ;
	}
	
	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {
		final WriteDocument doc = toWriteDocument(entry);
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {

				isession.updateDocument(doc);
				return null;
			}
		});

	}

	private WriteDocument toWriteDocument(InternalCacheEntry entry) {
		TreeNodeKey key = (TreeNodeKey) entry.getKey();
		AtomicMap value = (AtomicMap) entry.getValue();
		final WriteDocument doc = MyDocument.newDocument(key.idString());

		JsonObject jobj = new JsonObject();
		jobj.addProperty(DocEntry.ID, key.idString());
		jobj.addProperty(DocEntry.LASTMODIFIED, System.currentTimeMillis());
		jobj.add(DocEntry.PROPS, fromMapToJson(doc, key, value));

		doc.add(MyField.manual(DocEntry.VALUE, jobj.toString(), org.apache.lucene.document.Field.Store.YES, Index.NOT_ANALYZED));
		return doc;
	}

	private final static JsonObject fromMapToJson(WriteDocument doc, TreeNodeKey key, Map _map) {
		if (key.getContents() == Type.STRUCTURE) {
			JsonObject jso = new JsonObject();
			AtomicMap<String, Fqn> map = (AtomicMap<String, Fqn>) _map;
			for (Entry<String, Fqn> entry : map.entrySet()) {
				jso.put(entry.getKey(), entry.getKey());
			}
			return jso;
		} else {
			JsonObject jso = new JsonObject();
			AtomicMap<PropertyId, PropertyValue> map = (AtomicMap<PropertyId, PropertyValue>) _map;
			String parentPath = key.getFqn().isRoot() ? "" : key.getFqn().getParent().toString();
			doc.keyword(DocEntry.PARENT, parentPath) ;
			
			for (Entry<PropertyId, PropertyValue> entry : map.entrySet()) {
				final String pstring = entry.getKey().idString(); // if type == refer, @
				final PropertyValue pvalue = entry.getValue();
				jso.add(pstring, pvalue.asJsonArray());
				
				for(Object e : pvalue.asSet()){
					if (pstring.startsWith("@")){
						doc.keyword(pstring, ObjectUtil.toString(e)) ;
					} else {
						doc.unknown(pstring, e) ;
					}
				}
			}
			return jso;
		}
	}

	
	private int loadCount = 0 ;
	@Override
	public InternalCacheEntry load(Object _key) throws CacheLoaderException {
		try {


			TreeNodeKey key = (TreeNodeKey) _key;
			ReadDocument read = central.newSearcher().createRequest(new TermQuery(new Term(IKeywordField.ISKey, key.idString()))).findOne();

//			Debug.line(loadCount++ , "load", _key, read);
			if (read == null) {
				return null;
			}
			InternalCacheEntry readObject = DocEntry.create(JsonObject.fromString(read.get(DocEntry.VALUE)));
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
						String valueJson = doc.get(DocEntry.VALUE);
						if (StringUtil.isBlank(valueJson))
							return null;
						return DocEntry.create(JsonObject.fromString(valueJson));
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

}
