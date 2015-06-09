package net.ion.craken.node.problem.eviction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Action;
import net.ion.framework.util.Debug;
import net.ion.radon.util.csv.CsvReader;

public class TestEviction extends TestCase {

	
	private ReadSession session;

	public void setUp() throws Exception {
		Craken r = Craken.create() ;
		r.createWorkspace("evict", OldFileConfigBuilder.directory("./resource/temp/")) ;
		
		this.session = r.login("evict") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}
	
	// 12sec per 20k(create, about 700 over per sec 6.21, 44M)
	// 18sec per 20k(update, about 1k lower per sec 6.21 70M)
	public void testCreateWhenEmpty() throws Exception {
		// 20k : resetBy 14, createBy 12, mergeBy 41
		// 100k : resetBy 41, createBy 37, mergeBy 269
		// 500k : createBy 197
		int loopCount = 300000 ;
		long start = System.currentTimeMillis();
		session.tranSync(new SampleInsertJob("/bleujin/", loopCount, Action.CREATE)); 
		Debug.line(System.currentTimeMillis() - start);
		
//		session.pathBy("/bleujin").children().offset(10).debugPrint();
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
			if ((max % 9999) == 0) {
				System.out.print('.') ;
				wsession.continueUnit() ;
			} 
		}
		reader.close() ;
		Debug.line("endJob") ;
		return null;
	}
}

