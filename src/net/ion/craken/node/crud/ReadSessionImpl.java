package net.ion.craken.node.crud;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.omg.PortableServer.POAPackage.WrongAdapter;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;


public class ReadSessionImpl implements ReadSession{

	private Credential credential ;
	private AbstractWorkspace workspace ;
	public ReadSessionImpl(Credential credential, AbstractWorkspace workspace) {
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
		else throw new IllegalArgumentException("not found path :" + fqn) ;
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
