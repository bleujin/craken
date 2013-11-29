package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;

public class TestDefineWorkspace extends TestCase{


	public void testDuplDefine() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest(); // predefine
		try {
			r.defineWorkspaceForTest("test", CentralCacheStoreConfig.create()) ;  // dupl
			fail() ;
		} catch(IllegalStateException expect){
		}
		ReadSession session = r.login("test");
		r.shutdown() ;
		
		
	}
}
