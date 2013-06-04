package net.ion.craken.node.search;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteSessionImpl;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

public class WriteSearchSession implements WriteSession {

	private ReadSession readSession ;
	private WriteSession winner ;
	private Central central ;
	private AbstractWorkspace workspace ;
	
	public WriteSearchSession(ReadSearchSession readSearchSession, AbstractWorkspace workspace, Central central) {
		this.readSession = readSearchSession ;
		this.workspace = workspace ;
		this.central = central ;
	}

	public WriteNode pathBy(String fqn) {
		return WriteSearchNode.loadTo(this, workspace.getNode(fqn)) ;
	}
	
	public WriteNode pathBy(String fqn0, String... fqns) {
		return WriteSearchNode.loadTo(this, workspace.getNode(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/')))) ;
	}

	public WriteNode pathBy(Fqn fqn) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(fqn)) ;
	}
	
	public WriteNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return winner.exists(fqn) ;
	}

	@Override
	public void endCommit() {
		
	}

	@Override
	public void failRollback() {
		
	}

	@Override
	public Credential credential() {
		return readSession.credential();
	}

	@Override
	public Workspace workspace() {
		return workspace;
	}
	

}
