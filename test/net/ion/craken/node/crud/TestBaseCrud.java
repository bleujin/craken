package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.loaders.neo.NeoWorkspaceConfig;
import net.ion.craken.node.ReadSession;

public class TestBaseCrud extends TestCase {

	protected RepositoryImpl r ;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.inmemoryCreateWithTest() ; // pre define "test" ;
		
//		r.defineWorkspace("test", ISearcherWorkspaceConfig.create()) ;
//		r.defineWorkspace("test2", NeoWorkspaceConfig.create()) ;
		
		
		r.start() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}

	
}
