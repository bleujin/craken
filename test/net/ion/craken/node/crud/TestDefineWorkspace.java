package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;

public class TestDefineWorkspace extends TestCase {

	public void testDuplDefine() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest(); // predefine
		try {
			r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create()); // dupl
			fail();
		} catch (IllegalStateException expect) {
		}
		ReadSession session = r.login("test");
		r.shutdown();

	}
}
