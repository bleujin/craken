package net.ion.craken.node.search;

import java.util.Map;
import java.util.Stack;

import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;

public class WriteSearchSession extends AbstractWriteSession {

	private Central central ;
	private ReadSearchSession readSession ;
	private Stack<TouchEvent> events = new Stack<TouchEvent>() ;
	
	public WriteSearchSession(ReadSearchSession readSession, Workspace workspace, Central central) {
		super(readSession, workspace) ;
		this.readSession = readSession ;
		this.central = central ;
	}

	
	@Override
	public void endCommit() {
		readSession.asyncIndex(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				TouchEvent[] eventArray = events.toArray(new TouchEvent[0]);
				ReadSession readSession = WriteSearchSession.this.readSession() ;
				for (TouchEvent event : eventArray) {
					if (event.touch() == Touch.MODIFY){
						WriteDocument doc = MyDocument.newDocument(event.fqn().toString());
						doc.keyword(NodeCommon.NameProp, event.fqn().getLastElementAsString());
						if (! readSession.exists(event.fqn())) continue ;
						Map<PropertyId, PropertyValue> values = readSession.pathBy(event.fqn()).toMap();
						for (PropertyId key : values.keySet()) {
							if (key.type() == PType.REFER) {
								doc.keyword("@" + key.getString(), values.get(key).stringValue()) ;  
							} else {
								doc.unknown(key.getString(), values.get(key).value());
							}
						}
						isession.updateDocument(doc);
					} else if (event.touch() == Touch.REMOVE){
						isession.deleteTerm(new Term(IKeywordField.ISKey, event.fqn().toString())) ;
					} else {
						isession.deleteQuery(new WildcardQuery(new Term(IKeywordField.ISKey, event.fqn().toString() + "/*"))) ;
					}
				}
				events.clear() ;
				
				return null;
			}
		});
	}
	
	@Override
	public void failRollback() {
		events.clear() ;
	}

	@Override
	public void notifyTouch(Fqn fqn, Touch touch) {
		TouchEvent newEvent = new TouchEvent(fqn, touch) ;
		synchronized (events) {
			if (events.isEmpty()) events.add(newEvent) ;
			else if (! newEvent.equals(events.peek())){
				events.add(newEvent) ;
			}
		}
	}
}

class TouchEvent {
	
	private Fqn fqn ;
	private Touch touch ;
	public TouchEvent(Fqn fqn, Touch touch){
		this.fqn = fqn ;
		this.touch = touch ;
	}
	
	Fqn fqn(){
		return fqn ;
	}
	
	Touch touch(){
		return touch ;
	}
	
	@Override 
	public boolean equals(Object obj){
		if (! (obj instanceof TouchEvent)) return false ;
		TouchEvent that = (TouchEvent) obj ;
		return this.fqn.equals(that.fqn) && this.touch == that.touch ;
	}
	
	@Override
	public int hashCode(){
		return fqn.hashCode() + touch.hashCode() ;
	}
	
	public String toString(){
		return "TouchEvent[fqn=" + fqn + ", touch:" + touch + "]" ;
	}
}
