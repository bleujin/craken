package net.ion.craken.template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.ReadChildren;
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
	
	public void testTemplate() throws Exception {

		
		ReadNode found = session.pathBy("/bleujin");
		// found.children().ascending("dummy") ;
		Writer writer = new StringWriter();
		found.template("${foreach self.children child ,}${child}${end}", writer) ;
		
		Debug.line(writer) ;
	}
	
	public void testDirect() throws Exception {
		ReadNode found = session.pathBy("/bleujin");
		ReadChildren children = found.children().ascending("dummy");
		
		Engine engine = session.workspace().parseEngine();
		String result = engine.transform("${foreach children child ,}${child}${end}", MapUtil.<String, Object>create("children", children)) ;
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
		

		ReadNode found = session.pathBy("/bleujin");
		// found.children().ascending("dummy") ;
		Writer writer = new StringWriter();
		found.template("${self}", writer) ;
		
		Debug.line(writer) ;
	}
	
	
}
