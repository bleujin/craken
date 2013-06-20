package net.ion.craken.node.crud.util;

import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;

import com.google.common.base.Predicate;

public class ReadNodePredicate {

	public static Predicate<PredicateArgument> belowAt(final Fqn fqn) {
		return new Predicate<PredicateArgument>(){
			@Override
			public boolean apply(PredicateArgument arg) {
				Fqn that = arg.fqn() ;
				while(! that.isRoot()){
					if (that.equals(fqn)) return true ;
					that = that.getParent() ;
				}
				return false;
			}
		} ;
	}

	public static Predicate<PredicateArgument> belowAt(final String fqn) {
		return belowAt(Fqn.fromString(fqn)) ;
	}

	
	public static Predicate<PredicateArgument> page(final Page page) {
		return new Predicate<PredicateArgument>(){
			
			private int skip = -1 ;
			@Override
			public boolean apply(PredicateArgument arg) {
				skip++ ;
				return skip >= page.getStartLoc() && skip < page.getEndLoc() ;
			}
		} ;
	}
}
