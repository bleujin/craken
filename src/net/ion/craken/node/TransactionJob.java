package net.ion.craken.node;

public interface TransactionJob<T> {

	public T handle(WriteSession wsession) throws Exception ;
}
