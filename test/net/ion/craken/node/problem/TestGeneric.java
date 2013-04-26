package net.ion.craken.node.problem;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

@Listener
public class TestGeneric extends TestBaseCrud {

	public void testToBean() throws Exception {
		session.getWorkspace().addListener(this) ;

		WriteNode node = session.tran(new TransactionJob<WriteNode>() {
			@Override
			public WriteNode handle(WriteSession wsession) {
				return wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10) ;
			}
		}).get() ;
		
		Debug.line(node.property("name", "hero")) ;
		
		Debug.line(session.pathBy("/bleujin").property("name")) ;
	}
	
	
	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap> e){
		Debug.line(e) ;
	}
}
