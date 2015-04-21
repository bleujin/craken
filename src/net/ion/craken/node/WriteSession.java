package net.ion.craken.node;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.ion.craken.listener.CDDMListener;
import net.ion.craken.node.Workspace.InstantLogWriter;
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
	
	public WriteNode pathBy(String fqn0, Object... fqns)  ;

	public WriteNode createBy(String fqn);

//	public WriteNode logBy(String tranId);
	
	public void failRollback();

	public void endCommit() throws IOException;

	public Credential credential() ;

	public Workspace workspace() ;

	public void notifyTouch(WriteNode source, Fqn fqn, Touch touch, Map<String, Fqn> affected);

	public void continueUnit() throws IOException;

//	public WriteSession ignoreIndex(String... fields);
//
//	public PropertyId idInfoTo(PropertyId pid) ;

	public ChildQueryRequest queryRequest(String string) throws IOException, ParseException;
	
	public ReadSession readSession() ;

	public void prepareCommit() throws IOException;

	public WriteSession iwconfig(IndexWriteConfig wconfig);

	public IndexWriteConfig iwconfig();

	public WriteSession attribute(Class clz, Object value) ;

	public <T> T attribute(Class<T> clz) ;

	@Deprecated
	public List<TouchedRow> touched(Touch touch);
	

}
