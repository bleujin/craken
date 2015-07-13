package net.ion.craken.node.convert.rows;

import net.ion.craken.node.ReadNode;

public abstract class FieldRender<T> {

	public abstract T render(FieldContext fcontext, ReadNode current) ; 
}
