package net.ion.craken.node.crud.property;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import net.ion.craken.io.GridBlob;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestBlobProperty extends TestBaseCrud {

	public void testBlob() throws Exception {
		final InputStream input = new ByteArrayInputStream("HelloWorld".getBytes()) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("myid") ;
				wsession.pathBy("/bleujin").blob("img", input) ;
				return null;
			}
		}) ;
		
		
		GridBlob blob = session.pathBy("/bleujin").property("img").asBlob();
		Debug.line(blob);
		
		// session.workspace().gfs().gridBlob("/bleujin/img", metadata)
		
		
	}
	
}
