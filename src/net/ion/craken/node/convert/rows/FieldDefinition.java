package net.ion.craken.node.convert.rows;

import net.ion.craken.expression.Expression;
import net.ion.craken.expression.Projection;
import net.ion.craken.node.NodeCommonMap;
import net.ion.craken.node.ReadNode;

public class FieldDefinition {

	private String fieldName;
	private FieldContext fcontext;
	private FieldRender frender;

	public FieldDefinition(String fieldName, FieldRender frender){
		this.fieldName = fieldName ;
		this.frender = frender ;
	}

	public FieldDefinition fieldContext(FieldContext fcontext){
		this.fcontext = fcontext ;
		return this ;
	}
	
	public Projection createProjection() {
		return new Projection(new Expression(){
			@Override
			public Comparable value(NodeCommonMap node) {
				return (Comparable) frender.render(fcontext, (ReadNode)node) ;
			}
		}, fieldName);
	}
}
