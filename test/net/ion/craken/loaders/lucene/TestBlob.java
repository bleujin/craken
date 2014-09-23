package net.ion.craken.loaders.lucene;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestBlob extends TestCase {

	private RepositoryImpl r;
	private ReadSession rsession;
	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.rsession = r.login("test") ;
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testToMap() throws Exception {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/blob").property("name", "bleujin").blob("content", new ByteArrayInputStream("Hello".getBytes())) ;
				return null;
			}
		}) ;
		
		Map<PropertyId, PropertyValue> props = rsession.pathBy("/blob").toMap() ;
		
		
		assertEquals(true, props.containsKey(PropertyId.normal("content")));
		
		for (Entry<PropertyId, PropertyValue> entry : props.entrySet()) {
			Debug.line(entry.getKey(), entry.getValue().type(), entry.getValue().asString());
		}
		
	}
}
