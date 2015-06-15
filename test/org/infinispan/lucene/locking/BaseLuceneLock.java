/*jadclipse*/// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.

package org.infinispan.lucene.locking;

import java.io.Closeable;
import org.apache.lucene.store.Lock;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.lucene.FileCacheKey;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

class BaseLuceneLock extends Lock implements Closeable {

	BaseLuceneLock(Cache cache, String indexName, String lockName) {
		noCacheStoreCache = cache.getAdvancedCache().withFlags(new Flag[] { Flag.SKIP_CACHE_STORE, Flag.SKIP_CACHE_LOAD });
		this.lockName = lockName;
		this.indexName = indexName;
		keyOfLock = new FileCacheKey(indexName, lockName);
	}

	public boolean obtain() {
		Object previousValue = noCacheStoreCache.putIfAbsent(keyOfLock, keyOfLock);
		if (previousValue == null) {
			if (log.isTraceEnabled())
				log.tracef("Lock: %s acquired for index: %s", lockName, indexName);
			return true;
		}
		if (log.isTraceEnabled())
			log.tracef("Lock: %s not acquired for index: %s, was taken already.", lockName, indexName);
		return false;
	}

	public void clearLock() {
		Object previousValue = noCacheStoreCache.remove(keyOfLock);
		if (previousValue != null && log.isTraceEnabled())
			log.tracef("Lock removed for index: %s", indexName);
	}

	public boolean isLocked() {
		return noCacheStoreCache.containsKey(keyOfLock);
	}

	public void close() {
		clearLock();
	}

	private static final Log log = LogFactory.getLog(BaseLuceneLock.class);
	private final Cache noCacheStoreCache;
	private final String lockName;
	private final String indexName;
	private final FileCacheKey keyOfLock;

}

/*
 * DECOMPILATION REPORT
 * 
 * Decompiled from: D:\newgit\craken\lib\reflib\infinispan-embedded-query-7.2.2.Final.jar Total time: 153 ms Jad reported messages/errors: Exit status: 0 Caught exceptions:
 */