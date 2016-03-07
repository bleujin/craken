package net.ion.craken.node.crud;

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
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.util.ResponsePredicate;
import net.ion.framework.db.Rows;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.rosetta.Parser;

import org.apache.ecs.xml.XML;

import com.google.common.base.Function;

public class ChildQueryResponse {

	private SearchResponse response;
	private List<Fqn> found ;
	private ReadSession session ;
	private static Parser<SelectProjection> parser = ExpressionParser.selectProjection();

//	private final ColumnParser cparser ;
	
	public ChildQueryResponse(ReadSession session, SearchResponse response) {
		this.session = session ;
		this.response = response ;
//		this.cparser = session.workspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class) ;
	}

	
	private List<Fqn> found() {
		if (found == null){
			found = ListUtil.newList() ;
			try {
				
				for (ReadDocument doc : response.getDocument()){
					found.add(Fqn.fromString(doc.reserved(IKeywordField.DocKey))) ;
				};
			} catch(IOException ex){
				throw new IllegalStateException(ex); 
			}
		}
		return found ;
	}
	
	
	public static ChildQueryResponse create(ReadSession session, SearchResponse response) {
		return new ChildQueryResponse(session, response);
	}

	public ReadNode first() {
		return found().size() == 0 ? null : toList().get(0);
	}
	
	public List<ReadNode> toList(){
		List<ReadNode> list = ListUtil.newList() ;
		for (Fqn fqn : found()) {
			list.add(session.pathBy(fqn)) ;
		}
		return list ;
	}
	
	public List<Fqn> toFqns(){
		return found() ;
	}

	public int size() {
//		return response.totalCount() ;
		return found().size();
	}

	public void debugPrint() throws IOException {
		for (Fqn fqn : found()) {
			session.credential().tracer().println(session.pathBy(fqn)) ;
		}
	}

	public void debugPrint(String prop) {
		for (Fqn fqn : found()) {
			session.credential().tracer().println(session.pathBy(fqn) + " " + session.pathBy(fqn).property(prop)) ;
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

	public PredicatedResponse predicated(ResponsePredicate rp) {
		List<Fqn> result = ListUtil.newList() ;
		for (Fqn fqn : found()) {
			if (! rp.isContinue()) break ;
			if (rp.apply(session, fqn)) result.add(fqn) ;
		} 
		
		return PredicatedResponse.create(rp, session, result);
	}

	public <T> T transformer(Function<ChildQueryResponse, T> function) {
		return function.apply(this) ;
	}

	
	public Rows toRows(String expr) throws SQLException {
		return AdNodeRows.create(session, iterator(), expr);
		
//		return CrakenNodeRows.create(session, iterator(), cparser.parse(cols));
	}

	public <T> T each(Function<IteratorList<ReadNode>, T> function){
		return function.apply(iterator()) ;
	}

	
	public <T> T eachFqn(Function<IteratorList<Fqn>, T> function){
		return function.apply(iteratorFqn()) ;
	}

	public IteratorList<Fqn> iteratorFqn(){
		final List<Fqn> fqns = found();
		final Iterator<Fqn> iter = fqns.iterator() ;
		return new IteratorList<Fqn>(){
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public Fqn next() {
				return iter.next();
			}

			@Override
			public Iterator<Fqn> iterator() {
				return this;
			}

			@Override
			public List<Fqn> toList() {
				return fqns;
			}

			@Override
			public int count() {
				return fqns.size();
			}
		} ;
	}
	
	public IteratorList<ReadNode> iterator() {
		final List<Fqn> fqns = found();
		final Iterator<Fqn> iter = fqns.iterator();
		return new IteratorList<ReadNode>() {
			@Override
			public List<ReadNode> toList() {
				List<ReadNode> result = ListUtil.newList() ;
				for(Fqn fqn : fqns) {
					result.add(session.ghostBy(fqn)) ;
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ReadNode next() {
				return session.ghostBy(iter.next());
			}

			@Override
			public Iterator<ReadNode> iterator() {
				return this;
			}
			
			public int count() {
				return fqns.size() ;
			}
		};
	}


	public SearchRequest request() {
		return response.request() ;
	}


	
	public WalkRefChildren walkRefChildren(final String refName) {
		 return eachFqn(new Function<IteratorList<Fqn>, WalkRefChildren>() {
				@Override
				public WalkRefChildren apply(IteratorList<Fqn> iter) {
					return new WalkRefChildren(session, Fqn.ROOT, refName, iter.toList().iterator()) ;
				}
		});
	}



}
