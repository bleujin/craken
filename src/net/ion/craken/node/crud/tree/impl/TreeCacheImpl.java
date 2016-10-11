package net.ion.craken.node.crud.tree.impl;

import java.util.Map;
import java.util.Set;

import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.NodeNotExistsException;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.TreeNode;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.CacheException;
import org.infinispan.context.Flag;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 4.0
 */
public class TreeCacheImpl<K, V> extends TreeStructureSupport implements TreeCache<K, V> {
	private static final Log log = LogFactory.getLog(TreeCacheImpl.class);
	private static final boolean trace = log.isTraceEnabled();

	public TreeCacheImpl(Cache<?, ?> cache, ProxyHandler proxyHandler) {
		this(cache.getAdvancedCache(), proxyHandler);
	}

	public TreeCacheImpl(AdvancedCache<?, ?> cache, ProxyHandler proxyHandler) {
		super(cache, cache.getBatchContainer(), proxyHandler);
		if (cache.getCacheConfiguration().indexing().index().isEnabled())
			throw new CacheConfigurationException("TreeCache cannot be used with a Cache instance configured to use indexing!");
		assertBatchingSupported(cache.getCacheConfiguration());
		createRoot();
	}

	@Override
	public TreeNode<K, V> getRoot() {
		return getRoot(cache);
	}

	@Override
	public TreeNode<K, V> getRoot(Flag... flags) {
		return getRoot(cache.withFlags(flags));
	}

	private TreeNode<K, V> getRoot(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache) {
		return createTreeNode(cache, Fqn.ROOT);
	}

	@Override
	public V put(String fqn, K key, V value) {
		return put(cache, Fqn.fromString(fqn), key, value);
	}

	@Override
	public V put(String fqn, K key, V value, Flag... flags) {
		return put(cache.withFlags(flags), Fqn.fromString(fqn), key, value);
	}

	@Override
	public void put(Fqn fqn, Map<? extends K, ? extends V> data) {
		put(cache, fqn, data);
	}

	@Override
	public void put(Fqn fqn, Map<? extends K, ? extends V> data, Flag... flags) {
		put(cache.withFlags(flags), fqn, data);
	}

	private void put(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, Map<? extends K, ? extends V> data) {
			TreeNode<K, V> n = getNode(cache, fqn);
			if (n == null)
				createNodeInCache(cache, fqn);
			n = getNode(cache, fqn);
			n.putAll(data);
	}

	@Override
	public void put(String fqn, Map<? extends K, ? extends V> data) {
		put(cache, Fqn.fromString(fqn), data);
	}

	@Override
	public void put(String fqn, Map<? extends K, ? extends V> data, Flag... flags) {
		put(cache.withFlags(flags), Fqn.fromString(fqn), data);
	}

	@Override
	public V remove(Fqn fqn, K key) {
		return remove(cache, fqn, key);
	}

	@Override
	public V remove(Fqn fqn, K key, Flag... flags) {
		return remove(cache.withFlags(flags), fqn, key);
	}

	private V remove(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, K key) {
			AtomicMap<K, V> map = getData(cache, fqn.dataKey());
			return map == null ? null : map.remove(key);
	}

	@Override
	public V remove(String fqn, K key) {
		return remove(cache, Fqn.fromString(fqn), key);
	}

	@Override
	public V remove(String fqn, K key, Flag... flags) {
		return remove(cache.withFlags(flags), Fqn.fromString(fqn), key);
	}

	@Override
	public boolean removeNode(Fqn fqn) {
		return removeNode(cache, fqn);
	}

	@Override
	public boolean removeNode(Fqn fqn, Flag... flags) {
		return removeNode(cache.withFlags(flags), fqn);
	}

	private boolean removeNode(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
		if (fqn.isRoot())
			return false;
		boolean result;
			if (trace)
				log.tracef("About to remove node %s", fqn);
			TreeNode<K, V> n = getNode(cache, fqn.getParent());
			result = n != null && n.removeChild(fqn.getLastElement());
		if (trace)
			log.trace("TreeNode successfully removed");
		return result;
	}

