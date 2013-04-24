package net.ion.craken.node.crud;

import java.util.concurrent.Future;

import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;


public class ReadSessionImpl implements ReadSession{

	private Credential credential ;
	private WorkspaceImpl workspace ;
	public ReadSessionImpl(Credential credential, WorkspaceImpl workspace) {
		this.credential = credential.clearSecretKey() ;
		this.workspace = workspace ;
	}

	public ReadNode pathBy(String fqn) {
		return ReadNodeImpl.load(workspace.getNode(fqn)) ;
	}

	public ReadNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return workspace.exists(fqn);
	}

	public <T> Future<T> tran(TransactionJob<T> tjob) {
		return tran(tjob, TranExceptionHandler.PRINT) ;
	}

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSession tsession = new WriteSessionImpl(this, workspace);

		return workspace.tran(tsession, tjob, handler) ;
	}

	public WorkspaceImpl workspace() {
		return workspace;
	}

	@Override
	public Credential credential() {
		return credential;
	}

}
