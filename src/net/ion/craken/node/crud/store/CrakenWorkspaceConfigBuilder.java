package net.ion.craken.node.crud.store;

import java.io.IOException;

import javax.transaction.Transaction;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.impl.CrakenWorkspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.nsearcher.config.Central;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.DefaultCacheManager;

import com.google.common.cache.Cache;

public abstract class CrakenWorkspaceConfigBuilder {

	private CacheMode cacheMode = CacheMode.LOCAL ;
	
	private int maxEntry = 1000 ;
	private int eviMaxSegment = 50 ;

	public static CrakenWorkspaceConfigBuilder singleDir(String path) {
		return new SingleFileConfigBuilder(path);
	}
	
	public static CrakenWorkspaceConfigBuilder sifsDir(String indexPath, String dataPath) {
		return new SifsFileConfigBuilder(indexPath, dataPath);
	}

	public static CrakenWorkspaceConfigBuilder gridDir(String rootPath) throws IOException {
		return new GridFileConfigBuilder(rootPath);
	}

	public CrakenWorkspaceConfigBuilder maxEntry(int maxEntry){
		this.maxEntry = maxEntry ;
		return this ;
	}
	
	public CrakenWorkspaceConfigBuilder eviMaxSegment(int eviMaxSegment){
		this.eviMaxSegment = eviMaxSegment ;
		return this ;
	}
	
	public abstract CrakenWorkspaceConfigBuilder init(DefaultCacheManager dm, String wsName) throws IOException ;
	
	public CrakenWorkspaceConfigBuilder distMode(CacheMode cmode){
		this.cacheMode = cmode ;
		return this ;
	}
	
	public CacheMode cacheMode(){
		return cacheMode ;
	}

	public int maxEntry() {
		return maxEntry;
	}

	public int maxSegment(){
		return eviMaxSegment ;
	}

	public final String blobChunk(String wsName) {
		return wsName +  "-bchunk";
	}

	public final String blobMeta(String wsName){
		return wsName + "-bmeta" ;
	}

	
	public void createInterceptor(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, Central central, Cache<Transaction, IndexWriteConfig> trans){
	}

	public Workspace createWorkspace(Craken craken, AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache) throws IOException {
		return  new CrakenWorkspace(craken, cache, this);
	}


	
}
