package net.ion.craken.node;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;

import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.schedule.IExecutor;

public interface Repository {

	public void start() ;
	
	public void shutdown()  ;

	public IExecutor executor() ;
	
	public ReadSession login(String wsname) throws IOException  ;

	public ReadSession login(String wsname, Analyzer analyzer) throws IOException  ;

	public ReadSession login(Credential credential, String wsname, Analyzer analyzer) throws IOException  ;
	
	public <T> T getAttribute(String key, Class<T> clz) ;
	
	public RepositoryImpl putAttribute(String key, Object value) ;
}
