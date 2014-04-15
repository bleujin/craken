package net.ion.craken.listener;

import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.Fqn;

public class CDDRemovedEvent {

	private TreeNodeKey key;
	
	public CDDRemovedEvent(TreeNodeKey dataKey) {
		this.key = dataKey ;
	}

	public final static CDDRemovedEvent create(TouchedRow trow){
		return new CDDRemovedEvent(trow.target().dataKey()) ;
	}
	
	public static CDDRemovedEvent create(Fqn fqn) {
		return new CDDRemovedEvent(fqn.dataKey()) ;
	}
	
	public TreeNodeKey getKey(){
		return key ;
	}

}
