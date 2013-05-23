package net.ion.rosetta.node;

public class TranslateExpression extends Expression {

	private int xpos ;
	private int ypos ;
	public TranslateExpression(int xpos,  int ypos) {
		this.xpos = xpos ;
		this.ypos = ypos ;
	}

	@Override
	public boolean equals(Object obj){
		if (! (obj instanceof TranslateExpression)) return false ;
		TranslateExpression that = (TranslateExpression) obj ;
		return this.xpos == that.xpos && this.ypos == that.ypos ;
	}
	
	@Override
	public int hashCode(){
		return xpos * 13 + ypos  ;
	}
	
	public String toString(){
		return "[TranslateExpression(" + xpos + ", " + ypos + ")]" ;
		
	}
}
