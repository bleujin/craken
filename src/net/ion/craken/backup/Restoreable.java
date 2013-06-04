package net.ion.craken.backup;

import java.util.concurrent.ExecutionException;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;

public interface Restoreable {
	public int restore(ReadSession session) throws Exception ;
}
