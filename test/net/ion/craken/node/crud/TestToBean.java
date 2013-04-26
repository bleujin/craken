package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;

public class TestToBean extends TestBaseCrud {

	public void testToBean() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10) ;
				return null ;
			}
		}).get() ;
		
		ReadNode bleujin = session.pathBy("/bleujin");
		Debug.line(bleujin.toBean(Person.class).getName()) ;
	}
}


class Person {
	private String name ;
	private int age ;
	
	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}

}
