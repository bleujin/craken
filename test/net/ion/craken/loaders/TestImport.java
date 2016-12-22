package net.ion.craken.loaders;

import java.io.FileInputStream;
import java.util.Date;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.PGWorkspaceConfigBuilder;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.framework.db.DBController;
import net.ion.framework.db.manager.PostSqlDataSource;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import junit.framework.TestCase;

public class TestImport extends TestCase {

	
	public void testReadFile() throws Exception {
		FileInputStream fis = new FileInputStream("./resource/export.json") ;
		String readed = IOUtil.toStringWithClose(fis) ;
		
		JsonObject json = JsonObject.fromString(readed) ;
		int i = 0 ;
		for(String fqn : json.keySet()){
			i++ ;
			JsonObject props = json.asJsonObject(fqn) ;
			for(String pid : props.keySet()){
				JsonObject pvalue = props.asJsonObject(pid) ;
				pvalue.asString("vtype") ;
				pvalue.asJsonArray("vals") ;
			}
		}
		Debug.line(i);
	}
	
	public void testSaveToPG() throws Exception {
//		Craken craken = Craken.inmemoryCreateWithTest() ;
//		craken.createWorkspace("pg", new PGWorkspaceConfigBuilder("./resource/store/test")) ;
//		ReadSession session = craken.login("pg") ;

		
		DBController dc = new DBController(new PostSqlDataSource("jdbc:postgresql://127.0.0.1:5432/crawl", "bleujin", "bleujin")) ;
		dc.initSelf(); 

		FileInputStream fis = new FileInputStream("./resource/export.json") ;
		String readed = IOUtil.toStringWithClose(fis) ;
		
		JsonObject allNodes = JsonObject.fromString(readed) ;
		
		for(String fqn : allNodes.keySet()){
			
			if ("/".equals(fqn)) continue ;
			
			JsonObject node = new JsonObject() ;
			
			JsonObject props = allNodes.asJsonObject(fqn) ;
			JsonObject propValue = new JsonObject() ;
			for(String pid : props.keySet()){
				propValue.put(pid, props.asJsonObject(pid)) ;
			}
			
			node.put("__value", propValue) ;
			node.put("__parent", Fqn.fromString(fqn).getParent().toJson()) ;
			node.put("__lastmodified", new Date().getTime() + "") ;
			
			dc.createUserProcedure("node@mergeWith(?,?,?)").addParam(fqn).addParam(Fqn.fromString(fqn).name()).addParam(node.toString()).execUpdate() ;
		}
	
		dc.destroySelf();
	}
}
