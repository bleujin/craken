package net.ion.craken.node;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.WritableGridBlob;
import net.ion.craken.io.GridBlob.Metadata;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.config.Central;

import org.infinispan.loaders.AbstractCacheStoreConfig;

public abstract class AbstractWorkspace implements Workspace {

	private Repository repository;
	private GridFilesystem gfs;
	private TreeCache treeCache;
	private Central central;
	private String wsName;
	private AbstractCacheStoreConfig config;

	protected AbstractWorkspace(Repository repository, GridFilesystem gfs, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		this.repository = repository;
		this.gfs = gfs ;
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
	public TreeCache getCache() {
		return treeCache;
	}

	public void close() {
		final Object[] caches = treeCache.cache().getListeners().toArray(new Object[0]);
		for (Object listener : caches) {
			this.removeListener(listener);
		}

		// treeCache.getCache().stop() ;
		treeCache.stop();
	}
	
	public TreeNode createNode(IndexWriteConfig iwconfig, Fqn fqn){
		return treeCache.createWith(iwconfig, fqn) ; 
//		try {
//			beginTran() ;
//			return treeCache.createWith(fqn) ;
//		} finally {
//			endTran() ;
//		}
	}
	
	public TreeNode resetNode(IndexWriteConfig iwconfig, Fqn fqn){
		return treeCache.resetWith(iwconfig, fqn) ;
//		try {
//			beginTran() ;
//			return treeCache.resetWith(fqn) ;
//		} finally {
//			endTran() ;
//		}
	}

	public TreeNode pathNode(IndexWriteConfig iwconfig, Fqn fqn) {
		try {
			beginTran();
			return treeCache.mergeWith(iwconfig, fqn) ;
		} finally {
			endTran();
		}
	}
	

	public boolean exists(Fqn fqn) {
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
						if (ex instanceof Exception) throw (Exception)ex ; else throw new Exception(ex);

					ehandler.handle(wsession, ex);
					return null;
				} finally {
					workspace.endTran();
				}

			}
		});
	}

	public <T> Future<T> dump(final DumpSession dsession, final DumpJob<T> tjob, final TranExceptionHandler ehandler) {
		final AbstractWorkspace workspace = this;
		return repository.executor().submitTask(new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					dsession.beginTran();
					T result = tjob.handle(dsession);
					
					return result;
				} catch (Throwable ex) {
					// ex.printStackTrace() ;
					dsession.failRollback();
					if (ehandler == null)
						if (ex instanceof Exception) throw (Exception)ex ; else throw new Exception(ex);

					ehandler.handle(dsession, ex);
					return null;
				} finally {
					dsession.endCommit();
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

		treeCache.cache().addListener(listener);
		return this;
	}

	@Override
	public void removeListener(Object listener) {
		if (listener instanceof WorkspaceListener) {
			((WorkspaceListener) listener).unRegistered(this);
		}

		treeCache.cache().removeListener(listener);
	}

	public Metadata writeBlob(String fqnPath, Metadata meta, InputStream input) throws IOException {
		WritableGridBlob gblob = gfs.getWritableGridBlob(fqnPath, meta);
		IOUtil.copyNClose(input, gblob.outputStream());
		return gblob.getMetadata() ;
	}

	@Override
	public Central central() {
		return repository.central(wsName) ;
	}

	@Override
	public GridFilesystem gfs(){
		return gfs ;
	}
	
	public IExecutor executor() {
		return repository.executor();
	}
	
	@Override
	public AbstractCacheStoreConfig config() {
		return config;
	}


}
