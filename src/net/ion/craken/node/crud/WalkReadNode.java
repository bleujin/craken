package net.ion.craken.node.crud;

import java.util.Set;

import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.SetUtil;

public class WalkReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -4810786417922545471L;
	private WalkReadNode from;
	private int level;

	private WalkReadNode(ReadSession session, WalkReadNode from, TreeNode tnode, int level) {
		super(session, tnode);
		this.from = from ;
		this.level = level;
	}

	static WalkReadNode create(ReadSession session, WalkReadNode from, TreeNode child, int level) {
		return new WalkReadNode(session, from, child, level);
	}

	
	public int level() {
		return level;
	}


	public WalkReadNode from(){
		return from ;
	}

	public PropertyValue get(PropertyId key) {
		return treeNode().get(key);
	}

	Set<WalkReadNode> getReferences(String refName) {
		Set<WalkReadNode> result = SetUtil.newSet() ;
		
		for(TreeNode tnode : treeNode().getReferences(refName)){
			result.add(WalkReadNode.create(session(), this, tnode, (level+1))) ;
		}
		return result;
	}

	Set<WalkReadNode> getChildren() {
		Set<WalkReadNode> result = SetUtil.newSet() ;
		
		for(TreeNode tnode : treeNode().getChildren()){
			result.add(WalkReadNode.create(session(), this, tnode, (level+1))) ;
		}
		return result;
	}

	public String toString() {
		return this.getClass().getSimpleName() + "[fqn=" + fqn().toString() + ", level=" + level + "]";
	}


}

