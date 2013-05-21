package net.ion.craken.db;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import net.ion.craken.node.ReadSession;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;

public abstract class QueryPackage {

	private ReadSession session ;
	
	protected ReadSession session() {
		if (session == null) throw new UnsupportedOperationException();
		return session ;
	}
	
}
