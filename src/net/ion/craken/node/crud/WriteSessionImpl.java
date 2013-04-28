package net.ion.craken.node.crud;

import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;


public class WriteSessionImpl implements WriteSession {

	private ReadSession readSession ;
	private WorkspaceImpl workspace ;
	public WriteSessionImpl(ReadSession session, WorkspaceImpl workspace) {
		this.readSession = session ;
		this.workspace = workspace ;
	}

	public WriteNode pathBy(String fqn) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(fqn)) ;
	}

	public WriteNode pathBy(Fqn fqn) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(fqn)) ;
	}
	
	public WriteNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return workspace.exists(fqn);
	}

	@Override
	public void endCommit() {
		; // no action
	}

	@Override
	public void failRollback() {
		; // no action
	}

	public Credential credential() {
		return readSession.credential() ;
	}

	
}
