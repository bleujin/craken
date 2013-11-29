package net.ion.scriptexecutor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.scriptexecutor.manager.ManagerBuilder;
import net.ion.scriptexecutor.manager.ScriptManager;
import net.ion.scriptexecutor.script.ScriptResponse;

public class TestFirst extends TestCase {
	public ScriptManager manager;

	public void setUp() throws IOException {
		manager = ManagerBuilder.createBuilder().languages(ManagerBuilder.LANG.JAVASCRIPT).build();
		manager.createRhinoScript("envjs").defineScript(new FileReader("./resource/env.rhino.1.2.js")).setPreScript();
		manager.createRhinoScript("jquery").defineScript(new FileReader("./resource/jquery-1.10.2.min.js")).setPreScript();

		// manager.createRhinoScript("preScriptBinding Script").defineScript("").bind("preBind", "preScript").setPreScript(new RhinoCompileHandler() {
		// @Override
		// public void compileFailure(EvaluatorException e) {
		// }
		// });
		//
		// manager.createRhinoScript("Javascript Function Pre Script").defineScript("var load = function(str){print(str);};").setPreScript(new RhinoCompileHandler() {
		// @Override
		// public void compileFailure(EvaluatorException e) {
		// }
		// });
		//
		// manager.createRhinoScript("preScript Script").defineScript("var str = 'preScript';").setPreScript(new RhinoCompileHandler() {
		// @Override
		// public void compileFailure(EvaluatorException e) {
		// }
		// });

		manager.start();
	}

	public void tearDown() {
		manager.shutdown();
	}

	public void testHello() throws Exception {
		ScriptResponse response = manager.createRhinoScript("File Test Script").defineScript(new FileReader("./resource/testScript.js")).execute();
		assertTrue(response.printed().startsWith("Hello World!"));
	}
	
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
		String result = engine.transform("${node.property(name).stringValue()}", MapUtil.<String, Object>create("node", session.pathBy("/bleujin")));
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
		ScriptResponse response = manager.createRhinoScript("test").bind("session", session) 
			.defineScript("session.root().children().debugPrint()").execute() ;
		
		
		Debug.line(output.readOut()) ;
		
		r.shutdown() ;
	}
}

class MyOutput extends PrintStream {

	private StringBuilder builder = new StringBuilder() ;
	public MyOutput() throws IOException {
		super(File.createTempFile("out", "osuffix"));
	}

	public void write(byte b[]) {
		String s = new String(b);
		append(s.trim(), false);
	}

	public String readOut(){
		String result = builder.toString() ;
		builder = new StringBuilder() ;
		return result ;
	}
	
	public void write(byte b[], int off, int len) {
		String s = new String(b, off, len);
		append(s.trim(), false);
	}

	public void write(int b) {
		Integer i = new Integer(b);
		append(i.toString(), false);
	}

	public void println(String s) {
		append(s, true);
	}

	public void print(String s) {
		append(s, false);
	}

	public void print(Object obj) {
		if (obj != null)
			append(obj.toString(), false);
		else
			append("null", false);
	}

	public void println(Object obj) {
		if (obj != null)
			append(obj.toString(), true);
		else
			append("null", true);
	}

	private synchronized void append(String x, boolean newline) {
		builder.append(x) ;
		if(newline) builder.append("\r\n") ;
	}

}
