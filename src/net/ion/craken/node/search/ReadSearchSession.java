package net.ion.craken.node.search;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.AbstractReadSession;
import net.ion.craken.node.Credential;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

public class ReadSearchSession extends AbstractReadSession {

	private final Central central ;
	private Future<Void> lastCommand;
	
	ReadSearchSession(Credential credential, Workspace workspace, Central central) {
		super(credential, workspace) ;
		this.central = central ;
	}

	@Override
	public <T> T tranSync(TransactionJob<T> tjob) throws Exception {
		WriteSession tsession = new WriteSearchSession(this, workspace(), central);
		return workspace().tran(tsession, tjob, TranExceptionHandler.NULL).get() ;
	}

	@Override
	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSearchSession wsession = new WriteSearchSession(this, workspace(), central);
		return workspace().tran(wsession, tjob, handler) ;
	}

	public SearchNodeRequest createRequest(String query) throws ParseException, IOException {
		return createRequest(query, credential().analyzer()) ;
	}

	public SearchNodeRequest createRequest(Query query) throws ParseException, IOException {
		return SearchNodeRequest.create(this, central.newSearcher(), query);
	}

	public SearchNodeRequest createRequest(String query, Analyzer analyzer) throws ParseException, IOException {
		if (StringUtil.isBlank(query)){
			return createRequest(new MatchAllDocsQuery()) ;
		}
		
		return SearchNodeRequest.create(this, central.newSearcher(), central.searchConfig().parseQuery(analyzer, query));
	}
	
	
	
	
	
	public ReadSearchSession awaitIndex() throws InterruptedException, ExecutionException {
		if (lastCommand != null) lastCommand.get() ;
		
		return this;
	}
	
	public <T> T getIndexInfo(IndexInfoHandler<T> indexInfo) {
		return indexInfo.handle(this, central.newReader());
	}


	
	public Future<AtomicInteger> reIndex(final ReadNode top) {
		return central.newIndexer().asyncIndex(new IndexJob<AtomicInteger>() {
			private AtomicInteger aint = new AtomicInteger() ;
			@Override
			public AtomicInteger handle(IndexSession is) throws Exception {
				index(is, top) ;
				return aint ;
			}
			
			private void index(IndexSession is, ReadNode node) throws IOException{
				IteratorList<ReadNode> iter = node.children();
				while(iter.hasNext()){
					index(is, iter.next()) ;
				}
				is.updateDocument(makeDocument(node));
				aint.incrementAndGet() ;
			}
			
			private WriteDocument makeDocument(ReadNode node){
				Fqn fqn = node.fqn() ;
				WriteDocument doc = MyDocument.newDocument(fqn.toString());
				doc.keyword(NodeCommon.NameProp, fqn.getLastElementAsString());
				for (PropertyId nodeKey : node.keys()) {
					doc.unknown(nodeKey.getString(), node.property(nodeKey.getString()).value());
				}
				
				return doc ;
			}
		});
	}

	public void asyncIndex(IndexJob<Void> indexJob) {
		this.lastCommand = central.newIndexer().asyncIndex(indexJob);
	}





}
