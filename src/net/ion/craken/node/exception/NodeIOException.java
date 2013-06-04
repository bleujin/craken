package net.ion.craken.node.exception;

import java.io.IOException;

import org.infinispan.CacheException;

public class NodeIOException extends CacheException{

	private static final long serialVersionUID = 2207559222457044096L;

	public NodeIOException(IOException cause) {
		super(cause)  ;
	}

	public NodeIOException(String msg) {
		super(msg) ;
	}

}
