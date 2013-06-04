package net.ion.craken.expression;

import java.util.List;

public class SelectProjection extends ValueObject {

	private final List<Projection> projections ;
	public SelectProjection(List<Projection> projections){
		this.projections = projections ;
	} 
	
}
