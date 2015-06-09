package net.ion.craken.node.problem.distribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.radon.core.ContextParam;
import net.ion.radon.core.let.PathHandler;

public class TestDistBlob extends TestCase {

	
	public void testServer1() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/s1"));
		
		final Craken r = Craken.create() ;
		r.createWorkspace("test", OldFileConfigBuilder.directory("./resource/temp/s1")) ;
		r.start() ;
		
		Future<Radon> future = RadonConfiguration.newBuilder(9000).rootContext("r", r).add(new PathHandler(TestLet.class)).start() ;
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		future.get() ;
		
		new InfinityThread().startNJoin(); 
	}
	
	
	public void testServer2() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/s2"));
		
		final Craken r = Craken.create() ;
		r.createWorkspace("test", OldFileConfigBuilder.directory("./resource/temp/s2")) ;
		r.start() ;

		Future<Radon> future = RadonConfiguration.newBuilder(9010).rootContext("r", r).add(new PathHandler(TestLet.class)).start() ;
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});
		future.get() ;

		new InfinityThread().startNJoin(); 
	}

}

@Path("/test")
class TestLet {
	
	
	@Path("/{action}")
	@GET   // 
	public String action(@ContextParam("r") Craken r, @PathParam("action") String action) throws Exception{

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






