package net.ion.craken.node;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.GridBlob.Metadata;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.schedule.IExecutor;
import net.ion.nsearcher.config.Central;

import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.notifications.Listener;

@Listener
public interface Workspace {
	
	public String wsName() ;

	public TreeNode createNode(IndexWriteConfig iwconfig, Fqn fqn);

	public TreeNode resetNode(IndexWriteConfig iwconfig, Fqn fqn);

	public TreeNode pathNode(IndexWriteConfig iwconfig, Fqn fqn);
	
	public void close()  ;

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler handler) ;

	public <T> Future<T> dump(final DumpSession dsession, final DumpJob<T> tjob, final TranExceptionHandler handler) ;

	public boolean exists(Fqn fqn);

	public Workspace addListener(Object listener);

	public void removeListener(Object listener);
	
	public <T> T getAttribute(String key, Class<T> clz) ;

	public TreeCache getCache();

	public Metadata writeBlob(String fqnPath, Metadata meta, InputStream input) throws IOException;

	public IExecutor executor();

	public Workspace continueUnit(WriteSession wsession) ;

	public Central central();

	public AbstractCacheStoreConfig config();

	public GridFilesystem gfs();



}
