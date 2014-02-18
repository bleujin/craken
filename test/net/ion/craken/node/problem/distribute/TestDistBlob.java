package net.ion.craken.node.problem.distribute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.restlet.resource.Get;

import net.ion.craken.io.GridBlob;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.StringUtil;
import net.ion.nradon.Radon;
import net.ion.nradon.let.IServiceLet;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.annotation.ContextParam;
import net.ion.radon.core.annotation.PathParam;
import net.ion.radon.util.AradonTester;
import junit.framework.TestCase;

public class TestDistBlob extends TestCase {

	
	public void testServer1() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/s1"));
		
		final RepositoryImpl r = RepositoryImpl.create("s1") ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/s1")) ;
		r.start() ;
		
		Aradon aradon = AradonTester.create()
					.register("test", "/{action}", TestLet.class).getAradon() ;
		aradon.getServiceContext().putAttribute("r", r) ;
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		Radon radon = aradon.toRadon(9000).start().get() ;
		
		new InfinityThread().startNJoin(); 
	}
	
	
	public void testServer2() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/s2"));
		
		final RepositoryImpl r = RepositoryImpl.create("s2") ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/s2")) ;
		r.start() ;
		
		Aradon aradon = AradonTester.create()
					.register("test", "/{action}", TestLet.class).getAradon() ;
		aradon.getServiceContext().putAttribute("r", r) ;
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		Radon radon = aradon.toRadon(9010).start().get() ;
		new InfinityThread().startNJoin(); 
	}

}

class TestLet implements IServiceLet {
	
	
	@Get   // 
	public String action(@ContextParam("r") RepositoryImpl r, @PathParam("action") String action) throws Exception{

		ReadSession session = r.login("test") ;

		if ("list".equals(action)) {
			session.root().children().debugPrint();
		} else if ("index".equals(action)) {
			session.tranSync(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").property("name", "bleujin") ;
					return null;
				}
			}) ;
		} else if ("blob".equals(action)) {
			session.tranSync(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").blob("blob", new FileInputStream(new File("./resource/docs/html.zip"))) ;
					return null;
				}
			}) ;
		} else if ("view".equals(action)) {
			InputStream input = session.pathBy("/bleujin").property("blob").asBlob().toInputStream() ;
			int length = IOUtil.toByteArray(input).length ;
			IOUtil.closeQuietly(input);
			
			Debug.line(length) ;
		} else if ("exit".equals(action)){
			r.shutdown() ;
		} 
		
		return "success" ;
	}
	
	
}






