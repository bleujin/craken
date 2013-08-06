package net.ion.craken.node.crud;

import net.ion.craken.expression.Expression;
import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.TerminalParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.express.ExpressUtils;
import net.ion.framework.util.express.InfixExpress;
import net.ion.nsearcher.search.filter.BooleanFilter;
import net.ion.nsearcher.search.filter.TermFilter;
import net.ion.rosetta.Parser;

import org.apache.lucene.search.Filter;

import junit.framework.TestCase;

public class TestFilters extends TestCase {

	public void testExpression() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "a=='2'&&b=3||c in(3,4)");

		Debug.line(result) ;
	}

	public void testOneCondition() throws Exception {
		Filter filter = Filters.where("ta=='2'");
		assertEquals(true, filter instanceof TermFilter) ;
	}

	public void testAndCondition() throws Exception {
		Filter filter = Filters.where("ta=='2'&&b=3");
		assertEquals(true, filter instanceof BooleanFilter) ;
	}
	
	public void testBraceCondition() throws Exception {
		Filter filter = Filters.where("(ta=='2'&&b=3)||c=4");
		assertEquals(true, filter instanceof BooleanFilter) ;
	}
	
	public void testBetween() throws Exception {
		Filter filter = Filters.where("ta between 3 and 4");
		Debug.line(filter) ;
	}

	public void testIn() throws Exception {
		Filter filter = Filters.where("ta in (3, 4)");
		Debug.line(filter) ;
	}

}
