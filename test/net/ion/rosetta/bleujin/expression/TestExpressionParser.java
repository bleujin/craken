package net.ion.rosetta.bleujin.expression;

import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;


import junit.framework.TestCase;

public class TestExpressionParser extends TestCase {

	public void testExpression() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		
		Expression result = TerminalParser.parse(parser, "(this.a <= '2' and this.b=3) or this.c like 'a%'");
		Debug.line(result) ;
	}
}
