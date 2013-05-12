package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ecs.xhtml.s;

import com.google.common.base.Optional;

import net.ion.craken.node.AbstractWriteNode;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;

public class WriteNodeImpl extends AbstractWriteNode{

	private TreeNode<PropertyId, PropertyValue> inner ;
	
	private WriteNodeImpl(WriteSession wsession, TreeNode<PropertyId, PropertyValue> inner) {
		super(wsession) ;
		this.inner = inner ;
	}
	
	public static WriteNode loadTo(WriteSession wsession, TreeNode<PropertyId, PropertyValue> node) {
		return new WriteNodeImpl(wsession, node);
	}

	public WriteNode load(WriteSession wsession, TreeNode<PropertyId, PropertyValue> inner) {
		return new WriteNodeImpl(wsession, inner);
	}
	
	protected TreeNode<PropertyId, PropertyValue> tree(){
		return inner ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + inner.getFqn().toString() + "]";
	}

	public WriteChildren children(){
		final Iterator<TreeNode<PropertyId, PropertyValue>> iter = tree().getChildren().iterator();
		return new WriteChildren(session(), iter) ;
		
//		return new IteratorList<WriteNode>() {
//			@Override
//			public boolean hasNext() {
//				return iter.hasNext();
//			}
//
//			@Override
//			public WriteNode next() {
//				return WriteNodeImpl.this.load(WriteNodeImpl.this.wsession(), iter.next());
//			}
//
//			@Override
//			public void remove() {
//				iter.remove() ;
//			}
//			
//			public List<WriteNode> toList(){
//				List<WriteNode> result = ListUtil.newList() ;
//				while(hasNext()){
//					result.add(next()) ;
//				}
//				return result ;
//			}
//
//		};
	}

	
}
