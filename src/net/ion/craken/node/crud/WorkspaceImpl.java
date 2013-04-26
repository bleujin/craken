package net.ion.craken.node.crud;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.ion.craken.node.Repository;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.infinispan.notifications.Listener;

@Listener
public class WorkspaceImpl implements Workspace {

	private Repository repository;
	private String wsName;
	private Class<? extends Analyzer> indexAnal;
	private TreeCache treeCache;

	WorkspaceImpl(Repository repository, TreeCache treeCache, String wsName) {
		this.repository = repository;
		this.wsName = wsName;
		this.indexAnal = MyKoreanAnalyzer.class;
		this.treeCache = treeCache;

		this.treeCache.start();
	}

	public String wsName() {
		return wsName;
	}

	public Class<? extends Analyzer> indexAnalyzer() {
		return indexAnal;
	}

	public static WorkspaceImpl create(Repository repository, TreeCache treeCache, String wsName) {
		return new WorkspaceImpl(repository, treeCache, wsName);
	}

	public void close() {
		treeCache.stop();
	}

	// inner package
	TreeNode<PropertyId, PropertyValue> getNode(Fqn fqn) {
		try {
			beginTran();
			
			TreeNode found = treeCache.getNode(fqn);
			if (found == null) {
				if (!treeCache.exists(fqn)) {
					treeCache.put(fqn, MapUtil.EMPTY);
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
	
	TreeNode<PropertyId, PropertyValue> getNode(String fqn) {
		return getNode(Fqn.fromString(fqn)) ;
	}

	public boolean exists(Fqn fqn) {
		return treeCache.exists(fqn);
	}
	public boolean exists(String fqn) {
		return treeCache.exists(fqn);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler handler) {
		final WorkspaceImpl workspace = this;
		return repository.executor().submitTask(new Callable<T>() {

			@Override
			public T call() throws Exception {
				workspace.beginTran();
				try {
					T result = tjob.handle(wsession);
					workspace.endTran();
					return result;
				} catch (Throwable ex) {
					ex.printStackTrace() ;
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

	@Override
	public Workspace addListener(Object listener) {
		treeCache.getCache().addListener(listener) ;
		return this;
	}

	@Override
	public void removeListener(Object listener) {
		treeCache.getCache().removeListener(listener) ;
	}

}
