package net.ion.craken.db;

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
		throw new UnsupportedOperationException("currently not supported. if you must use, call bleujin");
	}

	@Override
	public IUserCommandBatch createUserCommandBatch(IDBController dc, String psql) {
		throw new UnsupportedOperationException("currently not supported. if you must use, call bleujin");
	}

	@Override
	public IUserProcedure createUserProcedure(IDBController dc, String psql) {
		return new CrakenUserProcedure(dc, manager, psql);
	}

	@Override
	public IUserProcedureBatch createUserProcedureBatch(IDBController dc, String psql) {
		return new CrakenUserProcedureBatch(dc, manager, psql);
	}

}
