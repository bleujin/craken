package net.ion.craken.tree;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;

public class PropertyId implements Serializable {
	private static final long serialVersionUID = 7910617121345547495L;
	
	public static enum PType implements Serializable {
		NORMAL, INNER, INLIST, REFER
	}

	private final PType type;
	private final String key;
	
	private PropertyId(PType type, String key){
		this.type = type ;
		this.key = key ;
	}
	
	public static final PropertyId normal(String key){
		return new PropertyId(PType.NORMAL, key) ;
	}

	public static final PropertyId inner(String key){
		return new PropertyId(PType.INNER, key) ;
	}

	public static final PropertyId inlist(String key){
		return new PropertyId(PType.INLIST, key) ;
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
}
