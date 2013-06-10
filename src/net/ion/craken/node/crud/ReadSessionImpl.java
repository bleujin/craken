package net.ion.craken.node.crud;

import java.util.concurrent.ExecutionException;

import net.ion.craken.node.AbstractReadSession;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.search.ReadSearchSession;


public class ReadSessionImpl extends AbstractReadSession{

	public ReadSessionImpl(Credential credential, AbstractWorkspace workspace) {
		super(credential, workspace) ;
	}

	@Override
	public ReadSessionImpl awaitIndex() throws InterruptedException, ExecutionException {
		
		return this;
	}

	
}
