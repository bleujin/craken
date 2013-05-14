package net.ion.craken.node;


public class SortElement {
	private String propId ;
	private boolean ascending ;
	
	public SortElement(String propId, boolean ascending) {
		this.propId = propId ;
		this.ascending = ascending ;
	}
	
	
	public String propid(){
		return propId ;
	}
	
	public boolean ascending(){
		return ascending ;
	}
}