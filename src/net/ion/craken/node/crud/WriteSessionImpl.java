package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;


public class WriteSessionImpl extends  AbstractWriteSession {

	public WriteSessionImpl(ReadSession session, Workspace workspace) {
		super(session, workspace) ;
	}

}
