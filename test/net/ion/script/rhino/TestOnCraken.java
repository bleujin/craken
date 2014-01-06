package net.ion.script.rhino;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class TestOnCraken extends TestBaseScript{

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
		RhinoResponse response = rengine.newScript("test").bind("session", session).defineScript("session.root().children().debugPrint()").exec();
		
		Debug.line(output.readOut()) ;
		r.shutdown() ;
	}
	
	
	
	
}




