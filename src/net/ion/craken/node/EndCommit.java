package net.ion.craken.node;

import net.ion.craken.io.Metadata;

public class EndCommit{
	private String tranId ;
	private Metadata metadata ;
	
	public EndCommit(String tranId, Metadata metadata){
		this.tranId = tranId ;
		this.metadata = metadata ;
	}

	public String tranId() {
		return tranId;
	}
	
	public Metadata metadata(){
		return metadata ;
	}
	
}