package net.ion.craken.node.problem;


import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.tree.Fqn;

public class TestPath extends TestBaseCrud{
	
	public void testFqn() throws Exception {
		assertEquals(Fqn.fromString("users/bleujin"), Fqn.fromString("/users/bleujin")) ;
		assertEquals(Fqn.fromString("/users/bleujin"), Fqn.fromString("/users//bleujin")) ;
	}
	
	public void testOpps() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "bleujin") ;
				return null;
			}
		}).get() ;
		assertEquals("bleujin", session.pathBy("/users//bleujin").property("name").value()) ;
	}

}
