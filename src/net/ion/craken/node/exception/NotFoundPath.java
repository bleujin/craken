package net.ion.craken.node.exception;

import net.ion.craken.tree.Fqn;

public class NotFoundPath extends IllegalArgumentException {

	private static final long serialVersionUID = -2355219913059667636L;
	
	public NotFoundPath(Throwable cause){
		super(cause) ;
	}

	public NotFoundPath(String msg, Throwable cause){
		super(msg, cause) ;
	}

	public NotFoundPath(String msg){
		super(msg) ;
	}

	public NotFoundPath(Fqn fqn) {
		this("not found path :" + fqn) ;
	}

}
