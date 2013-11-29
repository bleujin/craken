package net.ion.craken.node.crud;

import java.io.IOException;

import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;

import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.loaders.AbstractCacheStoreConfig;


public class WorkspaceImpl extends Workspace {


	WorkspaceImpl(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, AbstractCacheStoreConfig config) throws CorruptIndexException, IOException {
		super(repository, cache, wsName, config) ;
	}

	public static WorkspaceImpl create(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, AbstractCacheStoreConfig config) throws CorruptIndexException, IOException {
		return new WorkspaceImpl(repository, cache, wsName, config);
	}


}
