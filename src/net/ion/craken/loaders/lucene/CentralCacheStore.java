package net.ion.craken.loaders.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionLog;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.SearchResponse;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.loaders.modifications.Remove;
import org.infinispan.loaders.modifications.Store;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.xa.GlobalTransaction;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

@CacheLoaderMetadata(configurationClass = CentralCacheStoreConfig.class)
public class CentralCacheStore extends AbstractCacheStore implements SearcherCacheStore {

	private CentralCacheStoreConfig config;
	private Central central;
	private GridFilesystem gfs;
	private Address address;

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return CentralCacheStoreConfig.class;
	}

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (CentralCacheStoreConfig) config;
		EmbeddedCacheManager dm = cache.getCacheManager();
		final String wsName = StringUtil.substringBefore(cache.getName(), ".");
		this.gfs = new GridFilesystem(dm.<String, byte[]>getCache( wsName + ".blobdata")) ;
		this.address = dm.getAddress() ;
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

//	@Override
//	public SearcherCacheStore gfs(DefaultCacheManager dm, GridFilesystem gfs) {
//		this.address = dm.getAddress() ;
//		this.gfs = gfs;
//		return this;
//	}

	@Override
	public void stop() throws CacheLoaderException {
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
	public void removeAll(final Set<Object> keys) throws CacheLoaderException {
		central.newIndexer().index(new IndexJob<Boolean>() {
			@Override
			public Boolean handle(IndexSession isession) throws Exception {
				if (keys != null && !keys.isEmpty()) {
					for (Object _key : keys) {
						TreeNodeKey key = (TreeNodeKey) _key;
						isession.deleteTerm(new Term(IKeywordField.ISKey, key.idString()));
					}
				}
				return null;
			}
		});
	}

	@Override
	public boolean remove(Object _key) throws CacheLoaderException {
		final TreeNodeKey key = (TreeNodeKey) _key;
		if (key.getType().isStructure())
			return true;

		return central.newIndexer().index(new IndexJob<Boolean>() {
			@Override
			public Boolean handle(IndexSession isession) throws Exception {
				isession.deleteTerm(new Term(IKeywordField.ISKey, key.idString()));

				return Boolean.TRUE;
			}
		});
		// return true ;
	}

	@Override
	public void prepare(List<? extends Modification> mods, GlobalTransaction tx, boolean isOnePhase) throws CacheLoaderException {
		super.prepare(mods, tx, isOnePhase);
	}

	@Override
	public void commit(GlobalTransaction tx) throws CacheLoaderException {
		super.commit(tx);
	}

	protected void applyModifications(final List<? extends Modification> mods) throws CacheLoaderException {
		try {

//			Debug.line(mods) ;

			if (mods.size() == 1) {
				Modification mod = mods.get(0) ;
				if (mod.getType() == org.infinispan.loaders.modifications.Modification.Type.STORE) {  // restore
					Store s = (Store) mod;
					TreeNodeKey key = (TreeNodeKey) s.getStoredEntry().getKey();
					if (TransactionLog.isLogKey(key)){
						AtomicMap<PropertyId, PropertyValue> mapValue = (AtomicMap<PropertyId, PropertyValue>) s.getStoredEntry().getValue();
						if (mapValue.containsKey(TransactionLog.PropId.ADDRESS) && (this.address.toString().equals(mapValue.get(TransactionLog.PropId.ADDRESS).stringValue()))) {
//						if (this.address.toString().equals(mapValue.get(TransactionLog.PropId.ADDRESS).stringValue())) {
							return ;
						}
						
						central.newIndexer().index(CommitUnit.create(key, mapValue, this.gfs).index());
					}
				}
				return; // trans cache
			} else {
				List<? extends Modification> extMods = extractModiEvent(mods);
				
				for (Modification mod : extMods) {
					if (mod.getType() == org.infinispan.loaders.modifications.Modification.Type.STORE) {
						Store s = (Store) mod;
						TreeNodeKey key = (TreeNodeKey) s.getStoredEntry().getKey();
						AtomicMap<PropertyId, PropertyValue> mapValue = (AtomicMap<PropertyId, PropertyValue>) s.getStoredEntry().getValue();

						if (TransactionLog.isLogKey(key) && mapValue.containsKey(TransactionLog.PropId.CONFIG)) {
							central.newIndexer().index(CommitUnit.create(key, mapValue, this.gfs).index());
						} 
						// s.getStoredEntry().setLifespan(1000L) ;
					} else if (mod.getType() == org.infinispan.loaders.modifications.Modification.Type.REMOVE) {
						Remove r = (Remove) mod;
						final TreeNodeKey forRemoveKey = (TreeNodeKey) r.getKey();
						if (forRemoveKey.getType().isStructure())
							continue;
					}

				}

			}
		} catch(IOException ex) {
			throw new CacheLoaderException(ex) ;
		} finally {
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
	public void store(final InternalCacheEntry entry) throws CacheLoaderException {
		final TreeNodeKey key = (TreeNodeKey) entry.getKey();
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				throw new IllegalAccessError("-_-? not supported");
			}
		});
	}

	@Override
	public InternalCacheEntry load(Object _key) throws CacheLoaderException {
		try {

			TreeNodeKey key = (TreeNodeKey) _key;
			if (key.action() == Action.RESET || key.action() == Action.CREATE)
				return null; // if log, return

			if (key.getType().isStructure()) {
				List<ReadDocument> docs = central.newSearcher().createRequest(new TermQuery(new Term(DocEntry.PARENT, key.fqnString()))).selections(IKeywordField.ISKey).offset(1000000).find().getDocument();
				return DocEntry.create(key, docs);
			}
			ReadDocument findDoc = central.newSearcher().createRequest(new TermQuery(new Term(IKeywordField.ISKey, key.idString()))).selections(DocEntry.VALUE).findOne();
			if (findDoc == null) {
				return null;
			}

			InternalCacheEntry readObject = DocEntry.create(findDoc);
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

//			Map<String, String> commitData = central.newReader().commitUserData();
//			String lastCommitTime = commitData.get(IndexSession.LASTMODIFIED);
			SearchResponse response = central.newSearcher().createRequest("").selections(DocEntry.VALUE).offset(numEntries).selections(DocEntry.VALUE).find();
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

	@Override
	protected void purgeInternal() throws CacheLoaderException {

	}

	public static WriteDocument toWriteDocument(IndexSession isession, IndexWriteConfig indexConfig, Fqn fqn, AtomicMap<PropertyId, PropertyValue> props) {
		throw new UnsupportedOperationException("working...--");
	}

}

class CommitUnit {

	private TreeNodeKey tranKey;
	private IndexWriteConfig iwconfig;
	private long time;
	private InputStream input;
	private PropertyValue tranPropertyValue;

	private static IndexJob<Integer> BLANKJOB = new IndexJob<Integer>() {
		@Override
		public Integer handle(IndexSession session) throws Exception {
			return 0;
		}
	};

	CommitUnit(TreeNodeKey key, IndexWriteConfig iwconfig, long time, PropertyValue tranPropertyValue, InputStream input) {
		this.tranKey = key;
		this.iwconfig = iwconfig;
		this.time = time;
		this.tranPropertyValue = tranPropertyValue ;
		this.input = input;
	}
	
	public static CommitUnit create(TreeNodeKey key, AtomicMap<PropertyId, PropertyValue> value, GridFilesystem gfs) throws FileNotFoundException {
		long time = value.get(PropertyId.normal("time")).longValue(0);
		IndexWriteConfig wconfig = JsonObject.fromString(value.get(PropertyId.normal("config")).stringValue()).getAsObject(IndexWriteConfig.class) ;
		
		
		GridBlob gblob = value.get(PropertyId.normal("tran")).gfs(gfs).asBlob();
		return create(key, wconfig, time, gblob);
	}

	public static CommitUnit create(TreeNodeKey key, IndexWriteConfig wconfig, long time, GridBlob gblob) throws FileNotFoundException {
		return new CommitUnit(key, wconfig, time,  gblob.getMetadata().asPropertyValue(), gblob.toInputStream()) ;
	}
	public static CommitUnit test(TreeNodeKey key, IndexWriteConfig iwconfig, long time, PropertyValue tranValue, InputStream input) throws FileNotFoundException {
		return new CommitUnit(key, iwconfig, time, tranValue, input) ;
	}
	

	public boolean hasNotConfig() {
		return iwconfig == null;
	}

	public IndexWriteConfig indexWriteConfig() {
		return iwconfig;
	}

	static Map<String, Object> testCreateConfigMap() {
		Map<String, Object> result = MapUtil.newMap();
		result.put("config", new IndexWriteConfig().ignoreBodyField().keyword("name").num("age").ignore("noname").toJson().toString());
		result.put("time", 1234);
		return result;
	}

	long time() {
		return time;
	}

	IndexJob<Integer> index() {
		return new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				indexWriteConfig().indexSession(isession, tranKey, tranPropertyValue);

				int count = 0;
				try {
					
					JsonReader jreader = new JsonReader(new InputStreamReader(input, "UTF-8"));
					jreader.beginObject();

					while (jreader.hasNext()) {
						String oname = jreader.nextName();
						if ("config".equals(oname)) {
							jreader.skipValue();
						} else if ("time".equals(oname)) {
							jreader.nextLong();
						} else if ("logs".equals(oname)) {
							jreader.beginArray();
							while (jreader.hasNext()) {
								jreader.beginObject();
								
								String id = null ;
								Touch touch = null ;
								String path = null ;
								JsonObject val = null ;
								
								while (jreader.hasNext()) {
									String iname = jreader.nextName();
									if ("id".equals(iname)) {
										id = jreader.nextString();
									} else if ("path".equals(iname)) {
										path = jreader.nextString();
									} else if ("touch".equals(iname)) {
										touch = Touch.valueOf(jreader.nextString());
									} else if ("val".equals(iname)) {
										val = jreader.nextJsonObject();
									}
								}
								jreader.endObject();
								
								TransactionLog log = TransactionLog.create(id, path, touch, val);
//								Debug.line(touch, log) ;
								switch (touch) {
								case TOUCH :
								case MODIFY:
									log.writeDocument(isession, indexWriteConfig());
									break;
								case REMOVE:
									isession.deleteTerm(new Term(IKeywordField.ISKey, log.path()));
									break;
								case REMOVECHILDREN:
									isession.deleteQuery(new WildcardQuery(new Term(DocEntry.PARENT, Fqn.fromString(log.path()).startWith() )));
									break;
								default:
									throw new IllegalArgumentException("Unknown modification type " + log.touchType());
								}
							}
							jreader.endArray();
							// reader.skipValue() ;
						}
					}

					jreader.endObject();
					jreader.close();
				} finally {
					IOUtil.closeQuietly(input);
				}
				
//				WriteDocument tranDoc = isession.newDocument("/__transactions/abcd").keyword("dd", "ddddd");
//				isession.updateDocument(tranDoc) ;
				
				

				return count;
			}

		};
	}

//	IndexJob<Integer> index() {
//		if (hasNotConfig())
//			return CommitUnit.BLANKJOB;
//
//		return new IndexJob<Integer>() {
//			@Override
//			public Integer handle(IndexSession isession) throws Exception {
//				indexWriteConfig().indexSession(isession, tranKey);
//
//				for (TransactionLog log : logs()) {
//					switch (log.touchType()) {
//					case MODIFY:
//						log.writeDocument(isession, indexWriteConfig());
//						break;
//					case REMOVE:
//						isession.deleteTerm(new Term(IKeywordField.ISKey, log.path()));
//						break;
//					case REMOVECHILDREN:
//						isession.deleteTerm(new Term(DocEntry.PARENT, log.path()));
//						break;
//					default:
//						throw new IllegalArgumentException("Unknown modification type " + log.touchType());
//					}
//					log.writeLog(isession, tranKey);
//				}
//				return logSize();
//			}
//		};
//	}

}
