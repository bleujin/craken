package net.ion.scriptexecutor;

import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceHandler;
import net.ion.nradon.EventSourceMessage;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.WebSocketConnection;
import net.ion.nradon.WebSocketHandler;
import net.ion.nradon.ajax.BroadEchoWebSocket;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.eventsource.Main.Pusher;
import net.ion.nradon.handler.SimpleStaticFileHandler;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.scriptexecutor.manager.ManagerBuilder;
import net.ion.scriptexecutor.manager.ScriptManager;

public class TestSlide extends TestCase {
	
	public void testSlide() throws Exception {
		ExecutorService webThread = Executors.newSingleThreadExecutor();
		final Pusher pusher = new Pusher();
		
		ScriptManager manager = ManagerBuilder.createBuilder().languages(ManagerBuilder.LANG.JAVASCRIPT).build();
		manager.createRhinoScript("envjs").defineScript(new FileReader("./resource/env.rhino.1.2.js")).setPreScript();
		manager.createRhinoScript("jquery").defineScript(new FileReader("./resource/jquery-1.10.2.min.js")).setPreScript();
		manager.start() ;
		
		RepositoryImpl r = RepositoryImpl.testSingle();
		r.start() ;
		
		
		Radon radon = RadonConfiguration.newBuilder(9000)
			.add("/websocket/{id}", new BroadEchoWebSocket())
			.add("/script/{id}", new ScriptWebSocket(manager, r)) 
			.add("/events/{id}", new EventSourceHandler() {
                public void onOpen(EventSourceConnection conn) throws Exception {
                    pusher.addConnection(conn);
                }

                public void onClose(EventSourceConnection conn) throws Exception {
                    pusher.removeConnection(conn);
                }
            })
			.add("/*", new SimpleStaticFileHandler(new File("./resource/docs/"))).createRadon() ;
		
		radon.start().get() ;
		pusher.pushPeriodicallyOn(webThread);
		
		new InfinityThread().startNJoin() ;
	}
	

	private static class ScriptWebSocket implements WebSocketHandler {
		private List<WebSocketConnection> connections = new CopyOnWriteArrayList<WebSocketConnection>() ;
		private final ScriptManager manager;
		private final RepositoryImpl r;
		
		public ScriptWebSocket(ScriptManager manager, RepositoryImpl r) {
			this.manager = manager ;
			this.r = r ;
		}

		@Override
		public void onClose(WebSocketConnection wconn) throws Throwable {
			connections.remove(wconn) ;
		}

		@Override
		public void onMessage(WebSocketConnection wconn, String script) throws Throwable {
			ReadSession session = r.login("test");
			final MyOutput output = new MyOutput();
			session.credential().tracer(output) ;
			manager.createRhinoScript(wconn.getString("id")).bind("session", session).defineScript(script).execute() ;
			wconn.send(output.readOut()) ;
		}

		@Override
		public void onMessage(WebSocketConnection wconn, byte[] abyte0) throws Throwable {
			onMessage(wconn, new String(abyte0)) ;
		}

		@Override
		public void onOpen(WebSocketConnection wconn) throws Throwable {
			connections.add(wconn) ;
		}

		@Override
		public void onPing(WebSocketConnection wconn, byte[] abyte0) throws Throwable {
		}

		@Override
		public void onPong(WebSocketConnection wconn, byte[] abyte0) throws Throwable {
		}
	}
	
	 private static class Pusher {
	        private List<EventSourceConnection> connections = new CopyOnWriteArrayList<EventSourceConnection>() ;
	        private int count = 1;

	        public void addConnection(EventSourceConnection conn) {
	            conn.data("id", count++);
	            connections.add(conn);
	            broadcast("Client " + conn.data("id") + " joined");
	        }

	        public void removeConnection(EventSourceConnection conn) {
	            connections.remove(conn);
	            broadcast("Client " + conn.data("id") + " left");
	        }

	        public void pushPeriodicallyOn(ExecutorService webThread) throws InterruptedException, ExecutionException {
	            while (true) {
		        	Thread.sleep(1000 * 2) ;
	                webThread.submit(new Runnable() {
	                    public void run() {
	                        broadcast(new Date().toString());
	                    }
	                }).get();
	            }
	        }

	        private void broadcast(String message) {
	            for (EventSourceConnection connection : connections) {
	                connection.send(new EventSourceMessage(message + ", clients:" + connections.size()));
	            }
	        }
	    }
}
