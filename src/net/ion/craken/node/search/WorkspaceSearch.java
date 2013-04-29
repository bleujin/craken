package net.ion.craken.node.search;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.index.Term;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

@Listener
public class WorkspaceSearch extends AbstractWorkspace {

	private Central central;
	private Future<Void> lastCommand;

	private WorkspaceSearch(Repository repository, Central central, TreeCache<PropertyId, PropertyValue> treeCache, String wsName) {
		super(repository, treeCache, wsName) ;
		this.central = central;

		treeCache.getCache().addListener(this);
		treeCache.start();
	}

	static WorkspaceSearch create(Repository repository, Central central, TreeCache<PropertyId, PropertyValue> treeCache, String wsName) {
		return new WorkspaceSearch(repository, central, treeCache, wsName);
	}


	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> e) throws InterruptedException, ExecutionException {
		if (e.isPre())
			return;

		final TreeNodeKey key = e.getKey();
		final AtomicHashMap<PropertyId, PropertyValue> value = e.getValue();

//		if ("bleujin".equals(value.get("name")))
//			Debug.debug(value.get(NodeCommon.IDProp));

		if (e.getKey().getContents() == Type.DATA) {
			lastCommand = central.newIndexer().asyncIndex(new IndexJob<Void>() {
				@Override
				public Void handle(IndexSession isession) throws Exception {
					MyDocument doc = MyDocument.newDocument(key.getFqn().toString());
					doc.keyword(NodeCommon.NameProp, key.getFqn().getLastElementAsString());
					for (PropertyId key : value.keySet()) {
						doc.addUnknown(key.getString(), value.get(key).value());
					}
					isession.updateDocument(doc);
					return null;
				}
			});
		}
	}
	
	@CacheEntryRemoved
	public void entryRemoved(CacheEntryRemovedEvent<TreeNodeKey, AtomicHashMap> e) {
		if (e.isPre()) return ;
		final TreeNodeKey key = e.getKey();
		
		if (e.getKey().getContents() == Type.DATA) {
			lastCommand = central.newIndexer().asyncIndex(new IndexJob<Void>() {
				@Override
				public Void handle(IndexSession isession) throws Exception {
					isession.deleteTerm(new Term(IKeywordField.ISKey, key.getFqn().toString())) ;
					return null;
				}
				
			}) ;
		}
	}

	public void awaitIndex() throws InterruptedException, ExecutionException {
		if (lastCommand != null)
			lastCommand.get();
	}


}
