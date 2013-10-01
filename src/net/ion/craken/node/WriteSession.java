package net.ion.craken.node;

import java.io.IOException;

import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyValue;

import org.apache.lucene.queryparser.classic.ParseException;




public interface WriteSession extends ISession<WriteNode> {
	
	public WriteSession tranId(String myid) ;
	
	public String tranId() ;
	
	public PropertyValue idValue() ;
	
	public WriteNode resetBy(String fqn);
	
	public WriteNode pathBy(String fqn0, String... fqns)  ;

	public WriteNode createBy(String fqn);

	public void failRollback();

	public void endCommit() throws IOException;
	
	public Credential credential() ;

	public Workspace workspace() ;

	public void notifyTouch(WriteNode source, Fqn fqn, Touch touch);

	public void continueUnit() throws IOException;

	public IndexWriteConfig fieldIndexConfig() ;
	
//	public WriteSession ignoreIndex(String... fields);
//
//	public PropertyId idInfoTo(PropertyId pid) ;

	public ChildQueryRequest queryRequest(String string) throws IOException, ParseException;
	
	public ReadSession readSession() ;

	public void prepare() throws IOException;

	public WriteSession fieldIndexConfig(IndexWriteConfig wconfig);
}
