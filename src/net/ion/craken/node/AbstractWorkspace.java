package net.ion.craken.node;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;

public abstract class AbstractWorkspace implements Workspace{
	

	private Repository repository;
	private String wsName;
	private TreeCache treeCache;

	protected AbstractWorkspace(Repository repository, TreeCache treeCache, String wsName) {
		this.repository = repository;
		this.wsName = wsName;
		this.treeCache = treeCache;

//		this.treeCache.start();
	}


	public <T> T getAttribute(String key, Class<T> clz){
		return repository.getAttribute(key, clz) ;
	}
	
	
	public String wsName() {
		return wsName;
	}
	
	public TreeCache getCache(){
		return treeCache ;
	}


	public void close() {
		for(Object listener : treeCache.getCache().getListeners() ){
			this.removeListener(listener) ;
		}
		
		treeCache.getCache().stop() ;
		treeCache.stop();
	}

	public TreeNode<PropertyId, PropertyValue> getNode(Fqn fqn) {
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
	
	public TreeNode<PropertyId, PropertyValue> getNode(String fqn) {
		return getNode(Fqn.fromString(fqn)) ;
	}

	public boolean exists(Fqn fqn) {
		return treeCache.exists(fqn);
	}
	public boolean exists(String fqn) {
		return treeCache.exists(fqn);
	}

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler handler) {
		final AbstractWorkspace workspace = this;
		return repository.executor().submitTask(new Callable<T>() {

			@Override
			public T call() throws Exception {
				workspace.beginTran();
				try {
					T result = tjob.handle(wsession);
					workspace.endTran();
					return result;
				} catch (Exception ex) {
//					ex.printStackTrace() ;
					workspace.failEndTran();
					wsession.failRollback();
					handler.handle(wsession, ex);
					throw ex;
				} finally {
					wsession.endCommit();
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

	@Override
	public Workspace addListener(Object listener) {
		if (listener instanceof WorkspaceListener){
			((WorkspaceListener)listener).registered(this) ;
		}
		
		treeCache.getCache().addListener(listener) ;
		return this;
	}

	@Override
	public void removeListener(Object listener) {
		if (listener instanceof WorkspaceListener){
			((WorkspaceListener)listener).unRegistered(this) ;
		}
		
		treeCache.getCache().removeListener(listener) ;
	}
}
