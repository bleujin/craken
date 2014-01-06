package net.ion.craken.expression;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;

public class TestExpressionParser extends TestCase {

	public void testFirst() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		
		Expression result = TerminalParser.parse(parser, "this.a <= '2'");
		Debug.line(result) ;
	}
	

	public void testDefaultExpression() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		
		Expression result = TerminalParser.parse(parser, "(this.a <= '2' and this.b=3) or this.c like 'a%'");
		Debug.line(result) ;
	}
	
	public void testTuple() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "this.a in ('2','3', '4')");
		Debug.line(result) ;

		result = TerminalParser.parse(parser, "this.a not in ('2','3', '4')");
		Debug.line(result) ;

	}

	public void testBetween() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "this.a between 3 and 4");
		Debug.line(result) ;
	}
	

	public void testIsNull() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "this.a is null");
		Debug.line(result) ;
	}
	

//	public void testWildcard() throws Exception {
//		Parser<Expression> parser = ExpressionParser.expression();
//		Expression result = TerminalParser.parse(parser, "this.* = 3");
//		Debug.line(result) ;
//	}

	public void testFunction() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "substr(this.a, 2, 3) = '222'");
		Debug.line(result) ;
	}
	

	public void testUnary() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "not (this.a > 3) ");
		Debug.line(result) ;
	}
	
	public void testOp() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "this.a * this.b + 2 > 3");
		Debug.line(result) ;
	}
	
	public void testSimpleCaseWhen() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "case this.a when 'a' then 'd' end = 'd'");
		Debug.line(result) ;
	}
	
	public void testFullCaseWhen() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "case when (this.a >= 'a') then this.b else this.c end = 'd'");
		Debug.line(result) ;
		
	}
	
	public void testTrue() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "this.a = true");
		Debug.line(result) ;
	}
	
	
}
