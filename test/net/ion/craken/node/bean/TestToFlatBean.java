package net.ion.craken.node.bean;

import java.io.Serializable;
import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestToFlatBean extends TestBaseCrud {

	public void testToFlatBean() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10).property("created", new Date()) ;
				return null ;
			}
		}).get() ;
		
		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals("bleujin", bleujin.toBean(FlatPerson.class).name()) ;
		assertEquals(new Date().getDate(), bleujin.toBean(FlatPerson.class).created().getDate()) ;
	}
	
	
	
}
