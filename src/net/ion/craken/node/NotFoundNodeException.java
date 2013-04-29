package net.ion.craken.node;

public class NotFoundNodeException extends IllegalArgumentException{

	private static final long serialVersionUID = 7190165299640835366L;
	
	public NotFoundNodeException(String msg){
		super(msg) ;
	}

	public NotFoundNodeException(Throwable ex){
		super(ex) ;
	}
	
	public final static NotFoundNodeException notFoundNodePath(String fqn){
		return new NotFoundNodeException("not found path : " + fqn) ;
	}
	
}
