package net.ion.craken.node.where;

import java.util.List;

import org.apache.lucene.search.Sort;

import net.ion.craken.expression.Expression;
import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Filters;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;

public class TestWhere extends TestBaseCrud{

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					WriteNode node = wsession.pathBy("/" + i).property("num", i).property("odd", i % 2 == 0) ;
					if ( i % 2 == 0) node.property("odded", 1) ;
				}
				return null;
			}
		}) ;
	}
	
	public void testWhere() throws Exception {
		List<ReadNode> list = session.pathBy("/").children().where("((num > 5 && num <= 7) || odd = true) && exist = num").toList() ;
		assertEquals(6, list.size());
	}

	public void testWhereNot() throws Exception {
		ReadChildren found = session.pathBy("/").children().where("not (num > 5) && odd = true && exist = num");
		found.debugPrint(); 
		List<ReadNode> list = found.toList() ;
	}
	
	
	public void testQueryWhere() throws Exception {
		session.root().childQuery("").filter(Filters.where("(num >= 2)  && exist = num")).find().debugPrint();
	}
	
	public void testComposite() throws Exception {
		session.root().childQuery("").filter(Filters.where("not(num >= 2 && num < 5)  && exist = num"))
			.offset(5).skip(2).find().debugPrint();
		
		Sort sort = session.root().childQuery("").filter(Filters.where("not(num >= 2 && num < 5)  && exist = num")).sort() ;
		Debug.line(sort);
	}
	
	
	public void testQueryWhereNot() throws Exception {
		List<ReadNode> lower = session.root().childQuery("").filter(Filters.where("not (num >= 2)")).find().toList() ;
		assertEquals(2, lower.size());
		
		List<ReadNode> higher = session.root().childQuery("").filter(Filters.where("not(not (num >= 2))")).find().toList() ;
		assertEquals(8, higher.size());
	}

	public void testWhereNotAnd() throws Exception {
		ReadChildren found = session.pathBy("/").children().where("(not (num > 5))&& odd = true && exist = num");
		found.debugPrint();
	}

	
	public void testExpression() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection p = TerminalParser.parse(parser, "'abc' as a, def, substring(ddd)");
		Debug.line(p);

		Parser<Expression> ep = ExpressionParser.expression();
		Expression result = TerminalParser.parse(ep, "(num > 5 && num <= 7)"); // exist(odded)
		
		Debug.line(result);
	}

	
	 
}
