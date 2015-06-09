package net.ion.craken.node.crud.util;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.tree.Fqn;

public interface ResponsePredicate {

	public boolean apply(ReadSession session, Fqn thatFqn) ;

	public boolean isContinue();

}
