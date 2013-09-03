package net.ion.craken.node.problem.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.AbDocument;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.Searcher;
import net.ion.radon.impl.util.CsvReader;

public class TestLuceneLocal extends TestCase {

	private Central central;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.central = CentralConfig.newLocalFile().dirFile("./resource/lucene").build() ;
	}
	@Override
	protected void tearDown() throws Exception {
		central.close() ;
		super.tearDown();
	}
	
	
	// 62sec  600k(10000 per sec) - insertDocument
	// 81sec  600k(8000 per sce)  - updateDocument
	// 5 sec 20k (4000 per sec) - updateDocument 
	public void testIndexSpeed() throws Exception {
//		new File("./resource/search").delete();

		long start = System.currentTimeMillis();
		central.newIndexer().index(new SampleIndexWriteJob(20000));
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void testLoadCentral() throws Exception {
		Searcher searcher = central.newSearcher();
//		searcher.createRequest("name:bleujin").find().debugPrint();
		Debug.line(searcher.createRequest(SearchConstant.ISKey + ":/try2/*").find().totalCount()) ;
	}
	
	// 416 sec per 10k(25 per sec)
	public void testRepeatPerRequest() throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv");

		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t');
		final String[] headers = reader.readLine();
		String[] line = reader.readLine();
		
		int max = 20000 ;
		while (line != null && line.length > 0 && max-- > 0) {
			// if (headers.length != line.length ) continue ;
			final String[] aline = line ;
			final int amax = max ;
			central.newIndexer().index(new IndexJob<Void>(){
				@Override
				public Void handle(IndexSession isession) throws Exception {
					WriteDocument wdoc = isession.newDocument("/try2/" + amax);
					for (int ii = 0, last = headers.length; ii < last; ii++) {
						if (aline.length > ii)
							wdoc.keyword(headers[ii], aline[ii]);
					}
					isession.insertDocument(wdoc);
					return null;
				}
				
			}) ;
			
			line = reader.readLine();

			if ((max % 1000) == 0)
				System.out.print('.');
		}
	}
	
	
}

