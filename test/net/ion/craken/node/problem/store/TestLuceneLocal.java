package net.ion.craken.node.problem.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;

import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.MyDocument;
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
	
	public void testIndexSpeed() throws Exception {
//		new File("./resource/search").delete();

		long start = System.currentTimeMillis();
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv");

				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t');
				String[] headers = reader.readLine();
				String[] line = reader.readLine();
				int max = 600000;
				while (line != null && line.length > 0 && max-- > 0) {
					// if (headers.length != line.length ) continue ;
					WriteDocument wdoc = MyDocument.newDocument("/try2/" + max);
					for (int ii = 0, last = headers.length; ii < last; ii++) {
						if (line.length > ii)
							wdoc.keyword(headers[ii], line[ii]);
					}

					isession.insertDocument(wdoc);
					line = reader.readLine();

					if ((max % 1000) == 0)
						System.out.print('.');
				}
				return null;
			}
		});
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void testLoadCentral() throws Exception {
		Searcher searcher = central.newSearcher();
//		searcher.createRequest("name:bleujin").find().debugPrint();
		Debug.line(searcher.createRequest(SearchConstant.ISKey + ":/try2/*").find().totalCount()) ;
	}
	
	
}
