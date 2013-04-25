package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;

public class ReadNodeImpl implements ReadNode{

	private TreeNode inner;
	protected ReadNodeImpl(TreeNode<String, ? extends Object> inner) {
		this.inner = inner ;
	}

	public static ReadNode load(TreeNode<String, ? extends Object> inner) {
		return new ReadNodeImpl(inner);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof ReadNodeImpl)) return false ;
		ReadNodeImpl that = (ReadNodeImpl) obj ;
		return inner.equals(that.inner) ;
	}
	
	@Override
	public int hashCode(){
		return inner.hashCode() ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + inner.getFqn().toString() + "]";
	}

	
	
	// .. common 
	
	public Fqn fqn(){
		return inner.getFqn() ;
	}
	
	public int dataSize(){
		return inner.dataSize() ;
	}

	public ReadNode parent(){
		return load(inner.getParent()) ;
	}
	
	public boolean hasChild(String relativeFqn){
		return inner.hasChild(Fqn.fromString(relativeFqn)) ;
	}
	
	public ReadNode child(String fqn){
		final TreeNode child = inner.getChild(Fqn.fromString(fqn));
		if (child == null) throw new IllegalArgumentException("not found child : " + fqn) ; 
		return load(child) ;
	}
	
	public Set<String> childrenNames(){
		return inner.getChildrenNames() ;
	}
	
	public IteratorList<ReadNode> children(){
		
		final Iterator<TreeNode> iter = inner.getChildren().iterator();
		return new IteratorList<ReadNode>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ReadNode next() {
				return ReadNodeImpl.this.load(iter.next());
			}

			@Override
			public void remove() {
				iter.remove() ;
			}
			
			public List<ReadNode> toList(){
				List<ReadNode> result = ListUtil.newList() ;
				while(hasNext()){
					result.add(next()) ;
				}
				return result ;
			}
		};
	}

	public Object property(String key) {
		return inner.get(key);
	}

	public Optional optional(String key) {
		return Optional.fromNullable(inner.get(key));
	}
	

	public Set<String> keys(){
		return inner.getKeys() ;
	}

	public Map<String, ? extends Object> toMap() {
		return inner.getData();
	}
	
	public Object id(){
		return inner.getFqn() ;
	}
	
	
	

}
