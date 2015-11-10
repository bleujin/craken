package net.ion.bleujin.problem;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;

public class TestChildren extends TestCase {
	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.local() ;
		craken.createWorkspace("test", WorkspaceConfigBuilder.gridDir("./resource/temp/grid")) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.stop(); 
		super.tearDown();
	}
	
	public void testChildren() throws Exception {
		ReadSession session = craken.login("test") ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode wnode = wsession.pathBy("/a/b/c");
				wnode.toReadNode().debugPrint(); 
				
				wnode.property("greeting", "hello") ;
				return null;
			}
		}) ;
		
		
		session.pathBy("/").children().debugPrint(); 
	}
}
