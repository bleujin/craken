package net.ion.craken.tree;


import java.util.Map;
import java.util.Set;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.exception.NodeNotExistsException;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.config.ConfigurationException;
import org.infinispan.context.Flag;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class TreeCache<K, V> extends TreeStructureSupport {
	private static final Log log = LogFactory.getLog(TreeCache.class);
	private static final boolean trace = log.isTraceEnabled();

	private GridFilesystem gfs ;
	public TreeCache(Cache<PropertyId, PropertyValue> cache, GridFilesystem gfs) {
		this(cache.getAdvancedCache(), gfs);
	}

	private TreeCache(AdvancedCache<PropertyId, PropertyValue> cache, GridFilesystem gfs) {
		super(cache, cache.getBatchContainer());
		if (cache.getCacheConfiguration().indexing().enabled())
			throw new ConfigurationException("TreeCache cannot be used with a Cache instance configured to use indexing!");
		this.gfs = gfs ;
		assertBatchingSupported(cache.getCacheConfiguration());
		createRoot();
	}

	public TreeNode<PropertyId, PropertyValue> getRoot() {
		return getRoot(cache);
	}

	public TreeNode<PropertyId, PropertyValue> getRoot(Flag... flags) {
		return getRoot(cache.withFlags(flags));
	}

	private TreeNode<PropertyId, PropertyValue> getRoot(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache) {
		return createTreeNode(Fqn.ROOT, gfs, cache, batchContainer);
	}

	public TreeNode<PropertyId, PropertyValue> merge(Fqn fqn) {
		startAtomic();
		try {
			createNodeInCache(cache, fqn) ;
			return new TreeNodeImpl(fqn, gfs, cache, batchContainer);
		} finally {
			endAtomic();
		}
		
		
		
//		TreeNode<PropertyId, PropertyValue> n = getNode(cache, fqn);
//		if (n == null)
//			createNodeInCache(cache, fqn);
//		n = getNode(cache, fqn);
//		return n ;
	}

//	private TreeNode<PropertyId, PropertyValue> merge(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, Map<? extends PropertyId, ? extends PropertyValue> data) {
//		TreeNode<PropertyId, PropertyValue> n = getNode(cache, fqn);
//		if (n == null)
//			createNodeInCache(cache, fqn);
//		n = getNode(cache, fqn);
//		n.putAll(data);
//		return n ;
//	}
//
//	private V put(String fqn, K key, V value) {
//		return put(cache, Fqn.fromString(fqn), key, value);
//	}
//	
//	private V put(String fqn, K key, V value, Flag... flags) {
//		return put(cache.withFlags(flags), Fqn.fromString(fqn), key, value);
//	}
//	
//	private void put(Fqn fqn, Map<PropertyId, ? extends PropertyValue> data, Flag... flags) {
//		merge(cache.withFlags(flags), fqn, data);
//	}
//	
//	public void put(Fqn fqn, Map<? extends String, ?> data) {
//		Map<PropertyId, PropertyValue> modMap = MapUtil.newMap() ;
//		for (String key : data.keySet()) {
//			modMap.put(PropertyId.fromIdString(key), PropertyValue.createPrimitive(data.get(key))) ;
//		}
//		merge(cache, fqn, modMap);
//	}
//
//	private void put(String fqn, Map<? extends PropertyId, ? extends PropertyValue> data, Flag... flags) {
//		merge(cache.withFlags(flags), Fqn.fromString(fqn), data);
//	}

	public AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache(){
		return cache ;
	}
	
	
	public V remove(Fqn fqn, K key) {
		return remove(cache, fqn, key);
	}

	public V remove(Fqn fqn, K key, Flag... flags) {
		return remove(cache.withFlags(flags), fqn, key);
	}

	private V remove(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, K key) {
		AtomicMap<K, V> map = getAtomicMap(cache, new TreeNodeKey(fqn, TreeNodeKey.Type.DATA));
		return map == null ? null : map.remove(key);
	}

	public V remove(String fqn, K key) {
		return remove(cache, Fqn.fromString(fqn), key);
	}

	public V remove(String fqn, K key, Flag... flags) {
		return remove(cache.withFlags(flags), Fqn.fromString(fqn), key);
	}

	public boolean removeNode(Fqn fqn) {
		return removeNode(cache, fqn);
	}

	public boolean removeNode(Fqn fqn, Flag... flags) {
		return removeNode(cache.withFlags(flags), fqn);
	}

	public void begin() {
		super.startAtomic();
	}

	public void failEnd() {
		super.failAtomic();
	}

	public void end() {
		super.endAtomic();
	}

	private boolean removeNode(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
		if (fqn.isRoot())
			return false;
		boolean result;
		if (trace) log.tracef("About to remove node %s", fqn);
		TreeNode<PropertyId, PropertyValue> n = getNode(cache, fqn.getParent());
		result = n != null && n.removeChild(fqn.getLastElement());
		if (trace) log.trace("Node successfully removed");
		return result;
	}

	public boolean removeNode(String fqn) {
		return removeNode(cache, Fqn.fromString(fqn));
	}

	public boolean removeNode(String fqn, Flag... flags) {
		return removeNode(cache.withFlags(flags), Fqn.fromString(fqn));
	}
	
	
	
	
	

	private TreeNode<PropertyId, PropertyValue> getNode(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
		if (exists(fqn))
			return createTreeNode(fqn, gfs, cache, batchContainer);
		else
			return null;
	}

//	private TreeNode<PropertyId, PropertyValue> getNode(Fqn fqn) {
//		return getNode(cache, fqn);
//	}
//	
//	private TreeNode<PropertyId, PropertyValue> getNode(Fqn fqn, Flag... flags) {
//		return getNode(cache.withFlags(flags), fqn);
//	}
//	
//	private TreeNode<PropertyId, PropertyValue> getNode(String fqn) {
//		return getNode(cache, Fqn.fromString(fqn));
//	}
//
//	private TreeNode<PropertyId, PropertyValue> getNode(String fqn, Flag... flags) {
//		return getNode(cache.withFlags(flags), Fqn.fromString(fqn));
//	}

	private TreeNode<PropertyId, PropertyValue> createTreeNode(Fqn fqn, GridFilesystem gfs, AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, BatchContainer batchContainer) {
		return new TreeNodeImpl(fqn, gfs, cache, batchContainer);
	}

	
	
	
	
	
//	private V get(Fqn fqn, K key) {
//		return get(cache, fqn, key);
//	}
//
//	private V get(Fqn fqn, K key, Flag... flags) {
//		return get(cache.withFlags(flags), fqn, key);
//	}
//
//	private V get(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, K key) {
//		Map<K, V> m = getAtomicMap(cache, new TreeNodeKey(fqn, TreeNodeKey.Type.DATA));
//		if (m == null)
//			return null;
//		return m.get(key);
//	}

	
	
	
	
	
	
//	private boolean exists(String f) {
//		return exists(cache, Fqn.fromString(f));
//	}
//
//	private boolean exists(String fqn, Flag... flags) {
//		return exists(cache.withFlags(flags), Fqn.fromString(fqn));
//	}
//
//	private boolean exists(Fqn fqn, Flag... flags) {
//		return exists(cache.withFlags(flags), fqn);
//	}
//
//	private V get(String fqn, K key) {
//		return get(cache, Fqn.fromString(fqn), key);
//	}
//
//	private V get(String fqn, K key, Flag... flags) {
//		return get(cache.withFlags(flags), Fqn.fromString(fqn), key);
//	}

	
	
	public void move(Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
		move(cache, nodeToMoveFqn, newParentFqn);
	}

	public void move(Fqn nodeToMoveFqn, Fqn newParentFqn, Flag... flags) throws NodeNotExistsException {
		move(cache.withFlags(flags), nodeToMoveFqn, newParentFqn);
	}

	private void move(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
		if (trace) log.tracef("Moving node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
		if (nodeToMoveFqn == null || newParentFqn == null)
			throw new NullPointerException("Cannot accept null parameters!");

		if (nodeToMoveFqn.getParent().equals(newParentFqn)) {
			if (trace) log.trace("Not doing anything as this node is equal with its parent");
			// moving onto self! Do nothing!
			return;
		}

		// Depth first. Lets start with getting the node we want.
		boolean success = false;
		try {
			// check that parent's structure map contains the node to be moved. in case of optimistic locking this
			// ensures the write skew is properly detected if some other thread removes the child
			TreeNode<PropertyId, PropertyValue> parent = getNode(cache, nodeToMoveFqn.getParent());
			if (!parent.hasChild(nodeToMoveFqn.getLastElement())) {
				 if (trace) log.trace("The parent does not have the child that needs to be moved. Returning...");
				return;
			}
			TreeNode<PropertyId, PropertyValue> nodeToMove = getNode(cache.withFlags(Flag.FORCE_WRITE_LOCK), nodeToMoveFqn);
			if (nodeToMove == null) {
				if (trace) log.trace("Did not find the node that needs to be moved. Returning...");
				return; // nothing to do here!
			}
			if (!exists(newParentFqn)) {
				// then we need to silently create the new parent
				createNodeInCache(cache, newParentFqn);
				if (trace) log.tracef("The new parent (%s) did not exists, was created", newParentFqn);
			}

			// create an empty node for this new parent
			Fqn newFqn = Fqn.fromRelativeElements(newParentFqn, nodeToMoveFqn.getLastElement());
			createNodeInCache(cache, newFqn);
			TreeNode<PropertyId, PropertyValue> newNode = getNode(cache, newFqn);
			Map<PropertyId, PropertyValue> oldData = nodeToMove.getData();
			if (oldData != null && !oldData.isEmpty())
				newNode.putAll(oldData);
			for (Object child : nodeToMove.getChildrenNames()) {
				// move kids
				if (trace) log.tracef("Moving child %s", child);
				Fqn oldChildFqn = Fqn.fromRelativeElements(nodeToMoveFqn, child);
				move(cache, oldChildFqn, newFqn);
			}
			removeNode(cache, nodeToMoveFqn);
			success = true;
		} finally {
			if (!success) {
				failAtomic();
			}
		}
		log.tracef("Successfully moved node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
	}

	public void move(String nodeToMove, String newParent) throws NodeNotExistsException {
		move(cache, Fqn.fromString(nodeToMove), Fqn.fromString(newParent));
	}

	public void move(String nodeToMove, String newParent, Flag... flags) throws NodeNotExistsException {
		move(cache.withFlags(flags), Fqn.fromString(nodeToMove), Fqn.fromString(newParent));
	}

	public Map<PropertyId, PropertyValue> getData(Fqn fqn) {
		return getData(cache, fqn);
	}

	public Map<PropertyId, PropertyValue> getData(Fqn fqn, Flag... flags) {
		return getData(cache.withFlags(flags), fqn);
	}

	private Map<PropertyId, PropertyValue> getData(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
		TreeNode<PropertyId, PropertyValue> node = getNode(cache, fqn);
		if (node == null)
			return null;
		else
			return node.getData();
	}

	public Set<PropertyId> getKeys(String fqn) {
		return getKeys(cache, Fqn.fromString(fqn));
	}

	public Set<PropertyId> getKeys(String fqn, Flag... flags) {
		return getKeys(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	public Set<PropertyId> getKeys(Fqn fqn) {
		return getKeys(cache, fqn);
	}

	public Set<PropertyId> getKeys(Fqn fqn, Flag... flags) {
		return getKeys(cache.withFlags(flags), fqn);
	}

	private Set<PropertyId> getKeys(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
		TreeNode<PropertyId, PropertyValue> node = getNode(cache, fqn);
		if (node == null)
			return null;
		else
			return node.getKeys();
	}

	public void clearData(String fqn) {
		clearData(cache, Fqn.fromString(fqn));
	}

	public void clearData(String fqn, Flag... flags) {
		clearData(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	public void clearData(Fqn fqn) {
		clearData(cache, fqn);
	}

	public void clearData(Fqn fqn, Flag... flags) {
		clearData(cache.withFlags(flags), fqn);
	}

	public void clearData(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
		TreeNode<PropertyId, PropertyValue> node = getNode(cache, fqn);
		if (node != null)
			node.clearData();
	}

	public V put(Fqn fqn, K key, V value) {
		return put(cache, fqn, key, value);
	}

	public V put(Fqn fqn, K key, V value, Flag... flags) {
		return put(cache.withFlags(flags), fqn, key, value);
	}

	private V put(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, K key, V value) {
		createNodeInCache(cache, fqn);
		Map<K, V> m = getAtomicMap(cache, new TreeNodeKey(fqn, TreeNodeKey.Type.DATA));
		return m.put(key, value);
	}

	public Cache<TreeNodeKey, AtomicMap<?, ?>> getCache() {
		// Retrieve the advanced cache as a way to retrieve the cache behind the cache adapter.
		return cache.getAdvancedCache();
	}

	// ------------------ nothing different; just delegate to the cache
	public void start() throws CacheException {
		cache.start();
		createRoot();
	}

	public void stop() {
		cache.stop();
	}

	private void createRoot() {
		if (!exists(Fqn.ROOT))
			createNodeInCache(cache, Fqn.ROOT);
	}

	public String toString() {
		return cache.toString();
	}

	public GridFilesystem gfs() {
		return gfs;
	}
}
