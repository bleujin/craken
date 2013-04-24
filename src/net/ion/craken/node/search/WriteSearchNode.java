package net.ion.craken.node.search;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;

public class WriteSearchNode implements WriteNode{


	private TreeNode inner ;
	
	private WriteSearchNode(TreeNode inner) {
		this.inner = inner ;
	}

	public static WriteSearchNode load(TreeNode inner) {
		return new WriteSearchNode(inner);
	}
	
	private TreeNode inner(){
		return inner ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + inner.getFqn().toString() + "]";
	}

	
	
	public WriteNode property(String key, Object value) {
		inner().put(key, value) ;
		return this ;
	}
	
	public Object propertyIfAbsent(String key, Object value){
		return inner.putIfAbsent(key, value) ;
	}
	
	public Object replace(String key, Object value){
		return inner.replace(key, value) ;
	}
	
	public boolean replace(String key, Object oldValue, Object newValue){
		return inner.replace(key, oldValue, newValue) ;
	}
	
	public WriteNode propertyAll(Map<String, ? extends Object> map){
		inner.putAll(map) ;
		return this ;
	}
	
	public WriteNode replaceAll(Map<String, ? extends Object> newMap){
		inner.replaceAll(newMap) ;
		return this ;
	}
	
	
	public WriteNode unset(String key){
		inner().remove(key) ;
		return this ;
	}
	
	
	public WriteNode clear(){
		inner().clearData() ;
		return this ;
	}
	
	
	public WriteNode addChild(String relativeFqn){
		Iterator<Object> iter = Fqn.fromString(relativeFqn).peekElements().iterator();
		
		TreeNode last = inner ;
		while(iter.hasNext()){
			last = last.addChild(Fqn.fromElements(iter.next()));
		}
		
		return load(last) ;
	}
	
	
	public boolean removeChild(String fqn){
		return inner.removeChild(Fqn.fromString(fqn)) ;
	}
	
	public void removeChildren(){
		inner.removeChildren() ;
	}
	
	
	
	
	
	// common
	public Fqn fqn(){
		return inner.getFqn() ;
	}
	
	public int dataSize(){
		return inner.dataSize() ;
	}
	
	public WriteNode parent(){
		return load(inner().getParent()) ;
	}
	
	
	public boolean hasChild(String fqn){
		return inner.hasChild(Fqn.fromString(fqn)) ;
	}
	
	public WriteNode child(String fqn){
		return load(inner().getChild(Fqn.fromString(fqn))) ;
	}
	
	public Set<String> childrenNames(){
		return inner().getChildrenNames() ;
	}
	
	public IteratorList<WriteNode> children(){
		final Iterator<TreeNode> iter = inner().getChildren().iterator();
		return new IteratorList<WriteNode>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public WriteNode next() {
				return WriteSearchNode.this.load(iter.next());
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
	
	public Set<String> keys(){
		return inner().getKeys() ;
	}
	
	public Object property(String key){
		return inner().get(key) ;
	}
	
	public Optional optional(String key){
		return Optional.fromNullable(inner().get(key)) ;
	}
	
	public Object id(){
		return inner.getFqn() ;
	}

}
