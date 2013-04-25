package net.ion.craken.node.search;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.ecs.xml.XML;
import org.neo4j.helpers.Predicates;

import com.google.common.base.Predicate;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.search.util.PredicateArgument;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.search.SearchResponse;

public class SearchNodeResponse {

	private SearchResponse response;
	private List<Fqn> found = ListUtil.newList();
	private ReadSearchSession session ;
	public SearchNodeResponse(ReadSearchSession session, SearchResponse response) {
		this.session = session ;
		this.response = response ;
		for (MyDocument doc : response.getDocument()){
			found.add(Fqn.fromString(doc.get(IKeywordField.ISKey))) ;
		};
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
		for (ReadNode node : toList()) {
			Debug.line(node) ;
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

}
