package net.ion.craken.aradon;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.WebSocketConnection;

import org.jboss.netty.channel.ChannelFuture;

public class FakeConnection implements WebSocketConnection {

	private Map<String, Object> datas = MapUtil.newMap() ;
	private List<String> received = ListUtil.newList() ;

	public static FakeConnection create(String id) {
		final FakeConnection result = new FakeConnection();
		result.data("id", id);
		return result ;
	}

	@Override
	public WebSocketConnection close() {
		return this;
	}

	@Override
	public WebSocketConnection data(String key, Object value) {
		datas.put(key, value) ;
		return this;
	}

	@Override
	public String getString(String key) {
		return ObjectUtil.toString(datas.get(key));
	}

	@Override
	public WebSocketConnection ping(byte[] message) {
		return this;
	}

	@Override
	public WebSocketConnection pong(byte[] message) {
		return this;
	}

	@Override
	public WebSocketConnection send(String message) {
		received.add(message) ;
		return this;
	}

	@Override
	public void execute(Runnable command) {
		command.run() ;
	}

	@Override
	public Map<String, Object> data() {
		return datas;
	}

	@Override
	public Object data(String key) {
		return datas.get(key);
	}

	@Override
	public Set<String> dataKeys() {
		return datas.keySet();
	}

	public String recentMsg() {
		return received.get(received.size() -1) ;
	}

	
	
	
	

	@Override
	public WebSocketConnection send(byte[] message) {
		return this;
	}

	@Override
	public WebSocketConnection send(byte[] message, int offset, int length) {
		return this;
	}
	
	@Override
	public Executor handlerExecutor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpRequest httpRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChannelFuture sendFuture(String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String version() {
		// TODO Auto-generated method stub
		return null;
	}


	
}
