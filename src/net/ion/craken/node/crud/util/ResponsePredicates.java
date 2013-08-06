package net.ion.craken.node.crud.util;

import net.ion.craken.expression.Expression;
import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;
import net.ion.rosetta.Parser;

public class ResponsePredicates {

	public static ResponsePredicate belowAt(final Fqn fqn) {
		return new ResponsePredicate(){
			@Override
			public boolean apply(ReadSession session, Fqn thatFqn) {
				while(! thatFqn.isRoot()){
					if (thatFqn.equals(fqn)) return true ;
					thatFqn = thatFqn.getParent() ;
				}
				return false;
			}

			@Override
			public boolean isContinue() {
				return true;
			}
		} ;
	}

	public static ResponsePredicate belowAt(final String fqn) {
		return belowAt(Fqn.fromString(fqn)) ;
	}

	
	public static ResponsePredicate where(final String where){
		return new ResponsePredicate() {
			
			@Override
			public boolean isContinue() {
				return true;
			}
			
			@Override
			public boolean apply(ReadSession session, Fqn thatFqn) {
				Parser<Expression> parser = ExpressionParser.expression();
				final Expression expression = TerminalParser.parse(parser, where);
				
				return Boolean.TRUE.equals(expression.value(session.pathBy(thatFqn)));
			}
		};
	}
	
	
	
	public static ResponsePredicate page(final Page page) {
		return new ResponsePredicate(){
			private int skip = -1 ;
			@Override
			public boolean apply(ReadSession session, Fqn thatFqn) {
				skip++ ;
				return skip >= page.getStartLoc() && skip < page.getEndLoc() ;
			}
			
			@Override
			public boolean isContinue() {
				return skip < page.getEndLoc() ;
			}
		} ;
	}
}
