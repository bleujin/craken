package net.ion.craken.node.crud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.exception.NodeIOException;
import net.ion.craken.tree.ExtendPropertyId;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.GridBlob;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.search.filter.TermFilter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.infinispan.io.GridFilesystem;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.sun.corba.se.spi.orbutil.threadpool.Work;

public class WriteNodeImpl implements WriteNode {

	private WriteSession wsession;
	private TreeNode tnode;

	public enum Touch implements PropertyValue.ReplaceValue<String> {
		MODIFY, REMOVE, REMOVECHILDREN, TOUCH;

		@Override
		public String replaceValue() {
			return this.toString();
		}

		public VType vtype() {
			return VType.STR;
		}
	}

	private WriteNodeImpl(WriteSession wsession, TreeNode tnode) {
		this.wsession = wsession;
		this.tnode = tnode;
	}

	public static WriteNode loadTo(WriteSession wsession, TreeNode tnode) { // by pathBy
		return loadTo(wsession, tnode, Touch.TOUCH);
	}

	public static WriteNode loadTo(WriteSession wsession, TreeNode tnode, Touch touch) { // by pathBy
		final WriteNodeImpl result = new WriteNodeImpl(wsession, tnode);
		wsession.notifyTouch(result, result.fqn(), touch, MapUtil.EMPTY);
		return result;
	}

	public WriteNode load(WriteSession wsession, TreeNode tnode) {
		final WriteNodeImpl result = new WriteNodeImpl(wsession, tnode);
		wsession.notifyTouch(result, result.fqn(), Touch.TOUCH, MapUtil.EMPTY);
		return result;
	}

	protected TreeNode tree() {
		return tnode;
	}

	public WriteSession session() {
		return wsession;
	}

	private PropertyId createNormalId(String key) {
		return PropertyId.normal(key);
	}

	private PropertyId createReferId(String key) {
		return PropertyId.refer(key);
	}

	public ReadNode toReadNode() {
		return new ReadNodeImpl(wsession.readSession(), tnode);
	}

	public WriteNode property(String key, Object value) {
		if (value instanceof PropertyValue) {
			return property(createNormalId(key), (PropertyValue) value);
		} else if (value != null && value.getClass().isArray()) {

			int length = Array.getLength(value);
			Set set = SetUtil.newOrdereddSet();
			for (int i = 0; i < length; i++) {
				set.add(Array.get(value, i));
			}
			return property(PropertyId.normal(key), PropertyValue.createPrimitive(set));
		}
		return property(createNormalId(key), PropertyValue.createPrimitive(value));
	}

	public WriteNode encrypt(String key, String value) throws IOException {
		property(key, readSession().encrypt(value)) ;

		return this;
	}

	public WriteNode property(PropertyId pid, PropertyValue pvalue) {
		touch(Touch.MODIFY);

		tree().put(pid, pvalue);
		return this;
	}

	public WriteNode propertyIfAbsent(String key, Object value) {
		touch(Touch.MODIFY);

		tree().putIfAbsent(createNormalId(key), PropertyValue.createPrimitive(value));
		return this;
	}

	public PropertyValue propertyIfAbsentEnd(String key, Object value) {
		touch(Touch.MODIFY);

		return ObjectUtil.coalesce(tree().putIfAbsent(createNormalId(key), PropertyValue.createPrimitive(value)), PropertyValue.NotFound);
	}

	public PropertyValue replace(String key, Object value) {
		touch(Touch.MODIFY);
		return ObjectUtil.coalesce(tree().replace(createNormalId(key), PropertyValue.createPrimitive(value)), PropertyValue.NotFound);
	}

	public boolean replace(String key, Object oldValue, Object newValue) {
		touch(Touch.MODIFY);
		return tree().replace(createNormalId(key), PropertyValue.createPrimitive(oldValue), PropertyValue.createPrimitive(newValue));
	}

	public WriteNode append(String key, Object value) {
		return append(key, new Object[] { value });
	}

	public WriteNode append(String key, Object... value) {
		touch(Touch.MODIFY);
		PropertyValue findValue = property(key);
		if (findValue == PropertyValue.NotFound)
			findValue = PropertyValue.createBlank();

		findValue.append(value);

		tree().put(createNormalId(key), findValue);
		return this;
	}

