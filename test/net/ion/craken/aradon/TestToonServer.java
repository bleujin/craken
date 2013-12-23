package net.ion.craken.aradon;

import java.util.concurrent.CountDownLatch;

import org.restlet.data.Method;

import net.ion.craken.aradon.bean.RepositoryEntry;
import net.ion.craken.aradon.bean.RhinoEntry;
import net.ion.craken.aradon.let.DebugTalkHandler;
import net.ion.craken.aradon.let.ResourceFileHandler;
import net.ion.craken.aradon.let.ScriptLet;
import net.ion.craken.aradon.let.ScriptTalkHandler;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.aradon.AradonHandler;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Request;
import net.ion.radon.aclient.RequestBuilder;
import net.ion.radon.aclient.Response;
import net.ion.radon.aclient.websocket.WebSocket;
import net.ion.radon.aclient.websocket.WebSocketTextListener;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.EnumClass.IMatchMode;
import net.ion.radon.core.config.Configuration;
import net.ion.radon.core.config.ConfigurationBuilder;
import junit.framework.TestCase;

public class TestToonServer extends TestCase {

	
	public void testRun() throws Exception {
		RhinoEntry rengine = RhinoEntry.test() ;
		RepositoryEntry repository = RepositoryEntry.test();
		

		Configuration config = ConfigurationBuilder.newBuilder().aradon()
			.addAttribute(RepositoryEntry.EntryName, repository)
			.addAttribute(RhinoEntry.EntryName, rengine)
			.sections().restSection("script")
				.path("jscript").addUrlPattern("/{name}.{format}").matchMode(IMatchMode.STARTWITH).handler(ScriptLet.class)
			.build();
		
		Aradon aradon = Aradon.create(config);
		TalkEngine tengine = TalkEngine.create(aradon) ;
		tengine.registerHandler(new ScriptTalkHandler()) ;
		tengine.registerHandler(new DebugTalkHandler()) ;
		
		AradonHandler ahandler = AradonHandler.create(aradon);

		Radon radon = RadonConfiguration.newBuilder(9000)
			.add("/script/*", ahandler)
			.add("/websocket/{id}", tengine)
			.add("/resource/*", new ResourceFileHandler("./resource/"))
			.createRadon() ;
		
		radon.start().get() ;
		
		new InfinityThread().startNJoin() ;
		// radon.stop().get() ;
	}
	
	public void testHttpScript() throws Exception {
		NewClient client = NewClient.create();
		
		String script = "session.tranSync(function(wsession){" +
				"	wsession.pathBy('/bleujin').property('name', params.asString('name')).property('age', params.asInt('age'));" +
				"}) ;" +
				"" +
				"session.pathBy('/bleujin').toRows('name, age').toString();" ;
		Request request = new RequestBuilder()
			.setMethod(Method.POST)
			.setUrl("http://61.250.201.157:9000/script/bleujin.string")
				.addParameter("name", "bleujin").addParameter("age", "20")
				.addParameter("script", script).build();

		Response response = client.executeRequest(request).get();
		Debug.line(response.getTextBody()) ;
		
		client.close() ;
	}
	
	
	public void testWebSocket() throws Exception {
		NewClient client = NewClient.create();
		final CountDownLatch cd = new CountDownLatch(1) ;
		WebSocket ws = client.createWebSocket("ws://61.250.201.157:9000/websocket/bleujin", new WebSocketTextListener() {
			@Override
			public void onOpen(WebSocket arg0) {
			}
			
			@Override
			public void onError(Throwable arg0) {
			}
			
			@Override
			public void onClose(WebSocket arg0) {
			}
			
			@Override
			public void onMessage(String received) {
				Debug.line(received) ;
				cd.countDown() ;
			}
			@Override
			public void onFragment(String arg0, boolean arg1) {
			}
		});
		
		String msg = TalkMessage.fromStript("session.pathBy('/bleujin').toRows('name, age').toString();").toPlainMessage() ;
		Debug.line(msg) ;
		ws.sendTextMessage(msg) ;
		cd.await() ;
		
		client.close();
	}
	
	
}
