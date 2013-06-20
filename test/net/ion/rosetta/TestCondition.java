package net.ion.rosetta;

import junit.framework.TestCase;
import net.ion.rosetta.functors.Binary;
import net.ion.rosetta.functors.Map;
import net.ion.rosetta.node.BinaryExpression;
import net.ion.rosetta.node.Expression;

public class TestCondition extends TestCase {
	
	public void testCal() throws Exception {
		String source = "node.a > 3" ;
		
		Condition.CONDITION.parse(source) ;
	}

}


class Condition {

	enum BinaryOperator implements Binary<Expression> {
		GT {
			public Expression map(Expression a, Expression b) {
				return new BinaryExpression(a, b, ">");
			}
		},
		GTE {
			public Expression map(Expression a, Expression b) {
				return new BinaryExpression(a, b, ">=");
			}
		}
	}

	static final Parser<Expression> STRING = Terminals.StringLiteral.PARSER.map(new Map<String, Expression>() {
		public Expression map(String s) {
			return Expression.create(s);
		}
	});

	private static final Terminals OPERATORS = Terminals.operators(">", ">=", "(", ")");

	static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	static final Parser<?> TOKENIZER = Parsers.or((Parser)Terminals.DecimalLiteral.TOKENIZER, OPERATORS.tokenizer());

	static Parser<?> term(String... names) {
		return OPERATORS.token(names);
	}

	static <T> Parser<T> op(String name, T value) {
		return term(name).retn(value);
	}

	static Parser<Expression> condition(Parser<Expression> atom) {
		Parser.Reference<Expression> ref = Parser.newReference();
		Parser<Expression> unit = ref.lazy().between(term("("), term(")")).or(atom);
		Parser<Expression> parser = new OperatorTable<Expression>().infixl(op(">", BinaryOperator.GT), 10).infixl(op(">=", BinaryOperator.GTE), 10).build(unit);
		ref.set(parser);
		return parser;
	}

	public static final Parser<Expression> CONDITION = condition(STRING).from(TOKENIZER, IGNORED);
}