	@Override
	public WriteNode unset(String key, Object value) {
		return unset(key, new Object[] { value });
	}

	@Override
	public WriteNode refTos(String refName, String fqn) {
		return refTos(refName, new String[] { fqn });
	}

	@Override
	public WriteNode unRefTos(String refName, String fqn) {
		return unRefTos(refName, new String[] { fqn });
	}

	public WriteNode unset(String key, Object... values) {
		touch(Touch.MODIFY);
		final PropertyId propId = createNormalId(key);
		PropertyValue pvalue = tree().remove(propId);

		if (pvalue == null)
			return this;

		if (values != null && values.length > 0) {
			pvalue.remove(values);
			property(propId, pvalue);
		}

		if (pvalue.isBlob()) {
			File file = gfs().getFile(pvalue.asBlob().path());
			if (file.exists()) file.delete() ;
		}
		return this;
	}

	public WriteNode clear() {
		touch(Touch.MODIFY);

		removeBlobIfExist();
		tree().clearData();
		return this;
	}

	private void removeBlobIfExist() {
		for (PropertyId pid : keys()) {
			PropertyValue pvalue = propertyId(pid);
			if (pvalue.isBlob()) {
				File file = gfs().getFile(pvalue.asBlob().path());
				if (file.exists()) file.delete() ;
			}
		}
	}

