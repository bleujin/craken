package net.ion.craken.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.script.ScriptException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
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
import net.ion.radon.core.ContextParam;
import net.ion.radon.core.let.PathHandler;
import net.ion.script.rhino.Scripter;
import net.ion.script.rhino.RhinoResponse;

import org.apache.commons.fileupload.FileItem;

public class TestDist extends TestCase{

	
	public void testC1Server() throws Exception {
		runServer("c1", 9000) ;
	}

	public void testC2Server() throws Exception {
		runServer("c2", 9001) ;
	}

	
	private void runServer(String targetDir, int port) throws Exception {
		FileUtil.deleteDirectory(new File("./resource/" + targetDir)) ;
		
		Scripter rengine = Scripter.create() ;
		
		RepositoryImpl repository = RepositoryImpl.create();
		repository.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/" + targetDir)) ;
		repository.start() ;
		
		Radon radon = RadonConfiguration.newBuilder(port).add(new PathHandler(UploadLet.class, ScriptLet.class))
					.rootContext("repository", repository).rootContext("scriptm", rengine)
					.start().get();
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testLocal() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/c3")) ;
		RepositoryImpl repository = RepositoryImpl.create();
		repository.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/c3")) ;
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

@Path("/script")
class ScriptLet {

	@Path("/{name")
	@POST
	public String runScript(@ContextParam("repository") RepositoryImpl r, @ContextParam("scriptm") Scripter rengine, @PathParam("name") String name, @FormParam("script") String script) throws IOException, ScriptException{
		
		ReadSession session = r.login("test");
		String pcontent = "new function(){"
			+ " this.call = function(){ " + script + " }"
			+ "} " ;
		
		Object result = rengine.define(name, pcontent).bind("session", session).callFn(name + ".call", RhinoResponse.ReturnNative) ;
		
		return ObjectUtil.toString(result) ;
	}
} 


@Path("/upload")
class UploadLet {
	
	@GET
	@Path("/{action}")
	public String hello(@ContextParam("repository") RepositoryImpl r, @PathParam("action") String fileName) throws IOException{
		ReadSession session = r.login("test") ;
		InputStream input = session.pathBy("/bleujin").property(fileName).asBlob().toInputStream();

		String str = IOUtil.toStringWithClose(input) ;
		return "Hello World " + fileName + " " + str.length();
	}
	
	
	@POST
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

