package net.ion.rosetta.node;

public class RotateExpression extends Expression {

	private int angle ;
	public RotateExpression(Integer angle) {
		this.angle = angle ;
	}

	
	@Override
	public boolean equals(Object obj){
		if (! (obj instanceof RotateExpression)) return false ;
		RotateExpression that = (RotateExpression) obj ;
		return this.angle == that.angle ;
	}
	
	@Override
	public int hashCode(){
		return angle ;
	}
}
