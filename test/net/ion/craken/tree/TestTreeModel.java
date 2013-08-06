package net.ion.craken.tree;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;

public class TestTreeModel extends TestBaseCrud {

	public void testFirst() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").addChild("address").property("city", "seoul") ;
				wsession.pathBy("/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		
		
		TreeCache tcache = session.workspace().getCache();
		Cache cache = tcache.cache();
		
		for (Object key : cache.keySet()){
			AtomicHashMap value = (AtomicHashMap) cache.get(key);
			Debug.line(key, value.entrySet()) ;
		}
		
	}
	
}
