package net.ion.craken.version;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.MapUtil;

public class TestNewVersion extends TestCase {

	private ReadSession session ;
	private Craken r;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest();
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
				wsession.root().child("bleujin").property("name", "bleujin");
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
	
	public void testChild() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin/address").property("city", "seoul").property("", "") ;
				return null;
			}
		}) ;
		
		ReadNode node = session.pathBy("/bleujin").child("address");
		assertEquals("/bleujin/address", node.fqn().toString()) ;
	}
	
	public void testMergeChildInWriteSession() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/a/b/c/d/e/f").property("name", "line") ;
				wsession.root().child("/1/2/3/4/5/6").property("name", "line") ;
				return null;
			}
		}).get() ;
		
		
		Map<Fqn, ReadNode> childrenMap = MapUtil.newMap() ;
		for (ReadNode node : session.pathBy("/").children().toList()) {
			childrenMap.put(node.fqn(), node) ;
		}
		
		assertEquals(2, childrenMap.size()) ;
		assertEquals(true, childrenMap.containsKey(Fqn.fromString("/a"))) ;
		assertEquals(true, childrenMap.containsKey(Fqn.fromString("/1"))) ;

		assertEquals(true, session.exists("/a/b") && session.exists("/a") && session.exists("/a/b/c")) ;
		
	}
}
