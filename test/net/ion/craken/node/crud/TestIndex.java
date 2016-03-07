package net.ion.craken.node.crud;

import java.io.FileInputStream;

import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import junit.framework.TestCase;

public class TestIndex extends TestCase{

	
	public void testIndex() throws Exception {
		Central central = CentralConfig.newRam().build() ;
		
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument().number("artid", 123).text("", IOUtil.toStringWithClose(new FileInputStream("d:/sample.txt"))).insert() ;
				return null;
			}
		}) ;
		
		central.newSearcher().createRequest("artid:123").find().debugPrint(); 
	}
}
