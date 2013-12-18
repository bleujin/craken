package net.ion.craken.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ObjectUtil;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.aradon.AradonHandler;
import net.ion.nradon.let.IServiceLet;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.annotation.ContextParam;
import net.ion.radon.core.annotation.FormParam;
import net.ion.radon.core.annotation.PathParam;
import net.ion.radon.util.AradonTester;
import net.ion.script.rhino.RhinoEngine;
import net.ion.script.rhino.RhinoResponse;

import org.apache.commons.fileupload.FileItem;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class TestDist extends TestCase{

	
	public void testC1Server() throws Exception {
		runServer("c1", 9000) ;
	}

	public void testC2Server() throws Exception {
		runServer("c2", 9001) ;
	}

	
	private void runServer(String targetDir, int port) throws Exception {
		FileUtil.deleteDirectory(new File("./resource/" + targetDir)) ;
		
		RhinoEngine rengine = RhinoEngine.create().start().get() ;
		
		RepositoryImpl repository = RepositoryImpl.create();
		repository.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/" + targetDir)) ;
		repository.start() ;
		
		Aradon aradon = AradonTester.create()
			.putAttribute("repository", repository)
			.putAttribute("scriptm", rengine)
			.register("upload", "/{action}", UploadLet.class)
			.register("script", "/{name}", ScriptLet.class)
			.getAradon() ;

		Radon radon = RadonConfiguration.newBuilder(port)
			.add("/*", AradonHandler.create(aradon)).start().get();
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testLocal() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/c3")) ;
		RepositoryImpl repository = RepositoryImpl.create();
		repository.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/c3")) ;
		repository.start() ;
		ReadSession session = repository.login("test"); 
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		
		new InfinityThread().startNJoin() ;
	}

}

class ScriptLet implements IServiceLet {

	@Post
	public String runScript(@ContextParam("repository") RepositoryImpl r,
				@ContextParam("scriptm") RhinoEngine rengine,
				@PathParam("name") String name, @FormParam("script") String script) throws IOException{
		ReadSession session = r.login("test");
		RhinoResponse response = rengine.newScript(name).bind("session", session).defineScript(script).exec();
		
		return ObjectUtil.toString(response.getReturn(Object.class)) ;
	}
} 


class UploadLet implements IServiceLet {
	
	@Get
	public String hello(@ContextParam("repository") RepositoryImpl r, @PathParam("action") String fileName) throws IOException{
		ReadSession session = r.login("test") ;
		InputStream input = session.pathBy("/bleujin").property(fileName).asBlob().toInputStream();

		String str = IOUtil.toStringWithClose(input) ;
		return "Hello World " + fileName + " " + str.length();
	}
	
	
	@Post
	public String upload(@ContextParam("repository") RepositoryImpl r, @PathParam("action") final String fileName, @FormParam("myfile") FileItem fitem) throws Exception {
		
		final InputStream input = fitem.getInputStream();
		ReadSession session = r.login("test") ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").blob(fileName, input);
				return null;
			}
		}) ;
		input.close() ;
		
		return fileName ;
	}

}