	@Override
	public boolean removeNode(String fqn) {
		return removeNode(cache, Fqn.fromString(fqn));
	}

	@Override
	public boolean removeNode(String fqn, Flag... flags) {
		return removeNode(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	@Override
	public TreeNode<K, V> getNode(Fqn fqn) {
		return getNode(cache, fqn);
	}

	@Override
	public TreeNode<K, V> getNode(Fqn fqn, Flag... flags) {
		return getNode(cache.withFlags(flags), fqn);
	}

	private TreeNode<K, V> getNode(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
			if (exists(cache, fqn))
				return createTreeNode(cache, fqn);
			else
				return null;
	}

	public TreeNode<K, V> createTreeNode(AdvancedCache<?, ?> cache, Fqn fqn) {
		return new TreeNodeImpl<K, V>(this, fqn, cache, batchContainer, proxyHandler);
	}

	@Override
	public TreeNode<K, V> getNode(String fqn) {
		return getNode(cache, Fqn.fromString(fqn));
	}

	@Override
	public TreeNode<K, V> getNode(String fqn, Flag... flags) {
		return getNode(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	@Override
	public V get(Fqn fqn, K key) {
		return get(cache, fqn, key);
	}

	@Override
	public V get(Fqn fqn, K key, Flag... flags) {
		return get(cache.withFlags(flags), fqn, key);
	}

	private V get(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, K key) {
		Map<K, V> m = getData(cache, fqn.dataKey());
		if (m == null)
			return null;
		return m.get(key);
	}

	@Override
	public boolean exists(String f) {
		return exists(cache, Fqn.fromString(f));
	}

	@Override
	public boolean exists(String fqn, Flag... flags) {
		return exists(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	@Override
	public boolean exists(Fqn fqn, Flag... flags) {
		return exists(cache.withFlags(flags), fqn);
	}

	@Override
	public V get(String fqn, K key) {
		return get(cache, Fqn.fromString(fqn), key);
	}

	@Override
	public V get(String fqn, K key, Flag... flags) {
		return get(cache.withFlags(flags), Fqn.fromString(fqn), key);
	}

	@Override
	public void move(Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
		move(cache, nodeToMoveFqn, newParentFqn);
	}

	@Override
	public void move(Fqn nodeToMoveFqn, Fqn newParentFqn, Flag... flags) throws NodeNotExistsException {
		move(cache.withFlags(flags), nodeToMoveFqn, newParentFqn);
	}

	private void move(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
		if (trace)
			log.tracef("Moving node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
		if (nodeToMoveFqn == null || newParentFqn == null)
			throw new NullPointerException("Cannot accept null parameters!");

		if (nodeToMoveFqn.getParent().equals(newParentFqn)) {
			if (trace)
				log.trace("Not doing anything as this node is equal with its parent");
			// moving onto self! Do nothing!
			return;
		}

		// Depth first. Lets start with getting the node we want.
		startAtomic();
		boolean success = false;
		try {
			// Use the FORCE_WRITE_LOCK for the first read operation on the source, the source's parent,
			// and the destination.
			AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cacheForWrite = cache.withFlags(Flag.FORCE_WRITE_LOCK);

			// check that parent's structure map contains the node to be moved. in case of optimistic locking this
			// ensures the write skew is properly detected if some other thread removes the child
			TreeNode<K, V> parent = getNode(cacheForWrite, nodeToMoveFqn.getParent());
			if (!parent.hasChild(nodeToMoveFqn.getLastElement())) {
				if (trace)
					log.trace("The parent does not have the child that needs to be moved. Returning...");
				return;
			}
			TreeNode<K, V> nodeToMove = getNode(cacheForWrite, nodeToMoveFqn);
			if (nodeToMove == null) {
				if (trace)
					log.trace("Did not find the node that needs to be moved. Returning...");
				return; // nothing to do here!
			}
			if (!exists(cacheForWrite, newParentFqn)) {
				// then we need to silently create the new parent
				createNodeInCache(cache, newParentFqn);
				if (trace)
					log.tracef("The new parent (%s) did not exists, was created", newParentFqn);
			}

			// create an empty node for this new parent
			Fqn newFqn = Fqn.fromRelativeElements(newParentFqn, nodeToMoveFqn.getLastElement());
			createNodeInCache(cache, newFqn);
			TreeNode<K, V> newNode = getNode(cache, newFqn);
			Map<K, V> oldData = nodeToMove.getData();
			if (oldData != null && !oldData.isEmpty())
				newNode.putAll(oldData);
			for (Object child : nodeToMove.getChildrenNames()) {
				// move kids
				if (trace)
					log.tracef("Moving child %s", child);
				Fqn oldChildFqn = Fqn.fromRelativeElements(nodeToMoveFqn, child);
				move(cache, oldChildFqn, newFqn);
			}
			removeNode(cache, nodeToMoveFqn);
			success = true;
		} finally {
			if (success) {
				endAtomic();
			} else {
				failAtomic();
			}
		}
		log.tracef("Successfully moved node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
	}

	@Override
	public void move(String nodeToMove, String newParent) throws NodeNotExistsException {
		move(cache, Fqn.fromString(nodeToMove), Fqn.fromString(newParent));
	}

	@Override
	public void move(String nodeToMove, String newParent, Flag... flags) throws NodeNotExistsException {
		move(cache.withFlags(flags), Fqn.fromString(nodeToMove), Fqn.fromString(newParent));
	}

	@Override
	public Map<K, V> getData(Fqn fqn) {
		return getData(cache, fqn);
	}

	@Override
	public Map<K, V> getData(Fqn fqn, Flag... flags) {
		return getData(cache.withFlags(flags), fqn);
	}

	private Map<K, V> getData(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
			TreeNode<K, V> node = getNode(cache, fqn);
			if (node == null)
				return null;
			else
				return node.getData();
	}

	@Override
	public Set<K> getKeys(String fqn) {
		return getKeys(cache, Fqn.fromString(fqn));
	}

	@Override
	public Set<K> getKeys(String fqn, Flag... flags) {
		return getKeys(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	@Override
	public Set<K> getKeys(Fqn fqn) {
		return getKeys(cache, fqn);
	}

	@Override
	public Set<K> getKeys(Fqn fqn, Flag... flags) {
		return getKeys(cache.withFlags(flags), fqn);
	}

	private Set<K> getKeys(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
			TreeNode<K, V> node = getNode(cache, fqn);
			if (node == null)
				return null;
			else
				return node.getKeys();
	}

	@Override
	public void clearData(String fqn) {
		clearData(cache, Fqn.fromString(fqn));
	}

	@Override
	public void clearData(String fqn, Flag... flags) {
		clearData(cache.withFlags(flags), Fqn.fromString(fqn));
	}

	@Override
	public void clearData(Fqn fqn) {
		clearData(cache, fqn);
	}

	@Override
	public void clearData(Fqn fqn, Flag... flags) {
		clearData(cache.withFlags(flags), fqn);
	}

	public void clearData(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn) {
			TreeNode<K, V> node = getNode(cache, fqn);
			if (node != null)
				node.clearData();
	}

	@Override
	public V put(Fqn fqn, K key, V value) {
		return put(cache, fqn, key, value);
	}

	@Override
	public V put(Fqn fqn, K key, V value, Flag... flags) {
		return put(cache.withFlags(flags), fqn, key, value);
	}

	private V put(AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache, Fqn fqn, K key, V value) {
			createNodeInCache(cache, fqn);
			Map<K, V> m = getData(cache, fqn.dataKey());
			return m.put(key, value);
	}

	@Override
	public Cache<?, ?> getCache() {
		// Retrieve the advanced cache as a way to retrieve
		// the cache behind the cache adapter.
		return cache;
	}

	// ------------------ nothing different; just delegate to the cache
	@Override
	public void start() throws CacheException {
		cache.start();
		createRoot();
	}

	@Override
	public void stop() {
		cache.stop();
	}

	private void createRoot() {
		if (!exists(Fqn.ROOT))
			createNodeInCache(Fqn.ROOT);
	}

	public String toString() {
		return cache.toString();
	}
}
