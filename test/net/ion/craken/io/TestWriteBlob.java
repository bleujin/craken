package net.ion.craken.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.tree.impl.GridBlob;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;

public class TestWriteBlob extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.create();
		r.createWorkspace("3rdparty", OldFileConfigBuilder.directory("./resource/store/3rdparty"));
		this.session = r.login("3rdparty");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testFirst() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/files").property("name", "bleujin").blob("file", getClass().getResourceAsStream("TestWriteBlob.class"));
				return null;
			}
		});

		session.root().walkChildren().debugPrint();
	}

	public void testDebugWalk() throws Exception {
		session.pathBy("/inner/3rdparty").children().debugPrint(); 
	}
	
	public void testFind() throws Exception {
		ReadNode find = session.pathBy("/inner/3rdparty/hello.txt");
		
		File file = find.property("file").asBlob().toFile() ;
		
		Debug.line(file.length());
		
		find.debugPrint();
	}
	
	public void testCheckChildren() throws Exception {
		final AtomicInteger ai = new AtomicInteger() ;
		Long sumByte = session.pathBy("/inner/3rdparty").walkChildren().offset(20000).eachNode(new ReadChildrenEach<Long>() { // alert offset
			@Override
			public Long handle(ReadChildrenIterator citer) {
				long sumByte = 0L ;
				while(citer.hasNext()){
					PropertyValue prop = citer.next().property("file");
					if (prop.isBlob()) {
						ai.incrementAndGet() ;
						GridBlob blob = prop.asBlob();
						sumByte += blob.toFile().length() ;
					}
				}
				return sumByte;
			}
		}) ;
		Debug.line(sumByte, ai.intValue());
	}
	
	public void testDirWalk() throws Exception {
		final AtomicLong sumBytes = new AtomicLong() ;
		final AtomicInteger count = new AtomicInteger() ;
		
		Files.walkFileTree(new File("C:/temp/inner/3rdparty"), new FileVisitor() {
			public FileVisitResult visitFile(File file) throws IOException {
				sumBytes.addAndGet(file.length()) ;
				count.incrementAndGet() ;
				return FileVisitResult.CONTINUE ;
			}
		});
		
		Debug.line(sumBytes, count);
	}

	public void testWalk() throws Exception { // 227947468, 12027
		final AtomicLong sumBytes = new AtomicLong() ;
		final AtomicInteger count = new AtomicInteger() ;

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(final WriteSession wsession) throws Exception {
				final AtomicInteger acount = new AtomicInteger();
				FileVisitor visitor = new FileVisitor() {
					public FileVisitResult visitFile(File file) throws IOException {
						String fqnPath = StringUtil.replace(file.getPath().substring(7), "\\", "/");
						sumBytes.addAndGet(file.length()) ;
						FileInputStream fis = new FileInputStream(file);
						try {
							wsession.pathBy(fqnPath).property("filename", file.getName()).blob("file", fis);
							count.incrementAndGet(); 
						} catch (Exception e) {
							IOUtil.close(fis);
						}
						int count = acount.incrementAndGet();
						if ((count % 100) == 0) {
							Debug.line(count);
							wsession.continueUnit();
						}

						return FileVisitResult.CONTINUE;
					}
				};
				Files.walkFileTree(new File("C:/temp/inner/3rdparty"), visitor);

				return null;
			}
		});
		Debug.line(sumBytes, count);
	}

}
