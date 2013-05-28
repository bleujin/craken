package net.ion.craken.expression;

import java.util.List;

import net.ion.craken.expression.Expression;
import net.ion.rosetta.OperatorTable;
import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;
import net.ion.rosetta.Parser.Reference;
import net.ion.rosetta.functors.Binary;
import net.ion.rosetta.functors.Pair;
import net.ion.rosetta.functors.Unary;
import net.ion.rosetta.misc.Mapper;
import static net.ion.craken.expression.TerminalParser.parse;
import static net.ion.craken.expression.TerminalParser.phrase;
import static net.ion.craken.expression.TerminalParser.term;


public class ExpressionParser {

	static final Parser<Expression> NULL = TerminalParser.term("null").<Expression> retn(NullExpression.instance);
	
	public static final Parser<Expression> NUMBER = curry(NumberExpression.class).sequence(TerminalParser.NUMBER);

	static final Parser<Expression> QUALIFIED_NAME = curry(QualifiedNameExpression.class).sequence(TerminalParser.QUALIFIED_NAME);

	static final Parser<Expression> QUALIFIED_WILDCARD = curry(WildcardExpression.class).sequence(TerminalParser.QUALIFIED_NAME, TerminalParser.phrase(". *"));

	static final Parser<Expression> WILDCARD = TerminalParser.term("*").<Expression> retn(new WildcardExpression(QualifiedName.of())).or(QUALIFIED_WILDCARD);

	static final Parser<Expression> STRING = curry(StringExpression.class).sequence(TerminalParser.STRING);

	
	public static Parser<Expression> expression(){
		Reference<Expression> conditionRef = Parser.newReference();
		Parser<Expression> expr = ExpressionParser.expression(conditionRef.lazy());
		Parser<Expression> cond = ExpressionParser.condition(expr);
		conditionRef.set(cond) ;
		return cond ;
	}
	
	
	static Parser<Expression> functionCall(Parser<Expression> param) {
		return curry(FunctionExpression.class).sequence(TerminalParser.QUALIFIED_NAME, term("("), param.sepBy(TerminalParser.term(",")), term(")"));
	}

	static Parser<Expression> tuple(Parser<Expression> expr) {
		return curry(TupleExpression.class).sequence(term("("), expr.sepBy(term(",")), term(")"));
	}

	static Parser<Expression> simpleCase(Parser<Expression> expr) {
		return curry(SimpleCaseExpression.class).sequence(term("case"), expr, whenThens(expr, expr), term("else").next(expr).optional(), term("end"));
	}

	static Parser<Expression> fullCase(Parser<Expression> cond, Parser<Expression> expr) {
		return curry(FullCaseExpression.class).sequence(term("case"), whenThens(cond, expr), term("else").next(expr).optional(), term("end"));
	}
	
	private static Parser<List<Pair<Expression, Expression>>> whenThens(Parser<Expression> cond, Parser<Expression> expr) {
		return Parsers.pair(term("when").next(cond), term("then").next(expr)).many1();
	}

	static <T> Parser<T> paren(Parser<T> parser) {
		return parser.between(term("("), term(")"));
	}
	
	static Parser<Expression> arithmetic(Parser<Expression> atom) {
		Reference<Expression> reference = Parser.newReference();
		Parser<Expression> operand = Parsers.or(paren(reference.lazy()), functionCall(reference.lazy()), atom);
		Parser<Expression> parser = new OperatorTable<Expression>().infixl(binary("+", Op.PLUS), 10).infixl(binary("-", Op.MINUS), 10).infixl(binary("*", Op.MUL), 20).infixl(binary("/", Op.DIV), 20)
				.infixl(binary("%", Op.MOD), 20).prefix(unary("-", Op.NEG), 50).build(operand);
		reference.set(parser);
		return parser;
	}
	
	static Parser<Expression> expression(Parser<Expression> cond) {
		Reference<Expression> reference = Parser.newReference();
		Parser<Expression> lazyExpr = reference.lazy();
		Parser<Expression> atom = Parsers.or(NUMBER, STRING, WILDCARD, QUALIFIED_NAME, simpleCase(lazyExpr), fullCase(cond, lazyExpr));
		Parser<Expression> expression = arithmetic(atom).label("expression");
		reference.set(expression);
		return expression;
	}
	
	static Parser<Expression> condition(Parser<Expression> expr) {
		Parser<Expression> atom = Parsers.or(compare(expr), in(expr), notIn(expr));
		return logical(atom);
	}
	
	
	/** boolean **/
	
	static Parser<Expression> compare(Parser<Expression> expr) {
		return Parsers.or(compare(expr, ">", Op.GT), compare(expr, ">=", Op.GE), compare(expr, "<", Op.LT), compare(expr, "<=", Op.LE), compare(expr, "=", Op.EQ), compare(expr, "<>", Op.NE),
				nullCheck(expr), like(expr), between(expr));
	}

	static Parser<Expression> like(Parser<Expression> expr) {
		return curry(LikeExpression.class).sequence(expr, Parsers.or(term("like").retn(true), phrase("not like").retn(false)), expr, term("escape").next(expr).optional());
	}

	static Parser<Expression> nullCheck(Parser<Expression> expr) {
		return curry(BinaryExpression.class).sequence(expr, phrase("is not").retn(Op.NOT).or(phrase("is").retn(Op.IS)), NULL);
	}

	static Parser<Expression> logical(Parser<Expression> expr) {
		Reference<Expression> ref = Parser.newReference();
		Parser<Expression> parser = new OperatorTable<Expression>().prefix(unary("not", Op.NOT), 30).infixl(binary("and", Op.AND), 20).infixl(binary("or", Op.OR), 10)
				.build(paren(ref.lazy()).or(expr)).label("logical expression");
		ref.set(parser);
		return parser;
	}

	static Parser<Expression> between(Parser<Expression> expr) {
		return curry(BetweenExpression.class).sequence(expr, Parsers.or(term("between").retn(true), phrase("not between").retn(false)), expr, term("and"), expr);
	}
	
	static Parser<Expression> in(Parser<Expression> expr) {
		return binaryExpression(Op.IN).sequence(expr, term("in"), tuple(expr));
	}

	static Parser<Expression> notIn(Parser<Expression> expr) {
		return binaryExpression(Op.NOT_IN).sequence(expr, phrase("not in"), tuple(expr));
	}


	
	/** logical **/
	


	
	
	
	/** utility **/
	
	private static Parser<Expression> compare(Parser<Expression> operand, String name, Op op) {
		return curry(BinaryExpression.class).sequence(operand, term(name).retn(op), operand);
	}

	
	private static Parser<Binary<Expression>> binary(String name, Op op) {
		return term(name).next(binaryExpression(op).binary());
	}
	private static Parser<Unary<Expression>> unary(String name, Op op) {
		return term(name).next(unaryExpression(op).unary());
	}
	private static Mapper<Expression> binaryExpression(Op op) {
		return curry(BinaryExpression.class, op);
	}
	
	private static Mapper<Expression> unaryExpression(Op op) {
		return curry(UnaryExpression.class, op);
	}

	private static Mapper<Expression> curry(Class<? extends Expression> clazz, Object... args) {
		return Mapper.curry(clazz, args);
	}
}
