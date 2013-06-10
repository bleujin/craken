package net.ion.craken.db;

import net.ion.craken.node.ReadSession;

public abstract class QueryPackage {

	private ReadSession session ;
	
	protected ReadSession session() {
		if (session == null) throw new UnsupportedOperationException();
		return session ;
	}
	
}
