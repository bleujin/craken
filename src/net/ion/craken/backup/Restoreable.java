package net.ion.craken.backup;

import net.ion.craken.node.ReadSession;

public interface Restoreable {
	public int restore(ReadSession session) throws Exception ;
}
