package net.ion.craken.loaders.lucene;

import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.Repository;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.nsearcher.config.Central;

public interface SearcherCacheStore {

	public Central central() ;
//	public SearcherCacheStore gfs(DefaultCacheManager dm, GridFilesystem gfs);
}
