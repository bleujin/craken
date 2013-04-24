package net.ion.craken.node.search;

import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNode;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectId;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

@Listener
public class WorkspaceSearch implements Workspace {

	private Repository repository;
	private Central central;
	private TreeCache treeCache;
	private String wsName;
	private Class<? extends Analyzer> indexAnal;
	private Future<Void> lastCommand;

	private WorkspaceSearch(Repository repository, Central central, TreeCache treeCache, String wsName) {
		this.repository = repository;
		this.central = central;
		this.treeCache = treeCache;
		this.wsName = wsName;
		this.indexAnal = MyKoreanAnalyzer.class;

		treeCache.getCache().addListener(this);
		this.treeCache.start();
	}

	public String wsName() {
		return wsName;
	}

	public Class<? extends Analyzer> indexAnalyzer() {
		return indexAnal;
	}

	static WorkspaceSearch create(Repository repository, Central central, TreeCache treeCache, String wsName) {
		return new WorkspaceSearch(repository, central, treeCache, wsName);
	}

	public void close() {
		treeCache.stop();
	}

	// inner package
	TreeNode<String, ? extends Object> getNode(String fqn) {
		try {
			beginTran();
			TreeNode found = treeCache.getNode(fqn);
			if (found == null) {
				if (!treeCache.exists(fqn)) {
					treeCache.put(Fqn.fromString(fqn), MapUtil.EMPTY);
					found = getNode(fqn);
				}

				TreeNode parent = found.getParent();
				while (! parent.getFqn().isRoot()) {
					parent = parent.getParent();
				}
			}
			return found;
		} finally {
			endTran();
		}
	}

	public boolean exists(String fqn) {
		return treeCache.exists(fqn);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler handler) {
		final WorkspaceSearch workspace = this;
		return repository.executor().submitTask(new Callable<T>() {

			@Override
			public T call() throws Exception {
				workspace.beginTran();
				try {
					T result = tjob.handle(wsession);
					workspace.endTran();
					return result;
				} catch (Throwable ex) {
					workspace.failEndTran();
					wsession.failRollback();
					handler.handle(wsession, ex);
				} finally {
					wsession.endCommit();
				}
				return null;
			}

		});
	}

	private void failEndTran() {
		treeCache.failEnd();
	}

	private void endTran() {
		treeCache.end();
	}

	private void beginTran() {
		treeCache.begin();
	}

	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap> e) throws InterruptedException, ExecutionException {
		if (e.isPre())
			return;

		final TreeNodeKey key = e.getKey();
		final AtomicHashMap<String, ?> value = e.getValue();

//		if ("bleujin".equals(value.get("name")))
//			Debug.debug(value.get(NodeCommon.IDProp));

		if (e.getKey().getContents() == Type.DATA) {
			lastCommand = central.newIndexer().asyncIndex(new IndexJob<Void>() {
				@Override
				public Void handle(IndexSession session) throws Exception {
					MyDocument doc = MyDocument.newDocument(key.getFqn().toString());
					doc.keyword(NodeCommon.NameProp, key.getFqn().getLastElementAsString());
					for (String key : value.keySet()) {
						doc.addUnknown(key, value.get(key));
					}
					session.updateDocument(doc);
					return null;
				}
			});
		}
	}

	public void awaitIndex() throws InterruptedException, ExecutionException {
		if (lastCommand != null)
			lastCommand.get();
	}
}
