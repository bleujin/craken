package net.ion.craken.listener;

import java.util.Map;

import net.ion.craken.node.TransactionJob;

public abstract class AsyncCDDModifyHandler implements AsyncCDDHandler {

	private String pathPattern;
	public AsyncCDDModifyHandler(String pathPattern){
		this.pathPattern = pathPattern ;
	}
	
	public String pathPattern() {
		return pathPattern ;
	}
	public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
		return null ;
	}

}
