package net.ion.craken.node.ref;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestRefPathBy extends TestBaseCrud {

	public void testNotExistRefThrowExcpetion() throws Exception {
		session.pathBy("/"); // not throw
		try {
			session.pathBy("/").ref("friend");
			fail();
		} catch (IllegalArgumentException expect) {
		}
	}

	public void testNotExistRefsReturnEmpty() throws Exception {
		IteratorList<ReadNode> refs = session.pathBy("/").refs("friend");
		assertEquals(0, refs.toList().size());
	}

	public void testRefInReadSession() throws Exception {
		try {
			session.root().ref("notfound");
			fail();
		} catch (IllegalArgumentException expect) {
		}
	}

	public void testRefInWriteSession() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				try {
					wsession.root().ref("notfound");
					fail();
				} catch (IllegalArgumentException expect) {
				}
				;
				return null;
			}
		});
	}

	public void testNotFoundRefInReadSession() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().refTos("notfound", "/notfound/path");
				return null;
			}
		});
		
		ReadNode notfound = session.root().ref("notfound");
		assertEquals(0, notfound.keys().size()) ;
	}
	
	public void testNotFoundRefInWriteSession() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().refTos("notfound", "/notfound/path");
				WriteNode notfound = wsession.root().ref("notfound");
				
				assertEquals(0, notfound.keys().size()) ;
				return null;
			}
		});
		
	}
	

	
	
	
	
	public void testRefsInWriteSession() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				IteratorList<WriteNode> iter = wsession.root().refs("notfound");
				while(iter.hasNext()){
					WriteNode node = iter.next();
					assertEquals(0, node.keys().size()) ;
				}
				
				return null;
			}
		});
	}

	public void testRefsInReadSession() throws Exception {
		IteratorList<ReadNode> iter = session.root().refs("notfound");
		while(iter.hasNext()){
			ReadNode node = iter.next();
			assertEquals(0, node.keys().size()) ;
		}
				
	}
	

	
	
	
	
	

}