	public WriteNode blob(String key, File file) {
		try {
			return blob(key, new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new NodeIOException(e);
		}
	}

	public WriteNode blob(String key, InputStream input) {
		try {
			final String path = fqn().toString() + "/" + key + ".dat";
			
			PropertyValue gtvalue = GridBlob.create(gfs(), path).saveAt(input).asPropertyValue() ;
			property(PropertyId.normal(key), gtvalue);

		} catch (IOException e) {
			throw new NodeIOException(e);
		} finally {
			IOUtil.closeSilent(input);
		}

		return this;
	}

	public WriteNode addChild(String relativeFqn) {
		return wsession.pathBy(Fqn.fromRelativeFqn(fqn(), Fqn.fromString(relativeFqn)));
	}

	public boolean removeSelf() {
		removeChildren() ;
		return parent().removeChild(fqn().name());
	}

	public boolean removeChild(String childPath) {
		final Fqn target = Fqn.fromRelativeFqn(fqn(), Fqn.fromString(childPath));

		// WriteNodeImpl found = (WriteNodeImpl) wsession.pathBy(target) ;
		// found.removeBlobIfExist();

		Map<String, Fqn> removed = tree().removeChild(target.name());
		touchRemoved(Touch.REMOVE, target, removed);
		return removed.size() > 0;
	}

	public boolean removeChildren() {
		Map<String, Fqn> removed = tree().removeChildren();
		touchRemoved(Touch.REMOVECHILDREN, this.fqn(), removed);
		return removed.size() > 0;
	}

	public boolean hasProperty(String pid) {
		return hasPropertyId(PropertyId.fromIdString(pid));
	}

	public boolean hasPropertyId(PropertyId pid) {
		return keys().contains(pid);
	}

	public WriteNode ref(String refName) {
		// PropertyValue findProp = propertyId(PropertyId.refer(refName)) ;
		// if (findProp == PropertyValue.NotFound) throw new IllegalArgumentException("not found ref :" + refName) ;
		// return session().pathBy(Fqn.fromString(findProp.stringValue()));

		PropertyId referId = createReferId(refName);
		if (hasPropertyId(referId)) {
			String refPath = propertyId(referId).stringValue();
			if (StringUtil.isBlank(refPath))
				throw new IllegalArgumentException("not found ref :" + refName);
			return wsession.pathBy(refPath);
		} else {
			throw new IllegalArgumentException("not found ref :" + refName);
		}
	}

	public IteratorList<WriteNode> refs(String refName) {

		PropertyId referId = createReferId(refName);
		final Set values = hasPropertyId(referId) ? propertyId(referId).asSet() : SetUtil.EMPTY;
		final Iterator<String> iter = values.iterator();

		return new IteratorList<WriteNode>() {
			@Override
			public List<WriteNode> toList() {
				List<WriteNode> result = ListUtil.newList();
				while (iter.hasNext()) {
					result.add(wsession.pathBy(iter.next()));
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public WriteNode next() {
				return wsession.pathBy(iter.next());
			}

			@Override
			public Iterator<WriteNode> iterator() {
				return this;
			}

			public int count() {
				return values.size();
			}
		};
	}

	public WriteChildren refChildren(String refName) {
		final Iterator<String> refIter = propertyId(PropertyId.refer(refName)).asSet().iterator();
		Iterator<TreeNode> titer = new Iterator<TreeNode>() {
			@Override
			public boolean hasNext() {
				return refIter.hasNext();
			}

			@Override
			public TreeNode next() {
				return TreeNode.create(session().workspace(), Fqn.fromString(refIter.next()));
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("readonly");
			}
		};

		return new WriteChildren(session(), tnode, titer);
	}

	public WriteNode fromJson(JsonObject json) {
		for (Entry<String, JsonElement> entry : json.entrySet()) {
			append(this, entry.getKey(), entry.getValue());
		}

		return this;
	}

	private void append(WriteNode that, String propId, JsonElement json) {
		if (json.isJsonNull()) {
			return;
		} else if (json.isJsonPrimitive()) {
			if (propId.startsWith("@")) {
				that.refTos(propId.substring(1), json.getAsJsonPrimitive().getAsString());
			} else {
				that.append(propId, json.getAsJsonPrimitive().getValue());
			}
		} else if (json.isJsonArray()) {
			for (JsonElement jele : json.getAsJsonArray().toArray()) {
				append(that, propId, jele);
			}
		} else if (json.isJsonObject()) {
			for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
				append(that.child(propId), entry.getKey(), entry.getValue());
			}
		}
	}

	public WriteNode refTo(String refName, String fqn) {
		PropertyId referId = createReferId(refName);
		if (StringUtil.isBlank(fqn))
			tree().remove(referId);
		else
			tree().put(referId, PropertyValue.createPrimitive(fqn));

		if (!refName.startsWith("__"))
			touch(Touch.MODIFY);
		return this;
	}

	private GridFilesystem gfs() {
		return wsession.workspace().gfs();
	}

	public WriteNode refTos(String refName, String... fqns) {

		PropertyId referId = createReferId(refName);
		PropertyValue findValue = propertyId(referId);
		if (findValue == PropertyValue.NotFound)
			findValue = PropertyValue.createBlank();

		for (String fqn : fqns) {
			findValue.append(fqn);
		}

		tree().put(referId, findValue);
		touch(Touch.MODIFY);

		return this;
	}

	public WriteNode unRefTos(String refName, String... fqns) {
		PropertyId referId = createReferId(refName);
		PropertyValue findValue = propertyId(referId);
		if (fqns == null || fqns.length == 0 || findValue == PropertyValue.NotFound) {
			tree().remove(referId);
		} else {
			Set<String> removedRefs = tree().remove(referId).asSet();
			for (String ref : removedRefs) {
				if (!ArrayUtil.contains(fqns, ref))
					refTos(refName, ref);
			}
		}

		touch(Touch.MODIFY);
		return this;
	}

	// common
	public Fqn fqn() {
		return tree().fqn();
	}

	public int dataSize() {
		return tree().dataSize();
	}

	public WriteNode parent() {
		return load(session(), tree().parent());
	}

	public <T> T transformer(Function<WriteNode, T> function) {
		return function.apply(this);
	}

	public boolean hasChild(String fqn) {
		return tree().hasChild(Fqn.fromString(fqn));
	}

	public WriteNode child(String fqn) {
		return wsession.pathBy(Fqn.fromRelativeFqn(this.fqn(), Fqn.fromString(fqn)));
		// return load(wsession(), tree().getChild(Fqn.fromString(fqn))) ;
	}

	public WriteNode root() {
		return wsession.root();
	}

	public Set<String> childrenNames() {
		Set<String> set = SetUtil.orderedSet(SetUtil.newSet());
		for (Object object : tree().getChildrenNames()) {
			set.add(ObjectUtil.toString(object));
		}
		return set;
	}

	public Set<PropertyId> keys() {
		return tree().keys();
	}

	public Set<PropertyId> normalKeys() {
		return Sets.filter(keys(), new Predicate<PropertyId>() {
			@Override
			public boolean apply(PropertyId pid) {
				return pid.type() == PType.NORMAL;
			}
		});
	}

	public boolean hasRef(String refName) {
		return keys().contains(createReferId(refName));
	}

	public boolean hasRef(String refName, Fqn fqn) {
		return propertyId(createReferId(refName)).asSet().contains(fqn.toString());
	}

	public PropertyValue property(String key) {
		return propertyId(createNormalId(key));
	}

	public PropertyValue increase(String key) {
		property(key, property(key).asLong(0) + 1);
		return property(key);
	}

	public PropertyValue extendProperty(String propPath) {
		return ExtendPropertyId.create(propPath).propValue(this);
	}

	public PropertyValue propertyId(PropertyId pid) {
		PropertyValue result = ObjectUtil.coalesce(tree().get(pid), PropertyValue.NotFound);
		if (result.isBlob())
			result.gfs(wsession.workspace().gfs());
		return result;
	}

	public Map<PropertyId, PropertyValue> toMap() {
//		return tnode.readMap() ;
		try {
			Map<PropertyId, PropertyValue> readMap = tnode.readMap();
			return new HashMap(readMap) ;
		} catch(IllegalStateException e){ // concurrent removed
			return MapUtil.EMPTY ;
		}
//		return Collections.unmodifiableMap(readMap);
	}

	public Object id() {
		return fqn();
	}

	public String toString() {
		return this.getClass().getSimpleName() + "[fqn=" + tree().fqn().toString() + ", " + this.fqn().dataKey().action() + "]";
	}

	public WriteChildren children() {
		final Iterator<TreeNode> iter = tree().getChildren().iterator();
		return new WriteChildren(session(), tnode, iter);
	}

	private void touch(Touch touch) {
		session().notifyTouch(this, this.fqn(), touch, MapUtil.create(fqn().toString(), fqn()));
	}

	private void touchRemoved(Touch touch, Fqn target, Map<String, Fqn> affected) {
		session().notifyTouch(this, target, touch, affected);
	}

	private ReadSession readSession() {
		return wsession.readSession();
	}

	public ChildQueryRequest childQuery(Query query) throws ParseException, IOException {
		return ChildQueryRequest.create(readSession(), readSession().newSearcher(), query);
	}

	public ChildQueryRequest childQuery(String query) throws IOException, ParseException {
		if (StringUtil.isBlank(query))
			return childQuery(new TermQuery(new Term(EntryKey.PARENT, this.fqn().toString())));

		Analyzer analyzer = readSession().workspace().central().searchConfig().queryAnalyzer();
		final ChildQueryRequest result = ChildQueryRequest.create(readSession(), readSession().newSearcher(), readSession().workspace().central().searchConfig().parseQuery(analyzer, query));
		result.filter(new TermFilter(EntryKey.PARENT, this.fqn().toString()));

		return result;
	}

	public ChildQueryRequest childQuery(String query, boolean includeDecentTree) throws ParseException, IOException {
		if (!includeDecentTree)
			return childQuery(query);

		if (StringUtil.isBlank(query))
			return childQuery(this.fqn().childrenQuery());

		Analyzer analyzer = readSession().queryAnalyzer();
		final ChildQueryRequest result = ChildQueryRequest.create(readSession(), readSession().newSearcher(), session().workspace().central().searchConfig().parseQuery(analyzer, query));
		result.filter(new QueryWrapperFilter(this.fqn().childrenQuery()));

		return result;
	}

	
	public WriteNode reindex(boolean includeSub){
		wsession.workspace().reindex(this, wsession.workspace().central().indexConfig().indexAnalyzer(), includeSub) ;
		
		return this ;
	}
	
	public WriteNode reindex(Analyzer anal, boolean includeSub){
		wsession.workspace().reindex(this, anal, includeSub) ;
		return this ;
	}
}
