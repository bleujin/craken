package net.ion.craken;

import java.io.Serializable;

import org.infinispan.Cache;


public abstract class AbstractEntry<E extends AbstractEntry> implements Serializable{
	
	private transient LegContainer container ;
	void setContainer(LegContainer<E> container) {
		this.container = container ;
	}
	
	public abstract EntryKey key() ;
//	public abstract DataNode put(String id, Serializable value) ;
//	public abstract Serializable getValue(String id);
	public final E save(){
		container.putNode(this) ;
		return (E) this ;
	}
	

	public final E remove() {
		return (E) container.remove(this.key()) ;
	}

}
