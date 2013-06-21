package net.ion.craken.tree;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestFqn extends TestBaseCrud {

	public void testElement() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/a/b/c/d/e/f").property("name", "bleujin");
				return null;
			}
		});

		assertEquals("/a/b", session.pathBy("/a", "b").fqn().toString());
		assertEquals("/a/b/c", session.pathBy("/a", "b", "c").fqn().toString());
		assertEquals("bleujin", session.pathBy("/a", "b", "c", "d", "e", "f").property("name").value());

		assertEquals("bleujin", session.pathBy("a", "b", "c", "d", "e", "f").property("name").value());
	}
	
	public void testToString() throws Exception {
		Fqn fqn = Fqn.fromString("/bleujin") ;
		
		Debug.line(fqn.toString()) ;
	}
}
