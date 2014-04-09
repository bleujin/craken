package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestOtherWorkspace extends TestCase {

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
	
	public void testViewConfig() throws Exception {
		r.defineWorkspace("test1", ISearcherWorkspaceConfig.create().location("")) ;
		r.defineWorkspace("test2", ISearcherWorkspaceConfig.create().location("")) ;
		
		r.start() ;
		try {
			
			ReadSession s1 = r.login("test1") ;
			ReadSession s2 = r.login("test2") ;
			s1.tranSync(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").property("name", "bleujin") ;
					wsession.pathBy("/hero").property("name", "hero") ;
					return null;
				}
			}) ;

			s1.tranSync(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					return null;
				}
				
			}) ;

		} finally {
			r.shutdown() ;
		}
		
		
		
	}

}
