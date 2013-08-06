package net.ion.craken.node.crud.util;

import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;

import com.google.common.base.Predicate;

public interface ResponsePredicate {

	public boolean apply(ReadSession session, Fqn thatFqn) ;

	public boolean isContinue();

}
