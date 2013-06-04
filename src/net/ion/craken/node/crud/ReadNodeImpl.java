package net.ion.craken.node.crud;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.exception.NodeNotExistsException;
import net.ion.craken.tree.ExtendPropertyId;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.db.Rows;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.collections.IteratorUtils;

import com.google.common.base.Function;
import com.google.common.base.Optional;

public class ReadNodeImpl implements ReadNode{

	private ReadSession session ;
	private TreeNode<PropertyId, PropertyValue> treeNode;
	protected ReadNodeImpl(ReadSession session, TreeNode<PropertyId, PropertyValue> inner) {
		this.session = session ;
		this.treeNode = inner ;
	}

	public static ReadNode load(ReadSession session, TreeNode<PropertyId, PropertyValue> inner) {
		return new ReadNodeImpl(session, inner);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof ReadNodeImpl)) return false ;
		ReadNodeImpl that = (ReadNodeImpl) obj ;
		return treeNode.equals(that.treeNode) ;
	}
	
	@Override
	public int hashCode(){
		return treeNode.hashCode() ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + treeNode.getFqn().toString() + "]";
	}

	
	
	// .. common 
	
	public ReadSession session(){
		return session ;
	}
	
	public Fqn fqn(){
		return treeNode.getFqn() ;
	}
	
	public int dataSize(){
		return treeNode.dataSize() ;
	}

	public ReadNode parent(){
		return load(session, treeNode.getParent()) ;
	}
	
	public boolean hasChild(String relativeFqn){
		return treeNode.hasChild(Fqn.fromString(relativeFqn)) ;
	}
	
	public ReadNode child(String fqn){
//		return session.pathBy(Fqn.fromRelativeFqn(this.fqn(), Fqn.fromString(fqn))) ;
		final TreeNode child = treeNode.getChild(Fqn.fromString(fqn));
		if (child == null) throw new IllegalArgumentException("not found child : " + fqn) ; 
		return load(session, child) ;
	}
	
	public ReadNode root(){
		return session.root() ;
	}
	
	public Set<String> childrenNames(){
		Set<String> set = SetUtil.orderedSet(SetUtil.newSet());
		for (Object object : treeNode.getChildrenNames()) {
			set.add(ObjectUtil.toString(object)) ;
		}
		return set ;
	}
	
	public ReadChildren children(){
		return new ReadChildren(session, treeNode.getChildren().iterator()) ;
	}

	public PropertyValue property(String key) {
		return property(PropertyId.normal(key)) ;
	}


	public PropertyValue extendProperty(String propPath) {
		return ExtendPropertyId.create(propPath).propValue(this) ;
	}

	
	public PropertyValue property(PropertyId pid) {
		return ObjectUtil.coalesce(treeNode.get(pid), PropertyValue.NotFound);
	}

//	public Optional<PropertyValue> optional(String key) {
//		return Optional.fromNullable(treeNode.get(PropertyId.normal(key)));
//	}
	

	public Set<PropertyId> keys(){
		return treeNode.getKeys() ;
	}

	public Map<PropertyId, PropertyValue> toMap() {
		return Collections.unmodifiableMap(treeNode.getData());
	}
	
	public <T> T transformer(Function<ReadNode, T> function){
		return function.apply(this) ;
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
				result.put('@' + entry.getKey().getString(), set) ;
			}
		}
		
		IteratorList<ReadNode> children = children();
		if (descendantDepth > 0 && children.hasNext()) {
			while(children.hasNext()){
				final ReadNode next = children.next();
				result.put('/' + next.fqn().getLastElementAsString(), next.toPropertyMap(childDepth)) ;
			}
		}
		
		return Collections.unmodifiableMap(result) ;
	}
	
	
	public Object id(){
		return treeNode.getFqn() ;
	}
	
	public boolean hasProperty(PropertyId pid){
		return keys().contains(pid) ;
	}
	
	public boolean hasRef(String refName){
		return keys().contains(PropertyId.refer(refName)) ;
	}
	
	public boolean hasRef(String refName, Fqn fqn){
		return property(PropertyId.refer(refName)).asSet().contains(fqn.toString()) ;
	}
	
	public ReadNode ref(String refName){
		PropertyId referId = PropertyId.refer(refName);
		if (hasProperty(referId)) {
			Object val = property(referId).value() ;
			if (val == null ) throw new NodeNotExistsException("not found ref :" + refName) ;

			return session.pathBy(val.toString(), true) ;
		} else {
			throw new NodeNotExistsException("not found ref :" + refName) ;
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
		return transformer(Functions.beanCGIFunction(clz)) ;
//		return ToBeanStrategy.EasyByJson.toBean(this, clz) ;
	}
	
	
	public Rows toRows(String... cols){
		return transformer(Functions.rowsFunction(session, cols)) ;
	}
}
