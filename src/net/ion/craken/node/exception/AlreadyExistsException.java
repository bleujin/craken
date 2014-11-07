package net.ion.craken.node.exception;

import net.ion.craken.tree.Fqn;

import org.infinispan.commons.CacheException;

public class AlreadyExistsException extends CacheException {

	private static final long serialVersionUID = -8102513077953554457L;

	public AlreadyExistsException(Fqn fqn) {
		super("already exists path :" + fqn.toString());
	}

	public AlreadyExistsException(String msg) {
		super(msg);
	}

	public AlreadyExistsException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
