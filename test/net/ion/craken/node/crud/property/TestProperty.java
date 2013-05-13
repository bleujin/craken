package net.ion.craken.node.crud.property;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestProperty extends TestBaseCrud {

	
	public void testIdIsHangul() throws Exception {
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("이름", "bleu").property("성", "jin").property("풀 네임", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("jin", session.pathBy("/bleujin").property("성").value()) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("풀 네임").value()) ;
		
	}
}
