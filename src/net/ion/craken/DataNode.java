package net.ion.craken;

import java.io.Serializable;

public interface DataNode extends Serializable{
	
	public NodeKey getKey() ;
	public DataNode put(String id, Serializable value) ;
	public Serializable getValue(String id);
}
