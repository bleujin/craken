package net.ion.craken.loaders;

import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.loaders.modifications.Remove;
import org.infinispan.loaders.modifications.Store;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.util.concurrent.ConcurrentMapFactory;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public abstract class AbCacheStore extends AbCacheLoader implements CacheStore {

	private static final Log log = LogFactory.getLog(AbCacheStore.class);

	private Map<GlobalTransaction, List<? extends Modification>> transactions;
	private AbstractCacheStoreConfig config;
	protected ExecutorService purgerService;
	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
	protected boolean multiThreadedPurge = false;

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (AbstractCacheStoreConfig) config;
	}

	protected final int getConcurrencyLevel() {
		return cache() == null || cache().getConfiguration() == null ? 16 : cache().getConfiguration().getConcurrencyLevel();
	}

	@Override
	public void start() throws CacheLoaderException {
		if (config == null)
			throw new IllegalStateException("Make sure you call super.init() from CacheStore extension");
		if (config.isPurgeSynchronously()) {
			purgerService = new WithinThreadExecutor();
		} else {
			multiThreadedPurge = supportsMultiThreadedPurge() && config.getPurgerThreads() > 1;
			final String loaderName = getClass().getSimpleName();
			purgerService = Executors.newFixedThreadPool(supportsMultiThreadedPurge() ? config.getPurgerThreads() : 1, new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					// Thread name: <cache>-<CacheStore>-<purger>-ID
					Thread t = new Thread(r, (cache() == null ? "" : cache().getName() + '-') + loaderName + '-' + THREAD_COUNTER.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});
		}
		transactions = ConcurrentMapFactory.makeConcurrentMap(64, getConcurrencyLevel());
	}

	protected boolean supportsMultiThreadedPurge() {
		return false;
	}

	@Override
	public void stop() throws CacheLoaderException {
		purgerService.shutdownNow();
	}

	@Override
	public void purgeExpired() throws CacheLoaderException {
		if (purgerService == null)
			throw new IllegalStateException("purgerService is null (did you call super.start() from cache loader implementation ?");
		purgerService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					purgeInternal();
				} catch (CacheLoaderException e) {
					log.problemPurgingExpired(e);
				}
			}
		});
	}

	protected abstract void purgeInternal() throws CacheLoaderException;

	protected void applyModifications(List<? extends Modification> mods) throws CacheLoaderException {
		for (Modification m : mods) {
			switch (m.getType()) {
			case STORE:
				Store s = (Store) m;
				store(s.getStoredEntry());
				Debug.line(s, s.getStoredEntry(), m) ;
				break;
			case CLEAR:
				clear();
				break;
			case REMOVE:
				Remove r = (Remove) m;
				remove(r.getKey());
				break;
			default:
				throw new IllegalArgumentException("Unknown modification type " + m.getType());
			}
		}
	}

	@Override
	public void prepare(List<? extends Modification> mods, GlobalTransaction tx, boolean isOnePhase) throws CacheLoaderException {
		if (isOnePhase) {
			applyModifications(mods);
		} else {
			transactions.put(tx, mods);
		}
	}

	@Override
	public void rollback(GlobalTransaction tx) {
		transactions.remove(tx);
	}

	@Override
	public CacheStoreConfig getCacheStoreConfig() {
		return config;
	}

	@Override
	public void commit(GlobalTransaction tx) throws CacheLoaderException {
		List<? extends Modification> list = transactions.remove(tx);
		if (list != null && !list.isEmpty())
			applyModifications(list);
	}

	@Override
	public void removeAll(Set<Object> keys) throws CacheLoaderException {
		if (keys != null && !keys.isEmpty()) {
			for (Object key : keys)
				remove(key);
		}
	}

	protected static void safeClose(InputStream stream) throws CacheLoaderException {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (Exception e) {
			throw new CacheLoaderException("Problems closing input stream", e);
		}
	}

	protected static void safeClose(OutputStream stream) throws CacheLoaderException {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (Exception e) {
			throw new CacheLoaderException("Problems closing output stream", e);
		}
	}

	protected StreamingMarshaller getMarshaller() {
		return marshaller;
	}
	

	public void fromStream(ObjectInput inputStream) throws CacheLoaderException {
		throw new UnsupportedOperationException();
	}

	public void toStream(ObjectOutput outputStream) throws CacheLoaderException {
		throw new UnsupportedOperationException();
	}

}