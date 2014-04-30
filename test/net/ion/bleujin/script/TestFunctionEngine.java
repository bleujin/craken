package net.ion.bleujin.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.omg.CORBA.DynAnyPackage.Invalid;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import junit.framework.TestCase;

public class TestFunctionEngine extends TestCase {

	private ScriptEngine engine;
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ScriptEngineManager manager = new ScriptEngineManager();  
        this.engine = manager.getEngineByName("JavaScript");
        this.r = RepositoryImpl.inmemoryCreateWithTest() ;
        this.session = r.start().login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testCallFunction() throws Exception {
  
        // JavaScript code in a String  
        String script = "function fn(greeting, name) { print(greeting + ', ' + name); return name;}";  
        Debug.line(engine.eval(script)); // evaluate script  
  
        // javax.script.Invocable is an optional interface.  
        // Check whether your script engine implements or not!  
        // Note that the JavaScript engine implements Invocable interface.  
        Invocable inv = (Invocable) engine;  
  
        // invoke the global function named "hello"  
        Object result = inv.invokeFunction("fn", "hello", "Scripting!!" );
        Debug.line(result, result.getClass());
	}
	
	
	public void testBinding() throws Exception {
		engine.put("session", session);
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/afields/bleujin").property("afieldId", "bleujin") ;
				return null;
			}
			
		}) ;
		
		engine.eval(new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("afield.rs")))) ;
		Object afield = engine.get("afield") ;
		
		Invocable inv = (Invocable)engine ;
		
		Object result = inv.invokeMethod(afield, "listBy", 0, 2) ;
		Debug.debug(result, result.getClass());
	}
	
	public void testGuavaFunction() throws ScriptException, IOException, NoSuchMethodException {
		
		Object test = engine.eval(new FileReader("./test/net/ion/airkjh/script/test.js"));
		
		Invocable invoker = (Invocable)engine;
		Object result = invoker.invokeMethod(test, "fn", ListUtil.toList("a", "b", "c").iterator());
		
		Debug.debug(result);
	}
	
}
