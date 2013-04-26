package net.ion.craken.tree;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonParser;

public class PropertyValue implements Serializable {

	private static final long serialVersionUID = 4614113174797214253L;
	
	private final JsonElement inner ;
	
	public PropertyValue(JsonElement inner){
		this.inner = inner ;
	}
	
	private Object writeReplace() throws ObjectStreamException {
		return new SerializeStringJson(inner.toString()) ;
	}

	public JsonElement jsonElement() {
		return inner ;
	}

	public static PropertyValue primitive(Object value) {
		return new PropertyValue(JsonParser.fromObject(value).getAsJsonPrimitive()) ;
	}

	public Date asDate() {
		return inner.getAsJsonPrimitive().getAsDate() ;
	}

	public JsonArray asArray() {
		return inner.getAsJsonArray();
	}
	
}

class SerializeStringJson implements Serializable {
	private static final long serialVersionUID = -6058220419345126634L;

	private String jsonString ;
	public SerializeStringJson(String jsonString) {
		this.jsonString = jsonString ;
	}
	
	private Object readResolve() throws ObjectStreamException{
		return new PropertyValue(JsonParser.fromString(jsonString)) ;
	}
	
}
