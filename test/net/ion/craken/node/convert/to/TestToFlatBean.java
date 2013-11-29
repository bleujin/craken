package net.ion.craken.node.convert.to;

import java.util.Date;
import java.util.Set;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.sample.FlatPerson;
import net.ion.craken.node.crud.TestBaseCrud;

import org.apache.commons.lang.builder.ToStringBuilder;

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
	
	
	public void testAppend() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").append("name", "bleujin", "name", "jin") ;
				return null;
			}
		}).get() ;
		Person person = session.pathBy("/bleujin").toBean(Person.class);
		assertEquals(true, person.contains("bleujin")) ;
		assertEquals(true, person.contains("name")) ;
		assertEquals(true, person.contains("jin")) ;
		
	}
	
	
}

class Person {
	private Set<String> name ;
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
	
	public boolean contains(String find){
		return name.contains(find) ;
	}
}
