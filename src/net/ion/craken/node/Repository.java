package net.ion.craken.node;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.schedule.IExecutor;

import org.apache.lucene.analysis.Analyzer;

public interface Repository {

	public RepositoryImpl start() throws IOException, InterruptedException ;
	
	public RepositoryImpl shutdown()  ;

	public IExecutor executor() ;
	
	public ReadSession login(String wsname) throws IOException  ;

	public ReadSession login(String wsname, Analyzer analyzer) throws IOException  ;

	public ReadSession login(Credential credential, String wsname, Analyzer analyzer) throws IOException  ;
	
	public <T> T getAttribute(String key, Class<T> clz) ;
	
	public Repository putAttribute(String key, Object value) ;
	
	public String selfName() ;

	public List<String> memberNames();
	
	public Logger logger() ;

//	public Central central(String wsName);

//	public RepositoryListener listener();
}
