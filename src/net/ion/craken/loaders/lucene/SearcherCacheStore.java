package net.ion.craken.loaders.lucene;

import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import net.ion.nsearcher.config.Central;

public interface SearcherCacheStore {

	public Central central() ;
	public SearcherCacheStore lastSyncModified(long lastSyncModified) ;
	public long lastSyncModified() throws CorruptIndexException, IOException ;
}
