package net.ion.nsearcher.index;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.config.IndexConfig;
import net.ion.nsearcher.exception.IndexException;
import net.ion.nsearcher.search.SingleSearcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;

public class Indexer implements Closeable{

	private Central central;
	private IndexConfig iconfig;
	private SingleSearcher searcher ;
	
	private IndexExceptionHandler<?> ehandler = IndexExceptionHandler.DEFAULT ;
	private IndexWriter iwriter;
	
	private Indexer(CentralConfig config, IndexConfig iconfig, Central central, SingleSearcher searcher) throws IOException {
		this.central = central;
		this.iconfig = iconfig ;
		this.searcher = searcher ;
		
//		this.iwriter = new IndexWriter(searcher.central().dir(), searcher.central().indexConfig().newIndexWriterConfig(iconfig.indexAnalyzer()));
	}
	
	public Analyzer analyzer() {
		return this.iconfig.indexAnalyzer();
	}

	
	public static Indexer create(CentralConfig config, IndexConfig iconfig, Central central, SingleSearcher searcher) throws IOException {
		return new Indexer(config, iconfig, central, searcher);
	}

	public <T> T index(IndexJob<T> indexJob) {
		return index(iconfig.indexAnalyzer(), indexJob) ;
	}
	
	public <T> T index(Analyzer analyzer, final IndexJob<T> indexJob) {
		return index("emanon", analyzer, indexJob) ;
	}

	public <T> T index(String name, Analyzer analyzer, IndexJob<T> indexJob) {
		try {
			return asyncIndex(name, analyzer, indexJob).get() ;
		} catch (InterruptedException e) {
			ehandler.onException(indexJob, e) ;
		} catch (ExecutionException e) {
			ehandler.onException(indexJob, e) ;
		}
		return null ;
	}

	
	public <T> T index(Analyzer analyzer, IndexJob<T> indexJob, IndexExceptionHandler<T> handler) {
		try {
			return asyncIndex("emanon", analyzer, indexJob).get() ;
		} catch (InterruptedException e) {
			return handler.onException(indexJob, e) ;
		} catch (ExecutionException e) {
			return handler.onException(indexJob, e) ;
		}
	}

	
	public Indexer onExceptionHander(IndexExceptionHandler<Void> ehandler){
		this.ehandler = ehandler ;
		return this ;
	}

	public <T> Future<T> asyncIndex(IndexJob<T> indexJob) {
		return asyncIndex(central.indexConfig().indexAnalyzer(), indexJob) ;
	}

	public <T> Future<T> asyncIndex(final Analyzer analyzer, IndexJob<T> indexJob) {
		return asyncIndex("emanon", analyzer, indexJob) ;
	}

	public <T> Future<T> asyncIndex(String name, IndexJob<T> indexJob) {
		return asyncIndex(name, iconfig.indexAnalyzer(), indexJob);
	}

	public <T> Future<T> asyncIndex(final String name, final Analyzer analyzer, final IndexJob<T> indexJob) {
		return asyncIndex(name, analyzer, indexJob, ehandler) ;
	}
	
	private IndexWriter makeIndexWriter() throws IOException{
		return new IndexWriter(searcher.central().dir(), searcher.central().indexConfig().newIndexWriterConfig(iconfig.indexAnalyzer()));
	}
	
	private synchronized  IndexWriter indexWriter() throws IOException{
		if (iwriter == null){
			this.iwriter = new IndexWriter(searcher.central().dir(), searcher.central().indexConfig().newIndexWriterConfig(iconfig.indexAnalyzer()));
		}
		return iwriter ;
	}
	
	public <T> Future<T> asyncIndex(final String name, final Analyzer analyzer, final IndexJob<T> indexJob, final IndexExceptionHandler handler) {
		
		return iconfig.indexExecutor().submit(new Callable<T>(){
			public T call() throws Exception {
				IndexSession session = null ;
				Lock lock = central.writeLock() ;
				try {
					lock.lock();
					session = IndexSession.create(searcher, analyzer, Indexer.this.indexWriter());
					session.begin(name) ;
					T result = indexJob.handle(session);
					
					session.commit() ;
					return result;
				} catch(Throwable ex) {
					if (session != null) session.rollback();
					handler.onException(indexJob, ex) ;
//					return null ;
					throw new IndexException(ex.getMessage(), ex) ;
				} finally {
					session.end() ;
					lock.unlock();
				}
			}
		}) ;
	}

	public void close() {
		iconfig.indexExecutor().shutdown() ;
	}



	
}
