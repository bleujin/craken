package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractReadSession;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;


public class ReadSessionImpl extends AbstractReadSession{

	public ReadSessionImpl(Credential credential, AbstractWorkspace workspace) {
		super(credential, workspace) ;
	}

	
}
