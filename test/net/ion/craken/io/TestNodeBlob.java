package net.ion.craken.io;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;

import com.amazonaws.util.StringInputStream;

public class TestNodeBlob extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FileUtil.deleteDirectory(new File(ISearcherWorkspaceConfig.create().location())) ;
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().maxNodeEntry(5)) ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testRead() throws Exception {
		ReadNode readNode = session.pathBy("/bleujin");
		assertEquals("bleujin", readNode.property("name").stringValue()) ;
		Debug.line(IOUtil.toStringWithClose(readNode.property("blob").asBlob().toInputStream())) ;
	}
	
	public void testBlobOutputStream() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				final WriteNode wnode = wsession.pathBy("/bleujin").property("name", "bleujin").blob("blob", new StringInputStream("LongLongString"));
				
				return null;
			}
		}) ;
		
		ReadNode readNode = session.pathBy("/bleujin");
		assertEquals("bleujin", readNode.property("name").stringValue()) ;
		Debug.line(IOUtil.toStringWithClose(readNode.property("blob").asBlob().toInputStream())) ;
		
	}
	
//	public void testBlobOutputStream() throws Exception {
//		session.tranSync(new TransactionJob<Void>() {
//			@Override
//			public Void handle(WriteSession wsession) throws Exception {
//				final WriteNode wnode = wsession.pathBy("/bleujin").property("name", "bleujin");
//				WritableGridBlob gblob = wnode.blob("blob");
//				
//				final GridOutputStream output = gblob.outputStream();
//				for (int i = 0; i < 5; i++) {
//					IOUtil.copy(new StringInputStream("LongLongString"), output)  ;
//				}
//				IOUtil.closeQuietly(output) ;
//				wnode.property(PropertyId.normal("blob"), gblob.getMetadata().asPropertyValue()) ;
//				return null;
//			}
//		}) ;
//		
//		ReadNode readNode = session.pathBy("/bleujin");
//		assertEquals("bleujin", readNode.property("name").stringValue()) ;
//		Debug.line(IOUtil.toStringWithClose(readNode.property("blob").asBlob().toInputStream())) ;
//		
//	}

}
