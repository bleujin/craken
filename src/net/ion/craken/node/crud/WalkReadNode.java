package net.ion.craken.node.crud;

import java.util.List;
import java.util.Set;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

public class WalkReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -4810786417922545471L;
	private WalkReadNode parent;
	private int level;
	private Fqn path;

	private WalkReadNode(ReadSession session, WalkReadNode parent, Fqn fqn, int level) {
		super(session, fqn);
		this.parent = parent ;
		this.level = level;
		this.path = parent == null ? fqn : Fqn.fromRelativeElements(parent.path(), fqn.name()) ;
	}

	static WalkReadNode create(ReadSession session, WalkReadNode from, Fqn fqn, int level) {
		return new WalkReadNode(session, from, fqn, level);
	}

	
	public int level() {
		return level;
	}

	
	public Fqn path(){
		return path ;
	}

	public String path(String div){
		return div + StringUtil.join(StringUtil.split(path.toString(), "/"), div) ;
	}

	public String propertyPath(ReadNode parent, String pid, String div) {
		List<String> result = ListUtil.newList() ;
		for (String name : path.peekElements()) {
			result.add(parent.child(name).asString(pid)) ;
		}
		return div + StringUtil.join(result, div);
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
		return this.getClass().getSimpleName() + "[fqn=" + fqn().toString() + ", path=" + path + ", level=" + level + "]";
	}


}

