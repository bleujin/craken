package net.ion.bleujin.atag;

import junit.framework.TestCase;
import net.ion.craken.expression.Expression;
import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.TerminalParser;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;

public class TestParser extends TestCase {

	
	public void testCondition() throws Exception {
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, "(creDay >= 3) && (a == '3' || a = 4 && exist = abc3)");
		
		
		Debug.line(result) ;
	}
	
}
