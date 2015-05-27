package net.ion.craken.node;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ion.craken.listener.CDDMListener;
import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.lucene.queryparser.classic.ParseException;

public abstract class AbstractWriteSession implements WriteSession {

	private ReadSession rsession;
	private IndexWriteConfig iwconfig = new IndexWriteConfig();

	private Set<TouchedRow> logRows = ListOrderedSet.decorate(ListUtil.newList());
	private String tranId;

	private Set<Fqn> ancestorsFqn = SetUtil.newSet();
	private Mode mode = Mode.NORMAL;
	private Map<String, Object> attrs = MapUtil.newMap() ;
	
	private enum Mode {
		NORMAL, RESTORE, OVERWRITE
	}

	protected AbstractWriteSession(ReadSession rsession, Workspace workspace) {
		this.rsession = rsession;
	}

	public WriteNode createBy(String fqn) {
		return createBy(Fqn.fromString(fqn));
	}

	public WriteNode createBy(Fqn fqn) {
		return workspace().createNode(this, ancestorsFqn, fqn);
	}

	public WriteNode resetBy(String fqn) {
		return resetBy(Fqn.fromString(fqn));
	}

	public WriteNode resetBy(Fqn fqn) {
		return workspace().resetNode(this, ancestorsFqn, fqn);
	}

	public WriteNode pathBy(String fqn) {
		return pathBy(Fqn.fromString(fqn));
	}

	public WriteNode pathBy(String fqn0, Object... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/')));
	}

	public WriteNode pathBy(Fqn fqn) {
		return workspace().writeNode(this, this.ancestorsFqn, fqn);
	}

	public WriteNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return workspace().exists(Fqn.fromString(fqn));
	}

	public boolean exists(Fqn fqn) {
		return workspace().exists(fqn);
	}

	@Override
	public void prepareCommit() throws IOException {
	}

	public void restore() {
		this.mode = Mode.RESTORE;
	}

	public void restoreOverwrite() {
		this.mode = Mode.OVERWRITE;
	}
	
	public WriteSession attribute(Class clz, Object value){
		attrs.put(clz.getCanonicalName(),  value) ;
		return this ;
	}
	
	public <T> T attribute(Class<T> clz){
		return clz.cast(attrs.get(clz.getCanonicalName())) ;
	}
	
	
	public List<TouchedRow> touched(Touch touch){
		List<TouchedRow> result = ListUtil.newList() ;
		for (TouchedRow row : logRows) {
			if (row.touch() == touch){
				result.add(row);
			} 
		}
		return result ;
	}
	
	
	public Set<TouchedRow> logRows(){
		return logRows ;
	}

	@Override
	public void endCommit() throws IOException {
		CDDMListener cddm = attribute(CDDMListener.class) ;
		TransactionJob tjob = attribute(TransactionJob.class) ;
		TranExceptionHandler ehandler = attribute(TranExceptionHandler.class) ;
		
		NodeWriter logWriter = rsession.workspace().createLogWriter(this, rsession);
		cddm.fireRow(this, tjob, ehandler);
		
		logWriter.writeLog(logRows) ;
		logRows = ListOrderedSet.decorate(ListUtil.newList());
	}

	@Override
	public void failRollback() {

	}

	@Override
	public void notifyTouch(WriteNode source, Fqn targetFqn, Touch touch, Map<String, Fqn> affected) {
		if ((touch == Touch.TOUCH) || (targetFqn.isRoot() && touch == Touch.TOUCH))
			return;
		
		TouchedRow trow = TouchedRow.create(source, touch, targetFqn, affected);
		logRows.add(trow);
	}

	public IndexWriteConfig iwconfig() {
		return iwconfig;
	}

	@Override
	public Credential credential() {
		return rsession.credential();
	}

	public ReadSession readSession() {
		return rsession;
	}

	public Workspace workspace() {
		return rsession.workspace();
	}

	public void continueUnit() throws IOException {
		workspace().continueUnit(this);
	}

	public WriteSession iwconfig(IndexWriteConfig iwconfig) {
		this.iwconfig = iwconfig;
		return this;
	}

	@Override
	public ChildQueryRequest queryRequest(String query) throws IOException, ParseException {
		return root().childQuery(query, true);
	}

}
