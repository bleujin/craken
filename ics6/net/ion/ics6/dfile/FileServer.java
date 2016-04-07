package net.ion.ics6.dfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.ecs.xhtml.center;
import org.infinispan.configuration.cache.CacheMode;

import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.impl.MemoryWorkspace;
import net.ion.craken.node.crud.store.MemoryWorkspaceBuilder;
import net.ion.craken.tree.ByteObject;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.config.RadonConfigurationBuilder;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.nradon.handler.logging.LoggingHandler;
import net.ion.nradon.handler.logging.SimpleLogSink;
import net.ion.nradon.netty.NettyWebServer;
import net.ion.radon.core.let.PathHandler;
import net.ion.radon.handler.FavIconHandler;

public class FileServer {

	
	public static void main(String[] args) throws Exception {
		new FileServer().runner(9000);
	}


	private NettyWebServer radon;
	private FileContext fcontext;

	public FileServer runner(int port) throws IOException, InterruptedException, ExecutionException {
		Craken icraken = Craken.create() ;
		
		icraken.createWorkspace("fcommon", new MemoryWorkspaceBuilder().distMode(CacheMode.DIST_SYNC)) ;
		icraken.start();
		
		final ReadSession rsession = icraken.login("fcommon") ;
		final String selfAddress = rsession.workspace().repository().addressId() ;
		rsession.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/upload/{action}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				String path = event.property("path").asString() ;
				String address = event.property("address").asString() ;
				ByteObject bo = (ByteObject)event.property("bytes").asObject() ;

				if (! selfAddress.equals(address)){
					Debug.line(path, bo.bytes().length, rsession.workspace().repository().addressId());
				}

				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		}) ;
		
		
		
		RadonConfigurationBuilder builder = RadonConfiguration.newBuilder(port)
				.add("/favicon.ico", new FavIconHandler())
				.add(new LoggingHandler(new SimpleLogSink()))
				.add(new SimpleStaticFileHandler("./resource/working/", Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("static-io-thread-%d"))).welcomeFile("index.html"))
				.add("/web/*", new PathHandler(FileUploadWeb.class).prefixURI("/web"))
				.add(new HttpHandler() {
					public void onEvent(EventType eventtype, Radon radon) {
					}
					public int order() {
						return 1000;
					}
					public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
						response.status(404).content("not found path : " + request.uri()).end();
					}
				});


		this.fcontext = FileContext.create(icraken);
		builder.rootContext("FileContext", fcontext);

		this.radon = builder.createRadon() ;
		radon.getConfig().getServiceContext().putAttribute(FileServer.class.getCanonicalName(), radon);
		
		radon.start().get() ;
		return this ;
	}
	
	
	public FileContext fcontext(){
		return this.fcontext;
	}
	
	public void shutdown() throws InterruptedException, ExecutionException {
		fcontext.stop(); 
		radon.stop().get() ;
	}
}
