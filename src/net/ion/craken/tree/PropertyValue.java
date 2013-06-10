package net.ion.craken.tree;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import net.ion.craken.io.BlobProxy;
import net.ion.craken.io.BlobValue;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.exception.NodeIOException;
import net.ion.craken.node.exception.NodeNotValidException;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.lang.builder.ToStringBuilder;

public class PropertyValue implements Serializable, Comparable<PropertyValue> {

	private final static long serialVersionUID = 4614113174797214253L;
	public final static PropertyValue NotFound = new PropertyValue(SetUtil.EMPTY) ; 
	
	private final Set values ;
	private transient GridFilesystem gfs;
	
	private PropertyValue(Set set){
		this.values = SetUtil.orderedSet(set) ;
	}
	
//	private Object writeReplace() throws ObjectStreamException {
//		return new SerializedPropertyValue(values.toString()) ;
//	}
	
	PropertyValue gfs(GridFilesystem gfs){
		this.gfs = gfs ;
		return this ;
	}

	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
	
	public Object value() {
		Iterator iter = values.iterator() ;
		return iter.hasNext() ? iter.next() : null ;
	}
	
	public Set asSet(){
		return Collections.unmodifiableSet(values) ;
	}

	public static PropertyValue createPrimitive(Object value) {
		return value == null ? new PropertyValue(SetUtil.newSet())  : new PropertyValue(SetUtil.create(value)) ;
	}

	public PropertyValue append(Object... vals) {
		Object firstValue = value() ;
		for (Object val : vals) {
			if (val == null) continue ;
			if (firstValue == null){
				firstValue = val ;
			}
			if (! (firstValue.getClass().equals(val.getClass()))){
				throw new NodeNotValidException("disallow different type in same property vlaue") ;
			}
			
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
	
	public BlobValue asBlob() {
		final Object value = value() ;
		if (value == null) return null;
		if (gfs == null) throw new NodeIOException("this value not accessable") ;
		if (value instanceof BlobProxy) {
			return BlobValue.create(gfs, (BlobProxy)value) ;
		}
		throw new NodeIOException("this value is not blob type") ;
	}
	
	public int size(){
		return values.size() ;
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

	public int hashCode(){
		return values.hashCode() ;
	}
	
	public boolean equals(Object obj){
		if (! (obj instanceof PropertyValue)) return false ;
		PropertyValue that = (PropertyValue) obj;
		return this.values.equals(that.values) ;
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
