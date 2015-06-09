package net.ion.bleujin.craken;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.configuration.cache.CacheMode;

public class TestProxyHandler extends TestCase {

	private ReadSession session;
	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.local() ;
		craken.createWorkspace("test", WorkspaceConfigBuilder.indexDir("./resource/store/test").distMode(CacheMode.LOCAL)) ;
		this.session = craken.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.stop(); 
		super.tearDown();
	}
	
	public void testCreate() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f").property("name", "value") ;
				wsession.pathBy("/a1/b2/c2/d2/e2/f2").property("name", "value") ;
				return null;
			}
		}) ;
	}
	
	public void testRead() throws Exception {
//		session.root().children().debugPrint();
		assertEquals("value", session.pathBy("/a/b/c/d/e/f").property("name").asString()) ;
	}
	
	public void testRemoveChildren() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
//				wsession.iwconfig().ignoreIndex() ;
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/bleujin").append("age", 20, 30) ;
				wsession.pathBy("/hero/jin/dummy1").property("name", "dummy1");
				wsession.pathBy("/hero/jin/dummy2").property("name", "dummy2");
				wsession.pathBy("/hero/jin").removeChildren() ;
//				wsession.pathBy("/hero").removeSelf() ;
				return null;
			}
		}) ;
		
		session.root().childQuery("age:30", true).find().debugPrint(); 
	}
	
	public void testCache() throws Exception {
		Cache<?, ?> cache = session.workspace().cache() ;
		CloseableIteratorSet<?> keys = cache.keySet() ;
		for (Object key : keys) {
			Debug.line(key, key.getClass(), cache.get(key), cache.get(key).getClass());
		}
	}
	
	
	public void testIsChid() throws Exception {
		Fqn abcd = Fqn.fromString("/a/b/c/d") ;
		Fqn abc = Fqn.fromString("/a/b/c") ;
		Fqn ab = Fqn.fromString("/a/b") ;
		assertEquals(true, abcd.isChildOrEquals(abcd));
	}
}
