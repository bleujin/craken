package net.ion.craken.node.problem.speed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Action;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.radon.util.csv.CsvReader;

public class TestInsertSpeed extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FileUtil.deleteDirectory(new File("./resource/temp/insert")) ;
		
		this.r = Craken.create();
		r.createWorkspace("test", OldFileConfigBuilder.directory("./resource/store/insert"));
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testReset() throws Exception {
		int loopCount = 200000 ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin") ;
				return null;
			}
		}) ;
		long start = System.currentTimeMillis();
		session.tranSync(new SampleInsertJob("/bleujin/", loopCount, Action.RESET));
		Debug.line(System.currentTimeMillis() - start) ;
		session.pathBy("/bleujin").children().offset(10).debugPrint() ;
	}
	
	
	public void xtestRead() throws Exception {
		session.pathBy("/bleujin").children().debugPrint() ;
	}
	
	
	// 12sec per 20k(create, about 700 over per sec 6.21, 44M)
	// 18sec per 20k(update, about 1k lower per sec 6.21 70M)
	public void testCreateWhenEmpty() throws Exception {
		// 20k : resetBy 14, createBy 12, mergeBy 41
		// 100k : resetBy 41, createBy 37, mergeBy 269
		// 500k : createBy 197
		int loopCount = 100000 ;
		long start = System.currentTimeMillis();
		session.tranSync(new SampleInsertJob("/bleujin/", loopCount, Action.CREATE)); 
		Debug.line(System.currentTimeMillis() - start);
		
		session.pathBy("/bleujin").children().offset(10).debugPrint();
	}
	
	public void testCount() throws Exception {
		long start = System.currentTimeMillis() ;
		int count = session.pathBy("/bleujin").childrenNames().size() ;
		Debug.line(System.currentTimeMillis() - start, count);
	}
	
	public void testSearchCount() throws Exception {
		long start = System.currentTimeMillis() ;
		session.pathBy("/bleujin").childQuery("").find().debugPrint();
		int count = session.pathBy("/bleujin").childQuery("").offset(10).find().totalCount() ;
		Debug.line(System.currentTimeMillis() - start, count);
	}
	
	public void testChildCount() throws Exception {
		long start = System.currentTimeMillis() ;
		List<ReadNode> nodes = session.pathBy("/bleujin").children().offset(10000).toList();
		Debug.line(System.currentTimeMillis() - start) ;
		start = System.currentTimeMillis() ;
		for(ReadNode node : nodes){
			if (node.property("id").stringValue().equals("/m/0hxf_x2")) Debug.line(node.property("section_name")) ;
		}
		Debug.line(System.currentTimeMillis() - start) ;
	}
	
	
	// 18, 14, 14, 15, 14  -> 95M
	public void xtestLoop() throws Exception {
		for (int i = 0; i < 5; i++) {
			testCreateWhenEmpty() ;
		}
	}

	public void testFind() throws Exception {
		long start = System.currentTimeMillis();
		for (int i : ListUtil.rangeNum(20)) {
			Debug.line(session.pathBy("/" + RandomUtil.nextInt(200)).toMap());
		}
		Debug.line(System.currentTimeMillis() - start);

	}

}


class SampleInsertJob implements TransactionJob<Void> {

	private String prefix;
	private int max = 0 ;
	private Action action = Action.RESET;
	
	public SampleInsertJob(String prefix, int max){
		this(prefix, max, Action.RESET) ;
	}

	public SampleInsertJob(String prefix, int max, Action action){
		this.prefix = prefix ;
		this.max = max ;
		this.action = action ;
	}
	

	public SampleInsertJob(int max){
		this("/", max) ;
	}
	
	@Override
	public Void handle(WriteSession wsession) throws Exception {
//		wsession.fieldIndexConfig().ignoreBodyField() ;
		
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		
		
		while(line != null && line.length > 0 && max-- > 0 ){
//			if (headers.length != line.length ) continue ;
			WriteNode wnode = null ;
			if (action == Action.RESET) {
				wnode = wsession.resetBy(prefix + max) ;
			} else if (action == Action.CREATE){
				wnode = wsession.createBy(prefix + max);				
			} else {
				wnode = wsession.pathBy(prefix + max);
			}
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) wnode.property(headers[ii], line[ii]) ;
			}
			line = reader.readLine() ;
			if ((max % 5000) == 0) {
				System.out.print('.') ;
				wsession.continueUnit() ;
			} 
		}
		reader.close() ;
		Debug.line("endJob") ;
		return null;
	}
}

