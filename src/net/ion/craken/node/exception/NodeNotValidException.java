package net.ion.craken.node.exception;

import org.infinispan.CacheException;


public class NodeNotValidException extends CacheException {
	private static final long serialVersionUID = 6576866180835456994L;

	public NodeNotValidException() {
	}

	public NodeNotValidException(Throwable cause) {
		super(cause);
	}

	public NodeNotValidException(String msg) {
		super(msg);
	}

	public NodeNotValidException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
