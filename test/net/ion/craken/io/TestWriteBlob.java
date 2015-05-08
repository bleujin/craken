package net.ion.craken.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ecs.xhtml.s;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.tree.GridBlob;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;

public class TestWriteBlob extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		r.createWorkspace("3rdparty", WorkspaceConfigBuilder.directory("./resource/store/3rdparty"));
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
		
		Files.walkFileTree(Paths.get("C:/temp/inner/3rdparty"), new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				File file = path.toFile();
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
				FileVisitor<? super Path> visitor = new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						String fqnPath = StringUtil.replace(path.toFile().getPath().substring(7), "\\", "/");
						File file = path.toFile();
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
				Files.walkFileTree(Paths.get("C:/temp/inner/3rdparty"), visitor);

				return null;
			}
		});
		Debug.line(sumBytes, count);
	}

}
