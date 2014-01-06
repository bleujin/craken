package net.ion.craken.loaders.neo;

import java.io.File;
import java.io.IOException;

import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.loaders.lucene.LazyCentralConfig;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;

public class NeoWorkspaceConfig extends WorkspaceConfig {

	private static final long serialVersionUID = 5891372400491793884L;
	
	private String location = "";
	private String neoLocation = "./resource/neo" ; 
	private int lockTimeoutMs = 60 * 1000 ;

	private int maxNodeEntry = 15000 ;
	private LazyCentralConfig lazyConfig = new LazyCentralConfig() ;

	public NeoWorkspaceConfig() {
		setCacheLoaderClassName(NeoWorkspaceStore.class.getName());
	}
	
	public static NeoWorkspaceConfig create() {
		return new NeoWorkspaceConfig();
	}

	public static NeoWorkspaceConfig createWithEmpty() throws IOException {
		final NeoWorkspaceConfig result = new NeoWorkspaceConfig();
		FileUtil.deleteDirectory(new File(result.neoLocation)) ;
		return result;
	}
	
	public static NeoWorkspaceConfig createDefault() {
		return NeoWorkspaceConfig.create().neoLocation("./resource/neo").location("") ;
	}
	

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability("location");
		this.location = location ;
//		this.properties.put("location", location) ;
	}

	public void setDataLocation(String dlocation) {
		testImmutability("location");
		this.neoLocation = dlocation ;
//		this.properties.put("location", location) ;
	}

	
	public NeoWorkspaceConfig resetDir() throws IOException{
		FileUtil.deleteDirectory(new File(location)) ;
		return this ;
	}
	
	public String location() {
		return location;
	}

	public String neoLocation(){
		return neoLocation ;
	}
	
	public NeoWorkspaceConfig location(String path) {
		setLocation(path) ;
		return this;
	}

	public NeoWorkspaceConfig neoLocation(String dpath) {
		setDataLocation(dpath) ;
		return this;
	}



	public NeoWorkspaceConfig maxNodeEntry(int maxNodeEntry){
		this.maxNodeEntry = maxNodeEntry ;
		return this ;
	}
	
	public int maxNodeEntry(){
		return maxNodeEntry ;
	}


	public NeoWorkspaceConfig lockTimeoutMs(int lockTimeoutMs){
		this.lockTimeoutMs = lockTimeoutMs ;
		return this ;
	}
	
	public int lockTimeoutMs(){
		return lockTimeoutMs ;
	}
	

	
	
	public Central buildCentral() throws CorruptIndexException, IOException {
		Directory dir = null ;
		if (StringUtil.isBlank(location)) {
			dir = new RAMDirectory() ;
		} else {
			final File file = new File(location);
			if (! file.exists()) file.mkdirs() ;
			dir = FSDirectory.open(file) ;
		}
		final Central result = lazyConfig.dir(dir).indexConfigBuilder().indexAnalyzer(new MyKoreanAnalyzer()).parent().searchConfigBuilder().queryAnalyzer(new MyKoreanAnalyzer()).build();
		return result ;
	}
	
	public CentralConfig centralConfig(){
		return lazyConfig ;
	}

	@Override
	public Workspace createWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName) throws IOException {
		return new NeoWorkspace(repository, cache, wsName, this);
	}
}
