package net.ion.craken.node.crud;

import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.PropertyId;

public class TestFirst extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("search") ;
		session = r.login("search") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}
	
	public void testLoad() throws Exception {
		ReadNode node = session.pathBy("/") ;
		Set<PropertyId> keys = node.keys() ;
		
		assertEquals(0, keys.size()) ;
	}
	
	public void testExists() throws Exception {
		assertEquals(true, session.exists("/")) ;
		
		ReadNode node = session.pathBy("/") ;
		ReadNode root = session.root() ;
		
		assertEquals(true, node.equals(root)) ;
		
		assertEquals(true, session.exists("/")) ;
		assertEquals(false, session.exists("/test")) ;
	}
	
	public void testWrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").asString()) ;

	}
	
	public void testRead() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").asString()) ;
		
	}
	
	
	
}
