package net.ion.craken.db;

import org.apache.ecs.xhtml.del;

import net.ion.craken.node.ReadSession;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.IUserCommand;
import net.ion.framework.db.procedure.IUserCommandBatch;
import net.ion.framework.db.procedure.IUserProcedure;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.db.procedure.OracleRepositoryService;
import net.ion.framework.db.procedure.RepositoryService;

public class CrakenRepositoryService extends RepositoryService {

	private OracleRepositoryService inner = new OracleRepositoryService() ;
	private CrakenManager manager ;
	
	public CrakenRepositoryService(CrakenManager manager){
		this.manager = manager ;
	}
	
	@Override
	public IUserCommand createUserCommand(IDBController dc, String psql) {
		return null;
	}

	@Override
	public IUserCommandBatch createUserCommandBatch(IDBController dc, String psql) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IUserProcedure createUserProcedure(IDBController dc, String psql) {
		final IUserProcedure delegate = inner.createUserProcedure(dc, psql);
		return new CrakenUserProcedure(dc, manager, delegate);
	}

	@Override
	public IUserProcedureBatch createUserProcedureBatch(IDBController dc, String psql) {
		// TODO Auto-generated method stub
		return null;
	}

}
