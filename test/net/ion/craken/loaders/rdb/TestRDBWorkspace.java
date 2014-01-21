package net.ion.craken.loaders.rdb;

import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.SetUtil;

import org.infinispan.container.entries.InternalCacheEntry;

public class TestRDBWorkspace extends TestCase {

	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest();
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testDefault() throws Exception {
		ReadSession session = r.login("test");
		session.tranSync(TransactionJobs.dummy("/bleujin", 10));
		
	}
	
	public void testStart() throws Exception {
		r.defineWorkspaceForTest("rdb", RDBWorkspaceConfig.createDefault());
		r.start();
		
	}
	
	public void testInsert() throws Exception {
		r.defineWorkspaceForTest("rdb", RDBWorkspaceConfig.createDefault());
		r.start();

		ReadSession session = r.login("rdb");
		session.tranSync(TransactionJobs.dummy("/bleujin", 10));
	}
	
	
	public void testLoad() throws Exception {
		r.defineWorkspaceForTest("rdb", RDBWorkspaceConfig.createDefault());
		r.start();
		
		ReadSession session = r.login("rdb");
		session.pathBy("/bleujin").children().debugPrint(); 
		
		session.pathBy("/bleujin/7").toRows("name, dummy").debugPrint(); 
	}
	
	public void testWorkspace() throws Exception {
		r.defineWorkspaceForTest("rdb", RDBWorkspaceConfig.createDefault());
		r.start();
		
		
		ReadSession session = r.login("rdb");
		RDBWorkspace workspace = (RDBWorkspace)session.workspace() ;
		RDBWorkspaceStore wstore = workspace.store() ;
		
		Set<InternalCacheEntry> set = wstore.loadAll() ;
		assertEquals(11, set.size());
		wstore.loadAllKeys(SetUtil.EMPTY) ;
	}
	
	
	
}
