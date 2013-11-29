package net.ion.craken.version;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;

public class TestNewVersion extends TestCase {

	private ReadSession session ;
	private RepositoryImpl r;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest();
		this.session = r.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testFirst() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin").toRows("name, age").debugPrint() ;
	}
	
	
	public void testAddChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		session.pathBy("/bleujin").toRows("name, age").debugPrint() ;
	}
	
	
	public void testDepth() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/sroot/emps/bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		session.pathBy("/sroot").children().debugPrint() ;
		
	}
}
