package net.ion.craken.node.crud;

import java.util.Set;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.util.SetUtil;

public class WalkReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -4810786417922545471L;
	private WalkReadNode parent;
	private int level;

	private WalkReadNode(ReadSession session, WalkReadNode parent, Fqn fqn, int level) {
		super(session, fqn);
		this.parent = parent ;
		this.level = level;
	}

	static WalkReadNode create(ReadSession session, WalkReadNode from, Fqn fqn, int level) {
		return new WalkReadNode(session, from, fqn, level);
	}

	
	public int level() {
		return level;
	}


	public WalkReadNode from(){
		return parent ;
	}

	public PropertyValue get(PropertyId key) {
		return treeNode().get(key);
	}

	Set<WalkReadNode> getReferences(String refName) {
		Set<WalkReadNode> result = SetUtil.newSet() ;
		
		for(Fqn refFqn : treeNode().getReferencesFqn(refName)){
			result.add(WalkReadNode.create(session(), this, refFqn, (level+1))) ;
		}
		return result;
	}

	Set<WalkReadNode> getChildren() {
		Set<WalkReadNode> result = SetUtil.newSet() ;
		
		for(Fqn childFqn : treeNode().getChildrenFqn()){
			result.add(WalkReadNode.create(session(), this, childFqn, (level+1))) ;
		}
		return result;
	}

	public String toString() {
		return this.getClass().getSimpleName() + "[fqn=" + fqn().toString() + ", level=" + level + "]";
	}


}

