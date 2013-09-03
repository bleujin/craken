package net.ion.craken.node;

public interface DumpJob<T> {
	public T handle(DumpSession dsession) throws Exception ;
}
