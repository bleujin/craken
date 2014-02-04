package net.ion.rosetta.bleujinx;

import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.expression.QualifiedName;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;
import net.ion.rosetta.Scanners;
import net.ion.rosetta.Terminals;
import net.ion.rosetta.Tokens.Tag;
import net.ion.rosetta.functors.Map;
import net.ion.rosetta.misc.Mapper;

public class TestStudyToken extends TestCase {

	final static String[] OPERATORS = {"+", "-", "*", "/", "%", ">", "<", "=", "==", ">=", "<=", "<>", ".", ",", "(", ")", "[", "]", "&&", "||"};
	final static Terminals TERMS = Terminals.caseInsensitive(OPERATORS, new String[0]);
	final Parser<?> TOKENIZER =  Parsers.or(Terminals.DecimalLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, TERMS.tokenizer());

	public void testArrayExpr() throws Exception {

		Parser<String> parser = Parsers.between(term("["), Terminals.fragment(Tag.RESERVED, Tag.IDENTIFIER), term("]")).or(Terminals.Identifier.PARSER);
//		parser = arrayParser().or(Terminals.Identifier.PARSER);
		final Parser<QualifiedName> qparser = Mapper.curry(QualifiedName.class).sequence(parser.sepBy1(term("."))) ;
		
		String source = "a.d.[abc]";
//		Debug.line(TerminalParser.parse(qparser, source)) ;
		Debug.line(qparser.from(TOKENIZER, Scanners.SQL_DELIMITER).parse(source));

//		Debug.line(arrayParser().parse("'a'")) ;
	}
	
	
	public void testBetween() throws Exception {
		Parser<Object> p = Parsers.sequence(Parsers.between(term("["), Terminals.Identifier.PARSER, term("]")).or(Terminals.Identifier.PARSER).sepBy1(term("."))) ;
//		Parser<String> p = Parsers.between(term("["), Terminals.fragment(Tag.RESERVED, Tag.IDENTIFIER), term("]")).or(Terminals.Identifier.PARSER) ;
//		Debug.line(TerminalParser.parse(p, "abc")) ;
		
//		Debug.line(p.parse("abc")) ;
//		p = Terminals.Identifier.PARSER ;
		Object parsed = p.from(TOKENIZER, Scanners.SQL_DELIMITER).parse("[abc].b.c.[def]");
		Debug.debug(parsed, parsed.getClass()) ;
	}

	public void testBetween2() throws Exception {
		Parser<List<String>> np = Terminals.fragment(Tag.IDENTIFIER, Tag.RESERVED).many();
		
//		np = Parsers.between(term("["), Terminals.fragment(Tag.RESERVED, Tag.IDENTIFIER), term("]")).or(Terminals.Identifier.PARSER).many() ;
		
		Debug.line(np.from(TOKENIZER, Scanners.SQL_DELIMITER).parse("abc.b.c")) ;
		Debug.line(np.from(TOKENIZER, Scanners.SQL_DELIMITER).parse("[abc].b.c")) ;
		
	}

	
	public void testWhenBetweenParser() throws Exception {
		Parser<String> parser = Parsers.between(term("["), Terminals.fragment(Tag.RESERVED, Tag.IDENTIFIER), term("]")).or(Terminals.Identifier.PARSER);
		printParsed(parser) ;
	}
	
	
	public void testWhenArrayIdenfier() throws Exception {
		Parser<String> parser = Scanners.ARRAY_IDENTIFIER.or(Terminals.Identifier.PARSER) ;
		printParsed(parser) ;
	}
	
	public void testWhen() throws Exception {
		Parser<String> parser = Parsers.between(term("["), Terminals.Identifier.PARSER, term("]")).or(Terminals.Identifier.PARSER);
		printParsed(parser) ;
	}

	
	public void testWhen2() throws Exception {
		Parser<String> parser = term("[").next(Terminals.Identifier.PARSER).followedBy(term("]")).source().or(Terminals.Identifier.PARSER);
		printParsed(parser) ;
	}

	

	private void printParsed(Parser<String> parser){
		final Parser<QualifiedName> qparser = Mapper.curry(QualifiedName.class).sequence(parser.sepBy1(term("."))) ;
		System.out.println(qparser.from(TOKENIZER, Scanners.SQL_DELIMITER).parse("a.b.[c]")) ;
	}
	
	
	
	public static Parser<?> term(String term) {
		return Mapper._(TERMS.token(term));
	}


}





