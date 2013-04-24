package net.ion.craken.tree;

import java.io.Serializable;

import net.ion.framework.util.ObjectId;

public class TreeNodeId implements Serializable {

	private static final long serialVersionUID = -4074310150517366517L;

	private String idstring ;
	public TreeNodeId(ObjectId oid){
		this.idstring = oid.toString() ;
	}
	
	public boolean equals(Object o){
		if (! (o instanceof TreeNodeId)) return false ;
		TreeNodeId that = (TreeNodeId) o ;
		return this.idstring.equals(that.idstring) ;
	}
	
	public int hashCode(){
		return idstring.hashCode() ;
	}

	public String toString(){
		return "NodeId{" + idstring  + "}";
	}
}
