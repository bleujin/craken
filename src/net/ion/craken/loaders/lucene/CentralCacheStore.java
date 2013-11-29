package net.ion.craken.loaders.lucene;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.SearchResponse;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TermQuery;
import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoader;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.xa.GlobalTransaction;

import com.google.common.base.Function;

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


	public static CacheLoader blank() {
		return new CentralCacheStore();
	}

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
	}

	@Override
	public void store(final InternalCacheEntry entry) throws CacheLoaderException {
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
}
