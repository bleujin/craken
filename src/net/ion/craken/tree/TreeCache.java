package net.ion.craken.tree;


import java.util.Map;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.exception.NodeNotExistsException;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheException;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.config.ConfigurationException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class TreeCache extends TreeStructureSupport {
	private static final Log log = LogFactory.getLog(TreeCache.class);
	private static final boolean trace = log.isTraceEnabled();

	private GridFilesystem gfs ;
	public TreeCache(Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, GridFilesystem gfs) {
		this(cache.getAdvancedCache(), gfs);
	}

	private TreeCache(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, GridFilesystem gfs) {
		super(cache, cache.getBatchContainer());
		this.gfs = gfs ;
		if (cache.getCacheConfiguration().indexing().enabled())
			throw new ConfigurationException("TreeCache cannot be used with a Cache instance configured to use indexing!");
		assertBatchingSupported(cache.getCacheConfiguration());
		createRoot();
	}

	public TreeNode getRoot() {
		return createTreeNode(Fqn.ROOT, batchContainer);
	}
	
	public TreeNode createWith(IndexWriteConfig iwconfig, Fqn fqn){
		cache.put(fqn.contentKey().createAction(), new AtomicHashMap<PropertyId, PropertyValue>()) ;
		
		if (log.isTraceEnabled()) log.tracef("Created node %s", fqn);
		return new TreeNode(fqn, cache, gfs, batchContainer) ;
	}

	public TreeNode resetWith(IndexWriteConfig iwconfig, Fqn fqn){
		cache.put(fqn.contentKey().resetAction(), new AtomicHashMap<PropertyId, PropertyValue>()) ;
		
		if (log.isTraceEnabled()) log.tracef("Reset node %s", fqn);
		return new TreeNode(fqn, cache, gfs, batchContainer) ;
	}

	
//	public TreeNode logWith(IndexWriteConfig iwconfig, Fqn fqn){
//		cache.put(new TreeNodeKey(fqn, TreeNodeKey.Type.SYSTEM).resetAction(), new AtomicHashMap<PropertyId, PropertyValue>()) ;
//		
//		if (log.isTraceEnabled()) log.tracef("Reset node %s", fqn);
//		return new TreeNode(fqn, cache, batchContainer) ;
//	}


	public TreeNode mergeWith(IndexWriteConfig iwconfig, Fqn fqn) {
		
		mergeAncestor(iwconfig, fqn) ;
		
		if (log.isTraceEnabled()) log.tracef("Merged node %s", fqn);
		return new TreeNode(fqn, cache, gfs, batchContainer);
	}


	
	
	
	
	public AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache(){
		return cache.getAdvancedCache() ;
	}

	private boolean removeNode(Fqn fqn) {
		if (fqn.isRoot())
			return false;
		boolean result;
		if (trace) log.tracef("About to remove node %s", fqn);
		
		TreeNode n = getNode(fqn.getParent());
		result = n != null && n.removeChild(fqn.getLastElement());
		if (trace) log.trace("Node successfully removed");
		return result;
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

	
	

	private TreeNode getNode(Fqn fqn) {
		if (exists(fqn))
			return createTreeNode(fqn, batchContainer);
		else
			return null;
	}

	private TreeNode createTreeNode(Fqn fqn, BatchContainer batchContainer) {
		return new TreeNode(fqn, cache, gfs, batchContainer);
	}

	public void move(GridFilesystem gfs, Fqn nodeToMoveFqn, Fqn newParentFqn) throws NodeNotExistsException {
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
			TreeNode parent = getNode(nodeToMoveFqn.getParent());
			if (!parent.hasChild(nodeToMoveFqn.getLastElement())) {
				 if (trace) log.trace("The parent does not have the child that needs to be moved. Returning...");
				return;
			}
			TreeNode nodeToMove = getNode(nodeToMoveFqn);
			if (nodeToMove == null) {
				if (trace) log.trace("Did not find the node that needs to be moved. Returning...");
				return; // nothing to do here!
			}
			if (!exists(newParentFqn)) {
				// then we need to silently create the new parent
				mergeAncestor(newParentFqn);
				if (trace) log.tracef("The new parent (%s) did not exists, was created", newParentFqn);
			}

			// create an empty node for this new parent
			Fqn newFqn = Fqn.fromRelativeElements(newParentFqn, nodeToMoveFqn.getLastElement());
			mergeAncestor(newFqn);
			TreeNode newNode = getNode(newFqn);
			Map<PropertyId, PropertyValue> oldData = nodeToMove.getData();
			if (oldData != null && !oldData.isEmpty())
				newNode.putAll(oldData);
			for (Object child : nodeToMove.getChildrenNames()) {
				// move kids
				if (trace) log.tracef("Moving child %s", child);
				Fqn oldChildFqn = Fqn.fromRelativeElements(nodeToMoveFqn, child);
				move(gfs, oldChildFqn, newFqn);
			}
			removeNode(nodeToMoveFqn);
			success = true;
		} finally {
			if (!success) {
				failAtomic();
			}
		}
		log.tracef("Successfully moved node '%s' to '%s'", nodeToMoveFqn, newParentFqn);
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
		if (!exists(Fqn.ROOT)) {
			mergeAncestor(Fqn.ROOT);
		}
	}

	public String toString() {
		return cache.toString();
	}

}
