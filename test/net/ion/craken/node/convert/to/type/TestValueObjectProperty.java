package net.ion.craken.node.convert.to.type;

import java.util.Date;
import java.util.Set;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestValueObjectProperty extends TestBaseCrud {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
//	public void xtestString() throws Exception { // not supported
//		session.tran(new TransactionJob<Void>() {
//			@Override
//			public Void handle(WriteSession wsession) {
//				wsession.pathBy("/value").property("st", "string")
//				.property("sb1", new StringBuilder("stringbuilder")).property("sb2", new StringBuffer("stringbuffer")) 
//				.property("date", new Date()).property("cal", Calendar.getInstance());
//				return null;
//			}
//		}).get() ;
//		ValueBean vb = session.pathBy("/value").toBean(ValueBean.class) ;
//		
//		assertEquals("string", vb.string()) ;
//		assertEquals("stringbuilder", vb.stringbuilder().toString()) ;
//		assertEquals("stringbuffer", vb.stringbuffer().toString()) ;
//		assertEquals(new Date().getDate(), vb.date().getDate()) ;
//		assertEquals(Calendar.getInstance().getTime().getDate(), vb.cal().getTime().getDate()) ;
//	}
	
	
	public void testPrimitive() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/value").property("st", "string").property("in", 10).property("lo", 10L).property("date", new Date());
				return null;
			}
		}).get() ;
		ValueBean vb = session.pathBy("/value").toBean(ValueBean.class) ;
		
		assertEquals("string", vb.string()) ;
		assertEquals(10, vb.in()) ;
		assertEquals(10L, vb.lo()) ;
		assertEquals(new Date().getDate(), vb.date().getDate()) ;
	}
	
	public void testSetValuesObjectWhenAppend() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/slist").append("values", "s1", "s2", "s3").append("dates", new Date(2000,1,2), new Date(2000,1,1)).property("single", "value") ;
				return null;
			}
		}).get() ;
		
		ValueSet vb = session.pathBy("/slist").toBean(ValueSet.class) ;
		assertEquals(3, vb.values().size()) ;
		assertEquals(2, vb.dates().size()) ;
		assertEquals(1, vb.single().size()) ;
	}
	
	
}

class ValueSet {
	private Set<String> values ;
	private Set<Date> dates ;
	private Set<String> single ;
	
	public Set<String> values(){
		return values ;
	}
	
	public Set<Date> dates(){
		return dates ;
	}
	public Set<String> single(){
		return single ;
	}
}


class ValueBean {
	
	private String st ;
	private int in ;
	private long lo ;
	private Date date ;
	
	public String string(){
		return st ;
	}
	public int in(){
		return in ;
	}
	public long lo(){
		return lo ;
	}
	public Date date(){
		return date ;
	}
}

