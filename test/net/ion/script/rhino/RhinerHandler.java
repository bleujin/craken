package net.ion.script.rhino;

public interface RhinerHandler<T> {
	
	public T onSuccess(Object result) ;
	public T onFail(Exception ex) ;
	
}
