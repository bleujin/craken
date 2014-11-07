package net.ion.craken.node.crud;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.LRUMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;

public class TestWorkspaceConfig extends TestCase {

	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testReadWhenNotDefined() throws Exception {
		ReadSession session = r.login("notdefined") ;
		session.root().children().debugPrint(); 
	}
	

	public void testCreateWorkspace() throws Exception {
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/store/test")) ;
		ReadSession session = r.login("test");
		
		session.tran(TransactionJobs.HelloBleujin) ;
	}
	
//	public void testWorkspaceNames() throws Exception {
//		r.defineWorkspace("search") ;
//		r.defineWorkspace("temp") ;
//		
//		Debug.line(r.workspaceNames()) ;
//		
//	}
//	
	public void testWorkspaceConfig() throws Exception {
		r.defineWorkspace("search") ;
		
		ReadSession session = r.login("search") ;
		Workspace workspace = session.workspace() ;
		Debug.line(workspace.cache().getCacheConfiguration()) ; 
	}
}
