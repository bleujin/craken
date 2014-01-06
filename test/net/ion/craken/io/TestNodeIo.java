package net.ion.craken.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.exception.NodeIOException;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;

public class TestNodeIo extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FileUtil.deleteDirectory(new File(ISearcherWorkspaceConfig.create().location())) ;
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create()) ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testSaveAsProperty() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my").blob("config", new FileInputStream("./resource/config/server-simple.xml")).property("date", new Date());
				
				PropertyValue value = bleujin.property("config");
				Debug.line(value.stringValue()) ;
				assertEquals(true, value.isBlob()) ;
				
				GridBlob blob = value.asBlob() ;
				
				Metadata meta = blob.getMetadata() ;
				String readString = IOUtil.toStringWithClose(blob.toInputStream()) ;
				return null;
			}
		}) ;
	}
	
	
	public void testHello() throws Exception {
		 session.tranSync(new TransactionJob<Void>() {
			 public Void handle(WriteSession wsession) throws Exception {
				 wsession.pathBy("/blobtest").property("name", "korea").blob("text", 
						 new com.amazonaws.util.StringInputStream("Long Long String")) ;
				 return null;
			 }
		 });
	 
		 Debug.line(IOUtil.toStringWithClose(session.pathBy("/blobtest").property("text").asBlob().toInputStream())) ;
	}
	
	public void testWR() throws Exception {
		final GridBlob blob = writeAndRead("config", "./resource/config/server-simple.xml");
		String readString = IOUtil.toString(blob.toInputStream());
		assertEquals(true, readString.startsWith("<?xml version=")) ;
	}
	
	public void testBigFile() throws Exception {
		File file = IOUtil.createTempFile("tmp");
		Writer writer = new FileWriter(file);
		for (int i = 0 ; i < 10240; i++) {
			IOUtil.write("0123456789", writer) ;
		}
		writer.close() ;
		
		final GridBlob blob = writeAndRead("jar", file.getAbsolutePath());
		
		InputStream input = blob.toInputStream();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		IOUtil.copy(input, output) ;
		
		Debug.line(output.toByteArray().length) ;
		final PropertyValue property = session.pathBy("/bleujin/my").property("jar");
		Debug.line(property.stringValue()) ;		
	}


	public void testSmallFile() throws Exception {
		File[] files = FileUtil.findFiles(new File("./test"), new FileFilter() {
			int max = 10 ;
			@Override
			public boolean accept(File file) {
				return !file.isDirectory() && max-- > 0;
			}
		}, true);
		
		for (File file : files) {
			writeAndRead(file.getName(), file.getCanonicalPath());
		}
	}
	
	
	
	private GridBlob writeAndRead(final String propId, final String fileName) throws Exception{
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my").blob(propId, new FileInputStream(fileName));
				return null;
			}
		}) ;
		
		final PropertyValue property = session.pathBy("/bleujin/my").property(propId);
		return property.asBlob();
	}
	
	
	
	public void testRead() throws Exception {
		final PropertyValue property = session.pathBy("/bleujin/my").property("config");

		Debug.line(property.stringValue()) ;
		
		final GridBlob blob = property.asBlob();
		String readString = IOUtil.toString(blob.toInputStream());
		assertEquals(true, readString.startsWith("<?xml version=")) ;
	}
	
	
	public void testWhenNodeRemoved() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my").blob("config", new FileInputStream("./resource/config/server-simple.xml"));
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin/my").unset("config") ;
				return null;
			}
		}) ;
		
		final PropertyValue property = session.pathBy("/bleujin/my").property("config");

		try {
			final GridBlob blob = property.asBlob();
			fail() ;
		} catch(NodeIOException expect){
			
		}
	}
	
	public void testEmpty() throws Exception {
		Debug.line() ;
	}
	
	public void testOverwrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my").blob("config", new FileInputStream("./lib/aradon_0.9.jar"));
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my").blob("config", new FileInputStream("./resource/config/server-simple.xml"));
				return null;
			}
		}) ;
	}
	
	public void testReadMap() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin/my") ;
				assertEquals(0, bleujin.toMap().size()) ;
				
				bleujin.property("name", "bleujin") ;
				Debug.line(bleujin.toMap().keySet()) ;
				assertEquals(2, bleujin.toMap().size()) ;
				return null;
			}
		}) ;
	}
	
}
