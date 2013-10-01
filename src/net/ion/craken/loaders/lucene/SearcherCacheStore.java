package net.ion.craken.loaders.lucene;

import java.io.IOException;

import net.ion.craken.io.GridFilesystem;
import net.ion.nsearcher.config.Central;

import org.apache.lucene.index.CorruptIndexException;

public interface SearcherCacheStore {

	public Central central() ;
	public SearcherCacheStore lastSyncModified(long lastSyncModified) ;
	public long lastSyncModified() throws CorruptIndexException, IOException ;
	public SearcherCacheStore gfs(GridFilesystem gfs);
}
