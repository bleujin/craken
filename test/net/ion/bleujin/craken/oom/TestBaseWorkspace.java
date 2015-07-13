package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.bleujin.craken.TestCraken;
import net.ion.craken.io.FileVisitor;
import net.ion.craken.io.Files;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

public class TestBaseWorkspace extends TestCase{

	public TransactionJob<Void> makeJob(){
		return makeJob(10000) ;
	}
	
	public TransactionJob<Void> makeJob(final int maxcount){
		return new TransactionJob<Void>() {

			private AtomicInteger count = new AtomicInteger(0);
			private long start = System.currentTimeMillis();

			@Override
			public Void handle(final WriteSession wsession) throws Exception {
				wsession.iwconfig().ignoreIndex() ;
				
				Files.walkFileTree(new File("C:/crawl/enha/wiki"), new FileVisitor() {
					private long start = System.currentTimeMillis();

					public FileVisitResult visitFile(File file) throws IOException {
						try {
							if (file.isDirectory())
								return FileVisitResult.CONTINUE;
							int icount = count.incrementAndGet();
							
							if (icount >= maxcount) return FileVisitResult.TERMINATE ;
							
							if ((icount % 200) == 0) {
								System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
								this.start = System.currentTimeMillis();
								wsession.continueUnit();
							}

							String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
							String wpath = makePath(file) ;
							wsession.pathBy(wpath).property("content", content);

							return FileVisitResult.CONTINUE;
						} catch (Throwable e) {
							System.err.println(file);
							throw new IOException(e);
						}
					}
					
					private String makePath(File path) throws IOException{
//						return "/" + new ObjectId().toString() ;
						return TestCraken.makePathString(path) ;
					}
					
				});
				
				
				return null;
			}
		} ;
	}

	public IndexJob<Void> makeIndexJob(final int maxcount) {
		return new IndexJob<Void>() {
			private AtomicInteger count = new AtomicInteger(0);
			private long start = System.currentTimeMillis();

			@Override
			public Void handle(final IndexSession isession) throws Exception {
				Files.walkFileTree(new File("C:/crawl/enha/wiki"), new FileVisitor() {
					private long start = System.currentTimeMillis();

					public FileVisitResult visitFile(File file) throws IOException {
						try {
							if (file.isDirectory())
								return FileVisitResult.CONTINUE;
							int icount = count.incrementAndGet();

							if (icount >= maxcount)
								return FileVisitResult.TERMINATE;

							if ((icount % 300) == 0) {
								System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
								this.start = System.currentTimeMillis();
								isession.continueUnit();
							}

							String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
							String wpath = makePathString(file);
							isession.newDocument(wpath).text("content", content).update();

							return FileVisitResult.CONTINUE;
						} catch (Throwable e) {
							System.err.println(file);
							throw new IOException(e);
						}
					}
				});
				return null;
			}

			public String makePathString(File path) throws IOException {
				return TestCraken.makePathString(path) ;
			}

		} ;
	}
}
