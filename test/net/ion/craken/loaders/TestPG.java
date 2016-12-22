package net.ion.craken.loaders;

import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.manager.PostSqlDataSource;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestPG extends TestCase {
	
	public void testCall() throws Exception {
		 DBController dc = new DBController(new PostSqlDataSource("jdbc:postgresql://127.0.0.1:5432/crawl", "bleujin", "bleujin")) ;
		 dc.initSelf(); 
		 
		 Rows rows = dc.execQuery("select 1 as col where 1 =2") ;
		 
		 Debug.line(rows.first(), rows.first(), rows.getString("col"));
		 
		 
		 dc.destroySelf() ;
	}

}
