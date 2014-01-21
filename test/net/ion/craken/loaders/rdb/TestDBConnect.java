package net.ion.craken.loaders.rdb;

import junit.framework.TestCase;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.ICombinedUserProcedures;
import net.ion.framework.db.procedure.IUserCommand;
import net.ion.framework.db.procedure.IUserCommandBatch;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

public class TestDBConnect extends TestCase  {

	
	public void xtestConnect() throws Exception {
		IDBController dc = RDBWorkspaceConfig.createDefault().buildDBController() ;
		dc.initSelf() ;
		
		
		ICombinedUserProcedures cpt = dc.createCombinedUserProcedures("yabb");
		IUserCommandBatch batch = dc.createUserCommandBatch("insert into craken_tblt(fqn, props) values(?, ?)");
		for (int i = 0; i < 0 ; i++) {
			batch.addBatchParam(0, "/bleujin/" + i) ;
			batch.addBatchClob(1, RandomUtil.nextRandomString(20)) ;
		}
		cpt.add(batch, ICombinedUserProcedures.UPDATE_COMMAND) ;
		IUserCommand select = dc.createUserCommand("select * from craken_tblt");
		cpt.add(select, "select", ICombinedUserProcedures.QUERY_COMMAND) ;
		
		cpt.execUpdate() ;
		Object result = cpt.getResultMap().get("select") ;
		
		Debug.line(result) ;
		
		dc.destroySelf() ;
	}

}
