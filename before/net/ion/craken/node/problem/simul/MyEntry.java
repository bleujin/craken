package net.ion.craken.node.problem.simul;

import java.io.ObjectStreamException;
import java.io.Serializable;

import net.ion.framework.parse.gson.JsonObject;

public class MyEntry implements Serializable {

	private static final long serialVersionUID = 4233450207680471635L;

	private JsonObject inner ;

	public MyEntry(){
		this(new JsonObject()) ;
	}
	MyEntry(JsonObject jo) {
		this.inner = jo ;
	}


	public MyEntry prop(String key, Object value) {
		inner.put(key, value);
		return this;
	}

	Object writeReplace() throws ObjectStreamException {
		return new JsonString(inner);
	}
	public MyEntry props(String[] headers, String[] cols) {
		for (int i = 0 ; i < headers.length ; i++) {
			prop(headers[i], cols[i] + " ") ;
		}
		return this;
	}

}


class JsonString implements Serializable {
	private static final long serialVersionUID = -4979209820610147833L;
	
	private String json ;
	public JsonString(JsonObject jo) {
		json = jo.toString() ;
	}

	Object readResolve(){
		return new MyEntry(JsonObject.fromString(json)) ;
	}
	
}
