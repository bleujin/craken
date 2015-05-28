package net.ion.bleujin.craken.oom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.io.GridFilesystem;

import net.ion.bleujin.craken.TestCraken;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.radon.util.csv.CsvReader;
import junit.framework.TestCase;

public class TestGridWorkspace extends TestBaseWorkspace {

	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.local();
	}

	@Override
	protected void tearDown() throws Exception {
		craken.shutdown();
		super.tearDown();
	}

	public void testGrid() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/grid"));
		
		craken.createWorkspace("grid", CrakenWorkspaceConfigBuilder.gridDir("./resource/grid")) ;
		
		ReadSession session = craken.login("grid") ;
		long start = System.currentTimeMillis() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {

				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				int max = 100000 ;
				String prefix = "/drug/";
				long rangeTime = System.currentTimeMillis() ;
				while(line != null && line.length > 0 && max-- > 0 ){
					WriteNode wnode = wsession.pathBy(prefix + max);
					for (int ii = 0, last = headers.length; ii < last ; ii++) {
						if (line.length > ii) wnode.property(headers[ii], line[ii]) ;
					}
					
					line = reader.readLine() ;
					if ((max % 1000) == 0) {
						wsession.continueUnit() ;
						Debug.line("rangetime", System.currentTimeMillis() - rangeTime) ;
						rangeTime = System.currentTimeMillis();
					} 
				}
				reader.close() ;
				Debug.line("endJob") ;
				return null;			
			}
		}) ;
		
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void testView() throws Exception {
		craken.createWorkspace("grid", CrakenWorkspaceConfigBuilder.gridDir("./resource/grid")) ;
		
		ReadSession session = craken.login("grid") ;
		Debug.line(session.root().childQuery("", true).find().totalCount()) ;
		
		session.root().walkChildren().offset(10).debugPrint();
	}
	
	
	public void testSampleIndex() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/grid"));
		craken.createWorkspace("grid", CrakenWorkspaceConfigBuilder.gridDir("./resource/grid")) ;
		
		ReadSession session = craken.login("grid") ;
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int index = 0; index < 500000; index++) {
					wsession.pathBy("/emp/" + index).property("index", index) ;
					if( (index +1) % 1000 == 0 ) {
						System.out.print(".");
						wsession.continueUnit(); 
					}
				}
				return null;
			}
		}) ;
	}
	
	public void testSampleView() throws Exception {
		craken.createWorkspace("grid", CrakenWorkspaceConfigBuilder.gridDir("./resource/grid")) ;
		
		ReadSession session = craken.login("grid") ;
		session.pathBy("/emp").children().debugPrint(); 
	}
	
	
	public void testWikiDirect() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/grid"));

		craken.createWorkspace("grid", CrakenWorkspaceConfigBuilder.gridDir("./resource/grid")) ;
		ReadSession session = craken.login("grid");
		final GridFilesystem gfs = session.workspace().gfs() ;
		
		final AtomicInteger count = new AtomicInteger(0);
		
		Files.walkFileTree(Paths.get(new File("C:/crawl/enha/wiki").toURI()), new SimpleFileVisitor<Path>() {
			private long start = System.currentTimeMillis();

			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				File file = path.toFile();
				try {
					if (file.isDirectory())
						return FileVisitResult.CONTINUE;
					int icount = count.incrementAndGet();
					
					if (icount > 100000) return FileVisitResult.TERMINATE ;
					
					if ((icount % 300) == 0) {
						System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
						this.start = System.currentTimeMillis();
					}

					String wpath = TestCraken.makePathString(path) ;
					File gfile = gfs.getFile(wpath) ;
					if (! gfile.getParentFile().exists()){
						gfile.getParentFile().mkdirs() ;
					}
					
					IOUtil.copyNClose(new FileInputStream(file), gfs.getOutput(wpath));

					return FileVisitResult.CONTINUE;
				} catch (Exception e) {
					System.err.println(file);
					throw new IOException(e);
				}
			}
		});
		craken.shutdown() ;
		
	}
	
	public void testConfirmOOM() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/grid"));
		craken.createWorkspace("grid", CrakenWorkspaceConfigBuilder.sifsDir("./resource/grid"));
		ReadSession session = craken.login("grid");
		session.tran(makeJob(200000));
	}
	

}
