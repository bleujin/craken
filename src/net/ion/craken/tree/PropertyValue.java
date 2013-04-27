package net.ion.craken.tree;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonNull;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;

public class PropertyValue implements Serializable {

	private final static long serialVersionUID = 4614113174797214253L;
	public final static PropertyValue NotFound = new PropertyValue(JsonNull.INSTANCE) ; 
	
	private final JsonElement inner ;
	
	PropertyValue(JsonElement inner){
		this.inner = inner ;
	}
	
	private Object writeReplace() throws ObjectStreamException {
		return new SerializedPropertyValue(inner.toString()) ;
	}

	public JsonElement jsonElement() {
		return inner ;
	}

	public Date asDate() {
		return inner.getAsJsonPrimitive().getAsDate() ;
	}

	public Object value() {
		return inner.isJsonPrimitive() ? inner.getAsJsonPrimitive().getValue() : null;
	}

	
	
	
	public JsonArray asArray() {
		return inner.getAsJsonArray();
	}
	public static PropertyValue createPrimitive(Object value) {
		return value == null ? PropertyValue.NotFound : new PropertyValue(JsonParser.fromObject(value).getAsJsonPrimitive()) ;
	}

}

class SerializedPropertyValue implements Serializable {
	private static final long serialVersionUID = -6058220419345126634L;

	private String jsonString ;
	public SerializedPropertyValue(String jsonString) {
		this.jsonString = jsonString ;
	}
	
	private Object readResolve() throws ObjectStreamException{
		return new PropertyValue(JsonParser.fromString(jsonString)) ;
	}
	
}
