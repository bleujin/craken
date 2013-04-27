package net.ion.craken.node.crud.property;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestInnerChild extends TestBaseCrud {

	public void testInner() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").addChild("address").property("city", "busan").parent().property("name", "bleujin");
				return null;
			}
		}).get();

		assertEquals("busan", session.pathBy("/bleujin/address").property("city").value());
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value());

		Debug.line(session.root().childrenNames()) ;
	}
}
