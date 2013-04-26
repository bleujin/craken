package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

import com.google.common.base.Optional;

public abstract class AbstractWriteNode implements WriteNode {

	protected abstract TreeNode inner() ;
	protected abstract WriteNode load(TreeNode inner) ;
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + fqn() + "]";
	}

	public WriteNode property(String key, Object value) {
		inner().put(key, value) ;
		return this ;
	}
	
	public Object propertyIfAbsent(String key, Object value){
		return inner().putIfAbsent(key, value) ;
	}
	
	public Object replace(String key, Object value){
		return inner().replace(key, value) ;
	}
	
	public boolean replace(String key, Object oldValue, Object newValue){
		return inner().replace(key, oldValue, newValue) ;
	}
	
	public WriteNode propertyAll(Map<String, ? extends Object> map){
		inner().putAll(map) ;
		return this ;
	}
	
	public WriteNode replaceAll(Map<String, ? extends Object> newMap){
		inner().replaceAll(newMap) ;
		return this ;
	}
	
	
	public WriteNode unset(String key){
		inner().remove(key) ;
		return this ;
	}
	
	
	public WriteNode clear(){
//		Object id = id() ;
		inner().clearData() ;
//		inner().put(NodeCommon.IDProp, id) ;
		return this ;
	}
	
	
	public WriteNode addChild(String relativeFqn){
//		final TreeNode find = inner.addChild(Fqn.fromString(relativeFqn));
//		return load(find) ;
		
		
		Iterator<Object> iter = Fqn.fromString(relativeFqn).peekElements().iterator();
		
		TreeNode last = inner() ;
		while(iter.hasNext()){
			last = last.addChild(Fqn.fromElements(iter.next()));
		}
		return load(last) ;
	}
	
	
	public boolean removeChild(String fqn){
		return inner().removeChild(Fqn.fromString(fqn)) ;
	}
	
	public void removeChildren(){
		inner().removeChildren() ;
	}
	
	
	public WriteNode refTo(String refName, String fqn, boolean isFirst){
		List<String> list = (List<String>) property(refName) ;
		if (list == null || list.size() == 0) {
			list = ListUtil.<String>newList();
		}
		if (isFirst) list.add(0, fqn) ; 
		else list.add(fqn) ;
		
		return property(refName, list) ;
	}
	
	
	public WriteNode refToLast(String refName, String fqn){
		return refTo(refName, fqn.toString(), false) ;
	}
	public WriteNode refToFirst(String refName, String fqn){
		return refTo(refName, fqn.toString(), true) ;
	}
	
	
	// common
	public Fqn fqn(){
		return inner().getFqn() ;
	}
	
	public int dataSize(){
		return inner().dataSize() ;
	}
	
	public WriteNode parent(){
		return load(inner().getParent()) ;
	}
	
	
	public boolean hasChild(String fqn){
		return inner().hasChild(Fqn.fromString(fqn)) ;
	}
	
	public WriteNode child(String fqn){
		return load(inner().getChild(Fqn.fromString(fqn))) ;
	}
	
	public Set<String> childrenNames(){
		return inner().getChildrenNames() ;
	}
		
	public Set<String> keys(){
		return inner().getKeys() ;
	}
	
	public Object property(String key) {
		return inner().get(key);
	}

	public Optional optional(String key) {
		return Optional.fromNullable(inner().get(key));
	}
	
	
	public Object id(){
		return fqn() ;
	}
}
