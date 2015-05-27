package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import junit.framework.TestCase;

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
				Files.walkFileTree(Paths.get(new File("C:/crawl/enha/wiki").toURI()), new SimpleFileVisitor<Path>() {
					private long start = System.currentTimeMillis();

					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						File file = path.toFile();
						try {
							if (file.isDirectory())
								return FileVisitResult.CONTINUE;
							int icount = count.incrementAndGet();
							
							if (icount >= maxcount) return FileVisitResult.TERMINATE ;
							
							if ((icount % 300) == 0) {
								System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
								this.start = System.currentTimeMillis();
								wsession.continueUnit();
							}

							String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
							String wpath = makePathString(path) ;
							wsession.pathBy(wpath).property("content", content);

							return FileVisitResult.CONTINUE;
						} catch (Throwable e) {
							System.err.println(file);
							throw new IOException(e);
						}
					}
				});
				return null;
			}
			public String makePathString(Path path) {
				Iterator<Path> iter = path.iterator() ;
				List<String> result = ListUtil.newList() ;
				while(iter.hasNext()){
					result.add(String.valueOf(iter.next()));
				}
				return "/" + StringUtil.join(result, "/") ;
			}
		} ;
	}
}
