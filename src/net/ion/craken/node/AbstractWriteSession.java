package net.ion.craken.node;

import java.io.IOException;
import java.util.Set;

import net.ion.craken.node.Workspace.InstantLogWriter;
import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.lucene.queryparser.classic.ParseException;

public abstract class AbstractWriteSession implements WriteSession {

	private ReadSession rsession;
	private IndexWriteConfig iwconfig = new IndexWriteConfig();

	private Set<LogRow> logRows = ListOrderedSet.decorate(ListUtil.newList());
	private String tranId;

	private Set<Fqn> ancestorsFqn = SetUtil.newSet();
	private Mode mode = Mode.NORMAL;

	private enum Mode {
		NORMAL, RESTORE, OVERWRITE
	}

	protected AbstractWriteSession(ReadSession rsession, Workspace workspace) {
		this.rsession = rsession;
	}

	public WriteNode createBy(String fqn) {
		return createBy(Fqn.fromString(fqn)) ;
	}

	public WriteNode createBy(Fqn fqn) {
		return workspace().createNode(this, ancestorsFqn, fqn);
	}

	public WriteNode resetBy(String fqn) {
		return resetBy(Fqn.fromString(fqn)) ;
	}

	public WriteNode resetBy(Fqn fqn) {
		return workspace().resetNode(this, ancestorsFqn, fqn);
	}

	public WriteNode pathBy(String fqn) {
		return pathBy(Fqn.fromString(fqn));
	}

	public WriteNode pathBy(String fqn0, String... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/')));
	}

	public WriteNode pathBy(Fqn fqn) {
		return workspace().writeNode(this, this.ancestorsFqn, fqn) ;
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

	@Override
	public void endCommit() throws IOException {
		InstantLogWriter logWriter = rsession.workspace().createLogWriter(this, rsession);

//		for (Fqn parentFqn : ancestorsFqn) { // create parent node
//			workspace().pathNode(parentFqn, true);
//			logRows.add(LogRow.create(pathBy(parentFqn), Touch.MODIFY, parentFqn));
//		}

		
//		Debug.debug(logRows.size(), logRows) ;
		
		logWriter.beginLog(logRows);
		for (LogRow row : logRows) {
			logWriter.writeLog(row);
		}
		logWriter.endLog();
		logRows.clear();

		//		
		//		
		// if (this.mode != Mode.NORMAL) { // restore mode
		// // workspace().getCache().cache().clear() ;
		// return ;
		// }
		//		
		// logWriter.beginLog(this) ; // user will define tranId & config after..
		//		
		//		
		// for (Fqn parentFqn : ancestorsFqn) { // create parent node
		// workspace().pathNode(parentFqn, true) ;
		// logRows.add(LogRow.create(Touch.MODIFY, parentFqn, pathBy(parentFqn))) ;
		// }
		//
		// for (LogRow row : logRows) {
		// row.saveLog(this) ;
		// }
		//
		// logWriter.endLog() ;
	}

	@Override
	public void failRollback() {

	}

	@Override
	public void notifyTouch(WriteNode source, Fqn targetFqn, Touch touch) {
		if ((touch == Touch.TOUCH) || (targetFqn.isRoot() && touch == Touch.TOUCH))
			return;
		logRows.add(LogRow.create(source, touch, targetFqn));
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

	static class LogRow {

		private WriteNode source;
		private Touch touch;
		private Fqn target;

		LogRow(WriteNode source, Touch touch, Fqn target) {
			this.source = source;
			this.touch = touch;
			this.target = target;
		}

		final static LogRow create(WriteNode source, Touch touch, Fqn target) {
			return new LogRow(source, touch, target);
		}

		public Touch touch() {
			return touch;
		}

		public Fqn target() {
			return target;
		}

		public WriteNode source() {
			return source;
		}

		@Override
		public boolean equals(Object obj) {
			if (!LogRow.class.isInstance(obj))
				return false;

			LogRow that = (LogRow) obj;
			return this.touch == that.touch && this.target.equals(that.target);
		}

		@Override
		public int hashCode() {
			return target.hashCode() + touch.ordinal();
		}

		public String toString() {
			return target + ", " + touch;
		}
	}

}
