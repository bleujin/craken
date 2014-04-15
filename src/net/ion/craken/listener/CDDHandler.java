package net.ion.craken.listener;

import java.util.Map;

import net.ion.craken.node.TransactionJob;

public interface CDDHandler {

	public String pathPattern() ;
	public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) ;
	public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) ;
}