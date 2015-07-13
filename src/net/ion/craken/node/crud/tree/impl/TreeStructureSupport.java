package net.ion.craken.node.crud.tree.impl;

import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.TreeNode;
import net.ion.framework.util.Debug;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.batch.BatchContainer;
import org.infinispan.util.concurrent.locks.LockManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class TreeStructureSupport extends AutoBatchSupport {
	private static final Log log = LogFactory.getLog(TreeStructureSupport.class);

	protected final AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache;
	protected ProxyHandler proxyHandler;

	@SuppressWarnings("unchecked")
	public TreeStructureSupport(AdvancedCache<?, ?> cache, BatchContainer batchContainer, ProxyHandler proxyHandler) {
		this.cache = (AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache;
		this.batchContainer = batchContainer;
		this.proxyHandler = proxyHandler;
	}

	public boolean exists(Fqn f) {
		return exists(cache, f);
	}

	protected boolean exists(AdvancedCache<?, ?> cache, Fqn f) {
		return cache.containsKey(f.dataKey()); // && cache.containsKey(f.struKey());
	}

	/**
	 * @return true if created, false if this was not necessary.
	 */
	boolean createNodeInCache(Fqn fqn) {
		return createNodeInCache(cache, fqn);
	}

	protected boolean createNodeInCache(AdvancedCache<?, ?> cache, Fqn fqn) {
		TreeNodeKey dataKey = fqn.dataKey();
		TreeNodeKey structureKey = fqn.struKey();
		if (cache.containsKey(dataKey) && cache.containsKey(structureKey))
			return false;
		Fqn parent = fqn.getParent();
		if (!fqn.isRoot()) {
			if (!exists(cache, parent))
				createNodeInCache(cache, parent);
			AtomicMap<Object, Fqn> parentStructure = getStructure(cache, parent.struKey());
			parentStructure.put(fqn.getLastElement(), fqn);
		}
		getStructure(cache, structureKey);
		getData(cache, dataKey);
		if (log.isTraceEnabled())
			log.tracef("Created node %s", fqn);
		return true;
	}

	public static boolean isLocked(LockManager lockManager, Fqn fqn) {
		return ((lockManager.isLocked(fqn.struKey()) && lockManager.isLocked(fqn.dataKey())));
	}

	/**
	 * Returns a String representation of a tree cache.
	 */
	public static String printTree(TreeCache<?, ?> cache, boolean details) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n");

		// walk tree
		sb.append("+ ").append(Fqn.SEPARATOR);
		if (details)
			sb.append("  ").append(cache.getRoot().getData());
		sb.append("\n");
		addChildren(cache.getRoot(), 1, sb, details);
		return sb.toString();
	}

	private static void addChildren(TreeNode<?, ?> node, int depth, StringBuilder sb, boolean details) {
		for (TreeNode<?, ?> child : node.getChildren()) {
			for (int i = 0; i < depth; i++)
				sb.append("  "); // indentations
			sb.append("+ ");
			sb.append(child.getFqn().getLastElementAsString()).append(Fqn.SEPARATOR);
			if (details)
				sb.append("  ").append(child.getData());
			sb.append("\n");
			addChildren(child, depth + 1, sb, details);
		}
	}

	protected final <K, V> AtomicMap<K, V> getData(AdvancedCache<?, ?> cache, TreeNodeKey dataKey) {
		return AtomicMapLookup.getAtomicMap((AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache, dataKey);
	}

	protected final <K, V> AtomicMap<Object, Fqn> getStructure(AdvancedCache<?, ?> cache, TreeNodeKey struKey) {
		AtomicMap<Object, Fqn> result = AtomicMapLookup.getAtomicMap((AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache, struKey, false);
		if (result == null) {
			result = AtomicMapLookup.getAtomicMap((AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache, struKey, true);
			result.putAll(proxyHandler.handleStructure(struKey, result));
		}
		return result;
	}
}
