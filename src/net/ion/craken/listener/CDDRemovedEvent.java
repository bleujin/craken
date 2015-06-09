package net.ion.craken.listener;

import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;

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
