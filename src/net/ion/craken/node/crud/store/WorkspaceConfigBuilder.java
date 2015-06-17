package net.ion.craken.node.crud.store;

import java.io.IOException;

import javax.transaction.Transaction;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.nsearcher.config.Central;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.DefaultCacheManager;

import com.google.common.cache.Cache;

public abstract class WorkspaceConfigBuilder {

	private CacheMode cacheMode = CacheMode.LOCAL ;
	
	private int maxEntry = 1000 ;
	private int eviMaxSegment = 20 ;

	public static WorkspaceConfigBuilder indexDir(String path) throws IOException {
		return new IndexFileConfigBuilder(path);
	}
	
	public static WorkspaceConfigBuilder sifsDir(String rootPath) throws IOException{
		return new SifsFileConfigBuilder(rootPath);
	}

	public static WorkspaceConfigBuilder gridDir(String rootPath) throws IOException {
		return new GridFileConfigBuilder(rootPath);
	}
	
	public static WorkspaceConfigBuilder oldDir(String rootPath) {
		return new OldFileConfigBuilder(rootPath);
	}

	public static WorkspaceConfigBuilder memoryDir(){
		return new SessionWorkspaceBuilder() ;
	}

	public WorkspaceConfigBuilder maxEntry(int maxEntry){
		this.maxEntry = maxEntry ;
		return this ;
	}
	
	public WorkspaceConfigBuilder maxSegment(int eviMaxSegment){
		this.eviMaxSegment = eviMaxSegment ;
		return this ;
	}
	
	public abstract WorkspaceConfigBuilder build(DefaultCacheManager dm, String wsName) throws IOException ;
	
	public WorkspaceConfigBuilder distMode(CacheMode cmode){
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

	
	public void createInterceptor(TreeCache<PropertyId, PropertyValue> cache, Central central, Cache<Transaction, IndexWriteConfig> trans){
	}

	public abstract Workspace createWorkspace(Craken craken, AdvancedCache<PropertyId, PropertyValue> cache) throws IOException  ;



	
}
