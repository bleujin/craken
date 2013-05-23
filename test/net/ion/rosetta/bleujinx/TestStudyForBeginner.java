package net.ion.rosetta.bleujinx;

import java.util.List;

import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;
import net.ion.rosetta.Scanners;
import net.ion.rosetta.functors.Map;
import net.ion.rosetta.functors.Pair;
import net.ion.rosetta.node.Expression;
import net.ion.rosetta.node.RectExpression;
import net.ion.rosetta.node.RotateExpression;
import net.ion.rosetta.node.TranslateExpression;


import scala.reflect.generic.UnPickler.Scan;

import junit.framework.TestCase;

public class TestStudyForBeginner extends TestCase{

	
	private Parser<Integer> whitespaceInteger(){
		return Scanners.WHITESPACES.next(Scanners.INTEGER).map(new Map<String, Integer>() {
			@Override
			public Integer map(String from) {
				return Integer.parseInt(from);
			}
		}) ;
	}
	
	public void testWhitespaceInteger() throws Exception {
		assertEquals(new Integer(19), whitespaceInteger().parse("19")) ;
		assertEquals(new Integer(19), whitespaceInteger().parse("    19")) ;
		assertEquals(new Integer(20), whitespaceInteger().parse(" 20")) ;
	}
	
	private Parser<RotateExpression> rotateExpression(){
		return Scanners.string("rotate").next(whitespaceInteger())
		.map(new Map<Integer, RotateExpression>() {
			@Override
			public RotateExpression map(Integer from) {
				return new RotateExpression(from);
			}
		}) ;
	}
	
	
	public void testRotateExpressionParser() throws Exception {
		assertEquals(new RotateExpression(90), rotateExpression().parse("rotate  90")) ;
	}
	
	
	private Parser<TranslateExpression> translateExpression(){
		return Scanners.string("translate").next(Parsers.tuple(whitespaceInteger(), whitespaceInteger()))
			.map(new Map<Pair<Integer, Integer>, TranslateExpression>() {
				@Override
				public TranslateExpression map(Pair<Integer, Integer> pair) {
					return new TranslateExpression(pair.a, pair.b);
				}
			}) ;
	}
	
	
	public void testTranslateExpressionParser() throws Exception {
		assertEquals(new TranslateExpression(50, 50), translateExpression().parse("translate 50 50")) ;
	}

	private Parser<RectExpression> rectExpression() {
		return Scanners.string("rect").next(whitespaceInteger().times(4))
		.map(new Map<List<Integer>, RectExpression>() {
			@Override
			public RectExpression map(List<Integer> list) {
				return new RectExpression(list.get(0), list.get(1), list.get(2), list.get(3));
			}
		}) ;
	}

	
	
	public void testRectExpressionParser() throws Exception {
		assertEquals(new RectExpression(10, 10, 20, 20), rectExpression().parse("rect  10  10  20  \n 20")) ;
	}
	
	
	private Parser<Expression> expresssion(){
		return Parsers.or(rectExpression(), translateExpression(), rotateExpression()) ;
	}
	
	public void testExpressionParserTest() throws Exception {
		assertEquals(new TranslateExpression(10, 20), expresssion().parse("translate 10 20")) ;
		assertEquals(new RectExpression(10, 10, 20, 20), expresssion().parse("rect 10 10 20 20")) ;
	}
	
	private Parser<List<Expression>> block(){
		return Parsers.between(Scanners.string("do").next(Scanners.WHITESPACES), expresssion().sepBy(Scanners.WHITESPACES), Scanners.WHITESPACES.next(Scanners.string("end"))) ;
	}
	
	public void testBlockParserTest() throws Exception {
		assertEquals("[[TranslateExpression(10, 20)]]", block().parse("do translate 10 20 end").toString()) ;
	}
	
	
	
	public Parser<List<List<Expression>>> geoGrammer(){
		return block().sepBy(Scanners.WHITESPACES) ;
	}
	
	public void testFullTest() throws Exception {
		Debug.line(geoGrammer().parse(example).toString()) ;
	}
	


	static final String example = "" +
			"do\n" +
			"  rect 0 0 1 1\n" +
			"  rotate 60\n" + 
			"end\n" + 
			"do\n" +
			"  rect 0 0 1 1\n" +
			"  translate 50 50\n" +
			"end" ;

}
