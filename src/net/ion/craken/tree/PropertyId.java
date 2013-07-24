package net.ion.craken.tree;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;

public class PropertyId implements Serializable {
	private static final long serialVersionUID = 7910617121345547495L;
	
	public static enum PType implements Serializable {
		NORMAL, REFER
	}

	private final PType type;
	private final String key;
	private boolean ignoreIndex = false ;
	
	private PropertyId(PType type, String key){
		this.type = type ;
		this.key = key ;
	}
	
	public static final PropertyId normal(String key){
		return new PropertyId(PType.NORMAL, key) ;
	}

	public static final PropertyId refer(String key){
		return new PropertyId(PType.REFER, key) ;
	}
	
	public boolean equals(Object o){
		if (! (o instanceof PropertyId)) return false ;
		PropertyId that = (PropertyId) o ;
		return this.type == that.type && this.key.equals(that.key) ; 
	}
	
	public int hashCode(){
		return key.hashCode() + type.hashCode() ;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}

	public String idString(){
		return (type == PType.REFER) ? "@" + key : key ;
	}
	
	public final static PropertyId fromIdString(String idString){
		return idString.startsWith("@") ? new PropertyId(PType.REFER, idString.substring(1)) : new PropertyId(PType.NORMAL, idString) ;
	}
	
	public String getString() {
		return key;
	}

	public PType type() {
		return type;
	}

	public PropertyId ignoreIndex() {
		ignoreIndex = true ;
		return this ;
	}
	
	public boolean isIgnoreIndex(){
		return ignoreIndex ;
	}
}
