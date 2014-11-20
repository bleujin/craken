package net.ion.craken.loaders;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.TreeNodeKey.Action;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.EachDocHandler;
import net.ion.nsearcher.search.EachDocIterator;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.executors.ExecutorAllCompletionService;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.persistence.PersistenceUtil;
import org.infinispan.persistence.TaskContextImpl;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

@Listener
public class CrakenStore implements AdvancedLoadWriteStore {
	private static final Log log = LogFactory.getLog(CrakenStore.class);
	private static final boolean trace = log.isTraceEnabled();
	private CrakenStoreConfiguration configuration;
	protected InitializationContext ctx;
	private Central central;

	@Override
	public void init(InitializationContext ctx) {
		this.ctx = ctx;
		this.configuration = ctx.getConfiguration();
	}

	@Override
	public void start() {
		try {
//			open the data file
//			String location = configuration.location();
//			if (StringUtil.isBlank(location))
//				location = "./resource/temp/memory-store";
//			File file = new File(location, ctx.getCache().getName() + ".dat");
//			if (!file.exists()) {
//				File dir = file.getParentFile();
//				if (!dir.mkdirs() && !dir.exists()) {
//					throw log.directoryCannotBeCreated(dir.getAbsolutePath());
//				}
//			}

			String name = ctx.getCache().getName();
			EmbeddedCacheManager cacheManager = ctx.getCache().getCacheManager();
			Cache<?, ?> metaCache = cacheManager.getCache(name + "-meta");
			Cache<?, ?> chunkCache = cacheManager.getCache(name + "-chunk");

			BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, chunkCache, metaCache, name);
			bcontext.chunkSize(1024 * 1024);
			Directory directory = bcontext.create();
			this.central = CentralConfig.oldFromDir(directory).build();

			this.configuration.store(this);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public void stop() {
		IOUtil.closeQuietly(central);
	}

	@CacheStopped
	public void whenStopped(CacheStoppedEvent event) {
		Debug.line("STOPPE");
	}

	/**
	 * The base class implementation calls {@link #load(Object)} for this, we can do better because we keep all keys in memory.
	 */
	@Override
	public boolean contains(Object _key) {
		try {
			final TreeNodeKey key = (TreeNodeKey) _key;
			if (key.getType().isStructure())
				return true;

			String id = key.getFqn().toString();
			return central.newSearcher().createRequestByKey(id).findOne() != null;
		} catch (IOException e) {
			return false;
		} catch (ParseException e) {
			return false;
		}
	}

	@Override
	public void write(final MarshalledEntry entry) {

	}

	@Override
	public void clear() {

	}

	@Override
	public boolean delete(Object _key) {
		return true;
	}

	@Override
	public MarshalledEntry load(Object _key) {
		return _load(_key, true, true);
	}

	private MarshalledEntry _load(Object _key, boolean fetchValue, boolean fetchMetaValue) {
		try {
			TreeNodeKey key = (TreeNodeKey) _key;
			if (key.action() == Action.RESET || key.action() == Action.CREATE)
				return null; // if log, return

			if (key.getType().isStructure()) {
				List<ReadDocument> docs = central.newSearcher().createRequest(new TermQuery(new Term(EntryKey.PARENT, key.fqnString()))).selections(IKeywordField.DocKey).offset(1000000).find().getDocument();
				AtomicHashMap<String, Fqn> values = new AtomicHashMap<String, Fqn>();
				for (ReadDocument doc : docs) {
					String fqnString = doc.idValue();
					values.put(fqnString, Fqn.fromString(fqnString));
				}
				InternalMetadata metadataBb = null;
				return ctx.getMarshalledEntryFactory().newMarshalledEntry(key, values, metadataBb);
			}

			ReadDocument findDoc = central.newSearcher().createRequestByKey(key.idString()).selections(EntryKey.VALUE).findOne();
			if (findDoc == null) {
				return null;
			}
			return entryFromDoc(key, findDoc);
		} catch (IOException e) {
			return null;
		} catch (ParseException ex) {
			return null;
		}
	}

	private MarshalledEntry entryFromDoc(TreeNodeKey key, ReadDocument findDoc) {
		InternalMetadata metadataBb = null;
		AtomicHashMap<PropertyId, PropertyValue> nodeValue = new AtomicHashMap<PropertyId, PropertyValue>();
		JsonObject raw = JsonObject.fromString(findDoc.asString(EntryKey.VALUE));
		JsonObject props = raw.asJsonObject(EntryKey.PROPS);
		for (Entry<String, JsonElement> entry : props.entrySet()) {
			String pkey = entry.getKey();
			JsonElement pvalue = entry.getValue();
			if (pkey.startsWith("@")){
				nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.createPrimitive(pvalue.getAsString()));
			} else {
				nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.loadFrom(key, pkey, pvalue.getAsJsonObject()));
			}
			
		}

		return ctx.getMarshalledEntryFactory().newMarshalledEntry(key, nodeValue, metadataBb);
	}

	@Override
	public void process(KeyFilter filter, final CacheLoaderTask task, Executor executor, final boolean fetchValue, final boolean fetchMetadata) {

		if (true) return ;
		
		filter = PersistenceUtil.notNull(filter);
		Set<Object> keysToLoad = new HashSet<Object>(ctx.getCache().keySet());

		ExecutorAllCompletionService eacs = new ExecutorAllCompletionService(executor);

		final TaskContextImpl taskContext = new TaskContextImpl();
		for (final Object key : keysToLoad) {
			if (taskContext.isStopped())
				break;

			eacs.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						final MarshalledEntry marshalledEntry = _load(key, fetchValue, fetchMetadata);
						if (marshalledEntry != null) {
							Debug.line(task, marshalledEntry, fetchValue, fetchMetadata);
							task.processEntry(marshalledEntry, taskContext);
						}
						return null;
					} catch (Exception e) {
						log.errorExecutingParallelStoreTask(e);
						throw e;
					}
				}
			});
		}
		eacs.waitUntilAllCompleted();
		if (eacs.isExceptionThrown()) {
			throw new PersistenceException("Execution exception!", eacs.getFirstException());
		}
	}

	@Override
	public void purge(Executor threadPool, final PurgeListener task) {

		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				Set keys = ctx.getCache().keySet();
				
				if (task == null) return ;
				for (Object key : keys) {
					task.entryPurged(key);
				}
			}
		});
	}

	@Override
	public int size() {
		try {
			return central.newSearcher().search("*:*").totalCount();
		} catch (IOException e) {
			return 0;
		} catch (ParseException e) {
			return 0;
		}
	}

	public CrakenStoreConfiguration getConfiguration() {
		return configuration;
	}

	public Central central() {
		return central;
	}

}