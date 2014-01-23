package net.ion.craken.tree;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.framework.util.SetUtil;
import net.ion.radon.util.uriparser.URIPattern;
import net.ion.radon.util.uriparser.URIResolveResult;
import net.ion.radon.util.uriparser.URIResolver;
import net.ion.radon.util.uriparser.URITemplate;

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
	
	public void testRelative() throws Exception {
		Fqn child = Fqn.fromRelativeFqn(Fqn.root(), Fqn.fromString("/a/b/c/d"));
		Debug.debug(child) ;
	}
	
	
	public void testPattern() throws Exception {
		Fqn f = Fqn.fromString("/rooms/1234/members/bleujin") ;
		assertEquals(true, f.isPattern("/rooms/{roomId}/members/{userId}"));
		assertEquals("1234", f.resolve("/rooms/{roomId}/members/{userId}").get("roomId"));
		assertEquals(true, f.resolve("/rooms/{roomId}/members/{userId}").get("roomid") == null); // case sensitive
	}
	
	
}
