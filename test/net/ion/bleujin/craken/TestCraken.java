package net.ion.bleujin.craken;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestCraken extends TestCase {


	private Craken craken;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.inmemoryCreateWithTest() ;
		craken.createWorkspace("test", WorkspaceConfigBuilder.sifsDir("./resource/store/sifs")) ;
//		craken.createWorkspace("", OldFileConfigBuilder.sifs(indexPath, dataPath, blobPath)) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.stop();
		super.tearDown();
	}
	

	public void testFirst() throws Exception {
		ReadSession session = craken.login("test") ;
		
		session.tran(TransactionJobs.dummy("/bleujin", 10)) ;
		session.pathBy("/bleujin").children().debugPrint(); 
	}
	
	public final static String makePathString(File path) throws IOException {
		String fname = FilenameUtils.separatorsToUnix(path.getCanonicalPath());
		return "/" + fname.substring(FilenameUtils.getPrefixLength(fname)) ;

//		Iterator<Path> iter = path.iterator() ;
//		List<String> result = ListUtil.newList() ;
//		while(iter.hasNext()){
//			result.add(String.valueOf(iter.next()));
//		}
//		return "/" + StringUtil.join(result, "/") ;
	}
}
