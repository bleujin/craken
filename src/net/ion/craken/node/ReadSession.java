package net.ion.craken.node;

import java.util.concurrent.Future;


public interface ReadSession {

	public ReadNode pathBy(String fqn);

	public ReadNode root();

	public boolean exists(String fqn);

	public <T> Future<T> tran(TransactionJob<T> tjob);

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler);

	public Credential credential();

}
