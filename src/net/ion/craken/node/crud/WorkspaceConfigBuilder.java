package net.ion.craken.node.crud;

import net.ion.craken.node.crud.store.SingleFileConfigBuilder;
import net.ion.framework.util.StringUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public abstract class WorkspaceConfigBuilder {

	private final String path;
	private CacheMode cacheMode = CacheMode.LOCAL ;
	
	private int maxEntry = 10000 ;
	private int eviMaxSegment = 30 ;
	protected WorkspaceConfigBuilder(String path) {
		this.path = path ;
	}

	public static WorkspaceConfigBuilder directory(String path) {
		return new SingleFileConfigBuilder(path);
	}

	public WorkspaceConfigBuilder maxEntry(int maxEntry){
		this.maxEntry = maxEntry ;
		return this ;
	}
	
	public WorkspaceConfigBuilder eviMaxSegment(int eviMaxSegment){
		this.eviMaxSegment = eviMaxSegment ;
		return this ;
	}
	
	protected abstract CacheMode init(DefaultCacheManager dm, String wsName)  ;
	
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

	public int eviMaxSegment(){
		return eviMaxSegment ;
	}

	public String path(){
		return path ;
	}
	
	
	public final static String BlobChunk(String wsName) {
		return wsName +  "-bchunk";
	}

	public final static String BlobMeta(String wsName){
		return wsName + "-bmeta" ;
	}
}
