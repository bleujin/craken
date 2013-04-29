package net.ion.craken.node.search;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.Credential;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.TreeNode;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

public class ReadSearchSession implements ReadSession {

	private final Credential credential ;
	private WorkspaceSearch workspace;
	private final Central central ;
	
	ReadSearchSession(Credential credential, WorkspaceSearch workspace, Central central) {
		this.credential = credential.clearSecretKey() ;
		this.workspace = workspace ;
		this.central = central ;
	}

	public ReadNode pathBy(Fqn fqn) {
		return pathBy(fqn, false) ;
	}

	public ReadNode pathBy(String fqn) {
		return pathBy(Fqn.fromString(fqn)) ;
	}

	public ReadNode pathBy(Fqn fqn, boolean createIf) {
		if (createIf || exists(fqn)) return ReadNodeImpl.load(this, workspace.getNode(fqn));
		else throw new IllegalArgumentException("not found path :" + fqn) ;
	}

	public ReadNode pathBy(String fqn, boolean createIf) {
		return pathBy(Fqn.fromString(fqn), createIf) ;
	}

	
	@Override
	public boolean exists(String fqn) {
		return workspace.exists(fqn);
	}

	@Override
	public boolean exists(Fqn fqn) {
		return workspace.exists(fqn);
	}


	@Override
	public ReadNode root() {
		return pathBy("/") ;
	}

	@Override
	public <T> Future<T> tran(TransactionJob<T> tjob) {
		return tran(tjob, TranExceptionHandler.PRINT);
	}

	public <T> T tranSync(TransactionJob<T> tjob) {
		try {
			return tran(tjob).get() ;
		} catch (InterruptedException e) {
			throw new IllegalStateException(e) ;
		} catch (ExecutionException e) {
			throw new IllegalStateException(e) ;
		}
	}

	@Override
	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler) {
		WriteSearchSession wsession = new WriteSearchSession(this, workspace, central);
		return workspace.tran(wsession, tjob, handler) ;
	}

	public SearchNodeRequest createRequest(String query) throws ParseException, IOException {
		return createRequest(query, credential.analyzer()) ;
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
	

	public Credential credential(){
		return credential ;
	}
	
	public String wsName() {
		return workspace.wsName();
	}

	@Override
	public WorkspaceSearch getWorkspace() {
		return workspace;
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
			
			private MyDocument makeDocument(ReadNode node){
				Fqn fqn = node.fqn() ;
				MyDocument doc = MyDocument.newDocument(fqn.toString());
				doc.keyword(NodeCommon.NameProp, fqn.getLastElementAsString());
				for (PropertyId nodeKey : node.keys()) {
//					doc.addUnknown(nodeKey, node.property(nodeKey.toString()));
				}
				return doc ;
			}
		});
	}



}
