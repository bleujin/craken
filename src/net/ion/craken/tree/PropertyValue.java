package net.ion.craken.tree;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.SetUtils;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonNull;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.SetUtil;

public class PropertyValue implements Serializable {

	private final static long serialVersionUID = 4614113174797214253L;
	public final static PropertyValue NotFound = new PropertyValue(SetUtil.EMPTY) ; 
	
	private final Set values ;
	
	PropertyValue(Set set){
		this.values = SetUtil.orderedSet(set) ;
	}
	
//	private Object writeReplace() throws ObjectStreamException {
//		return new SerializedPropertyValue(values.toString()) ;
//	}


	public Object value() {
		Iterator iter = values.iterator() ;
		return iter.hasNext() ? iter.next() : null ;
	}
	
	public Set asSet(){
		return values ;
	}

	public static PropertyValue createPrimitive(Object value) {
		return value == null ? new PropertyValue(SetUtil.newSet())  : new PropertyValue(SetUtil.create(value)) ;
	}

	public PropertyValue append(Object... vals) {
		for (Object val : vals) {
			values.add(val) ;
		}
		return this ;
	}

}

//class SerializedPropertyValue implements Serializable {
//	private static final long serialVersionUID = -6058220419345126634L;
//
//	private String jsonString ;
//	public SerializedPropertyValue(String jsonString) {
//		this.jsonString = jsonString ;
//	}
//	
//	private Object readResolve() throws ObjectStreamException{
//		return new PropertyValue(JsonParser.fromString(jsonString)) ;
//	}
//	
//}
