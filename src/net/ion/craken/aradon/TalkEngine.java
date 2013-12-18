package net.ion.craken.aradon;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.testng.Assert;

import net.ion.bleujin.IOManager;
import net.ion.craken.aradon.bean.RepositoryEntry;
import net.ion.craken.aradon.bean.RhinoEntry;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nradon.WebSocketConnection;
import net.ion.nradon.WebSocketHandler;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.IService;
import net.ion.radon.core.TreeContext;
import net.ion.radon.core.context.OnEventObject;
import net.ion.radon.util.AradonTester;
import net.ion.script.rhino.RhinoEngine;

public class TalkEngine implements WebSocketHandler, OnEventObject {

	public enum DisConnectReason {
		DOPPLE, CLIENT, TIMEOUT;
	}

	private ConnManager cmanger = ConnManager.create();
	private List<TalkHandler> handlers = ListUtil.newList();

	private Aradon aradon;

	private TalkEngine(Aradon aradon) {
		Assert.assertNotNull(aradon);

		this.aradon = aradon;
		aradon.getServiceContext().putAttribute(TalkEngine.class.getCanonicalName(), this);
	}

	public static TalkEngine create(Aradon aradon) {
		return new TalkEngine(aradon);
	}
	
	public static TalkEngine test() throws Exception{
		Aradon aradon = Aradon.create() ;  
		aradon.getServiceContext().putAttribute(RepositoryEntry.EntryName, RepositoryEntry.test()) ;
		aradon.getServiceContext().putAttribute(RhinoEntry.EntryName, RhinoEntry.test()) ;
		final TalkEngine result = TalkEngine.create(aradon);
		
		aradon.start() ;
		return result;
	}
	

	public TalkEngine registerHandler(TalkHandler hanlder) {
		handlers.add(hanlder);
		return this;
	}

	public TalkEngine unregisterHander(TalkHandler handler) {
		handlers.remove(handler);
		return this;
	}

	public TreeContext context() {
		return aradon.getServiceContext();
	}

	@Override
	public void onOpen(WebSocketConnection conn) {
		UserConnection created = UserConnection.create(conn);
		cmanger.add(created);

		for (TalkHandler handler : handlers) {
			handler.onConnected(this, created);
		}
	}

	@Override
	public void onClose(WebSocketConnection conn) {
		final UserConnection found = cmanger.findBy(conn);
		for (TalkHandler handler : handlers) {
			handler.onClose(this, found);
		}
		cmanger.remove(found, DisConnectReason.CLIENT);
	}

	@Override
	public void onMessage(WebSocketConnection conn, String msg) {
		try {
			
			final UserConnection found = cmanger.findBy(conn);
			TalkMessage tmessage = TalkMessage.fromJsonString(msg);

			RepositoryEntry r = context().getAttributeObject(RepositoryEntry.EntryName, RepositoryEntry.class);
			ReadSession rsession = r.login("test");

			for (TalkHandler handler : handlers) {
				handler.onMessage(this, found, rsession, tmessage);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onMessage(WebSocketConnection conn, byte[] msg) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void onPing(WebSocketConnection conn, byte[] msg) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void onPong(WebSocketConnection conn, byte[] msg) {
		throw new UnsupportedOperationException("not supported");
	}

	ConnManager connManger() {
		return cmanger;
	}

	@Override
	public void onEvent(AradonEvent event, IService service) {
		if (event == AradonEvent.START) {
			for (TalkHandler handler : handlers) {
				handler.onEngineStart(this);
			}
		} else if (event == AradonEvent.STOP) {
			for (TalkHandler handler : handlers) {
				handler.onEngineStop(this);
			}
		}
	}

	
	// Only test
	void stop() {
		aradon.stop() ;
	}
}

class ConnManager {

	private Map<String, UserConnection> conns = MapUtil.newMap();

	private ConnManager() {
	}

	public UserConnection findBy(String id) {
		return conns.get(id);
	}

	public UserConnection findBy(WebSocketConnection wconn) {
		return conns.get(wconn.getString("id"));
	}

	public static ConnManager create() {
		return new ConnManager();
	}

	public UserConnection add(UserConnection uconn) {
		UserConnection existConn = conns.put(uconn.id(), uconn);
		if (existConn != null)
			existConn.close(this, TalkEngine.DisConnectReason.DOPPLE);
		return uconn;
	}

	public UserConnection remove(UserConnection uconn, TalkEngine.DisConnectReason reason) {
		conns.remove(uconn.id());
		uconn.close(this, reason);
		return uconn;
	}

	public boolean contains(WebSocketConnection conn) {
		return conns.containsValue(conn);
	}

}