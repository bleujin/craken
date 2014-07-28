package net.ion.script.rhino;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class TestOnCraken extends TestCase {

	public void testEngine() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		ReadSession session = r.login("test");
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		Engine engine = Engine.createDefaultEngine();
		String result = engine.transform("${node.property(\"name\").stringValue()}", MapUtil.<String, Object>create("node", session.pathBy("/bleujin")));
		Debug.line(result) ;
		
		r.shutdown() ;
	}
	

	public void testSession() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		ReadSession session = r.login("test");
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		final MyOutput output = new MyOutput();
		session.credential().tracer(output) ;
		
		Scripter rh = Scripter.create() ;
		rh.bind("session", session) ;
		
		rh.define("sample", "new function() {"
				+ " this.exec = function(){ session.root().children().debugPrint() ; }"
				+ "}") ;
		
		rh.callFn("sample.exec", RhinoResponse.ReturnNative) ;

		Debug.line(output.readOut()) ;
		r.shutdown() ;
	}
	
	
	
	
}




