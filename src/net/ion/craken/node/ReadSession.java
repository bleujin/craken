package net.ion.craken.node;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.node.search.ReadSearchSession;
import net.ion.craken.tree.Fqn;

import com.google.common.base.Function;


public interface ReadSession extends ISession<ReadNode> {

	public ReadNode pathBy(String fqn0, String... fqns);

	public ReadNode pathBy(String fqn, boolean emptyIfNotExist);

	public ReadNode pathBy(Fqn fqn, boolean emptyIfNotExist);

	public boolean exists(Fqn fqn);

	public <T> Future<T> tran(TransactionJob<T> tjob);

	public <T> Future<T> tran(TransactionJob<T> tjob, TranExceptionHandler handler);

	public <T> T tranSync(TransactionJob<T> tjob) throws Exception;

	public Workspace workspace();

	public <Ri, Rv, V> Future<V> mapReduce(NodeMapReduce<Ri, Rv> mapper, Function<Map<Ri, Rv>, V> function);

	public <Ri, Rv> Map<Ri, Rv> mapReduceSync(NodeMapReduce<Ri, Rv> mapper) throws InterruptedException, ExecutionException;

	public ReadSession awaitIndex() throws InterruptedException, ExecutionException ;
}
