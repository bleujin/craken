package net.ion.craken.node.search;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.Analyzer;

import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.node.crud.ReadSessionImpl;
import net.ion.craken.node.crud.WriteSessionImpl;
import net.ion.nsearcher.config.Central;

public class ReadSearchSession implements ReadSession {

	private final Credential credential ;
	private WorkspaceSearch workspace;
	private final Central central ;
	private Analyzer analyzer ;
	
	ReadSearchSession(Credential credential, WorkspaceSearch workspace, Central central) {
		this.credential = credential.clearSecretKey() ;
		this.workspace = workspace ;
		this.central = central ;
	}

	@Override
	public boolean exists(String fqn) {
		return workspace.exists(fqn);
	}

	@Override
	public ReadNode pathBy(String fqn) {
		return ReadNodeImpl.load(workspace.getNode(fqn));
	}

	@Override
	public ReadNode root() {
		return pathBy("/") ;
	}

	@Override
	public <T> Future<T> tran(TransactionJob<T> tjob) {
		return tran(tjob, TranExceptionHandler.PRINT);
	}

	public <T> T tranSync(TransactionJob<T> tjob) {
		try {
			return tran(tjob).get() ;
		} catch (InterruptedException e) {
			throw new IllegalStateException(e) ;
		} catch (ExecutionException e) {
			throw new IllegalStateException(e) ;
		}
	}

	@Override
	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSearchSession wsession = new WriteSearchSession(this, workspace, central);
		return workspace.tran(wsession, tjob, handler) ;
	}

	public SearchQuery createQuery() throws IOException, InterruptedException, ExecutionException {
//		workspace.awaitIndex() ;
		return SearchQuery.create(this, central.newSearcher()) ;
	}

	public Credential credential(){
		return credential ;
	}
	
	public String wsName() {
		return workspace.wsName();
	}


}
