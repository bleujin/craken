package net.ion.craken.node;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.infinispan.loaders.AbstractCacheStoreConfig;

import net.ion.craken.io.BlobProxy;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.config.Central;

public abstract class AbstractWorkspace implements Workspace {

	private Repository repository;
	private Central central;
	private String wsName;
	private TreeCache<PropertyId, PropertyValue> treeCache;
	private AbstractCacheStoreConfig config;

	protected AbstractWorkspace(Repository repository, TreeCache<PropertyId, PropertyValue> treeCache, String wsName, AbstractCacheStoreConfig config) {
		this.repository = repository;
		this.wsName = wsName;
		this.treeCache = treeCache;
		this.config = config ;
		// this.treeCache.start();
	}

	public <T> T getAttribute(String key, Class<T> clz) {
		return repository.getAttribute(key, clz);
	}

	public String wsName() {
		return wsName;
	}

	// only use for test
	public TreeCache<PropertyId, PropertyValue> getCache() {
		return treeCache;
	}

	public void close() {
		final Object[] caches = treeCache.getCache().getListeners().toArray(new Object[0]);
		for (Object listener : caches) {
			this.removeListener(listener);
		}

		// treeCache.getCache().stop() ;
		treeCache.stop();
	}

	public TreeNode<PropertyId, PropertyValue> getNode(Fqn fqn) {
		try {
			beginTran();

			TreeNode<PropertyId, PropertyValue> found = treeCache.getNode(fqn);
			if (found == null) {
				if (!treeCache.exists(fqn)) {
					treeCache.put(fqn, MapUtil.EMPTY);
					found = getNode(fqn);
				}

				TreeNode<PropertyId, PropertyValue> parent = found.getParent();
				while (!parent.getFqn().isRoot()) {
					parent = parent.getParent();
				}
			}

			return found;
		} finally {
			endTran();
		}
	}

	public TreeNode<PropertyId, PropertyValue> getNode(String fqn) {
		return getNode(Fqn.fromString(fqn));
	}

	public boolean exists(Fqn fqn) {
		return treeCache.exists(fqn);
	}

	public boolean exists(String fqn) {
		return treeCache.exists(fqn);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob) {
		return tran(wsession, tjob, null);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) {
		final AbstractWorkspace workspace = this;
		return repository.executor().submitTask(new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					workspace.beginTran();
					T result = tjob.handle(wsession);
					wsession.endCommit();
					return result;
				} catch (Throwable ex) {
					// ex.printStackTrace() ;
					workspace.failEndTran();
					wsession.failRollback();
					if (ehandler == null)
						throw new Exception(ex);

					ehandler.handle(wsession, ex);
					return null;
				} finally {
					workspace.endTran();
				}

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

	public Workspace continueUnit(WriteSession wsession) {
		wsession.endCommit();
		endTran();

		beginTran();
		return this;
	}

	@Override
	public Workspace addListener(Object listener) {
		if (listener instanceof WorkspaceListener) {
			((WorkspaceListener) listener).registered(this);
		}

		treeCache.getCache().addListener(listener);
		return this;
	}

	@Override
	public void removeListener(Object listener) {
		if (listener instanceof WorkspaceListener) {
			((WorkspaceListener) listener).unRegistered(this);
		}

		treeCache.getCache().removeListener(listener);
	}

	public BlobProxy blob(String fqnPath, InputStream input) throws IOException {
		OutputStream output = treeCache.gfs().getOutput(fqnPath);
		IOUtil.copyNClose(input, output);

		return BlobProxy.create(fqnPath);
	}

	@Override
	public Central central() {
		return repository.central(wsName) ;
	}

	public IExecutor executor() {
		return repository.executor();
	}
	
	@Override
	public AbstractCacheStoreConfig config() {
		return config;
	}


}
