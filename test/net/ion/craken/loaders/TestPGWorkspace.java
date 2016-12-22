package net.ion.craken.loaders;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.store.PGWorkspaceConfigBuilder;

public class TestPGWorkspace extends TestCase {
	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest() ;
		r.createWorkspace("pg", new PGWorkspaceConfigBuilder("./resource/store/test")) ;
		this.session = r.login("pg") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testWrite() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").refTo("self", "/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
	}
	
	public void testRead() throws Exception {
		ReadNode found = session.pathBy("/bleujin");
		found.ref("self").debugPrint();
	}
	
	public void testChildren() throws Exception {
		session.root().children().debugPrint();
	}
	
	
	public void testSearch() throws Exception {
		session.root().childQuery("name:bleujin").find().debugPrint();
	}
}
