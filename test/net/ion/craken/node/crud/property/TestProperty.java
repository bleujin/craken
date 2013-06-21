package net.ion.craken.node.crud.property;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.PropertyValue;

public class TestProperty extends TestBaseCrud {

	
	public void testIdIsHangul() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("이름", "bleu").property("성", "jin").property("풀네임", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("jin", session.pathBy("/bleujin").property("성").value()) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("풀네임").value()) ;
		
	}
	
	public void testCaseSensitive() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("FirstName", "bleu").property("LastName", "jin").property("FullName", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("jin", session.pathBy("/bleujin").property("LastName").value()) ;
		assertEquals(PropertyValue.NotFound, session.pathBy("/bleujin").property("lastname") ) ;
		
	}
}
