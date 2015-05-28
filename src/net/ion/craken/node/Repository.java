package net.ion.craken.node;

import java.io.IOException;
import java.util.List;

import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.framework.schedule.IExecutor;

import org.apache.lucene.analysis.Analyzer;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.logging.Log;

public interface Repository {

	public Repository start() throws IOException ;
	
	public Repository shutdown()  ;

	public IExecutor executor() ;
	
	public ReadSession login(String wsname) throws IOException  ;

	public ReadSession login(String wsname, Analyzer analyzer) throws IOException  ;

	public ReadSession login(Credential credential, String wsname, Analyzer analyzer) throws IOException  ;
	
	public <T> T getAttribute(String key, Class<T> clz) ;
	
	public Repository putAttribute(String key, Object value) ;
	
	public String addressId() ;
	
	public String memberId() ;

	public List<Address> memberAddress();
	
	public Log getLogger() ;

//	public Repository defineWorkspace(String wsName) throws IOException;

//	public Repository createWorkspace(String string, WorkspaceConfigBuilder distMode) ; 

//	public Central central(String wsName);

//	public RepositoryListener listener();
}
