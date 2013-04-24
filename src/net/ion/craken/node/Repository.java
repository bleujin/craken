package net.ion.craken.node;

import java.io.IOException;
import java.util.Map;

import net.ion.craken.node.crud.ReadSessionImpl;
import net.ion.craken.node.search.RepositorySearchImpl;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeCacheFactory;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public interface Repository {

	public void shutdown()  ;

	public IExecutor executor() ;
	
	public ReadSession testLogin(String wsname) throws IOException  ;
	
	public ReadSession login(Credential credential, String wsname) throws IOException  ;
	
}
