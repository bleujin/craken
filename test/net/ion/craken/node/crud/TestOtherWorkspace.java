package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;

public class TestOtherWorkspace extends TestCase {

	private Craken r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest();
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testViewConfig() throws Exception {
		r.createWorkspace("test1", WorkspaceConfigBuilder.indexDir(""));
		r.createWorkspace("test2", WorkspaceConfigBuilder.indexDir(""));

		r.start();
		final ReadSession s1 = r.login("test1");
		ReadSession s2 = r.login("test2");
		s1.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin");
				wsession.pathBy("/hero").property("name", "hero");
				return null;
			}
		});

		s2.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/jin").property("name", s1.pathBy("/bleujin").property("name"));
				return null;
			}

		});
		
		assertEquals("bleujin", s2.pathBy("/jin").property("name").asString());

	}

}
