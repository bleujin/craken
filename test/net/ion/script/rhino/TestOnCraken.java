package net.ion.script.rhino;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class TestOnCraken extends TestBaseScript{


	public void xtestEngine() throws Exception {
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
