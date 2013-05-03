package net.ion.craken.node.crud;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.PropertyHandler;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.bean.ToBeanStrategy;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.collections.IteratorUtils;

import com.amazonaws.transform.MapUnmarshaller;
import com.google.common.base.Optional;

public class ReadNodeImpl implements ReadNode{

	private ReadSession session ;
	private TreeNode<PropertyId, PropertyValue> tree;
	protected ReadNodeImpl(ReadSession session, TreeNode<PropertyId, PropertyValue> inner) {
		this.session = session ;
		this.tree = inner ;
	}

	public static ReadNode load(ReadSession session, TreeNode<PropertyId, PropertyValue> inner) {
		return new ReadNodeImpl(session, inner);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof ReadNodeImpl)) return false ;
		ReadNodeImpl that = (ReadNodeImpl) obj ;
		return tree.equals(that.tree) ;
	}
	
	@Override
	public int hashCode(){
		return tree.hashCode() ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + tree.getFqn().toString() + "]";
	}

	
	
	// .. common 
	
	public Fqn fqn(){
		return tree.getFqn() ;
	}
	
	public int dataSize(){
		return tree.dataSize() ;
	}

	public ReadNode parent(){
		return load(session, tree.getParent()) ;
	}
	
	public boolean hasChild(String relativeFqn){
		return tree.hasChild(Fqn.fromString(relativeFqn)) ;
	}
	
	public ReadNode child(String fqn){
//		return session.pathBy(Fqn.fromRelativeFqn(this.fqn(), Fqn.fromString(fqn))) ;
		final TreeNode child = tree.getChild(Fqn.fromString(fqn));
		if (child == null) throw new IllegalArgumentException("not found child : " + fqn) ; 
		return load(session, child) ;
	}
	
	public Set<Object> childrenNames(){
		return tree.getChildrenNames() ;
	}
	
	public IteratorList<ReadNode> children(){
		
		final Iterator<TreeNode<PropertyId, PropertyValue>> iter = tree.getChildren().iterator();
		return new IteratorList<ReadNode>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ReadNode next() {
				return ReadNodeImpl.this.load(session, iter.next());
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

	public PropertyValue property(String key) {
		return property(PropertyId.normal(key)) ;
	}

	public PropertyValue property(PropertyId pid) {
		return ObjectUtil.coalesce(tree.get(pid), PropertyValue.NotFound);
	}

	public Optional<PropertyValue> optional(String key) {
		return Optional.fromNullable(tree.get(PropertyId.normal(key)));
	}
	

	public Set<PropertyId> keys(){
		return tree.getKeys() ;
	}

	public Map<PropertyId, PropertyValue> toMap() {
		return Collections.unmodifiableMap(tree.getData());
	}
	
	public Map<String, Object> toPropertyMap(final int descendantDepth){
		final int childDepth = descendantDepth - 1 ;
		Map<String, Object> result = MapUtil.newMap() ;
		
		for(Entry<PropertyId, PropertyValue> entry : toMap().entrySet()) {
			if (entry.getKey().type() == PropertyId.PType.NORMAL){
				result.put(entry.getKey().getString(), entry.getValue().asSet().size() <= 1 ? entry.getValue().value() : entry.getValue().asSet()) ;
			} else if(entry.getKey().type() == PropertyId.PType.REFER && descendantDepth > 0) {
				IteratorList<ReadNode> refs = refs(entry.getKey().getString());
				Set<Map<String, Object>> set = SetUtil.orderedSet(SetUtil.newSet()) ;
				while(refs.hasNext()) {
					set.add(refs.next().toPropertyMap(childDepth)) ;
				}
				result.put('#' + entry.getKey().getString(), set) ;
			}
		}
		
		IteratorList<ReadNode> children = children();
		if (descendantDepth > 0 && children.hasNext()) {
			while(children.hasNext()){
				final ReadNode next = children.next();
				result.put('@' + next.fqn().getLastElementAsString(), next.toPropertyMap(childDepth)) ;
			}
		}
		
		return Collections.unmodifiableMap(result) ;
	}
	
	
	public Object id(){
		return tree.getFqn() ;
	}
	
	public boolean hasProperty(PropertyId pid){
		return keys().contains(pid) ;
	}
	
	public ReadNode ref(String refName){
		PropertyId referId = PropertyId.refer(refName);
		if (hasProperty(referId)) {
			Object val = property(referId).value() ;
			if (val == null ) throw new IllegalArgumentException("not found ref :" + refName) ;

			return session.pathBy(val.toString(), true) ;
		} else {
			throw new IllegalArgumentException("not found ref :" + refName) ;
		}
	}
	
	public IteratorList<ReadNode> refs(String refName){
		
		PropertyId referId = PropertyId.refer(refName);
		final Iterator<String> iter = hasProperty(referId) ? property(referId).asSet().iterator() : IteratorUtils.EMPTY_ITERATOR;
		
		return new IteratorList<ReadNode>() {
			@Override
			public List<ReadNode> toList() {
				List<ReadNode> result = ListUtil.newList() ;
				while(iter.hasNext()) {
					result.add(session.pathBy(iter.next())) ;
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ReadNode next() {
				return session.pathBy(iter.next(), true);
			}
		};
	}
	
	public <T> T toBean(Class<T> clz){
		return ToBeanStrategy.ProxyByCGLib.toBean(this, clz) ;
//		return ToBeanStrategy.EasyByJson.toBean(this, clz) ;
	}
	
}
