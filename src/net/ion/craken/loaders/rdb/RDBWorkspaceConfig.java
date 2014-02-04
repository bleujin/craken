package net.ion.craken.loaders.rdb;

import java.io.File;
import java.io.IOException;

import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.loaders.lucene.LazyCentralConfig;
import net.ion.craken.loaders.neo.NeoWorkspace;
import net.ion.craken.loaders.neo.NeoWorkspaceConfig;
import net.ion.craken.loaders.neo.NeoWorkspaceStore;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.db.DBController;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.manager.OracleDBManager;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ObjectUtil;
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

public class RDBWorkspaceConfig extends WorkspaceConfig {

	private static final long serialVersionUID = 5891372400491793884L;
	
	private String location = "";
	private int lockTimeoutMs = 60 * 1000 ;

	private int maxNodeEntry = 15000 ;
	private LazyCentralConfig lazyConfig = new LazyCentralConfig() ;

	private static final String JDBCURL = "jdbcURL";
	private static final String JDBCUserId = "userId" ;
	private static final String JDBCUserPwd = "userPwd";
	

	private String jdbcUrl ;
	private String userId ;
	private String userPwd ;
	
	public RDBWorkspaceConfig() {
		setCacheLoaderClassName(RDBWorkspaceStore.class.getName());
	}
	
	public static RDBWorkspaceConfig create(String jdbcURL, String userId, String userPwd) {
		RDBWorkspaceConfig result = new RDBWorkspaceConfig();
		result.otherProp(JDBCURL, jdbcURL) ;
		result.otherProp(JDBCUserId, userId) ;
		result.otherProp(JDBCUserPwd, userPwd) ;
		return result ;
	}
	
	public static RDBWorkspaceConfig createDefault() {
		return RDBWorkspaceConfig.create("jdbc:oracle:thin:@61.250.201.239:1521:qm10g", "bleujin", "redf").location("./resource/temp/rdb") ;
	}
	
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability(location);
		this.location = location ;
	}

	public RDBWorkspaceConfig resetDir() throws IOException{
		FileUtil.deleteDirectory(new File(location)) ;
		return this ;
	}
	
	public String location() {
		return location;
	}

	public RDBWorkspaceConfig location(String path) {
		setLocation(path) ;
		return this;
	}

	public RDBWorkspaceConfig maxNodeEntry(int maxNodeEntry){
		this.maxNodeEntry = maxNodeEntry ;
		return this ;
	}
	
	public int maxNodeEntry(){
		return maxNodeEntry ;
	}

	
	public RDBWorkspaceConfig lockTimeoutMs(int lockTimeoutMs){
		this.lockTimeoutMs = lockTimeoutMs ;
		return this ;
	}
	
	public int lockTimeoutMs(){
		return lockTimeoutMs ;
	}
	
	public void setUserPwd(String userPwd){
		this.userPwd = userPwd ;
	}
	
	public void setUserId(String userId){
		this.userId = userId ;
	}
	
	public void setJdbcURL(String jdbcUrl){
		this.jdbcUrl = jdbcUrl ;
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
	
	public DBController buildDBController() {
		return new DBController(new OracleDBManager(defaultValue(JDBCURL, jdbcUrl), defaultValue(JDBCUserId, userId), defaultValue(JDBCUserPwd, userPwd)));
	}
	
	public CentralConfig centralConfig(){
		return lazyConfig ;
	}

	@Override
	public Workspace createWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName) throws IOException {
		return new RDBWorkspace(repository, cache, wsName, this);
	}
}
