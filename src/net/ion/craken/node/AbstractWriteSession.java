package net.ion.craken.node;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Set;

import net.ion.craken.io.WritableGridBlob;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.lucene.queryparser.classic.ParseException;

public abstract class AbstractWriteSession implements WriteSession{

	private ReadSession readSession ;
	private Workspace workspace ;
	private IndexWriteConfig iwconfig = new IndexWriteConfig() ;
	
	private Set<LogRow> logRows = ListOrderedSet.decorate(ListUtil.newList()) ;
	private Set<Fqn> ancestorsFqn = SetUtil.newSet() ;
	private LogWriter logWriter = new LogWriter() ;
	
	private Mode mode = Mode.NORMAL ;
	private enum Mode {
		NORMAL, RESTORE, OVERWRITE
	}
	
	protected AbstractWriteSession(ReadSession readSession, Workspace workspace){
		this.readSession = readSession ;
		this.workspace = workspace ;
	}
	
	
	public WriteNode createBy(String fqn){
		final Fqn self = forCreateAncestor(fqn);
		return WriteNodeImpl.loadTo(this, workspace.createNode(iwconfig, self)) ;
	}
	
	public WriteNode resetBy(String fqn){
		final Fqn self = forCreateAncestor(fqn);
		return WriteNodeImpl.loadTo(this, workspace.resetNode(iwconfig, self)) ;
	}

	private Fqn forCreateAncestor(String fqn) {
		
		final Fqn self = Fqn.fromString(fqn);
		Fqn parent = self.getParent() ;
		while(true) {
			if (parent.isRoot() || parent.isSystem()) break ;
			ancestorsFqn.add(parent) ;
			parent = parent.getParent() ;
		}
		return self;
	}
	
	public WriteNode pathBy(String fqn) {
		return pathBy(Fqn.fromString(fqn)) ;
	}

	public WriteNode pathBy(String fqn0, String... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/'))) ;
	}

	public WriteNode pathBy(Fqn fqn) {

		Fqn parent = fqn.getParent() ;
		while(true) {
			if (parent.isRoot() || parent.isSystem()) break ;
			ancestorsFqn.add(parent) ;
			parent = parent.getParent() ;
		}
		
		return WriteNodeImpl.loadTo(this, workspace.pathNode(this.iwconfig, fqn));
	}
	
	
//	public WriteNode logBy(String tranId){
//		return WriteNodeImpl.loadTo(this, workspace.logNode(this.iwconfig, Fqn.fromString(tranId))) ;
//	}
	
	
	public WriteNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return workspace.exists(Fqn.fromString(fqn)) ;
	}

	private String tranFqn() {
		return this.tranId();
	}
	
	@Override
	public void prepare() throws IOException{
	}
	


	public void restore() {
		this.mode = Mode.RESTORE ;
	}

	public void restoreOverwrite() {
		this.mode = Mode.OVERWRITE ;
	}
	

	
	@Override
	public void endCommit() throws IOException {
		
		if (this.mode != Mode.NORMAL) { // restore mode
			workspace().getCache().cache().clear() ;
			return ;
		}
		
		logWriter.beginLog(this) ; // user will define tranId & config after..
		
		
		for (Fqn parentFqn : ancestorsFqn) { // create parent node
			workspace.pathNode(this.iwconfig, parentFqn) ;
			logRows.add(LogRow.create(Touch.MODIFY, parentFqn, pathBy(parentFqn))) ;
		}

		for (LogRow row : logRows) {
			row.saveLog(this) ;
		}

		logWriter.endLog() ;
	}
	
	@Override
	public void failRollback() {
		
	}

	
	@Override
	public void notifyTouch(WriteNode source, Fqn targetFqn, Touch touch) {
		if (targetFqn.isSystem()) return ; 
		
		source.refTo("__transaction", tranFqn()) ;
		logRows.add(LogRow.create(touch, targetFqn, source)) ;
	}
	
	@Override
	public Credential credential() {
		return readSession.credential();
	}
	
	@Override
	public Workspace workspace() {
		return workspace;
	}
	
	public ReadSession readSession(){
		return readSession ;
	}
	
	public void continueUnit() throws IOException{
		workspace().continueUnit(this) ;
	}
	
	public IndexWriteConfig fieldIndexConfig() {
		return iwconfig ;
	}
	
	public WriteSession fieldIndexConfig(IndexWriteConfig iwconfig){
		this.iwconfig = iwconfig ;
		return this ;
	}
	
	@Override
	public ChildQueryRequest queryRequest(String query) throws IOException, ParseException {
		return root().childQuery(query, true);
	}
	
	
	static class LogRow {
		
		private Touch touch;
		private Fqn target;
		private JsonObject nodeValue ;
		LogRow(Touch touch, Fqn target, JsonObject nodeValue){
			this.touch = touch ;
			this.target = target ;
			this.nodeValue = nodeValue ;
		}
		
		void saveLog(AbstractWriteSession wsession) throws IOException {
			
			String oid = new ObjectId().toString();
			if (nodeValue == null) {
				this.nodeValue = wsession.pathBy(target).transformer(Functions.<WriteNode>toJsonExpression());
			}
			
			wsession.logWriter.writeLog(oid, target, touch, nodeValue) ;
		}

		final static LogRow create(Touch touch, Fqn target, WriteNode source){
			
			return new LogRow(touch, target, (touch == Touch.MODIFY) ? null : source.transformer(Functions.<WriteNode>toJsonExpression())) ;
		}
		
		@Override
		public boolean equals(Object obj){
			LogRow that = (LogRow) obj ;
			return this.touch == that.touch && this.target.equals(that.target) ;
		}
		
		@Override
		public int hashCode(){
			return target.hashCode() + touch.ordinal() ;
		}
		
		public String toString(){
			return target + ", " + touch ; 
		}
		
	}
	
	static class LogWriter {

		private JsonWriter jwriter ;
		private WriteNode logNode;
		private WritableGridBlob wlobs ;
		
		public LogWriter beginLog(AbstractWriteSession wsession) throws IOException {
			
			final long thisTime = System.currentTimeMillis();
			this.logNode = wsession.createBy(wsession.tranId());
			this.wlobs = logNode.property("config", wsession.iwconfig.toJson().toString()).property("time", thisTime).blob("tran");
			Writer swriter = new BufferedWriter(new OutputStreamWriter(wlobs.outputStream(), Charset.forName("UTF-8")));
			
			jwriter = new JsonWriter(swriter) ;
			jwriter.beginObject() ;
			jwriter.jsonElement("config", wsession.iwconfig.toJson()) ;
			jwriter.name("time").value(thisTime) ;
			
			jwriter.name("logs") ;
			jwriter.beginArray() ;
			
			return this ;
		}
		
		public void writeLog(String oid, Fqn target, Touch touch, JsonObject nodeValue) throws IOException {
			jwriter.beginObject() ;
			jwriter.name("id").value(oid).name("path").value(target.toString()).name("touch").value(touch.name()).jsonElement("val", nodeValue) ;
			
			jwriter.endObject() ;
		}

		public LogWriter endLog() throws IOException{
			jwriter.endArray() ;
			jwriter.endObject() ;
			jwriter.close() ;
			logNode.property(PropertyId.normal("tran"), wlobs.getMetadata().asPropertyValue()) ;
			return this ;
		}
	}


}


