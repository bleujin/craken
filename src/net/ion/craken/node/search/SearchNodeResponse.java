package net.ion.craken.node.search;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.search.util.PredicateArgument;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.rosetta.Parser;

import org.apache.ecs.xml.XML;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class SearchNodeResponse {

	private SearchResponse response;
	private List<Fqn> found = ListUtil.newList();
	private ReadSearchSession session ;
	
//	private final ColumnParser cparser ;
	
	public SearchNodeResponse(ReadSearchSession session, SearchResponse response) {
		this.session = session ;
		this.response = response ;
		for (ReadDocument doc : response.getDocument()){
			found.add(Fqn.fromString(doc.reserved(IKeywordField.ISKey))) ;
		};
//		this.cparser = session.workspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class) ;
	}

	public static SearchNodeResponse create(ReadSearchSession session, SearchResponse response) {
		return new SearchNodeResponse(session, response);
	}

	public ReadNode first() {
		return found.size() == 0 ? null : toList().get(0);
	}
	
	public List<ReadNode> toList(){
		List<ReadNode> list = ListUtil.newList() ;
		for (Fqn fqn : found) {
			list.add(session.pathBy(fqn)) ;
		}
		return list ;
	}

	public int size() {
		return response.getDocument().size();
	}

	public void debugPrint() throws IOException {
		for (Fqn fqn : found) {
			Debug.line(session.pathBy(fqn)) ;
		}
	}

	public int totalCount() {
		return response.totalCount();
	}
	public long elapsedTime() {
		return response.elapsedTime();
	}
	
	public long startTime() {
		return response.startTime();
	}

	public XML toXML() {
		return response.toXML() ;
	}

	public void awaitPostFuture() throws InterruptedException, ExecutionException {
		response.awaitPostFuture() ;
	}

	public PredicatedResponse predicated(Predicate<PredicateArgument> predicate) {
		List<PredicateArgument> result = ListUtil.newList() ;
		for (Fqn fqn : found) {
			final PredicateArgument arg = PredicateArgument.create(session, fqn);
			if (predicate.apply(arg)) result.add(arg) ;
		} 
		
		return PredicatedResponse.create(predicate, result);
	}

	public <T> T transformer(Function<SearchNodeResponse, T> function) {
		return function.apply(this) ;
	}

	public Rows toRows(String expr) throws SQLException {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, expr);
		return AdNodeRows.create(session, iterator(), sp);
		
//		return CrakenNodeRows.create(session, iterator(), cparser.parse(cols));
	}

	public IteratorList<ReadNode> iterator() {
		final Iterator<Fqn> iter = found.iterator();
		return new IteratorList<ReadNode>() {
			@Override
			public List<ReadNode> toList() {
				List<ReadNode> result = ListUtil.newList() ;
				while(iter.hasNext()) {
					result.add(session.pathBy(iter.next())) ;
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ReadNode next() {
				return session.pathBy(iter.next());
			}
		};
	}

}
