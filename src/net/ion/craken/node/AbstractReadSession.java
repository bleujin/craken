package net.ion.craken.node;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.exception.NotFoundPath;
import net.ion.framework.util.StringUtil;

import com.google.common.base.Function;


public abstract class AbstractReadSession implements ReadSession {

	private Credential credential ;
	private Workspace workspace ;
	protected AbstractReadSession(Credential credential, Workspace workspace) {
		this.credential = credential.clearSecretKey() ;
		this.workspace = workspace ;
	}

	public ReadNode pathBy(String fqn0, Object... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/'))) ;
	}

	public ReadNode ghostBy(String fqn){
		return pathBy(Fqn.fromString((fqn.startsWith("/") ? fqn : "/" + fqn)), true) ;
	}
	
	public ReadNode ghostBy(String fqn0, Object... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/')), true) ;
	}
	
	public ReadNode ghostBy(Fqn fqn) {
		return pathBy(fqn, true) ;
	}

	public ReadNode pathBy(Fqn fqn) {
		return pathBy(fqn, false) ;
	}

	protected ReadNode pathBy(Fqn fqn, boolean emptyIfNotExist) {
		if (exists(fqn)) {
			return ReadNodeImpl.load(this, fqn);
		} else if (emptyIfNotExist) {
			return ReadNodeImpl.ghost(this, fqn) ;
		}
		else throw new NotFoundPath(fqn) ;
	}

	protected ReadNode pathBy(String fqn, boolean emptyIfNotExist) {
		return pathBy(Fqn.fromString(fqn), emptyIfNotExist) ;
	}

	public ReadNode pathBy(String fqn) {
		return pathBy(fqn, false) ;
	}

	
	public boolean exists(String fqn) {
		return workspace.exists(Fqn.fromString(fqn));
	}

	public boolean exists(Fqn fqn) {
		return workspace.exists(fqn);
	}


	public ReadNode root() {
		return pathBy(Fqn.ROOT);
	}

	public <T> Future<T> tran(TransactionJob<T> tjob) {
		return tran(tjob, TranExceptionHandler.PRINT) ;
	}

	@Override
	public <T> T tranSync(TransactionJob<T> tjob) throws Exception {
		return tranSync(tjob, TranExceptionHandler.PRINT) ;
	}


	public <T> T tranSync(TransactionJob<T> tjob, TranExceptionHandler handler) throws Exception {
		return tran(tjob, handler).get() ;
	}
	
	
	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSession tsession = workspace.newWriteSession(this) ;

		return workspace.tran(tsession, tjob, handler) ;
	}

	
	
	
	@Override
	public Workspace workspace() {
		return workspace;
	}

	@Override
	public Credential credential() {
		return credential;
	}

	public <Ri, Rv> Map<Ri, Rv> mapReduceSync(final NodeMapReduce<Ri, Rv> mapper) throws InterruptedException, ExecutionException{
		return asyncMapReduce(mapper).get();
	}

	
	public <Ri, Rv, V> Future<V> mapReduce(final NodeMapReduce<Ri, Rv> mapper, final Function<Map<Ri, Rv>, V> function){
		return workspace().executor().submit(new Callable<V>() {
			@Override
			public V call() throws Exception {
				Future<Map<Ri, Rv>> future = asyncMapReduce(mapper);
				return function.apply(future.get()) ;
			}
		}) ;
	}

	private <Ri, Rv> Future<Map<Ri, Rv>> asyncMapReduce(NodeMapReduce<Ri, Rv> mapper) {
		return workspace.mapReduce(mapper) ;
	}


}
