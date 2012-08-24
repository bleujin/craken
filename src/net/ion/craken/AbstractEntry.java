package net.ion.craken;

import java.io.Serializable;

import org.infinispan.Cache;


public abstract class AbstractEntry<T extends AbstractEntry> implements Serializable{
	
	private transient LegContainer container ;
	void setContainer(LegContainer<T> container) {
		this.container = container ;
	}
	
	public abstract NodeKey key() ;
//	public abstract DataNode put(String id, Serializable value) ;
//	public abstract Serializable getValue(String id);
	public final void save(){
		container.putNode(this) ;
	}
	

	public final T remove() {
		return (T) container.remove(this.key()) ;
	}

}
