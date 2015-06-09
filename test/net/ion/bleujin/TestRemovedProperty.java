package net.ion.bleujin;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.aradon.bean.CrakenEntry;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2014. 2. 25. Time: 오후 4:05 To change this template use File | Settings | File Templates.
 */

public class TestRemovedProperty extends TestCase {

	private CrakenEntry rentry;
	private ReadSession rsession;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		rentry = CrakenEntry.test();
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
		public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
            return new TransactionJob<Void>() {
                @Override
                public Void handle(WriteSession wsession) throws Exception {
                    Debug.line(wsession.pathBy(prefix + "/ryun").property("message").stringValue());
                    return null;
                }
            };

		}

		@Override
		public TransactionJob<Void> deleted(Map<String, String> stringStringMap, CDDRemovedEvent event) {
			return null;
		}
	}
}
