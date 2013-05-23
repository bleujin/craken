package net.ion.rosetta.query;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import net.ion.rosetta.OperatorTable;
import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;
import net.ion.rosetta.Scanners;
import net.ion.rosetta.Terminals;
import net.ion.rosetta.functors.Binary;
import net.ion.rosetta.functors.Map;
import net.ion.rosetta.pattern.Patterns;

public class QueryParser {

	private enum BinaryOperator implements Binary<Filter> {
		AND {
			public Filter map(Filter a, Filter b) {
				return new And(a, b);
			}
		},
		OR {
			public Filter map(Filter a, Filter b) {
				return new Or(a, b);
			}
		}
	}

	public static final Parser<String> SINGLE_QUOTE_STRING = Scanners.pattern(Patterns.regex("((\\\\.)|[^\'\\\\])*"), "quoted string").between(Scanners.isChar('\''), Scanners.isChar('\'')).source();
	public static final Map<String, String> SINGLE_QUOTE_STRING_MAP = new Map<String, String>() {
		public String map(String text) {
			return text.substring(1, text.length() - 1).replace("\\'", "'");
		}

		@Override
		public String toString() {
			return "SINGLE_QUOTE_STRING";
		}
	};

	private static final Parser<Filter> TOKEN = Terminals.StringLiteral.PARSER.map(new net.ion.rosetta.functors.Map<String, Filter>() {
		public Token map(String s) {
			return new Token(s);
		}
	});

	private static final Terminals OPERATORS = Terminals.operators("+", ",", "(", ")");

	private static final Parser<Object> TOKENIZER = Parsers.<Object> or(SINGLE_QUOTE_STRING.map(SINGLE_QUOTE_STRING_MAP), OPERATORS.tokenizer());

	private static Parser<?> term(String... names) {
		return OPERATORS.token(names);
	}

	private static <T> Parser<T> op(String name, T value) {
		return term(name).retn(value);
	}

	private static Parser<Filter> query(Parser<Filter> atom) {
		Parser.Reference<Filter> ref = Parser.newReference();
		Parser<Filter> unit = ref.lazy().between(term("("), term(")")).or(atom);
		Parser<Filter> parser = new OperatorTable<Filter>().infixl(op(",", BinaryOperator.OR), 10).infixl(op("+", BinaryOperator.AND), 20).build(unit);
		ref.set(parser);
		return parser;
	}

	private static Parser<Void> nodelim = Parsers.always();

	private static final Parser<Filter> parser = query(TOKEN).from(TOKENIZER, nodelim);

	public static Filter parse(String source) {
		/*
		 * String decoded = ""; char[] chars = source.toCharArray(); for (int i = 0; i < chars.length; i++) { if (chars[i] == '%') { int a = chars[++i]; int b = chars[++i]; if (a >= 'A') a = a-'A'; else a = a-'0'; if (b >= 'A') b = b-'A'; else b = b-'0'; decoded += (char)(16*a+b); } decoded += chars[i]; }
		 */
		source = source.replace("+", "%2B");
		try {
			source = URLDecoder.decode(source, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println(source);
		return parser.parse(source);
	}

	public static void main(String args[]) {
		// System.out.println(SINGLE_QUOTE_STRING.parse("'foo'"));
		System.out.println(QueryParser.parse("'hi: bye'+'ho!:bo+ o\\\'n'"));
	}
}
