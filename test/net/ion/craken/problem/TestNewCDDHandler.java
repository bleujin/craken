package net.ion.craken.problem;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDModifyHandler;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.Debug;

public class TestNewCDDHandler extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest();
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}

	public void testFirst() throws Exception {
		session.workspace().cddm().add(new CDDModifyHandler("/emp/{user}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, final CDDModifiedEvent event) {
				final String user = resolveMap.get("user");
				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {

						Debug.line(user + " edited", event.property("name").asString());
						return null;
					}
				};
			}
		});

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "bleujin");
				wsession.pathBy("/emp/ryun").property("name", "ryunhee");
				wsession.pathBy("/emp/hero").property("name", "hero");
				return null;
			}
		});

		session.workspace().cddm().await();
	}

	
	
	
	
	
}