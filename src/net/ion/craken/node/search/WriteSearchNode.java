package net.ion.craken.node.search;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import net.ion.craken.node.AbstractWriteNode;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;

public class WriteSearchNode extends AbstractWriteNode{


	private TreeNode tree ;
	
	private WriteSearchNode(WriteSession wsession, TreeNode inner) {
		super(wsession) ;
		this.tree = inner ;
	}

	public static WriteSearchNode loadTo(WriteSession wsession, TreeNode inner) {
		return new WriteSearchNode(wsession, inner);
	}
	
	public WriteSearchNode load(WriteSession wsession, TreeNode inner) {
		return new WriteSearchNode(wsession, inner);
	}
	
	protected TreeNode tree(){
		return tree ;
	}
	
	public IteratorList<WriteNode> children(){
		final Iterator<TreeNode> iter = tree().getChildren().iterator();
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
