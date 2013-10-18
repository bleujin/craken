package net.ion.craken.node.crud.property;

import java.util.Date;

import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestPropertyValue extends TestBaseCrud{

	
	public void testDate() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive(new Date());
		assertEquals(true, new Date().getTime() > 1380521825847L) ;
		Debug.line(pv.value(), pv.asJsonArray().get(0).getClass()) ;

	}
	
	public void testNotAllowDouble() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("double", 2000.0d) ;
				return null;
			}
		}) ;
		
		final PropertyValue property = session.pathBy("/bleujin").property("double");
		Debug.line(property.intValue(0)) ;
		
	}
	
	
}
