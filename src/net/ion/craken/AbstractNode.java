package net.ion.craken;

import java.io.Serializable;

import org.infinispan.Cache;


public abstract class AbstractNode implements Serializable{
	
	private transient LegContainer container ;
	void setContainer(LegContainer container) {
		this.container = container ;
	}
	
	public abstract NodeKey key() ;
//	public abstract DataNode put(String id, Serializable value) ;
//	public abstract Serializable getValue(String id);
	public final void save(){
		container.putNode(this) ;
	}
}
