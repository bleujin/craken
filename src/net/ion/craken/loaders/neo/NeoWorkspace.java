package net.ion.craken.loaders.neo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map.Entry;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderManager;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class NeoWorkspace extends Workspace {

	private NeoWorkspaceConfig config;
	private NeoWorkspaceStore nstore;
	private GraphDatabaseService graphDB;

	public NeoWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, NeoWorkspaceConfig config) {
		super(repository, cache, wsName, config);
		this.nstore = ((NeoWorkspaceStore) cache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore());
		this.config = config;
		this.graphDB = nstore.graphDB();
		
	}

	@Override
	public Central central() {
		return nstore.central();
	}

	@Override
	public WorkspaceConfig config() {
		return config;
	}

	@Override
	protected int storeData(InputStream input) throws IOException {
		final JsonReader reader = new JsonReader(new InputStreamReader(input, "UTF-8"));
		reader.beginObject();
		String configName = reader.nextName();
		final JsonObject config = reader.nextJsonObject();
		String logName = reader.nextName();

		Transaction tx = graphDB.beginTx();
		try {
			reader.beginArray();
			final int count = config.asInt("count");
			for (int i = 0; i < count; i++) {
				JsonObject tlog = reader.nextJsonObject();
				String path = tlog.asString("path");
				Touch touch = Touch.valueOf(tlog.asString("touch"));
				Action action = Action.valueOf(tlog.asString("action"));
				// Debug.line(path, touch) ;
				switch (touch) {
				case TOUCH:
					break;
				case MODIFY:
					JsonObject val = tlog.asJsonObject("val");
					if ("/".equals(path) && val.childSize() == 0)
						continue;

					Node mergeNode = mergeNode(graphDB, action, StringUtil.split(path, "/")) ;
					mergeNode.setProperty(EntryKey.LASTMODIFIED, System.currentTimeMillis()) ;
					mergeNode.setProperty(EntryKey.ID, path) ;
					for(Entry<String, JsonElement> entry : val.entrySet()){
						final PropertyId propertyId = PropertyId.fromIdString(entry.getKey());

						String propId = propertyId.getString();
						if (propertyId.type() == PType.NORMAL) {
							mergeNode.setProperty(propId, toNeoValue(entry)) ;
						} else if (propertyId.type() == PType.REFER) {
							mergeNode.setProperty('@' + propId, toNeoValue(entry)) ;
						}
					}

					break;
				case REMOVE:
					deleteNode(graphDB, action, path) ;
					break;
				case REMOVECHILDREN:
					deleteChildren(graphDB, action, findNode(graphDB, action, StringUtil.split(path, "/"))) ;
					break;
				default:
					throw new IllegalArgumentException("Unknown modification type " + touch);
				}
			}
			
			tx.success() ;
		} catch(Exception ex){
			ex.printStackTrace() ;
			tx.failure() ;
		} finally {
			tx.finish();
		}
		return 0;
	}

	private Object toNeoValue(Entry<String, JsonElement> entry) {
		JsonArray pvalue = entry.getValue().getAsJsonArray();
		
		Object[] values = pvalue.toObjectArray() ;
		if (values.length == 0) return null ;
		if (values.length == 1) return values[0] ; // String, Integer, Boolean, Float, Long, Double, Byte, Character, Short, Array
		
		Object firstValue = values[0] ;
		
		Object array = Array.newInstance(firstValue.getClass(), values.length);
		int index = 0 ;
		for (Object val : values) {
			Array.set(array, index++, val) ;
		}
		
		return array ; 
	}
	
	static boolean deleteChildren(GraphDatabaseService graphDB, Action action, String path){
		Node findNode = findNode(graphDB, action, StringUtil.split(path, "/"));
		if (findNode == null) return false ;
		deleteChildren(graphDB, action, findNode) ;
		return true;
	}
	
	static boolean deleteChildren(GraphDatabaseService graphDB, Action action, Node node){
		for (Relationship rel : findChildren(node)) {
			deleteChildren(graphDB, action, rel.getEndNode()) ;
		}
		node.delete() ;
		return true ;
	}
	
	static Iterable<Relationship> findChildren(Node parent){
		return parent.getRelationships(Direction.OUTGOING) ;
	}
	
	
	
	static boolean deleteNode(GraphDatabaseService graphDB, Action action, String path){
		Node current = rootNode(graphDB);
		String[] rels = StringUtil.split(path, "/") ;
		
		for (String rel : rels) {
			Relationship relation = current.getSingleRelationship(NeoWorkspace.relationType(rel), Direction.OUTGOING);
			if (relation == null) {
				return false;
			} else {
				current = relation.getEndNode();
			}
		}
		
		current.delete() ;
		return true;
		
	}

	private static Node rootNode(GraphDatabaseService graphDB) {
		return graphDB.getNodeById(0);
	}

	static Node findNode(GraphDatabaseService graphDB, Action action, String[] rels) {
		Node current = rootNode(graphDB);

		for (String rel : rels) {
			Relationship relation = current.getSingleRelationship(NeoWorkspace.relationType(rel), Direction.OUTGOING);
			if (relation == null) {
				return null;
			} else {
				current = relation.getEndNode();
			}
		}

		return current;
	}

	static Node mergeNode(GraphDatabaseService graphDB, Action action, String[] rels) throws CacheLoaderException {
		Node current = rootNode(graphDB);

		String path = "" ;
		for (String rel : rels) {
			Relationship relation = current.getSingleRelationship(relationType(rel), Direction.OUTGOING);
			path = path + "/" + rel ; 
			if (relation == null) {
				Transaction tx = graphDB.beginTx();
				try {
					Node newNode = graphDB.createNode();
					newNode.setProperty(EntryKey.ID, path) ;
					current.createRelationshipTo(newNode, relationType(rel));
					current = newNode;
					tx.success();
				} catch (Exception ex) {
					tx.failure();
					throw new CacheLoaderException(ex);
				} finally {
					tx.finish();
				}
			} else {
				current = relation.getEndNode();
			}
		}

		return current;
	}

	static RelationshipType relationType(final String rel) {
		return new RelationshipType() {
			@Override
			public String name() {
				return rel;
			}
		};
	}

	// use only test
	GraphDatabaseService graphDB() {
		return graphDB ;
	}

	public <T> T each(Function<Iterator<Node>, T> function) {
		return function.apply(graphDB.getAllNodes().iterator()) ;
	}
	
	public void debugPrint(){
		each(new Function<Iterator<Node>, Void>(){
			@Override
			public Void apply(Iterator<Node> iter) {
				while(iter.hasNext()){
					final Node next = iter.next();
					Debug.line(next, Iterators.toString(next.getPropertyKeys().iterator()),  Iterators.toString(next.getPropertyValues().iterator())) ;
				}
				return null;
			}
		}) ;
	}
	
}
