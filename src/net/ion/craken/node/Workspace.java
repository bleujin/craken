package net.ion.craken.node;

import java.util.concurrent.Future;

import net.ion.craken.node.crud.TestListener.DebugListener;

import org.infinispan.notifications.Listener;

@Listener
public interface Workspace {
	
	public String wsName() ;
	
	public void close()  ;

	public <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler handler) ;

	public boolean exists(String fqn);

	public Workspace addListener(Object listener);

	public void removeListener(Object listener);
	
}
