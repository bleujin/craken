package net.ion.craken.listener;

import java.util.Map;

import net.ion.craken.node.TransactionJob;

public abstract class CDDModifyHandler implements CDDHandler {

	private String pathPattern;
	public CDDModifyHandler(String pathPattern){
		this.pathPattern = pathPattern ;
	}
	
	public String pathPattern() {
		return pathPattern ;
	}
	public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
		return null ;
	}

}
