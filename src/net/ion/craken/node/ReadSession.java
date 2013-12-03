package net.ion.craken.node;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.IndexInfoHandler;
import net.ion.craken.tree.Fqn;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;

import com.google.common.base.Function;


public interface ReadSession extends ISession<ReadNode> {

	public ReadNode ghostBy(String fqn0, String... fqns);

	public ReadNode ghostBy(Fqn fqn);

	public ReadNode pathBy(String fqn0, String... fqns);

	public boolean exists(Fqn fqn);

	public <T> Future<T> tran(TransactionJob<T> tjob);

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler);

	public <T> T tranSync(TransactionJob<T> tjob) throws Exception;

	public <T> T tranSync(TransactionJob<T> tjob, TranExceptionHandler handler) throws Exception;

	public Workspace workspace();

	public <Ri, Rv, V> Future<V> mapReduce(NodeMapReduce<Ri, Rv> mapper, Function<Map<Ri, Rv>, V> function);

	public <Ri, Rv> Map<Ri, Rv> mapReduceSync(NodeMapReduce<Ri, Rv> mapper) throws InterruptedException, ExecutionException;

	@Deprecated
	public ReadSession awaitListener() throws InterruptedException, ExecutionException ;

	public Searcher newSearcher() throws IOException;

	public Central central();

	public <T> T indexInfo(IndexInfoHandler<T> indexInfo);

	public ChildQueryRequest queryRequest(String string) throws IOException, ParseException;
	
	public Analyzer queryAnalyzer();
	
	public ReadSession queryAnayzler(Analyzer analyzer) ;

	public TranLogManager logManager() throws IOException;

	public void attribute(String key, Object value);
	
	public Object attribute(String key) ;

}
