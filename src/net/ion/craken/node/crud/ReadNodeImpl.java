package net.ion.craken.node.crud;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.exception.NodeNotExistsException;
import net.ion.craken.tree.ExtendPropertyId;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.framework.db.Rows;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.search.filter.TermFilter;
import net.ion.rosetta.Parser;

import org.apache.commons.collections.IteratorUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class ReadNodeImpl implements ReadNode, Serializable {

	private static final long serialVersionUID = 1785904048897031227L;
	private transient ReadSession session ;
	private TreeNode tnode;
	protected ReadNodeImpl(ReadSession session, TreeNode tnode) {
		this.session = session ;
		this.tnode = tnode ;
	}

	public static ReadNode load(ReadSession session, TreeNode inner) {
		return new ReadNodeImpl(session, inner);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof ReadNodeImpl)) return false ;
		ReadNodeImpl that = (ReadNodeImpl) obj ;
		return tnode.equals(that.tnode) ;
	}
	
	@Override
	public int hashCode(){
		return tnode.hashCode() ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + tnode.fqn().toString() + "]";
	}

	
	// only use for test
	public TreeNode treeNode(){
		return tnode ;
	}
	
	// .. common 
	
	public ReadSession session(){
		return session ;
	}
	
	public Fqn fqn(){
		return tnode.fqn() ;
	}
	
	public int dataSize(){
		return tnode.keys().size() ;
	}

	public ReadNode parent(){
		return load(session, tnode.parent()) ;
	}
	
	public boolean hasChild(String relativeFqn){
		return tnode.hasChild(Fqn.fromString(relativeFqn)) ;
	}
	
	public ReadNode child(String fqn){
//		return session.pathBy(Fqn.fromRelativeFqn(this.fqn(), Fqn.fromString(fqn))) ;
		final TreeNode child = tnode.getChild(Fqn.fromString(fqn));
		if (child == null) throw new IllegalArgumentException("not found child : " + fqn) ; 
		return load(session, child) ;
	}
	
	
	public ReadNode root(){
		return session.root() ;
	}
	
	public Set<String> childrenNames(){
		Set<String> set = SetUtil.orderedSet(SetUtil.newSet());
		for (Object object : tnode.getChildrenNames()) {
			set.add(ObjectUtil.toString(object)) ;
		}
		return set ;
	}
	
	public ReadChildren children(){
		return new ReadChildren(session, tnode.getChildren().iterator()) ;
	}

	public PropertyValue property(String key) {
		return propertyId(PropertyId.normal(key)) ;
	}


	public PropertyValue extendProperty(String propPath) {
		return ExtendPropertyId.create(propPath).propValue(this) ;
	}

	
	public PropertyValue propertyId(PropertyId pid) {
		return ObjectUtil.coalesce(tnode.get(pid).gfs(gfs()), PropertyValue.NotFound);
	}

//	public Optional<PropertyValue> optional(String key) {
//		return Optional.fromNullable(treeNode.get(PropertyId.normal(key)));
//	}
	
	private GridFilesystem gfs(){
		return session.workspace().gfs() ;
	}
	
	public Set<PropertyId> keys(){
		return tnode.keys() ;
	}
	
	public Set<PropertyId> normalKeys(){
		return Sets.filter(keys(), new Predicate<PropertyId>(){
			@Override
			public boolean apply(PropertyId pid) {
				return pid.type() == PType.NORMAL ;
			}
		}) ;
	}



	public Map<PropertyId, PropertyValue> toMap() {
		return Collections.unmodifiableMap(tnode.readMap());
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
		return tnode.fqn() ;
	}
	
	public boolean hasProperty(PropertyId pid){
		return keys().contains(pid) ;
	}
	
	public boolean hasRef(String refName){
		return keys().contains(PropertyId.refer(refName)) ;
	}
	
	public boolean hasRef(String refName, Fqn fqn){
		return propertyId(PropertyId.refer(refName)).asSet().contains(fqn.toString()) ;
	}
	
	public ReadNode ref(String refName){
		PropertyId referId = PropertyId.refer(refName);
		if (hasProperty(referId)) {
			Object val = propertyId(referId).value() ;
			if (val == null ) throw new NodeNotExistsException("not found ref :" + refName) ;

			return session.ghostBy(val.toString()) ;
		} else {
			throw new NodeNotExistsException("not found ref :" + refName) ;
		}
	}
	
	public IteratorList<ReadNode> refs(String refName){
		
		PropertyId referId = PropertyId.refer(refName);
		final Iterator<String> iter = hasProperty(referId) ? propertyId(referId).asSet().iterator() : IteratorUtils.EMPTY_ITERATOR;
		
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
				return session.ghostBy(iter.next());
			}
		};
	}
	
	public <T> T toBean(Class<T> clz){
		return transformer(Functions.beanCGIFunction(clz)) ;
//		return ToBeanStrategy.EasyByJson.toBean(this, clz) ;
	}
	
	
	public Rows toRows(String expr){
		return transformer(Functions.rowsFunction(session, expr)) ;
	}
	
	public final static ReadNode ghost(ReadSession session, Fqn fqn){
		return new GhostReadNode(session, new GhostTreeNode(session, fqn));
	}

	
	@Override
	public ChildQueryRequest childQuery(String query) throws IOException, ParseException {
		if (StringUtil.isBlank(query)) return childQuery(new TermQuery(new Term(DocEntry.PARENT, this.fqn().toString()))) ;
		
		Analyzer analyzer = session.workspace().central().searchConfig().queryAnalyzer();
		final ChildQueryRequest result = ChildQueryRequest.create(session, session.newSearcher(), session.workspace().central().searchConfig().parseQuery(analyzer, query));
		result.filter(new TermFilter(DocEntry.PARENT, this.fqn().toString())) ;
		
		return result;
	} 
	
	
	public ChildQueryRequest childQuery(Query query) throws ParseException, IOException {
		return ChildQueryRequest.create(session, session.newSearcher(), query);
	}

	
	@Override
	public ChildQueryRequest childQuery(String query, boolean includeDecentTree) throws ParseException, IOException {
		if (! includeDecentTree) return childQuery(query) ;
		
		if (StringUtil.isBlank(query)) return childQuery(this.fqn().childrenQuery()) ;
		
		Analyzer analyzer = session().queryAnalyzer() ;
		final ChildQueryRequest result = ChildQueryRequest.create(session, session.newSearcher(), session.workspace().central().searchConfig().parseQuery(analyzer, query));
		result.filter(new QueryWrapperFilter(this.fqn().childrenQuery())) ;
		
		return result;
	}

	@Override
	public void template(String propId, Writer writer) throws IOException{
		Engine engine = session.workspace().parseEngine();
		String template = property(propId).stringValue();
		if (StringUtil.isBlank(template)) return ;
		
		String result = engine.transform(template, MapUtil.<String, Object>create("self", this)) ;
		writer.write(result) ;
	}
	
	public JsonObject toValueJson() {
		return tnode.toValueJson() ;
	}
	
}

class GhostReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -5073334525889136682L;
	GhostReadNode(ReadSession session, TreeNode inner) {
		super(session, inner) ;
	}
	
	@Override
	public <T> T toBean(Class<T> clz){
		return null;
	}
	
	@Override
	public Rows toRows(String expr){
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, expr);
		return AdNodeRows.create(session(), IteratorUtils.EMPTY_ITERATOR, sp);		
//		return FAKE ;
	}
}


class GhostTreeNode extends TreeNode {

	private ReadSession session ;
	GhostTreeNode(ReadSession session, Fqn fqn){
		super(session.workspace(), fqn) ;
		this.session = session ;
	}

	@Override
	public void clearData() {
		
	}

	@Override
	public int dataSize() {
		return 0;
	}

	@Override
	public PropertyValue get(PropertyId key) {
		return PropertyValue.NotFound;
	}


	@Override
	public TreeNode getChild(Fqn f) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}


	@Override
	public Set<TreeNode> getChildren() {
		return SetUtil.EMPTY;
	}


	@Override
	public Set<String> getChildrenNames() {
		return SetUtil.EMPTY;
	}


	@Override
	public Fqn fqn() {
		return super.fqn();
	}

	@Override
	public Set<PropertyId> keys() {
		return SetUtil.EMPTY;
	}

	@Override
	public TreeNode parent() {
		return ((ReadNodeImpl)session.ghostBy(super.fqn().getParent())).treeNode();
	}

	@Override
	public boolean hasChild(Fqn f) {
		return false;
	}

	@Override
	public boolean hasChild(Object o) {
		return false;
	}


	@Override
	public PropertyValue put(PropertyId key, PropertyValue value) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}


	@Override
	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	@Override
	public PropertyValue remove(PropertyId key) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}


	@Override
	public boolean removeChild(Fqn f) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	@Override
	public boolean removeChild(Object childName) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	@Override
	public void removeChildren() {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	@Override
	public PropertyValue replace(PropertyId key, PropertyValue value) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}


	@Override
	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	@Override
	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		throw new UnsupportedOperationException("current node is empty node") ;
	}

	
//	@Override
//	public Set<Object> getChildrenNames(Flag... flags) {
//		return SetUtil.EMPTY;
//	}
//
//	@Override
//	public Map<PropertyId, PropertyValue> getData(Flag... flags) {
//		return MapUtil.EMPTY;
//	}
//
//	@Override
//	public Set<PropertyId> getKeys(Flag... flags) {
//		return SetUtil.EMPTY;
//	}
//
//	@Override
//	public TreeNode<PropertyId, PropertyValue> getParent(Flag... flags) {
//		return ((ReadNodeImpl)session.pathBy(fqn.getParent(), true)).treeNode();
//	}

//	@Override
//	public boolean hasChild(Fqn f, Flag... flags) {
//		return false;
//	}
//	
//	@Override
//	public boolean hasChild(Object o, Flag... flags) {
//		return false;
//	}
//
//	@Override
//	public PropertyValue put(PropertyId key, PropertyValue value, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node") ;
//	}
//
//	@Override
//	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node") ;
//	}
//	@Override
//	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node") ;
//	}
//	@Override
//	public PropertyValue remove(PropertyId key, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
//
//	@Override
//	public boolean removeChild(Fqn f, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
//
//	@Override
//	public boolean removeChild(Object childName, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
//
//	@Override
//	public void removeChildren(Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
//
//	@Override
//	public PropertyValue replace(PropertyId key, PropertyValue value, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
//
//	@Override
//	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
//
//	@Override
//	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node");
//	}
	

//	@Override
//	public void clearData(Flag... flags) {
//		
//	}
//
//	@Override
//	public int dataSize(Flag... flags) {
//		return 0;
//	}
//
//	@Override
//	public PropertyValue get(PropertyId key, Flag... flags) {
//		return PropertyValue.NotFound;
//	}
//
//	@Override
//	public TreeNode<PropertyId, PropertyValue> getChild(Fqn f, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node") ;
//	}
//	
//	@Override
//	public TreeNode<PropertyId, PropertyValue> getChild(Object name, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node") ;
//	}
//	
//
//	@Override
//	public TreeNode<PropertyId, PropertyValue> addChild(Fqn f, Flag... flags) {
//		throw new UnsupportedOperationException("current node is empty node") ;
//	}
//
//	@Override
//	public Set<TreeNode<PropertyId, PropertyValue>> getChildren(Flag... flags) {
//		return SetUtil.EMPTY;
//	}

}