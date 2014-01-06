package net.ion.rosetta.node;

public class RectExpression  extends Expression{

	private int topx ;
	private int topy ;
	private int width ;
	private int heigth ;
	public RectExpression(int topx, int topy, int width, int height) {
		this.topx = topx ;
		this.topy = topy ;
		this.width = width ;
		this.heigth = height ;
	}
	

	@Override
	public boolean equals(Object obj){
		if (! (obj instanceof RectExpression)) return false ;
		RectExpression that = (RectExpression) obj ;
		return this.topx == that.topx && this.topy == that.topy && this.width == that.width && this.heigth == that.heigth ;
	}
	
	@Override
	public int hashCode(){
		return topx * 13 + topy  ;
	}
	

}
