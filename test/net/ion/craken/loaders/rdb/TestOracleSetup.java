package net.ion.craken.loaders.rdb;

import java.sql.Connection;
import java.sql.Statement;

import junit.framework.TestCase;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;

public class TestOracleSetup extends TestCase {

	public void testCallSQL() throws Exception {
		IDBController dc = RDBWorkspaceConfig.createDefault().buildDBController() ;
		dc.initSelf(); 
		
		String sql = IOUtil.toStringWithClose(this.getClass().getResourceAsStream("oracle.sql")) ;
		
		Debug.line(sql);
		dc.getRows("select * from tabs").debugPrint(); 
		
		dc.destroySelf(); 
	}
	
	public void xtestInit() throws Exception {
		IDBController dc = RDBWorkspaceConfig.createDefault().buildDBController() ;
		dc.initSelf(); 

		String sql = IOUtil.toStringWithClose(this.getClass().getResourceAsStream("oracle.sql")) ;
		String[] cmds = StringUtil.splitWorker(sql, "--SEP") ;
		for (String cmd : cmds) {
			if (StringUtil.isBlank(cmd)) continue ;
			String sqlcmd = StringUtil.trim(cmd) ;
			if (sqlcmd.endsWith(";")) sqlcmd = sqlcmd.substring(0, sqlcmd.length() -1) ;
			Debug.line(sqlcmd);
			dc.createUserCommand(sqlcmd).execUpdate() ;
		}
		
		dc.destroySelf(); 
	}
	
}
