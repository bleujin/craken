package net.ion.craken.node.crud;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.convert.rows.FieldContext;
import net.ion.craken.node.convert.rows.FieldDefinition;
import net.ion.craken.node.crud.tree.ExtendPropertyId;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.NodeNotExistsException;
import net.ion.craken.node.crud.tree.TreeNode;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyId.PType;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.db.Rows;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.filter.TermFilter;
import net.ion.rosetta.Parser;

import org.apache.commons.collections.IteratorUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.infinispan.context.Flag;
import org.infinispan.io.GridFilesystem;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class ReadNodeImpl implements ReadNode, Serializable {

	private static final long serialVersionUID = 1785904048897031227L;
	private transient ReadSession session;
	private Fqn fqn;

	protected ReadNodeImpl(ReadSession session, Fqn fqn) {
		this.session = session;
		this.fqn = fqn;
	}

	public static ReadNode load(ReadSession session, Fqn fqn) {
		ReadNodeImpl result = new ReadNodeImpl(session, fqn);
//		if (result.treeNode() == null) throw new NotFoundPath(fqn) ;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ReadNodeImpl))
			return false;
		ReadNodeImpl that = (ReadNodeImpl) obj;
		return treeNode().equals(that.treeNode());
	}

	@Override
	public int hashCode() {
		return fqn.hashCode();
	}

	public String toString() {
		return this.getClass().getSimpleName() + "[fqn=" + fqn().toString() + "]";
	}

	// only use for test
	public TreeNode<PropertyId, PropertyValue> treeNode() {
		TreeNode result = session.workspace().readNode(fqn);
		if (result == null) return new GhostTreeNode(session, fqn) ;
		return result;
		
	}

	// .. common

	public ReadSession session() {
		return session;
	}

	public Fqn fqn() {
		return fqn;
	}

	public int dataSize() {
		return treeNode().dataSize();
	}

	public ReadNode parent() {
		return load(session, fqn.getParent());
	}

	public boolean hasChild(String relativeFqn) {
		String[] names = StringUtil.split(relativeFqn, "/") ;
		return treeNode().hasChild(Fqn.fromElements(names));
	}

	public ReadNode child(String name) {
		String[] names = StringUtil.split(name, "/") ;
		Fqn childFqn = Fqn.fromRelativeElements(fqn, names) ;
		
		return session.pathBy(childFqn) ;
	}

	public ReadNode root() {
		return session.root();
	}

	public Set<String> childrenNames() {
		Set<String> set = SetUtil.orderedSet(SetUtil.newSet());
		for (Object object : treeNode().getChildrenNames()) {
			set.add(ObjectUtil.toString(object));
		}
		return set;
	}

	public ReadChildren children() {
		return new ReadChildren(session, fqn, treeNode().getChildrenFqn().iterator());
	}

	public PropertyValue property(String key) {
		return propertyId(PropertyId.normal(key));
	}

	public PropertyValue extendProperty(String propPath) {
		return ExtendPropertyId.create(propPath).propValue(this);
	}

	public PropertyValue propertyId(PropertyId pid) {
		if (treeNode().get(pid) == null)
			return PropertyValue.NotFound;
		return ObjectUtil.coalesce(treeNode().get(pid).gfs(gfs()), PropertyValue.NotFound);
	}

	// public Optional<PropertyValue> optional(String key) {
	// return Optional.fromNullable(treeNode.get(PropertyId.normal(key)));
	// }

	private GridFilesystem gfs() {
		return session.workspace().gfs();
	}

	public Set<PropertyId> keys() {
		TreeNode<PropertyId, PropertyValue> treeNode = treeNode();
		return treeNode.getKeys();
	}

	public Set<PropertyId> normalKeys() {
		return Sets.filter(keys(), new Predicate<PropertyId>() {
			@Override
			public boolean apply(PropertyId pid) {
				return pid.type() == PType.NORMAL;
			}
		});
	}

	public Map<PropertyId, PropertyValue> toMap() {
		return treeNode().getData() ;
	}

	public <T> T transformer(Function<ReadNode, T> function) {
		return function.apply(this);
	}

	public Map<String, Object> toPropertyMap(final int descendantDepth) {
		final int childDepth = descendantDepth - 1;
		Map<String, Object> result = MapUtil.newMap();

		for (Entry<PropertyId, PropertyValue> entry : toMap().entrySet()) {
			if (entry.getKey().type() == PropertyId.PType.NORMAL) {
				result.put(entry.getKey().getString(), entry.getValue().asSet().size() <= 1 ? entry.getValue().value() : entry.getValue().asSet());
			} else if (entry.getKey().type() == PropertyId.PType.REFER && descendantDepth > 0) {
				IteratorList<ReadNode> refs = refs(entry.getKey().getString());
				Set<Map<String, Object>> set = SetUtil.orderedSet(SetUtil.newSet());
				while (refs.hasNext()) {
					set.add(refs.next().toPropertyMap(childDepth));
				}
				result.put('@' + entry.getKey().getString(), set);
			}
		}

		IteratorList<ReadNode> children = children().iterator();
		if (descendantDepth > 0 && children.hasNext()) {
			while (children.hasNext()) {
				final ReadNode next = children.next();
				result.put('/' + next.fqn().getLastElementAsString(), next.toPropertyMap(childDepth));
			}
		}

		return Collections.unmodifiableMap(result);
	}

	public Object id() {
		return fqn;
	}

	public boolean hasProperty(String pid) {
		return hasPropertyId(PropertyId.fromIdString(pid));
	}

	public boolean hasPropertyId(PropertyId pid) {
		return keys().contains(pid);
	}

	public boolean hasRef(String refName) {
		return keys().contains(PropertyId.refer(refName));
	}

	public boolean hasRef(String refName, Fqn fqn) {
		return propertyId(PropertyId.refer(refName)).asSet().contains(fqn.toString());
	}

	public ReadNode ref(String refName) {
		// PropertyValue findProp = propertyId(PropertyId.refer(refName)) ;
		// if (findProp == PropertyValue.NotFound) throw new NodeNotExistsException("not found ref :" + refName) ;
		// return session().pathBy(Fqn.fromString(findProp.stringValue()));

		PropertyId referId = PropertyId.refer(refName);
		if (hasPropertyId(referId)) {
			String refPath = propertyId(referId).stringValue();
			if (StringUtil.isBlank(refPath))
				throw new NodeNotExistsException("not found ref :" + refName);

			return session.ghostBy(refPath);
		} else {
			throw new NodeNotExistsException("not found ref :" + refName);
		}
	}

	public IteratorList<ReadNode> refs(String refName) {
		PropertyId referId = PropertyId.refer(refName);
		final Set values = hasPropertyId(referId) ? propertyId(referId).asSet() : SetUtil.EMPTY_SET;
		final Iterator<String> iter = values.iterator();

		return new IteratorList<ReadNode>() {
			@Override
			public List<ReadNode> toList() {
				List<ReadNode> result = ListUtil.newList();
				while (iter.hasNext()) {
					result.add(session.pathBy(iter.next()));
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

			@Override
			public Iterator<ReadNode> iterator() {
				return this;
			}

			public int count() {
				return values.size();
			}
		};
	}

	public ReadChildren refChildren(String refName) {
		Iterator<Fqn> titer = treeNode().getReferencesFqn(refName).iterator();
		return new ReadChildren(session, fqn, titer);
	}

	public WalkRefChildren walkRefChildren(String refName) {
		return new WalkRefChildren(session, fqn, refName, treeNode().getReferencesFqn(refName).iterator());
	}
	
	public ReadChildren walkRefMeChildren(String refName, Fqn parentFqn) {
		ReadNode parent = session.ghostBy(parentFqn) ;
		ReadNode currentNode = this ;
		
		List<Fqn> currentDepth = ListUtil.newList() ;
		currentDepth.add(currentNode.fqn()) ;
		
		List<Fqn> result = ListUtil.newList() ;
	
		refMeChildren(result, currentDepth, refName, parent.children().toList()) ;
		return new ReadChildren(session, fqn, result.iterator());
	}

	private void refMeChildren(List<Fqn> result, List<Fqn> currentDepth, String refName, List<ReadNode> children) {
		List<Fqn> newDepth = ListUtil.newList() ;
		for(ReadNode child : children){
			for (Fqn cfqn : currentDepth) {
				if (child.hasRef(refName, cfqn)) {
					result.add(child.fqn()) ;
					newDepth.add(child.fqn()) ;
					break ;
				}
			}
		}
		if (newDepth.size() == 0) return ;
		refMeChildren(result, newDepth, refName, children) ;
	}

	public WalkReadChildren walkChildren() {
		return new WalkReadChildren(session, fqn, treeNode().getChildrenFqn().iterator());
	}

	public <T> T toBean(Class<T> clz) {
		return transformer(Functions.beanCGIFunction(clz));
	}

	public Rows toRows(String expr, FieldDefinition... fieldDefinitons) {
		return transformer(Functions.rowsFunction(session, expr, fieldDefinitons));
	}

	public final static ReadNode ghost(ReadSession session, Fqn fqn) {
		return new GhostReadNode(session, fqn);
	}

	@Override
	public ChildQueryRequest childQuery(String query) throws IOException {
		if (StringUtil.isBlank(query))
			return childQuery(new TermQuery(new Term(EntryKey.PARENT, this.fqn().toString())));

		Central central = session.workspace().central();
		Analyzer analyzer = central.searchConfig().queryAnalyzer();
		try {
			final ChildQueryRequest result = ChildQueryRequest.create(session, session.newSearcher(), central.searchConfig().parseQuery(central.indexConfig(), analyzer, query));
			result.filter(new TermFilter(EntryKey.PARENT, this.fqn().toString()));

			return result;
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
	
	public ChildQueryRequest childTermQuery(String name, String value, boolean includeDecentTree) throws IOException, ParseException {
		if (StringUtil.isBlank(name) || StringUtil.isBlank(value)) throw new ParseException(String.format("not defined name or value[%s:%s]", name, value)) ;
		
		final ChildQueryRequest result = ChildQueryRequest.create(session, session.newSearcher(), new TermQuery(new Term(name, value)));
		if (includeDecentTree){
			result.filter(new QueryWrapperFilter(this.fqn().childrenQuery()));
		} else {
			result.filter(new TermFilter(EntryKey.PARENT, this.fqn().toString()));
		}
		return result;
	}

	public ChildQueryRequest childQuery(Query query) throws IOException {
		return ChildQueryRequest.create(session, session.newSearcher(), query);
	}

	public ChildQueryRequest childQuery(Query query, boolean includeDecentTree) throws IOException {
		if (!includeDecentTree)
			return childQuery(query);

		Analyzer analyzer = session().queryAnalyzer();
		final ChildQueryRequest result = ChildQueryRequest.create(session, session.newSearcher(), query);
		result.filter(new QueryWrapperFilter(this.fqn().childrenQuery()));

		return result;
	}

	@Override
	public ChildQueryRequest childQuery(String query, boolean includeDecentTree) throws IOException {
		if (!includeDecentTree)
			return childQuery(query);

		if (StringUtil.isBlank(query))
			return childQuery(this.fqn().childrenQuery());

		try {
			Analyzer analyzer = session().queryAnalyzer();
			Central central = session.workspace().central();
			final ChildQueryRequest result = ChildQueryRequest.create(session, session.newSearcher(), central.searchConfig().parseQuery(central.indexConfig(), analyzer, query));
			result.filter(new QueryWrapperFilter(this.fqn().childrenQuery()));
			return result;
		} catch (ParseException e) {
			throw new IOException(e);
		}

	}

	@Override
	public RefQueryRequest refsToMe(String refName) throws IOException {
		if (StringUtil.isBlank(refName))
			throw new IllegalArgumentException("must required refName") ;

		Analyzer analyzer = session.workspace().central().searchConfig().queryAnalyzer();
		final RefQueryRequest result = RefQueryRequest.createMe(session, session.newSearcher(), fqn(), refName);
//		result.filter(new TermFilter(EntryKey.PARENT, this.fqn().toString()));

		return result;
	}

	@Override
	public RefQueryRequest refsToChildren(String refName) throws IOException {
		if (StringUtil.isBlank(refName))
			throw new IllegalArgumentException("must required refName") ;

		Analyzer analyzer = session.workspace().central().searchConfig().queryAnalyzer();
		final RefQueryRequest result = RefQueryRequest.createChildren(session, session.newSearcher(), fqn(), refName);
//		result.filter(new TermFilter(EntryKey.PARENT, this.fqn().toString()));

		return result;
	}


	
	
	@Override
	public void template(String propId, Writer writer) throws IOException {
		Engine engine = session.workspace().parseEngine();
		String template = property(propId).stringValue();
		if (StringUtil.isBlank(template))
			return;

		String result = engine.transform(template, MapUtil.<String, Object> create("self", this));
		writer.write(result);
	}

	public JsonObject toValueJson() {
		JsonObject result = new JsonObject();
		for (Entry<PropertyId, PropertyValue> prop : toMap().entrySet()) {
			result.add(prop.getKey().idString(), prop.getValue().json()); 
		}
		
		return result ;
	}

	public boolean isGhost() {
		return false;
	}

	@Override
	public void debugPrint() {
		transformer(Functions.READ_DEBUGPRINT);
	}
	
	public boolean isMatch(String key, String value) throws IOException{
		return this.property(key).defaultValue("").equals(session.encrypt(value)) ;
	}

}

class GhostReadNode extends ReadNodeImpl {

	private static final long serialVersionUID = -5073334525889136682L;
	private GhostTreeNode gnode ;

	GhostReadNode(ReadSession session, Fqn fqn) {
		super(session, fqn);
		this.gnode = new GhostTreeNode(session, fqn);
	}

	@Override
	public <T> T toBean(Class<T> clz) {
		return null;
	}

	public boolean isGhost() {
		return true;
	}

	@Override
	public Rows toRows(String expr, FieldDefinition... fieldDefinitons) {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, expr);
		FieldContext fcontext = new FieldContext() ;
		sp.add(fcontext, fieldDefinitons) ;
		
		return AdNodeRows.create(session(), IteratorUtils.EMPTY_ITERATOR, sp);
		// return FAKE ;
	}
	
	// only use for test
	public TreeNode<PropertyId, PropertyValue> treeNode() {
		return gnode ;
	}
}

class GhostTreeNode implements TreeNode<PropertyId, PropertyValue> {

	private ReadSession session;
	private Fqn fqn;

	GhostTreeNode(ReadSession session, Fqn fqn) {
		this.session = session;
		this.fqn = fqn ;
	}


	@Override
	public Set<TreeNode<PropertyId, PropertyValue>> getChildren() {
		return SetUtil.EMPTY;
	}

	@Override
	public Set<TreeNode<PropertyId, PropertyValue>> getChildren(Flag... flags) {
		return SetUtil.EMPTY;
	}

	@Override
	public Set<Object> getChildrenNames() {
		return SetUtil.EMPTY;
	}

	@Override
	public Set<Object> getChildrenNames(Flag... flags) {
		return SetUtil.EMPTY;
	}

	@Override
	public Map<PropertyId, PropertyValue> getData() {
		return MapUtil.EMPTY;
	}

	@Override
	public Map<PropertyId, PropertyValue> getData(Flag... flags) {
		return MapUtil.EMPTY;
	}

	@Override
	public Set<PropertyId> getKeys() {
		return SetUtil.EMPTY;
	}

	@Override
	public Set<PropertyId> getKeys(Flag... flags) {
		return SetUtil.EMPTY;
	}

	@Override
	public Fqn getFqn() {
		return fqn;
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> addChild(Fqn f) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> addChild(Fqn f, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public boolean removeChild(Fqn f) {
		return false;
	}

	@Override
	public boolean removeChild(Fqn f, Flag... flags) {
		return false;
	}

	@Override
	public boolean removeChild(Object childName) {
		return false;
	}

	@Override
	public boolean removeChild(Object childName, Flag... flags) {
		return false;
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Fqn f) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Fqn f, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Object name) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Object name, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue put(PropertyId key, PropertyValue value) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue put(PropertyId key, PropertyValue value, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue replace(PropertyId key, PropertyValue value) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue replace(PropertyId key, PropertyValue value, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map, Flag... flags) {
		throw new UnsupportedOperationException("current node is ghost node");
	}

	@Override
	public PropertyValue get(PropertyId key) {
		return PropertyValue.NotFound;
	}

	@Override
	public PropertyValue get(PropertyId key, Flag... flags) {
		return PropertyValue.NotFound;
	}

	@Override
	public PropertyValue remove(PropertyId key) {
		return PropertyValue.NotFound;
	}

	@Override
	public PropertyValue remove(PropertyId key, Flag... flags) {
		return PropertyValue.NotFound;
	}

	@Override
	public void clearData() {
	}

	@Override
	public void clearData(Flag... flags) {
	}

	@Override
	public int dataSize() {
		return 0;
	}

	@Override
	public int dataSize(Flag... flags) {
		return 0;
	}

	@Override
	public boolean hasChild(Fqn f) {
		return false;
	}

	@Override
	public boolean hasChild(Fqn f, Flag... flags) {
		return false;
	}

	@Override
	public boolean hasChild(Object o) {
		return false;
	}

	@Override
	public boolean hasChild(Object o, Flag... flags) {
		return false;
	}

	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public void removeChildren() {

	}

	@Override
	public void removeChildren(Flag... flags) {
	}

	@Override
	public Set<Fqn> getChildrenFqn() {
		return SetUtil.EMPTY;
	}

	@Override
	public Set<Fqn> getReferencesFqn(String refName) {
		return SetUtil.EMPTY;
	}
	
	public boolean isProxyStatus(){
		return false ;
	}

	public void proxyStatus(boolean b){
		; //
	}

}