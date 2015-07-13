package net.ion.bleujin.craken.oom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.bleujin.craken.TestCraken;
import net.ion.craken.io.FileVisitor;
import net.ion.craken.io.Files;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.radon.util.csv.CsvReader;

import org.infinispan.io.GridFilesystem;

public class TestICSWorkspace extends TestBaseWorkspace {

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

	public void testIcs() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/ics"));
		
		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics")) ;
		
		ReadSession session = craken.login("ics") ;
		long start = System.currentTimeMillis() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().ignoreIndex() ;
				
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
		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics")) ;
		
		ReadSession session = craken.login("ics") ;
//		Debug.line(session.root().childQuery("", true).find().totalCount()) ;
		
		session.root().children().offset(10).debugPrint();
	}
	
	
	public void testSampleIndex() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/ics"));
		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics")) ;
		
		ReadSession session = craken.login("ics") ;
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int index = 0; index < 10; index++) {
					wsession.pathBy("/emp/" + index).property("index", index) ;
					if( (index +1) % 100 == 0 ) {
						System.out.print(".");
						wsession.continueUnit(); 
					}
				}
				return null;
			}
		}) ;
		
		session.pathBy("/emp/0").debugPrint();
	}
	
	public void testSampleView() throws Exception {
		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics")) ;
		
		ReadSession session = craken.login("ics") ;
		session.root().walkChildren().debugPrint(); 
		session.root().childQuery("", true).find().debugPrint(); 
	}
	
	
	public void testWikiDirect() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/ics"));

		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics")) ;
		ReadSession session = craken.login("ics");
		final GridFilesystem gfs = session.workspace().gfs() ;
		
		final AtomicInteger count = new AtomicInteger(0);
		
		Files.walkFileTree(new File("C:/crawl/enha/wiki"), new FileVisitor() {
			private long start = System.currentTimeMillis();

			public FileVisitResult visitFile(File file) throws IOException {
				try {
					if (file.isDirectory())
						return FileVisitResult.CONTINUE;
					int icount = count.incrementAndGet();
					
					if (icount > 100000) return FileVisitResult.TERMINATE ;
					
					if ((icount % 300) == 0) {
						System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
						this.start = System.currentTimeMillis();
					}

					String wpath = TestCraken.makePathString(file) ;
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
		FileUtil.deleteDirectory(new File("./resource/ics"));
		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics"));
		ReadSession session = craken.login("ics");
		session.tran(makeJob(200000));
	}
	
	public void testConfirmView() throws Exception {
		craken.createWorkspace("ics", WorkspaceConfigBuilder.icsDir("./resource/ics"));
		ReadSession session = craken.login("ics");
		long start = System.currentTimeMillis() ;
//		session.root().walkChildren().eachNode(new ReadChildrenEach<Void>() {
//
//			@Override
//			public Void handle(ReadChildrenIterator citer) {
//				while(citer.hasNext()){
//					Debug.line(citer.next().fqn());
//				}
//				return null;
//			}
//		}) ;
//		Debug.line(System.currentTimeMillis() - start);
		start = System.currentTimeMillis() ;
		Debug.line(session.root().childQuery("", true).find().totalCount(), System.currentTimeMillis() - start) ;
	}
	

}
