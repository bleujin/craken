package net.ion.craken;

import java.io.Serializable;

import net.ion.craken.simple.EmanonKey;
import net.ion.framework.util.StringUtil;

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
	
	protected LegContainer<E> container(){
		return container ;
	}
	
	public boolean equals(Object _that) {
		if (!StringUtil.equals(this.getClass().getCanonicalName(), _that.getClass().getCanonicalName()))
			return false;
		AbstractEntry that = (AbstractEntry) _that;
		return key().equals(that.key());
	}

	public int hashCode() {
		return key().hashCode();
	}
	
}
