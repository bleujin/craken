package net.ion.craken;

import java.io.Serializable;

import org.infinispan.Cache;


public abstract class AbstractEntry<E extends AbstractEntry> implements Serializable{
	
	private transient LegContainer<E> container ;
	protected void setContainer(LegContainer<E> container) {
		this.container = container ;
	}
	
	public abstract EntryKey key() ;
	public final E save(){
		container.putNode((E)this) ;
		return (E) this ;
	}
	
	public final E remove() {
		return (E) container.removeByKey(this.key()) ;
	}
	
	
}
