package net.ion.craken.node.search;

import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.AbstractWriteNode;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;

public class WriteSearchNode extends AbstractWriteNode{


	private TreeNode<PropertyId, PropertyValue> tree ;
	
	private WriteSearchNode(WriteSession wsession, TreeNode<PropertyId, PropertyValue> inner) {
		super(wsession) ;
		this.tree = inner ;
	}

	public static WriteSearchNode loadTo(WriteSession wsession, TreeNode<PropertyId, PropertyValue> inner) {
		return new WriteSearchNode(wsession, inner);
	}
	
	public WriteSearchNode load(WriteSession wsession, TreeNode<PropertyId, PropertyValue> inner) {
		return new WriteSearchNode(wsession, inner);
	}
	
	protected TreeNode<PropertyId, PropertyValue> tree(){
		return tree ;
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
				return WriteSearchNode.this.load(WriteSearchNode.this.wsession(), iter.next());
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
