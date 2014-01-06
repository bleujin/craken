package net.ion.craken.expression;

import static net.ion.craken.expression.ExpressionParser.NUMBER;
import static net.ion.craken.expression.ExpressionParser.QUALIFIED_NAME;
import static net.ion.craken.expression.ExpressionParser.STRING;
import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;

public class TestColumnProjection extends TestCase {

	public void tesColumn() throws Exception {
		Parser<Expression> atom = Parsers.or(NUMBER, STRING, QUALIFIED_NAME); // WILDCARD, 
		Parser<Projection> parser = ExpressionParser.projection(atom);
		
		Debug.line(TerminalParser.parse(parser, "1")) ; 
		Debug.line(TerminalParser.parse(parser, "1 id")) ; 
		Debug.line(TerminalParser.parse(parser, "1 as id")) ; 
	}
	
	public void testColumns() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		Debug.line(TerminalParser.parse(parser, "1, 2, 'abc', this.name")) ; 
	}

	public void testSimpleCaseWhen() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		Debug.line(TerminalParser.parse(parser, "case this.name when 'bleujin' then 'self' else 'other' end name")) ; 
		Debug.line(TerminalParser.parse(parser, "case this.name when 'bleujin' then 'self' else 'other' end as name")) ; 
	}
	
	public void testFullCaseWhen() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		Debug.line(TerminalParser.parse(parser, "case when /*+ comment */ (this.age > 20) then 'self' else 'other' end as name")) ; 
	}
	
	public void testRelationName() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, "a.b b, a.c d");
		
		
		Debug.line(sp) ;
	}
	
	public void testParent() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, "parent.b b");
		
		
		Debug.line(sp) ;
	}

}
