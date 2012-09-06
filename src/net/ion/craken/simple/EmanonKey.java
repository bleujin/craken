package net.ion.craken.simple;

import net.ion.craken.EntryKey;

import org.apache.commons.lang.builder.ToStringBuilder;

public class EmanonKey implements EntryKey {

	private static final long serialVersionUID = -9184615641395191371L;
	private Object key;

	EmanonKey(Object key) {
		if (key == null) throw new IllegalArgumentException("key must be not null") ;
		this.key = key;
	}

	public static EmanonKey create(Object genKey) {
		return new EmanonKey(genKey);
	}

	public Object get() {
		return key;
	}
	
	public String getAsString(){
		return key.toString() ;
	}

	public boolean equals(Object _that) {
		if (!(_that instanceof EmanonKey))
			return false;
		EmanonKey that = (EmanonKey) _that;
		return this.key.equals(that.key);
	}

	public int hashCode() {
		return key.hashCode();
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
