package net.ion.craken.node;

import java.io.IOException;
import java.util.List;

import net.ion.craken.node.TransactionLog.PropId;
import net.ion.nsearcher.config.Central;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TermQuery;

public class TranLogManager {

	private ReadSession session;
	private Central central;
	private static TermQuery LogTypeQuery = new TermQuery(new Term("__type", "commitlog"));

	private TranLogManager(ReadSession session, Workspace workspace) {
		this.session = session ;
		this.central = workspace.central();
	}

	public static TranLogManager create(ReadSession session, Workspace workspace) {
		return new TranLogManager(session, workspace);
	}


	public void debugPrint() throws IOException {
		try {
			central.newSearcher().createRequest(LogTypeQuery).find().debugPrint();
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	public List<ReadNode> recentTran(long minTranTime) throws IOException, ParseException{
		return  session.ghostBy("/__transactions").childQuery("time:[" + minTranTime + " TO " + Long.MAX_VALUE + "]").find().toList() ;
	}

	public Long lastTranInfoBy() throws IOException, ParseException{
		ReadNode lastLog = session.ghostBy("/__transactions").childQuery("").descending("time").findOne();
		return (lastLog == null) ? 0L : lastLog.propertyId(PropId.TIME).longValue(0) ; 
	}
}
