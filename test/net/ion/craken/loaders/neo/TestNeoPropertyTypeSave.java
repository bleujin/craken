package net.ion.craken.loaders.neo;


import java.io.ByteArrayInputStream;
import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestNeoPropertyTypeSave extends TestCase {

	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testWriteType() throws Exception {
		FileUtil.deleteDirectory(new File(NeoWorkspaceConfig.createDefault().neoLocation()));
		r.defineWorkspace("test", NeoWorkspaceConfig.createDefault()) ;
		r.start() ;
		ReadSession session = r.login("test") ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name","bleujin").refTo("self", "/bleujin").append("locs", "seoul", "sungnam").property("age", 20)
					.blob("bbb", new ByteArrayInputStream("Yahooooo".getBytes("UTF-8")));
				
				return null;
			}
		}) ;

		ReadNode found = session.pathBy("/bleujin") ;
		
		assertEquals(VType.STR, found.property("name").type());
		assertEquals(VType.STR, found.property("locs").type());
		assertEquals(VType.STR, found.propertyId(PropertyId.fromIdString("@self")).type());
		assertEquals(VType.BLOB, found.property("bbb").type());

		r.shutdown() ;
		
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test",NeoWorkspaceConfig.createDefault()) ;
		r.start() ;
		session = r.login("test") ;
		
		found = session.pathBy("/bleujin") ;
		
		assertEquals(VType.STR, found.property("name").type());
		assertEquals(VType.STR, found.property("locs").type());
		assertEquals(VType.STR, found.propertyId(PropertyId.fromIdString("@self")).type());
		assertEquals(VType.BLOB, found.property("bbb").type());
		
	}
}
