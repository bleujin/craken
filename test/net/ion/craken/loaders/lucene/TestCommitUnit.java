package net.ion.craken.loaders.lucene;

import java.io.InputStream;
import java.io.StringWriter;

import junit.framework.TestCase;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;

public class TestCommitUnit extends TestCase {

	public void testReadJson() throws Exception {
		InputStream input = getClass().getResourceAsStream("/" + StringUtil.replace(getClass().getPackage().getName(), ".", "/") + "/test.dat");
		// String readString = IOUtil.toStringWithClose(input) ;

		CommitUnit cunit = CommitUnit.test(new TreeNodeKey(Fqn.fromString("/__transactions/12345"), Type.DATA), IndexWriteConfig.Default, 1, input);
		
		IndexJob<Integer> index = cunit.index();
		
		Central cen = CentralConfig.newRam().build();
		
		int count = cen.newIndexer().index(index) ;
		cen.destroySelf() ;
	}

	public void testIndexConfigWrite() throws Exception {
		StringWriter swriter = new StringWriter() ;
		JsonWriter jwriter = new JsonWriter(swriter);
		
		jwriter.beginObject() ;
		jwriter.jsonElement("config", IndexWriteConfig.Default.toJson()) ;
		jwriter.name("time").value(100) ;
		jwriter.endObject() ;
		
		
		jwriter.close() ;
		
		Debug.line(swriter.toString()) ;
	}

	public void testRun() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.createDefault().location("./resource/commit")) ;
		ReadSession session = r.login("test");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("12345") ;
				wsession.createBy("/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/bleujin").property("name", "hero").property("age", 20)  ; // .blob("blob", new StringInputStream("Long Long String"))  ;
				return null;
			}
		}) ;
		
		assertEquals("hero", session.pathBy("/bleujin").property("name").stringValue()) ;
		
//		Debug.line(IOUtil.toStringWithClose(tranInput)) ;
		
	}
	

}
