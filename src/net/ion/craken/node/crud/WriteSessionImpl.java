package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionLog;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.ObjectId;


public class WriteSessionImpl extends  AbstractWriteSession {

	private String tranId ;
	private PropertyValue idValue ;
	public WriteSessionImpl(ReadSession session, Workspace workspace) {
		super(session, workspace) ;
		final String idString = new ObjectId().toString();
		this.tranId = TransactionLog.newTranId(idString) ;
		this.idValue = PropertyValue.createPrimitive(idString) ;
	}

	public PropertyValue idValue(){
		return idValue ;
	}
	
	public WriteSession tranId(String myid){
		this.tranId = TransactionLog.newTranId(myid) ;
		return this ;
	}
	
	public String tranId(){
		return tranId ;
	}
	
}
