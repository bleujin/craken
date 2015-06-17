package net.ion.bleujin.craken;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;

import org.infinispan.configuration.cache.CacheMode;

public class TestMemoryWorkspace extends TestCase {

	private Craken craken;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

//		this.craken = Craken.inmemoryCreateWithTest() ;
		this.craken = Craken.local();
		
		craken.createWorkspace("test", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.LOCAL)) ;
		this.session = craken.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}
	
	
	public void testFirst() throws Exception {
		session.tran(TransactionJobs.HelloBleujin) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").asString()) ;
		session.root().walkChildren().debugPrint();
		
		assertEquals(0, session.root().childQuery("", true).find().size()) ;

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().reindex(true) ;
				return null;
			}
		}) ;
		
		session.root().childQuery("", true).find().debugPrint();
		assertEquals(2, session.root().childQuery("", true).find().size()) ; // included root
	}
	

}
