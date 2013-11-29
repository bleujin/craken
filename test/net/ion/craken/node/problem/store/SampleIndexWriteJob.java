package net.ion.craken.node.problem.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.radon.impl.util.CsvReader;

public class SampleIndexWriteJob implements IndexJob<Void> {

	private int max;

	public SampleIndexWriteJob(int max) {
		this.max = max;
	}

	@Override
	public Void handle(IndexSession isession) throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv");

		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t');
		String[] headers = reader.readLine();
		String[] line = reader.readLine();
		while (line != null && line.length > 0 && max-- > 0) {
			// if (headers.length != line.length ) continue ;
			WriteDocument wdoc = isession.newDocument("/try2/" + max);
			for (int ii = 0, last = headers.length; ii < last; ii++) {
				if (line.length > ii)
					wdoc.keyword(headers[ii], line[ii]);
			}

			isession.updateDocument(wdoc);
			line = reader.readLine();

			if ((max % 1000) == 0)
				System.out.print('.');
		}
		return null;
	}
}