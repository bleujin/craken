package net.ion.craken.tree;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;

import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

public class PropertyValue implements Serializable, Comparable<PropertyValue> {

	private final static long serialVersionUID = 4614113174797214253L;
	public final static PropertyValue NotFound = new PropertyValue(SetUtil.EMPTY) ; 
	
	private final Set values ;
	
	PropertyValue(Set set){
		this.values = SetUtil.orderedSet(set) ;
	}
	
//	private Object writeReplace() throws ObjectStreamException {
//		return new SerializedPropertyValue(values.toString()) ;
//	}


	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
	
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

	public String stringValue() {
		return ObjectUtil.toString(value());
	}

	public int intValue(int dftValue){
		try {
			return Integer.parseInt(stringValue()) ;
		} catch(NumberFormatException e){
			return dftValue ;
		}
	}

	public long longValue(long dftValue){
		try {
			return Long.parseLong(stringValue()) ;
		} catch(NumberFormatException e){
			return dftValue ;
		}
	}
	

	
	public <T> T value(T replaceValue) {
		final Object value = value();
		if (value == null ){
			return replaceValue ;
		}
		return (T)value;
	}

	@Override
	public int compareTo(PropertyValue that) {
		if (this.value() instanceof Comparable && that.value() instanceof Comparable) {
			return ((Comparable)this.value()).compareTo(that.value()) ;
		}  
		return 0;
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
