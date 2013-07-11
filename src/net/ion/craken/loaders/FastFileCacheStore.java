package net.ion.craken.loaders;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.config.ConfigurationException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.marshall.StreamingMarshaller;

@CacheLoaderMetadata(configurationClass = FastFileCacheStoreConfig.class)
public class FastFileCacheStore extends AbstractCacheStore {
	private static final byte[] MAGIC = new byte[] { 'F', 'C', 'S', '1' };

	private static final int KEYLEN_POS = 4;

	private static final int KEY_POS = 4 + 4 + 4 + 8;

	private FastFileCacheStoreConfig config;

	private FileChannel file;

	private Map<Object, FileEntry> entries;

	private SortedSet<FileEntry> freeList;

	private long filePos = MAGIC.length;

	/** {@inheritDoc} */
	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return FastFileCacheStoreConfig.class;
	}

	/** {@inheritDoc} */
	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (FastFileCacheStoreConfig) config;
	}

	/** {@inheritDoc} */
	@Override
	public void start() throws CacheLoaderException {
		super.start();
		try {
			// open the data file
			String location = config.getLocation();
			if (location == null || location.trim().length() == 0)
				location = "Infinispan-FileCacheStore";
			File dir = new File(location);
			if (!dir.exists() && !dir.mkdirs())
				throw new ConfigurationException("Directory " + dir.getAbsolutePath() + " does not exist and cannot be created!");

			File f = new File(location, cache.getName() + ".dat");
			file = new RandomAccessFile(f, "rw").getChannel();

			// initialize data structures
			// only use LinkedHashMap (LRU) for entries when cache store is bounded
			final Map<Object, FileEntry> entryMap;
			if (config.getMaxEntries() > 0)
				entryMap = new LinkedHashMap<Object, FileEntry>(16, 0.75f, true);
			else
				entryMap = new HashMap<Object, FileEntry>();
			entries = Collections.synchronizedMap(entryMap);
			freeList = Collections.synchronizedSortedSet(new TreeSet<FileEntry>());

			// check file format and read persistent state if enabled for the cache
			byte[] header = new byte[MAGIC.length];
			if (file.read(ByteBuffer.wrap(header), 0) == MAGIC.length && Arrays.equals(MAGIC, header) && cache.getCacheConfiguration().loaders().preload())
				preload();
			else
				// otherwise (unknown file format or no preload) just reset the file
				clear();
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public void stop() throws CacheLoaderException {
		try {
			if (file != null) {
				// reset state
				file.close();
				file = null;
				entries = null;
				freeList = null;
				filePos = MAGIC.length;
			}
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
		super.stop();
	}

	/**
	 * Rebuilds the in-memory index from file.
	 */
	private void preload() throws Exception {
		ByteBuffer buf = ByteBuffer.allocate(KEY_POS);
		for (;;) {
			// read FileEntry fields from file (size, keyLen etc.)
			buf.clear().limit(KEY_POS);
			file.read(buf, filePos);
			// return if end of file is reached
			if (buf.remaining() > 0)
				return;
			buf.flip();

			// initialize FileEntry from buffer
			FileEntry fe = new FileEntry(filePos, buf.getInt());
			fe.keyLen = buf.getInt();
			fe.dataLen = buf.getInt();
			fe.expiryTime = buf.getLong();

			// update file pointer
			filePos += fe.size;

			// check if the entry is used or free
			if (fe.keyLen > 0) {
				// load the key from file
				if (buf.capacity() < fe.keyLen)
					buf = ByteBuffer.allocate(fe.keyLen);

				buf.clear().limit(fe.keyLen);
				file.read(buf, fe.offset + KEY_POS);

				// deserialize key and add to entries map
				Object key = getMarshaller().objectFromByteBuffer(buf.array(), 0, fe.keyLen);
				entries.put(key, fe);
			} else {
				// add to free list
				freeList.add(fe);
			}
		}
	}

	/**
	 * The base class implementation calls {@link #load(Object)} for this, we can do better because we keep all keys in memory.
	 */
	@Override
	public boolean containsKey(Object key) throws CacheLoaderException {
		return entries.containsKey(key);
	}

	/**
	 * Allocates the requested space in the file.
	 * 
	 * @param len
	 *            requested space
	 * @return allocated file position and length as FileEntry object
	 */
	private FileEntry allocate(int len) {
		synchronized (freeList) {
			// lookup a free entry of sufficient size
			if (pctFree.allowFreeSpace()) {
				SortedSet<FileEntry> candidates = freeList.tailSet(new FileEntry(0, len));
				for (Iterator<FileEntry> it = candidates.iterator(); it.hasNext();) {
					FileEntry free = it.next();
					// ignore entries that are still in use by concurrent readers
					if (free.isLocked())
						continue;
	
					// found one, remove from freeList
					it.remove();
					return free;
				}
			}
			
			// no appropriate free section available, append at end of file
			FileEntry fe = new FileEntry(filePos, len);
			filePos += len;
			pctFree.increase() ;
			return fe;
		}
	}

	private PctFree pctFree = new PctFree() ;
	

	private static final byte[] ZERO_INT = { 0, 0, 0, 0 };

	/**
	 * Frees the space of the specified file entry (for reuse by allocate).
	 * 
	 * @param fe
	 *            FileEntry to free
	 */
	private void free(FileEntry fe) throws IOException {
		if (fe != null) {
			// invalidate entry on disk (by setting keyLen field to 0)
			file.write(ByteBuffer.wrap(ZERO_INT), fe.offset + KEYLEN_POS);
			freeList.add(fe);
		}
	}


	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {
		try {
			// serialize cache value
			byte[] key = getMarshaller().objectToByteBuffer(entry.getKey());
			byte[] data = getMarshaller().objectToByteBuffer(entry.toInternalCacheValue());

//			Debug.line(entry) ;
			// allocate file entry and store in cache file
			int len = KEY_POS + key.length + data.length;
			FileEntry fe = allocate(len);
			try {
				fe.expiryTime = entry.getExpiryTime();
				fe.keyLen = key.length;
				fe.dataLen = data.length;

				ByteBuffer buf = ByteBuffer.allocate(len);
				buf.putInt(fe.size);
				buf.putInt(fe.keyLen);
				buf.putInt(fe.dataLen);
				buf.putLong(fe.expiryTime);
				buf.put(key);
				buf.put(data);
				buf.flip();
				file.write(buf, fe.offset);

				// add the new entry to in-memory index
				fe = entries.put(entry.getKey(), fe);

				// if we added an entry, check if we need to evict something
				if (fe == null)
					fe = evict();
			} finally {
				// in case we replaced or evicted an entry, add to freeList
				free(fe);
			}
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}

	/**
	 * Try to evict an entry if the capacity of the cache store is reached.
	 * 
	 * @return FileEntry to evict, or null (if unbounded or capacity is not yet reached)
	 */
	private FileEntry evict() {
		if (config.getMaxEntries() > 0) {
			synchronized (entries) {
				if (entries.size() > config.getMaxEntries()) {
					Iterator<FileEntry> it = entries.values().iterator();
					FileEntry fe = it.next();
					it.remove();
					return fe;
				}
			}
		}
		return null;
	}


	@Override
	public void clear() throws CacheLoaderException {
		try {
			synchronized (entries) {
				synchronized (freeList) {
					// wait until all readers are done reading file entries
					for (FileEntry fe : entries.values())
						fe.waitUnlocked();
					for (FileEntry fe : freeList)
						fe.waitUnlocked();

					// clear in-memory state
					entries.clear();
					freeList.clear();

					// reset file
					file.truncate(0);
					file.write(ByteBuffer.wrap(MAGIC), 0);
					filePos = MAGIC.length;
					
				}
			}
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}


	@Override
	public boolean remove(Object key) throws CacheLoaderException {
		try {
			FileEntry fe = entries.remove(key);
			free(fe);
			pctFree.decrease() ;
			return fe != null;
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}


	@Override
	public InternalCacheEntry load(Object key) throws CacheLoaderException {
		try {
			final FileEntry fe;
			final boolean expired;
			
			synchronized (entries) {
				// lookup FileEntry of the key
				fe = entries.get(key);
				if (fe == null)
					return null;

				// if expired, remove the entry (within entries monitor)
				expired = fe.isExpired(System.currentTimeMillis());
				if (expired)
					entries.remove(key);

				// lock entry for reading before releasing entries monitor
				fe.lock();
			}

			final byte[] data;
			try {
				// if expired, free the file entry (after releasing entries monitor)
				if (expired) {
					free(fe);
					return null;
				}

				// load serialized data from disk
				data = new byte[fe.dataLen];
				file.read(ByteBuffer.wrap(data), fe.offset + KEY_POS + fe.keyLen);
			} finally {
				// no need to keep the lock for deserialization
				fe.unlock();
			}

			// deserialize data and recreate InternalCacheEntry
			return ((InternalCacheValue) getMarshaller().objectFromByteBuffer(data)).toInternalCacheEntry(key);
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		}
	}


	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		return load(Integer.MAX_VALUE);
	}


	@Override
	public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException {
		Set<Object> keys = loadAllKeys(null);
		Set<InternalCacheEntry> result = new HashSet<InternalCacheEntry>();
		for (Object key : keys) {
			InternalCacheEntry ice = load(key);
			if (ice != null) {
				result.add(ice);
				if (result.size() > numEntries)
					return result;
			}
		}
		return result;
	}


	@Override
	public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException {
		Set<Object> result;
		synchronized (entries) {
			result = new HashSet<Object>(entries.keySet());
		}
		if (keysToExclude != null)
			result.removeAll(keysToExclude);
		return result;
	}


	@Override
	protected void purgeInternal() throws CacheLoaderException {
		long now = System.currentTimeMillis();
		synchronized (entries) {
			for (Iterator<FileEntry> it = entries.values().iterator(); it.hasNext();) {
				FileEntry fe = it.next();
				if (fe.isExpired(now)) {
					it.remove();
					try {
						free(fe);
					} catch (Exception e) {
						throw new CacheLoaderException(e);
					}
				}
			}
		}
	}


	public void fromStream(ObjectInput inputStream) throws CacheLoaderException {
		// seems that this is never called by Infinispan (except by decorators)
		throw new UnsupportedOperationException();
	}


	public void toStream(ObjectOutput outputStream) throws CacheLoaderException {
		// seems that this is never called by Infinispan (except by decorators)
		throw new UnsupportedOperationException();
	}

	/**
	 * Helper class to represent an entry in the cache file.
	 * <p/>
	 * The format of a FileEntry on disk is as follows:
	 * <ul>
	 * <li>4 bytes: {@link #size}</li>
	 * <li>4 bytes: {@link #keyLen}, 0 if the block is unused</li>
	 * <li>4 bytes: {@link #dataLen}</li>
	 * <li>8 bytes: {@link #expiryTime}</li>
	 * <li>{@link #keyLen} bytes: serialized key</li>
	 * <li>{@link #dataLen} bytes: serialized data</li>
	 * </ul>
	 */
	private static class FileEntry implements Comparable<Object> {
		 // File offset of this block.
		private final long offset;
		// Total size of this block.
		private final int size;
		// Size of serialized key.
		private int keyLen;
		// Size of serialized data.
		private int dataLen;

		private long expiryTime = -1; // Time stamp when the entry will expire (i.e. will be collected by purge).
		private transient int readers = 0; // Number of current readers.

		private FileEntry(long offset, int size) {
			this.offset = offset;
			this.size = size;
		}

		private synchronized boolean isLocked() {
			return readers > 0;
		}

		private synchronized void lock() {
			readers++;
		}

		private synchronized void unlock() {
			readers--;
			if (readers == 0)
				notifyAll();
		}

		private synchronized void waitUnlocked() {
			while (readers > 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore, we don't expect anyone to interrupt us
				}
			}
		}

		private boolean isExpired(long now) {
			return expiryTime > 0 && expiryTime < now;
		}

		/** {@inheritDoc} */
		public int compareTo(Object o) {
			if (!(o instanceof FileEntry))
				throw new ClassCastException();
			if (this == o)
				return 0;
			FileEntry fe = (FileEntry) o;
			int diff = size - fe.size;
			return (diff != 0) ? diff : offset > fe.offset ? 1 : -1;
		}
	}
	
	

	public static Configuration testCacheStore(String location, int maxEntry) {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().hash().numOwners(2).unsafe()
				.eviction().maxEntries(maxEntry)
				.invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location", location)
				// ./resource/temp
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
	}
	 
}


class PctFree {
	private int addedCount = 0 ;
	
	public boolean allowFreeSpace() {
		return addedCount < 0; // 50%
	}

	public void increase(){
		addedCount++ ;
	}
	
	
	public void decrease() {
		addedCount-- ;
	}
	
}