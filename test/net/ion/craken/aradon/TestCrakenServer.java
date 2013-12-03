package net.ion.craken.aradon;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.Radon;
import net.ion.nradon.WebSocketConnection;
import net.ion.nradon.WebSocketHandler;
import net.ion.nradon.ajax.BroadEchoWebSocket;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.scriptexecutor.manager.ManagerBuilder;
import net.ion.scriptexecutor.manager.ScriptManager;

public class TestCrakenServer extends TestCase {

	public void testSlide() throws Exception {
		ScriptManager manager = ManagerBuilder.createBuilder().languages(ManagerBuilder.LANG.JAVASCRIPT).build();
		manager.createRhinoScript("envjs").defineScript(new FileReader("./resource/env.rhino.1.2.js")).setPreScript();
		manager.createRhinoScript("jquery").defineScript(new FileReader("./resource/jquery-1.10.2.min.js")).setPreScript();
		manager.start();

		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		r.start();

		Radon radon = RadonConfiguration.newBuilder(9000)
			.add("/websocket/{id}", new BroadEchoWebSocket())
			.add("/script/{id}", new ScriptWebSocket(manager, r))
			.add("/events/{id}", new EventSourceHandler() {
				public void onOpen(EventSourceConnection conn) throws Exception {
				}
	
				public void onClose(EventSourceConnection conn) throws Exception {
				} })
			.add("/*", new SimpleStaticFileHandler(new File("./resource/docs/"))).createRadon();

		radon.start().get();

		new InfinityThread().startNJoin();
	}

	
	private static class ScriptWebSocket implements WebSocketHandler {
		private List<WebSocketConnection> connections = new CopyOnWriteArrayList<WebSocketConnection>();
		private final ScriptManager manager;
		private final RepositoryImpl r;

		public ScriptWebSocket(ScriptManager manager, RepositoryImpl r) {
			this.manager = manager;
			this.r = r;
		}

		@Override
		public void onClose(WebSocketConnection wconn) throws Throwable {
			connections.remove(wconn);
		}

		@Override
		public void onMessage(WebSocketConnection wconn, String script) throws Throwable {
			ReadSession session = r.login("test");
			final MyOutput output = new MyOutput();
			session.credential().tracer(output);
			manager.createRhinoScript(wconn.getString("id")).bind("session", session).defineScript(script).execute();
			wconn.send(output.readOut());
		}

		@Override
		public void onMessage(WebSocketConnection wconn, byte[] abyte0) throws Throwable {
			onMessage(wconn, new String(abyte0));
		}

		@Override
		public void onOpen(WebSocketConnection wconn) throws Throwable {
			connections.add(wconn);
		}

		@Override
		public void onPing(WebSocketConnection wconn, byte[] abyte0) throws Throwable {
		}

		@Override
		public void onPong(WebSocketConnection wconn, byte[] abyte0) throws Throwable {
		}
	}


}


class MyOutput extends PrintStream {

	private StringBuilder builder = new StringBuilder() ;
	public MyOutput() throws IOException {
		super(File.createTempFile("out", "osuffix"));
	}

	public void write(byte b[]) {
		String s = new String(b);
		append(s.trim(), false);
	}

	public String readOut(){
		String result = builder.toString() ;
		builder = new StringBuilder() ;
		return result ;
	}
	
	public void write(byte b[], int off, int len) {
		String s = new String(b, off, len);
		append(s.trim(), false);
	}

	public void write(int b) {
		Integer i = new Integer(b);
		append(i.toString(), false);
	}

	public void println(String s) {
		append(s, true);
	}

	public void print(String s) {
		append(s, false);
	}

	public void print(Object obj) {
		if (obj != null)
			append(obj.toString(), false);
		else
			append("null", false);
	}

	public void println(Object obj) {
		if (obj != null)
			append(obj.toString(), true);
		else
			append("null", true);
	}

	private synchronized void append(String x, boolean newline) {
		builder.append(x) ;
		if(newline) builder.append("\r\n") ;
	}

}

