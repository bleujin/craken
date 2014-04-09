package net.ion.craken.template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.mte.Engine;
import net.ion.framework.mte.Renderer;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class TestTemplate extends TestBaseCrud{

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
	}
	
	public void testDirect() throws Exception {
		ReadNode found = session.pathBy("/bleujin");
		Engine engine = session.workspace().parseEngine();
		String result = engine.transform("${foreach self.children().gte(dummy,3).lte(dummy,5).descending(dummy) child ,}${child}${end}", MapUtil.<String, Object>create("self", found)) ;
		Debug.line(result) ;
	}
	
	public void testRender() throws Exception {
		Engine engine = session.workspace().parseEngine();
		engine.config().registerRenderer(ReadNode.class, new Renderer<ReadNode>(){
			@Override
			public String render(ReadNode node, Locale locale) {
				return node.transformer(Functions.toJson()).toString() ;
			}
		}) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("html", "${foreach self.children child ,}\n${child}${end}") ;
				return null;
			}
		}) ;
		
		

		ReadNode found = session.pathBy("/bleujin");
		// found.children().ascending("dummy") ;
		Writer writer = new StringWriter();
		found.template("html", writer) ;
		
		Debug.line(writer) ;
	}
	
	public void testAsTemplate() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception { // "${foreach self.children child ,}${child}${end}"
				wsession.pathBy("/bleujin").property("template", "${foreach self.children().gte(dummy,3).lte(dummy,5).descending(dummy) child ,}${child}${end}") ;
				return null;
			}
		}) ;
		
		
		ReadNode found = session.pathBy("/bleujin");
		found.children() ;
		Writer writer = new StringWriter();
		found.template("template", writer) ;
		
		Debug.line(writer) ;
	}
	
	
}
