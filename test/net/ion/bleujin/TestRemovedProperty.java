package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.craken.aradon.bean.RepositoryEntry;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import oracle.net.aso.r;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.context.TransactionalInvocationContextContainer;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2014. 2. 25. Time: 오후 4:05 To change this template use File | Settings | File Templates.
 */

public class TestRemovedProperty extends TestCase {

	private RepositoryEntry rentry;
	private ReadSession rsession;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		rentry = RepositoryEntry.test();
		rsession = rentry.login("test");
	}

	@Override
	public void tearDown() throws Exception {
		rentry.shutdown() ;
		super.tearDown();
	}

	
	private static final String prefix = "/test" ;
	
	public void testFirst() throws Exception {
		rsession.workspace().cddm().add(new TestListener());
		rsession.tranSync(new TransactionJob<Object>() {
			@Override
			public Object handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/connections/users/ryun").property("message", "hello");
				wsession.pathBy(prefix + "/ryun").property("message", "hello");
				return null;
			}
		});

		Thread.sleep(500);
		assertEquals("hello", rsession.pathBy(prefix + "/ryun").property("message").stringValue());

	}
	
	
	public class TestListener implements CDDHandler {
		@Override
		public String pathPattern() {
			return prefix + "/{node}";
		}

		@Override
		public TransactionJob<Void> modified(Map<String, String> resolveMap, CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
            return new TransactionJob<Void>() {
                @Override
                public Void handle(WriteSession wsession) throws Exception {
                    Debug.line(wsession.pathBy(prefix + "/ryun").property("message").stringValue());
                    return null;
                }
            };

		}

		@Override
		public TransactionJob<Void> deleted(Map<String, String> stringStringMap, CacheEntryRemovedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
			return null;
		}
	}
}
