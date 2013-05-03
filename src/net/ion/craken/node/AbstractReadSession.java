package net.ion.craken.node;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.node.crud.WorkspaceImpl;
import net.ion.craken.node.crud.WriteSessionImpl;
import net.ion.craken.node.exception.NotFoundPath;
import net.ion.craken.tree.Fqn;

public abstract class AbstractReadSession implements ReadSession{

	private Credential credential ;
	private AbstractWorkspace workspace ;
	protected AbstractReadSession(Credential credential, AbstractWorkspace workspace) {
		this.credential = credential.clearSecretKey() ;
		this.workspace = workspace ;
	}

	public ReadNode pathBy(String fqn) {
		return pathBy(Fqn.fromString(fqn)) ;
	}

	public ReadNode pathBy(Fqn fqn) {
		return pathBy(fqn, false) ;
	}

	public ReadNode pathBy(Fqn fqn, boolean createIf) {
		if (createIf || exists(fqn)) return ReadNodeImpl.load(this, workspace.getNode(fqn));
		else throw new NotFoundPath(fqn) ;
	}

	public ReadNode pathBy(String fqn, boolean createIf) {
		return pathBy(Fqn.fromString(fqn), createIf) ;
	}

	
	public boolean exists(String fqn) {
		return workspace.exists(fqn);
	}

	public boolean exists(Fqn fqn) {
		return workspace.exists(fqn);
	}


	public ReadNode root() {
		return pathBy("/");
	}

	public <T> Future<T> tran(TransactionJob<T> tjob) {
		return tran(tjob, TranExceptionHandler.PRINT) ;
	}

	@Override
	public <T> T tranSync(TransactionJob<T> tjob) throws InterruptedException, ExecutionException {
		return tran(tjob).get();
	}

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSession tsession = new WriteSessionImpl(this, workspace);

		return workspace.tran(tsession, tjob, handler) ;
	}

	public AbstractWorkspace workspace() {
		return workspace;
	}

	@Override
	public Credential credential() {
		return credential;
	}

	@Override
	public Workspace getWorkspace() {
		return workspace;
	}

}
