package net.ion.rosetta.bleujinx;

import java.math.BigDecimal;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.NumberUtil;
import net.ion.rosetta.OperatorTable;
import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;
import net.ion.rosetta.Scanners;
import net.ion.rosetta.Terminals;
import net.ion.rosetta.Tokens;
import net.ion.rosetta.bleujinx.Expression.Op;
import net.ion.rosetta.functors.Binary;
import net.ion.rosetta.functors.Map;
import net.ion.rosetta.pattern.Patterns;

import org.apache.commons.lang.builder.ToStringBuilder;

public class TestFirst extends TestCase {

	private static Parser<StringConstant> stringConstantParser(){
		
		return Scanners.quoted('\'', '\'').map(new Map<String, StringConstant>() {
			@Override
			public StringConstant map(String from) {
				return new StringConstant(net.ion.framework.util.StringUtil.substringBetween(from, "'", "'"));
			}
		}) ;
	}
	
	public void testStringConstant() throws Exception {
		StringConstant sc = stringConstantParser().parse("'foo'");
		assertEquals("foo", sc.value()) ;
	}
	
	private static Parser<DecimalConstant> decimalConstantParser(){
		return Scanners.DECIMAL.map(new Map<String, DecimalConstant>() {
			@Override
			public DecimalConstant map(String from) {
				return new DecimalConstant(NumberUtil.createBigDecimal(from));
			}
		}) ;
	}
	
	public void testDecimalParser() throws Exception {
		DecimalConstant dc = decimalConstantParser().parse("123") ;
		assertEquals(123, dc.asInt()) ;
	}
	
	private static Parser<? extends Valuable> expressionParser(){
		return Parsers.or(stringConstantParser(), decimalConstantParser()) ;
	}
	
	private static final Terminals OPERATORS = Terminals.operators("==", ">", ">=", "<", "<=", "(", ")");
	static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
	
	static Parser<?> term(String... names) {
		return OPERATORS.token(names);
	}
	
	
	enum BinaryOperator implements Binary<Valuable> {
		EQ {
			public Valuable map(Valuable a, Valuable b) {
				return new Expression(a, b, Op.EQ);
			}
		},
		GT {
			public Valuable map(Valuable a, Valuable b) {
				return new Expression(a, b, Op.GT);
			}
		},
		LT {
			public Valuable map(Valuable a, Valuable b) {
				return new Expression(a, b, Op.LT);
			}
		},
		LTE {
			public Valuable map(Valuable a, Valuable b) {
				return new Expression(a, b, Op.LTE);
			}
		}, 
		GTE {
			public Valuable map(Valuable a, Valuable b) {
				return new Expression(a, b, Op.GTE);
			}
		}
	}
	
	static <T> Parser<T> op(String name, T value) {
		return term(name).retn(value);
	}

	
	static Parser<Valuable> composite(Parser<? extends Valuable> atom) {
		Parser.Reference<Valuable> ref = Parser.newReference();
		Parser<Valuable> unit = ref.lazy().between(term("("), term(")")).or(atom);
		Parser<Valuable> parser = new OperatorTable<Valuable>()
			.infixl(op("==", BinaryOperator.EQ), 10)
			.infixl(op(">", BinaryOperator.GT), 10)
			.infixl(op("<", BinaryOperator.LT), 10)
			.infixl(op(">=", BinaryOperator.GTE), 10).build(unit);
		ref.set(parser);
		return parser;
	}

	private static final Parser<? extends Valuable> VALUE = 
//		Parsers.or(Scanners.SINGLE_QUOTE_CHAR.peek(), Terminals.DecimalLiteral.PARSER).map(new Map<String, Valuable>(){
		Terminals.StringLiteral.PARSER.map(new Map<String, Valuable>(){

			@Override
			public Valuable map(String from) {
				// TODO Auto-generated method stub
				return null;
			}
			
//		Terminals.DecimalLiteral.PARSER.map(new Map<String, Valuable>(){
//		@Override
//		public Valuable map(String from) {
//			return new DecimalConstant(NumberUtil.createBigDecimal(from));
//		}
		
	}) ;
		
//		Parsers.or(stringConstantParser(), decimalConstantParser()) ;
	
	public static final Parser<String> SINGLE_QUOTE_STRING = Scanners.pattern(Patterns.regex("((\\\\.)|[^\'\\\\])*"), "quoted string").between(Scanners.isChar('\''), Scanners.isChar('\'')).source();
	
	
	private static final Parser<Valuable> TOKEN = Terminals.StringLiteral.PARSER.map(new net.ion.rosetta.functors.Map<String, Valuable>() {
		public Valuable map(String s) {
			return new StringConstant(s);
		}
	});
	
	static final Parser<Double> NUMBER = Terminals.DecimalLiteral.PARSER.map(new Map<String, Double>() {
		public Double map(String s) {
			return Double.valueOf(s);
		}
	});
	
	static final Parser<Object> TOKENIZER = Parsers.<Object> or(
				SINGLE_QUOTE_STRING.map(new Map<String, String>() {
					public String map(String text) {
						return text.substring(1, text.length() - 1).replace("\\'", "'");
					}
				}), 
				Terminals.DecimalLiteral.TOKENIZER.map(new Map<Tokens.Fragment, String>() {
					@Override
					public String map(Tokens.Fragment from) {
						return from.text();
					}
				}), 
				OPERATORS.tokenizer());
	
	
	
	public void testComposite() throws Exception {
		Valuable result = composite(TOKEN).from(TOKENIZER, IGNORED).parse("'#a' > 333");
		Debug.line(result) ;
	}
	
	

//	static final Parser<Valuable> START = expressionParser().map(new Map<String, Valuable>() {
//		public Valuable map(String s) {
//			return expressionParser().;
//		}
//	});

	
}

class Expression implements Valuable<Boolean>{
	
	enum Op {
		EQ {
			public boolean is(Object left, Object right){
				return left.equals(right) ;
			}
		}, GT{
			public boolean is(Object left, Object right){
				return true ;
			}
		}, LT{
			public boolean is(Object left, Object right){
				return true ;
			}
		}, GTE{
			public boolean is(Object left, Object right){
				return true ;
			}
		}, LTE{
			public boolean is(Object left, Object right){
				return true ;
			}
		} ;
		
		public abstract boolean is(Object left, Object right) ;
	}

	private Valuable a ;
	private Valuable b ;
	private Op op ;

	public Expression(Valuable a, Valuable b, Op op) {
		this.a = a ;
		this.b = b ;
		this.op = op ;
	}

	@Override
	public Boolean value() {
		return op.is(a.value(), b.value());
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
	
}


interface Valuable<T> {
	public T value() ;
}



class StringConstant implements Valuable<String> {

	private String value ;
	public StringConstant(String value) {
		this.value = value ;
	}
	
	public String value() {
		return value;
	}
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
}

class DecimalConstant implements Valuable<Long> {

	private BigDecimal decimal ;
	public DecimalConstant(BigDecimal decimal) {
		this.decimal = decimal ;
	}
	
	public int asInt(){
		return decimal.intValue() ;
	}
	
	public Long value(){
		return decimal.longValue() ;
	}
}

