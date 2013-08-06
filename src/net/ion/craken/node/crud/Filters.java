package net.ion.craken.node.crud;

import java.nio.channels.UnsupportedAddressTypeException;

import net.ion.craken.expression.BinaryExpression;
import net.ion.craken.expression.Expression;
import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.TerminalParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectUtil;
import net.ion.nsearcher.config.SearchConfig;
import net.ion.nsearcher.search.filter.BooleanFilter;
import net.ion.nsearcher.search.filter.FilterUtil;
import net.ion.nsearcher.search.filter.TermFilter;
import net.ion.rosetta.Parser;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;

public class Filters {

	public static Filter eq(String field, Object value) {
		return new TermFilter(field, ObjectUtil.toString(value)) ;
	}

	public static Filter ne(String field, String value) {
		final BooleanFilter inner = new BooleanFilter();
		inner.add(new FilterClause(new TermFilter(field, ObjectUtil.toString(value)), Occur.MUST_NOT)) ;

		return inner;
	}

	public static Filter gte(String field, long min) {
		return NumericRangeFilter.newLongRange(field, min, Long.MAX_VALUE, true, true) ;
	}

	public static Filter gte(String field, String lowerTerm) {
		return new TermRangeFilter(field, lowerTerm, null, true, false) ;
	}

	public static Filter gt(String field, long min) {
		return NumericRangeFilter.newLongRange(field, min, Long.MAX_VALUE, false, true) ;
	}

	public static Filter wildcard(String field, Object value) {
		return new QueryWrapperFilter(new WildcardQuery(new Term(field, ObjectUtil.toString(value)))) ;
	}

	public static Filter query(SearchConfig sconfig, String query) throws ParseException {
		return new QueryWrapperFilter(sconfig.parseQuery(query)) ;
	}

	public static Filter lte(String field, long max) {
		return NumericRangeFilter.newLongRange(field, Long.MIN_VALUE, max, true, true) ;
	}

	public static Filter gt(String field, String lowerTerm) {
		return new TermRangeFilter(field, lowerTerm, null, false, false) ;
	}

	public static Filter lte(String field, String higherTerm) {
		return new TermRangeFilter(field, null, higherTerm, false, true) ;
	}

	public static Filter in(String field, String[] values) {
		TermsFilter filter = new TermsFilter();
		for (String value : values) {
			filter.addTerm(new Term(field, value)) ;
		}
		
		return filter;
	}

	public static Filter between(String field, long min, long max) {
		return NumericRangeFilter.newLongRange(field, min, max, true, true) ;
	}

	public static Filter lt(String field, long max) {
		return NumericRangeFilter.newLongRange(field, Long.MIN_VALUE, max, true, false) ;
	}

	public static Filter lt(String field, String higherTerm) {
		return new TermRangeFilter(field, null, higherTerm, false, false) ;
	}

	public static Filter between(String field, String minTerm, String maxTerm) {
		return FilterUtil.and(TermRangeFilter.Less(field, maxTerm), TermRangeFilter.More(field, minTerm)) ;
	}


	public static Filter where(String fnString) {
//		if (true) throw new UnsupportedOperationException("currently not supported") ;
		Parser<Expression> parser = ExpressionParser.expression();
		Expression result = TerminalParser.parse(parser, fnString);
		
		if (! (result instanceof BinaryExpression)) throw new IllegalArgumentException("can't make filter : " + fnString) ;
		BinaryExpression bexpression = (BinaryExpression) result ;
		
		return bexpression.filter() ;
	}

	
	public static Filter and(Filter... filters){
		return FilterUtil.and(filters) ;
	}

	public static Filter or(Filter... filters){
		return FilterUtil.or(filters) ;
	}
	

//	public ChildQueryRequest lte(String field, double max) {
//	filter(NumericRangeFilter.newDoubleRange(field, Double.MIN_VALUE, max, true, true));
//	return this;
//}

//public ChildQueryRequest gt(String field, double min) {
//	filter(NumericRangeFilter.newDoubleRange(field, min, Double.MAX_VALUE, false, true));
//	return this;
//}

//	public ChildQueryRequest gte(String field, double min) {
//	filter(NumericRangeFilter.newDoubleRange(field, min, Double.MAX_VALUE, true, true));
//	return this;
//}

//	public ChildQueryRequest between(String field, double min, double max) {
//		filter(NumericRangeFilter.newDoubleRange(field, min, max, true, true));
//		return this;
//	}
//	public ChildQueryRequest lt(String field, double max) {
//		filter(NumericRangeFilter.newDoubleRange(field, Double.MIN_VALUE, max, true, false));
//		return this;
//	}


}
