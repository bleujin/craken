package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.StringUtil;


public class WriteSessionImpl implements WriteSession {

	private ReadSession readSession ;
	private AbstractWorkspace workspace ;
	public WriteSessionImpl(ReadSession session, AbstractWorkspace workspace) {
		this.readSession = session ;
		this.workspace = workspace ;
	}

	public WriteNode pathBy(String fqn0, String... fqns) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/')))) ;
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
