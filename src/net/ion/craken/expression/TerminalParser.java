package net.ion.craken.expression;

import static net.ion.rosetta.Parsers.between;

import net.ion.rosetta.Parser;
import net.ion.rosetta.Parsers;
import net.ion.rosetta.Scanners;
import net.ion.rosetta.Terminals;
import net.ion.rosetta.Tokens.Tag;
import net.ion.rosetta.misc.Mapper;

/**
 * Lexers and terminal level parsers for SQL.
 */
public final class TerminalParser {

	private static final String[] OPERATORS = {"+", "-", "*", "/", "%", ">", "<", "=", ">=", "<=", "<>", ".", ",", "(", ")", "[", "]"};

	private static final String[] KEYWORDS = { "select", "distinct", "from", "as", "where", "group", "by", "having", "order", "asc", "desc", "and", "or", "not", "in", "exists", "between", "is",
			"null", "like", "escape", "inner", "outer", "left", "right", "full", "cross", "join", "on", "union", "all", "case", "when", "then", "else", "end" };

	private static final Terminals TERMS = Terminals.caseInsensitive(OPERATORS, KEYWORDS);

	private static final Parser<?> TOKENIZER = Parsers.or(Terminals.DecimalLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, TERMS.tokenizer());

	static final Parser<String> NUMBER = Terminals.DecimalLiteral.PARSER;
	static final Parser<String> STRING = Terminals.StringLiteral.PARSER;

	static final Parser<String> NAME = between(term("["), Terminals.fragment(Tag.RESERVED, Tag.IDENTIFIER), term("]")).or(Terminals.Identifier.PARSER);

	static final Parser<QualifiedName> QUALIFIED_NAME = Mapper.curry(QualifiedName.class).sequence(NAME.sepBy1(term(".")));

//	static final Parser<RelationName> RELATION_NAME = Mapper.curry(RelationName.class).sequence(NAME.sepBy1(term("@")));

	public static <T> T parse(Parser<T> parser, String source) {
		return parser.from(TOKENIZER, Scanners.SQL_DELIMITER).parse(source);
	}

	public static Parser<?> term(String term) {
		return Mapper._(TERMS.token(term));
	}

	public static Parser<?> phrase(String phrase) {
		return Mapper._(TERMS.phrase(phrase.split("\\s")));
	}
}
