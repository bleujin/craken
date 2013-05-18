package net.ion.craken.backup;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;

public interface Recoverable {
	public int recover(ReadSession session) ;
}
