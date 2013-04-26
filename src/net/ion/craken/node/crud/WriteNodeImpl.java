package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import net.ion.craken.node.AbstractWriteNode;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.WriteNode;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;

public class WriteNodeImpl extends AbstractWriteNode{

	private TreeNode<PropertyId, PropertyValue> inner ;
	
	private WriteNodeImpl(TreeNode<PropertyId, PropertyValue> inner) {
		this.inner = inner ;
	}
	
	public static WriteNode loadTo(TreeNode<PropertyId, PropertyValue> node) {
		return new WriteNodeImpl(node);
	}

	public WriteNode load(TreeNode<PropertyId, PropertyValue> inner) {
		return new WriteNodeImpl(inner);
	}
	
	protected TreeNode<PropertyId, PropertyValue> tree(){
		return inner ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + inner.getFqn().toString() + "]";
	}

	public IteratorList<WriteNode> children(){
		final Iterator<TreeNode<PropertyId, PropertyValue>> iter = tree().getChildren().iterator();
		return new IteratorList<WriteNode>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public WriteNode next() {
				return WriteNodeImpl.this.load(iter.next());
			}

			@Override
			public void remove() {
				iter.remove() ;
			}
			
			public List<WriteNode> toList(){
				List<WriteNode> result = ListUtil.newList() ;
				while(hasNext()){
					result.add(next()) ;
				}
				return result ;
			}

		};
	}

	
}
