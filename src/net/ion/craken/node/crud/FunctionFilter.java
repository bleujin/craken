package net.ion.craken.node.crud;

import java.io.IOException;

import net.ion.craken.expression.Expression;
import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.TerminalParser;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

import com.google.common.base.Predicate;

public class FunctionFilter extends Filter {
	private static final long serialVersionUID = 5988378458401369269L;
	
	private String fnExpression;
	public FunctionFilter(String fnExpression) {
		this.fnExpression = fnExpression ;
		
		
	}

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		OpenBitSet result = new OpenBitSet(reader.maxDoc());
		// TODO Auto-generated method stub
		return result;
	}

	public Filter create() {
		Parser<Expression> parser = ExpressionParser.expression();
		final Expression expression = TerminalParser.parse(parser, fnExpression);
		
		Debug.line(expression) ;
		
		return null ;
	}

}
