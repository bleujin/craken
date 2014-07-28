package net.ion.craken.node.problem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.radon.util.csv.CsvReader;

public class TestStoreSpeed extends TestCase {
	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.createDefault());
		this.session = r.login("test");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {

				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20);
				return null;
			}
		});
	}

	@Override
	protected void tearDown() throws Exception {
		// Thread.sleep(1000) ;
		r.shutdown();
		super.tearDown();
	}
	
	
	public void testSpeed() throws Exception {

		long start = System.currentTimeMillis();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {

				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv");
				int max = 100000;
				wsession.iwconfig().ignoreBodyField() ;
				try {
					CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
					reader.setFieldDelimiter('\t');
					String[] headers = reader.readLine();

					String[] line = reader.readLine();
					while (line != null && line.length > 0 && max-- > 0) {
						// if (headers.length != line.length ) continue ;
						WriteNode wnode = wsession.pathBy("/dummy/" + max);
						for (int ii = 0, last = headers.length; ii < last; ii++) {
							if (line.length > ii)
								wnode.property(headers[ii], line[ii]);
						}
						line = reader.readLine();
						if (max != 0 && (max % 10000) == 0) {
							System.out.print('.');
							wsession.continueUnit() ;
						}
					}
					reader.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} finally {

				}

				return null;
			}
		});
	} ;
}
