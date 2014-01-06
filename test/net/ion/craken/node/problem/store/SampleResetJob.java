package net.ion.craken.node.problem.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;
import net.ion.radon.impl.util.CsvReader;

public class SampleResetJob implements TransactionJob<Void> {

	private int max = 0 ;
	
	public SampleResetJob(int max){
		this.max = max ;
	}
	
	@Override
	public Void handle(WriteSession wsession) throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		while(line != null && line.length > 0 && max-- > 0 ){
//			if (headers.length != line.length ) continue ;
			WriteNode wnode = wsession.resetBy("/" + max);
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