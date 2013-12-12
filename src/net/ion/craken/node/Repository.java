package net.ion.craken.node;

import java.io.IOException;

import net.ion.framework.schedule.IExecutor;

import org.apache.lucene.analysis.Analyzer;

public interface Repository {

	public final static String SYSTEM_CACHE = "__system" ;
	
	public void start() throws IOException ;
	
	public void shutdown()  ;

	public IExecutor executor() ;
	
	public ReadSession login(String wsname) throws IOException  ;

	public ReadSession login(String wsname, Analyzer analyzer) throws IOException  ;

	public ReadSession login(Credential credential, String wsname, Analyzer analyzer) throws IOException  ;
	
	public <T> T getAttribute(String key, Class<T> clz) ;
	
	public Repository putAttribute(String key, Object value) ;

//	public Central central(String wsName);

//	public RepositoryListener listener();
}
