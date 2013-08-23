package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.AbstractReadSession;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;


public class ReadSessionImpl extends AbstractReadSession{

	private Analyzer queryAnalyzer ;
	
	public ReadSessionImpl(Credential credential, Workspace workspace, Analyzer queryAnalyzer) {
		super(credential, workspace) ;
		this.queryAnalyzer = queryAnalyzer ;
	}

	@Override
	public ReadSessionImpl awaitListener() throws InterruptedException, ExecutionException {
		
		return this;
	}

	@Override
	public Searcher newSearcher() throws IOException {
		return central().newSearcher();
	}


	@Override
	public <T> T indexInfo(IndexInfoHandler<T> indexInfo) {
		return indexInfo.handle(this, central().newReader());
	}

	
	public Future<AtomicInteger> reIndex(final ReadNode top) {
		throw new UnsupportedOperationException() ;
		
//		return central().newIndexer().asyncIndex(new IndexJob<AtomicInteger>() {
//			private AtomicInteger aint = new AtomicInteger() ;
//			@Override
//			public AtomicInteger handle(IndexSession is) throws Exception {
//				index(is, top) ;
//				return aint ;
//			}
//			
//			private void index(IndexSession is, ReadNode node) throws IOException{
//				IteratorList<ReadNode> iter = node.children();
//				while(iter.hasNext()){
//					index(is, iter.next()) ;
//				}
//				is.updateDocument(makeDocument(node));
//				aint.incrementAndGet() ;
//			}
//			
//			private WriteDocument makeDocument(ReadNode node){
//				Fqn fqn = node.fqn() ;
//				WriteDocument doc = MyDocument.newDocument(fqn.toString());
//				doc.keyword(NodeCommon.NameProp, fqn.getLastElementAsString());
//				for (PropertyId nodeKey : node.keys()) {
//					doc.unknown(nodeKey.getString(), node.property(nodeKey.getString()).value());
//				}
//				
//				return doc ;
//			}
//		});
	}

	public Central central() {
		return workspace().central();
	}

	
	@Override
	public ChildQueryRequest queryRequest(String query) throws IOException, ParseException {
		return root().childQuery(query, true);
	}

	@Override
	public Analyzer queryAnalyzer() {
		return queryAnalyzer;
	}

	@Override
	public ReadSession queryAnayzler(Analyzer analyzer) {
		this.queryAnalyzer = analyzer ;
		return this;
	}
}
