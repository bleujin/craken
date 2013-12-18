package net.ion.craken.aradon;

import java.util.Map;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import net.ion.craken.aradon.TalkEngine.DisConnectReason;
import net.ion.framework.util.HashUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nradon.WebSocketConnection;

public class UserConnection {

	private WebSocketConnection inner;

	private UserConnection(WebSocketConnection inner) {
		this.inner = inner ;
	}

	static UserConnection create(WebSocketConnection conn) {
		return new UserConnection(conn) ;
	}

	
	public String id(){
		return inner.getString("id") ;
	}
	
	public String asString(String key){
		return ObjectUtil.toString(inner.data(key)) ;
	}

	public Object asObject(String key){
		return inner.data(key) ;
	}
	
	public Map<String, Object> asMap(){
		return Collections.unmodifiableMap(inner.data()) ;
	}

	public void sendMessage(String message) {
		inner.send(message) ;
	}

	void close(ConnManager cmanager, DisConnectReason reason) {
		
		inner.close() ;
	}

	
	@Override
	public boolean equals(Object obj){
		if (obj instanceof UserConnection){
			return ((UserConnection)obj).id().equals(this.id()) ;
		}
		return false ;
	}
	
	@Override
	public int hashCode(){
		return this.id().hashCode() ;
	}
	
	public String toString(){
		return "UserConnection[" + id() + "]" ;
	}

}
