package net.ion.craken.loaders;

import net.ion.nsearcher.config.Central;

import org.infinispan.loaders.AbstractCacheStore;

public abstract class WorkspaceStore extends AbCacheStore{

	public abstract Central central() ;
//	public SearcherCacheStore gfs(DefaultCacheManager dm, GridFilesystem gfs);
}
