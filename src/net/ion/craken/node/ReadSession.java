package net.ion.craken.node;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.tree.Fqn;


public interface ReadSession extends ISession<ReadNode> {

	public ReadNode pathBy(String fqn0, String... fqns);

	public ReadNode pathBy(String fqn, boolean createIf);

	public ReadNode pathBy(Fqn fqn, boolean createIf);

	public boolean exists(Fqn fqn);

	public <T> Future<T> tran(TransactionJob<T> tjob);

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler);

	public <T> T tranSync(TransactionJob<T> tjob) throws Exception;

	public Workspace workspace();



}
