package net.ion.craken.simple;

import net.ion.craken.NodeKey;

public class SimpleKeyFactory {

	public static NodeKey create(Object genKey) {
		return new EmanonKey(genKey);
	}

}

class EmanonKey implements NodeKey {
	
	private static final long serialVersionUID = -9184615641395191371L;
	private Object key ;
	EmanonKey(Object key){
		this.key = key ;
	}
	
	public Object get(){
		return key ;
	}
	
	public boolean equals(Object _that){
		if (! (_that instanceof EmanonKey)) return false ;
		EmanonKey that = (EmanonKey) _that ;
		return this.key.equals(that.key) ;
	}
	
	public int hashCode(){
		return key.hashCode() ;
	}
}
